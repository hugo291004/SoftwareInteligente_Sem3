import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.core.behaviours.TickerBehaviour;

import java.util.*;

public class TaskAgent extends Agent {

    private boolean completed = false;
    private AID scrumMasterAID;
    private Task task;
    private Map<AID, Integer> proposals = new HashMap<>();
    private String conversationId;

    protected void setup() {
        System.out.println(getLocalName() + " iniciado.");

        Object[] args = getArguments();

        if (args != null && args.length > 0) {
            String taskData = (String) args[0];
            task = Task.fromString(taskData);
        }

        conversationId = "cfp-" + System.currentTimeMillis();

        addBehaviour(new RetryBehaviour(this, 10000));
        addBehaviour(new ProposalHandler());
        
    }

    private class RetryBehaviour extends TickerBehaviour {

        public RetryBehaviour(Agent a, long period) {
            super(a, period);
        }

        protected void onTick() {

            if (completed) {
                stop();
                return;
            }

            System.out.println(task.name + ": reintentando asignacion...");

            proposals.clear(); 
            requestDevelopers();
        }
    }

    //Buscar servicio de scrum master
    private AID findScrumMaster() {
        try {
            DFAgentDescription template = new DFAgentDescription();

            ServiceDescription sd = new ServiceDescription();
            sd.setType("scrum-master");

            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(this, template);

            if (result.length > 0) {
                return result[0].getName();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //Solicitar propuestas de developers
    private void requestDevelopers() {
        scrumMasterAID = findScrumMaster();

        if (scrumMasterAID == null) {
            System.out.println(task.name + ": No se encontro ScrumMaster");
            return;
        }

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

        msg.addReceiver(scrumMasterAID);
        msg.setConversationId(conversationId);
        msg.setContent(task.toString());
        send(msg);

        System.out.println(task.name + ": REQUEST enviado a ScrumMaster");
    }

    //Esperar por respuesta de los developers
    private class ProposalHandler extends CyclicBehaviour {

        public void action() {

            MessageTemplate mt = MessageTemplate.MatchConversationId(conversationId);
            ACLMessage msg = receive(mt);

            if (msg != null) {

                switch (msg.getPerformative()) {

                    case ACLMessage.INFORM:
                        handleProposals(msg);
                        break;

                    case ACLMessage.FAILURE:
                        System.out.println(task.name + ": fallo en ScrumMaster");
                        break;
                }

            } else {
                block();
            }
        }
    }

    //Recepcionar respuestas
    private void handleProposals(ACLMessage msg) {
        System.out.println(task.name + ": propuestas recibidas");

        // Formato esperado:
        // Dev1=3;Dev2=5
        String content = msg.getContent();

        String[] entries = content.split(";");

        for (String entry : entries) {
            String[] parts = entry.split("=");

            String devName = parts[0];
            int time = Integer.parseInt(parts[1]);

            proposals.put(new AID(devName, AID.ISLOCALNAME), time);
        }

        decideWinner();
    }


    private void decideWinner() {
        AID bestDev = null;
        int bestTime = Integer.MAX_VALUE;

        for (Map.Entry<AID, Integer> entry : proposals.entrySet()) {
            if (entry.getValue() < bestTime) {
                bestTime = entry.getValue();
                bestDev = entry.getKey();
            }
        }

        if (bestDev != null) {
            System.out.println(task.name + ": ganador → " + bestDev.getLocalName());
            sendAcceptance(bestDev);
            completed = true;
            doDelete(); 
        } else {
            System.out.println(task.name + ": nadie aceptó la tarea");
        }
    }


    private void sendAcceptance(AID winner) {
        ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);

        if (scrumMasterAID != null) {
            msg.addReceiver(scrumMasterAID);
        } else {
            System.out.println(task.name + ": ScrumMaster no disponible al enviar aceptacion");
            return;
        }
      
        msg.setConversationId(conversationId);
        msg.setContent("winner=" + winner.getLocalName());
        send(msg);

        System.out.println(task.name+ ": aceptacion enviada");
    }
}
