import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Speleologist extends Agent {

    private AID wumpusWorld;
    private AID navigationAgent;

    @Override
    protected void setup() {
        System.out.println("Speleologist: The speleologist agent " + getAID().getName() + " is ready.");

        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            protected void onTick() {
                myAgent.addBehaviour(new FindWumpusWorld());
            }
        });
    }

    @Override
    protected void takeDown() {
        System.out.println("Speleologist: The speleologist agent " + getAID().getName() + " terminating.");
    }

    private class FindWumpusWorld extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            System.out.println("Speleologist: Start finding a Wumpus world");

            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();

            sd.setType(Constants.WUMPUS_WORLD_TYPE);
            template.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);

                if (result != null && result.length > 0) {
                    wumpusWorld = result[0].getName();
                    myAgent.addBehaviour(new WumpusWorldStarting());
                } else {
                    System.out.println("Speleologist: Unfortunately, a Wumpus world hasn't been found.");
                    block();
                }

                isDone = true;
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean done() {
            return isDone;
        }
    }

    private class WumpusWorldStarting extends Behaviour {
        private MessageTemplate mt;
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    // Go inside
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    cfp.addReceiver(wumpusWorld);
                    cfp.setContent(Constants.GO_INSIDE);
                    cfp.setConversationId(Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID);
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());

                    myAgent.send(cfp);

                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId(
                                    Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID
                            ),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
                    );

                    step = 1;
                    break;
                case 1:
                    // Successfully jumped into cave
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            System.out.println("Speleologist: Found a Wumpus World!");

                            super.myAgent.addBehaviour(
                                    new TickerBehaviour(super.myAgent, 5000) {
                                        @Override
                                        protected void onTick() {
                                            myAgent.addBehaviour(new FindNavigatorAgent());
                                        }
                                    });

                            step++;
                        }
                    } else {
                        block();
                    }

                    break;
            }
        }

        @Override
        public boolean done() {
            return step == 2;
        }
    }

    private class FindNavigatorAgent extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();

            sd.setType(Constants.NAVIGATOR_AGENT_TYPE);
            template.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);

                if (result != null && result.length > 0) {
                    navigationAgent = result[0].getName();
                    myAgent.addBehaviour(new NavigatorAgentPerformer());

                    isDone = true;
                } else {
                    block();
                }
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean done() {
            return isDone;
        }
    }

    private class NavigatorAgentPerformer extends Behaviour {
        private int step = 0;
        private MessageTemplate mt;
        private ACLMessage reply;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    // Send message to navigator
                    ACLMessage inform = new ACLMessage(ACLMessage.INFORM);

                    inform.addReceiver(navigationAgent);
                    inform.setContent(Constants.ADVICE_PROPOSAL);
                    inform.setConversationId(Constants.NAVIGATOR_DIGGER_CONVERSATION_ID);
                    inform.setReplyWith("inform" + System.currentTimeMillis());

                    System.out.println("Speleologist: " + Constants.ADVICE_PROPOSAL);

                    myAgent.send(inform);

                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId(
                                    Constants.NAVIGATOR_DIGGER_CONVERSATION_ID
                            ),
                            MessageTemplate.MatchInReplyTo(inform.getReplyWith())
                    );

                    step++;
                    break;
                case 1:
                    // Receive message from navigator and ask environment
                    reply = myAgent.receive(mt);

                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.REQUEST) {
                            if (parseNavigatorMessageRequest(reply.getContent())) {
                                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);

                                req.addReceiver(wumpusWorld);
                                req.setContent(Constants.GAME_INFORMATION);
                                req.setConversationId(Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID);
                                req.setReplyWith("req" + System.currentTimeMillis());

                                myAgent.send(req);

                                mt = MessageTemplate.and(
                                        MessageTemplate.MatchConversationId(
                                                Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID
                                        ),
                                        MessageTemplate.MatchInReplyTo(req.getReplyWith())
                                );

                                step++;
                            } else
                                System.out.println("Speleologist: There is an unknown command!");
                        }
                    } else {
                        block();
                    }

                    break;
                case 2:
                    // receive from environment and ask navigator
                    reply = myAgent.receive(mt);

                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            String rep = reply.getContent();
                            ACLMessage mes = new ACLMessage(ACLMessage.INFORM);

                            mes.addReceiver(navigationAgent);
                            mes.setContent(Constants.INFORMATION_PROPOSAL_SPELEOLOGIST + rep);
                            mes.setConversationId(Constants.NAVIGATOR_DIGGER_CONVERSATION_ID);
                            mes.setReplyWith("mes" + System.currentTimeMillis());

                            System.out.println("Speleologist: " + Constants.INFORMATION_PROPOSAL_SPELEOLOGIST + rep);

                            myAgent.send(mes);
                            mt = MessageTemplate.and(
                                    MessageTemplate.MatchConversationId(
                                            Constants.NAVIGATOR_DIGGER_CONVERSATION_ID
                                    ),
                                    MessageTemplate.MatchInReplyTo(mes.getReplyWith())
                            );

                            step++;
                        }
                    } else
                        block();

                    break;
                case 3:
                    // Receive from navigator and make action
                    reply = myAgent.receive(mt);

                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            String message = reply.getContent();
                            String content = "";

                            String action = parseNavigatorMessageProposal(message);

                            switch (action) {
                                case Commands.TURN_LEFT:
                                    content = Constants.SPELEOLOGIST_TURN_LEFT;
                                    break;
                                case Commands.TURN_RIGHT:
                                    content = Constants.SPELEOLOGIST_TURN_RIGHT;
                                    break;
                                case Commands.MOVE_FORWARD:
                                    content = Constants.SPELEOLOGIST_MOVE_FORWARD;
                                    break;
                                case Commands.GRAB:
                                    content = Constants.SPELEOLOGIST_GRAB;
                                    break;
                                case Commands.SHOOT:
                                    content = Constants.SPELEOLOGIST_SHOOT;
                                    break;
                                case Commands.CLIMB:
                                    content = Constants.SPELEOLOGIST_CLIMB;
                                    break;
                                default:
                                    System.out.println("Speleologist: There is no right action!");
                                    break;
                            }

                            ACLMessage prop = new ACLMessage(ACLMessage.PROPOSE);

                            prop.addReceiver(wumpusWorld);
                            prop.setContent(content);
                            prop.setConversationId(Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID);
                            prop.setReplyWith("prop" + System.currentTimeMillis());

                            myAgent.send(prop);

                            mt = MessageTemplate.and(
                                    MessageTemplate.MatchConversationId(
                                            Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID
                                    ),
                                    MessageTemplate.MatchInReplyTo(prop.getReplyWith())
                            );
                            step++;
                        }
                    } else {
                        block();
                    }

                    break;
                case 4:
                    // Answer from env
                    reply = myAgent.receive(mt);

                    if (reply != null) {
                        String answer = reply.getContent();

                        switch (answer) {
                            case Constants.FAIL_MESSAGE:
                                System.out.println("Speleologist: You failed!");

                                step++;
                                doDelete();

                                break;
                            case Constants.OK_MESSAGE:
                                System.out.println("Speleologist: Wumpus world answers OK.");

                                step = 0;

                                break;
                            case Constants.WIN_MESSAGE:
                                System.out.println("Speleologist: The speleologist survived and won!");

                                step++;
                                doDelete();

                                break;
                        }
                    } else {
                        block();
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            return step == 5;
        }

        private String parseNavigatorMessageProposal(String instruction) {
            for (Map.Entry<Integer, String> entry : Commands.WORDS.entrySet()) {
                String value = entry.getValue();

                Pattern pattern = Pattern.compile("\\b" + value + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(instruction);

                if (matcher.find()) {
                    String res = matcher.group();
                    return res.length() > 0 ? res : "";
                }
            }
            return "";
        }

        private boolean parseNavigatorMessageRequest(String instruction) {
            String regex = "\\binformation\\b";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(instruction);

            if (matcher.find()) {
                String res = matcher.group();
                return !res.isEmpty();
            }

            return false;
        }

    }

    final static class Commands {
        static final String TURN_LEFT = "left";
        static final String TURN_RIGHT = "right";
        static final String MOVE_FORWARD = "forward";
        static final String GRAB = "grab";
        static final String SHOOT = "shoot";
        static final String CLIMB = "climb";
        static final Map<Integer, String> WORDS = new LinkedHashMap<Integer, String>() {{
            put(1, "left");
            put(2, "right");
            put(3, "forward");
            put(4, "grab");
            put(5, "shoot");
            put(6, "climb");
        }};
    }

}

