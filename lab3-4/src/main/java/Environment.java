import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class Environment extends Agent {

    private boolean isWumpusAlive = true;
    private boolean isGoldGrabbed;
    private boolean hasArrow = true;

    private Cave cave;
    private AgentPosition agentPosition;

    public Environment(Cave cave) {
        this.cave = cave;
    }

    public Environment() {
        this(new Cave(4, 4, Constants.INITIAL_WUMPUS_CAVE));
    }

    public Cave getCave() {
        return cave;
    }

    public boolean isWumpusAlive() {
        return isWumpusAlive;
    }

    public boolean isGoalGrabbed() {
        return isGoldGrabbed;
    }

    public AgentPosition getAgentPosition() {
        return agentPosition;
    }

    @Override
    protected void setup() {
        System.out.println("Wumpus world: The Wumpus world agent " + getAID().getName() + " is ready.");
        System.out.println("Wumpus world: Current world state:");
        System.out.println(cave);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Constants.WUMPUS_WORLD_TYPE);
        sd.setName(Constants.WUMPUS_SERVICE_DESCRIPTION);
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new SpeleologistConnect());
        addBehaviour(new EnvironmentInformation());
        addBehaviour(new ProcessAction());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Wumpus world: The Wumpus world agent " + getAID().getName() + " terminating.");
    }

    private class SpeleologistConnect extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String message = msg.getContent();
                if (Objects.equals(message, Constants.GO_INSIDE)) {
                    agentPosition = cave.getStart();

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent(Constants.OK_MESSAGE);

                    myAgent.send(reply);
                }

            } else {
                block();
            }
        }
    }

    private class EnvironmentInformation extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String message = msg.getContent();

                if (message.equals(Constants.GAME_INFORMATION)) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);

                    Perception current = getPerception();
                    String perception = current.toString();

                    reply.setContent(perception);
                    myAgent.send(reply);
                }

            } else {
                block();
            }
        }
    }

    private class ProcessAction extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String message = msg.getContent();
                System.out.println("Wumpus world: Current world state before the action:");
                System.out.println(cave);

                boolean sendTerminateMessage = false;
                boolean sendWinMessage = false;

                switch (message){
                    case Constants.SPELEOLOGIST_TURN_LEFT: turnLeft(); break;
                    case Constants.SPELEOLOGIST_TURN_RIGHT: turnRight(); break;
                    case Constants.SPELEOLOGIST_MOVE_FORWARD: sendTerminateMessage = moveForward(); break;
                    case Constants.SPELEOLOGIST_GRAB: grab();  break;
                    case Constants.SPELEOLOGIST_SHOOT: shoot(); break;
                    case Constants.SPELEOLOGIST_CLIMB:
                        if (climb()) sendWinMessage = true;
                        else sendTerminateMessage = true;
                        break;
                    default: System.out.println("Wumpus world: Wrong action!"); break;
                }

                System.out.println("Wumpus world: Current world state after the action:");
                System.out.println(cave);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);

                if (sendWinMessage) {
                    reset();
                    reply.setContent(Constants.WIN_MESSAGE);
                } else if (!sendTerminateMessage) {
                    reply.setContent(Constants.OK_MESSAGE);
                } else {
                    reset();
                    reply.setContent(Constants.FAIL_MESSAGE);
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private void turnLeft() {
        agentPosition = cave.turnLeft(agentPosition);
    }

    private void turnRight() {
        agentPosition = cave.turnRight(agentPosition);
    }

    private boolean moveForward() {
        agentPosition = cave.moveForward(agentPosition);
        return (isWumpusAlive && cave.getWumpus().equals(agentPosition.getRoom())) ||
                cave.isPit(agentPosition.getRoom());
    }

    private void grab() {
        if (cave.getGold().equals(agentPosition.getRoom())) isGoldGrabbed = true;
    }

    private void shoot() {
        if (hasArrow && isAgentFacingWumpus(agentPosition)) isWumpusAlive = false;
    }

    private boolean climb() {
        return agentPosition.getRoom().equals(new Cell(1, 1)) && isGoldGrabbed;
    }

    private boolean isAgentFacingWumpus(AgentPosition pos) {
        Cell wumpus = cave.getWumpus();

        switch (pos.getOrientation()) {
            case FACING_NORTH:
                return pos.getX() == wumpus.getX() && pos.getY() < wumpus.getY();
            case FACING_SOUTH:
                return pos.getX() == wumpus.getX() && pos.getY() > wumpus.getY();
            case FACING_EAST:
                return pos.getY() == wumpus.getY() && pos.getX() < wumpus.getX();
            case FACING_WEST:
                return pos.getY() == wumpus.getY() && pos.getX() > wumpus.getX();
        }

        return false;
    }

    public Perception getPerception() {
        Perception result = new Perception();
        AgentPosition pos = agentPosition;

        List<Cell> adjacentCellsForThisStep = Arrays.asList(
                new Cell(pos.getX()-1, pos.getY()),
                new Cell(pos.getX()+1, pos.getY()),
                new Cell(pos.getX(), pos.getY()-1),
                new Cell(pos.getX(), pos.getY()+1)
        );

        List<Cell> adjacentCellsForThisAndNextStep = new LinkedList<>();

        for (Cell cell : adjacentCellsForThisStep) {
            adjacentCellsForThisAndNextStep.addAll(Arrays.asList(
                    new Cell(cell.getX()-1, cell.getY()),
                    new Cell(cell.getX()+1, cell.getY()),
                    new Cell(cell.getX(), cell.getY()-1),
                    new Cell(cell.getX(), cell.getY()+1))
            );
        }

        adjacentCellsForThisAndNextStep.addAll(adjacentCellsForThisStep);

        for (Cell cell: adjacentCellsForThisAndNextStep) {
            if (cell.equals(cave.getWumpus())) result.setStench();
            if (cave.isPit(cell)) result.setBreeze();
        }

        if (pos.getRoom().equals(cave.getGold())) result.setGlitter();
        if (!isWumpusAlive) result.setScream();

        return result;
    }

    private void reset() {
        cave = new Cave(4, 4, Constants.INITIAL_WUMPUS_CAVE);
        agentPosition = cave.getStart();
        isWumpusAlive = true;
        isGoldGrabbed = false;
        hasArrow = true;
    }
}