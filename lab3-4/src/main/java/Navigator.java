import com.sun.deploy.util.ArrayUtil;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Navigator extends Agent {
    @Override
    protected void setup() {
        System.out.println("Navigator: The navigator agent " + getAID().getName() + " is ready.");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType(Constants.NAVIGATOR_AGENT_TYPE);
        sd.setName(Constants.NAVIGATOR_SERVICE_DESCRIPTION);

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new LocationRequests());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Navigator: The navigator agent " + getAID().getName() + " terminating.");
    }

    private class LocationRequests extends CyclicBehaviour {
        int actionCounter = 0;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                if (parseSpeleologistMessageRequest(msg.getContent())) {
                    ACLMessage reply = msg.createReply();

                    reply.setPerformative(ACLMessage.REQUEST);
                    reply.setContent(Constants.INFORMATION_PROPOSAL_NAVIGATOR);

                    System.out.println("Navigator: " + Constants.INFORMATION_PROPOSAL_NAVIGATOR);

                    myAgent.send(reply);
                } else if (parseSpeleologistMessageProposal(msg.getContent())) {
                    ACLMessage reply = msg.createReply();

                    reply.setPerformative(ACLMessage.PROPOSE);

                    String advice = getAdvice(msg.getContent());

                    reply.setContent(advice);

                    System.out.println("Navigator: " + advice);

                    myAgent.send(reply);
                } else
                    System.out.println("Navigator: Wrong message!");
            } else {
                block();
            }
        }

        private boolean parseSpeleologistMessageRequest(String instruction) {
            String regex = "\\bAdvice\\b";
            Pattern pattern = Pattern.compile(regex);

            Matcher matcher = pattern.matcher(instruction);

            if (matcher.find()) {
                String res = matcher.group();
                return !res.isEmpty();
            }

            return false;
        }

        private boolean parseSpeleologistMessageProposal(String instruction) {
            String regex = "\\bGiving\\b";
            Pattern pattern = Pattern.compile(regex);

            Matcher matcher = pattern.matcher(instruction);

            if (matcher.find()) {
                String res = matcher.group();
                return !res.isEmpty();
            }

            return false;
        }

        private String getAdvice(String content){
            boolean stench = false;
            boolean breeze = false;
            boolean glitter = false;
            boolean scream = false;
            String actionForAdvice = "";

            for (Map.Entry<Integer, String> entry : STATES.entrySet()) {
                String value = entry.getValue();

                Pattern pattern = Pattern.compile("\\b" + value + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(content);

                if (matcher.find()) {
                    switch (value){
                        case "Stench": stench = true;
                        case "Breeze": breeze = true;
                        case "Glitter": glitter = true;
                        case "Scream": scream = true;
                    }
                }
            }

//            Plug
            boolean perception = stench || breeze || glitter || scream;
            int[] forwardSteps = {0, 2, 4, 8, 9, 11};
            int[] rightSteps = {1, 6, 7, 10};

            if (IntStream.of(forwardSteps).anyMatch(n -> n == actionCounter)) {
                actionForAdvice = Constants.MESSAGE_FORWARD;
                actionCounter++;
            }
            else if (IntStream.of(rightSteps).anyMatch(n -> n == actionCounter)) {
                actionForAdvice = Constants.MESSAGE_RIGHT;
                actionCounter++;
            }
            else if (actionCounter == 3) {
                actionForAdvice = Constants.MESSAGE_LEFT;
                actionCounter++;
            }
            else if (actionCounter == 5) {
                actionForAdvice = Constants.MESSAGE_GRAB;
                actionCounter++;
            }
            else {
                actionForAdvice = Constants.MESSAGE_CLIMB;
                actionCounter++;
            }

            int rand = 1 + (int) (Math.random() * 3);
            switch (rand) {
                case 1: return Constants.ACTION_PROPOSAL1 + actionForAdvice;
                case 2: return Constants.ACTION_PROPOSAL2 + actionForAdvice;
                case 3: return Constants.ACTION_PROPOSAL3 + actionForAdvice;
                default: return "";
            }
        }

        final Map<Integer, String> STATES = new LinkedHashMap<Integer, String>() {{
            put(1, "Stench");
            put(2, "Breeze");
            put(3, "Glitter");
            put(4, "Scream");
        }};

    }
}