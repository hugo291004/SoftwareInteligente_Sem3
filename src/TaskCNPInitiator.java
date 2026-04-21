import java.util.Vector;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

class TaskCNPInitiator extends ContractNetInitiator {

    public TaskCNPInitiator(Agent a, ACLMessage cfp) {
        super(a, cfp);
    }

    @Override
    protected void handleAllResponses(Vector responses, Vector acceptances) {

        TaskAgent agent = (TaskAgent) myAgent;

        if (responses.isEmpty()) {
            agent.incrementAttempts();
            return;
        }

        ACLMessage best = null;
        int bestTime = Integer.MAX_VALUE;

        for (Object obj : responses) {
            ACLMessage msg = (ACLMessage) obj;

            if (msg.getPerformative() == ACLMessage.PROPOSE) {

                int time = extractTime(msg.getContent());

                if (time < bestTime) {
                    bestTime = time;
                    best = msg;
                }
            }
        }

        if (best == null) {
            agent.incrementAttempts();
            return;
        }

        for (Object obj : responses) {

            ACLMessage msg = (ACLMessage) obj;
            ACLMessage reply = msg.createReply();

            if (msg.equals(best)) {
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            } else {
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
            }

            acceptances.add(reply);
        }

        ((TaskAgent) myAgent).stopTicker();

    }

    private int extractTime(String content) {
        try {
            String[] parts = content.split(";");
            for (String part : parts) {
                if (part.startsWith("time=")) {
                    return Integer.parseInt(part.split("=")[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Integer.MAX_VALUE;
    }
}