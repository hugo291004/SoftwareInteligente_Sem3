import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;

import java.util.*;

public class ScrumMasterAgent extends Agent {

    private String currentTask;
    private String conversationId;
    private AID taskAgent;

    private Map<String, Integer> proposals = new HashMap<>();

    private List<AID> developers = new ArrayList<>();

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " scrum master iniciado.");

	      // Cambiar por las dirección de la máquina virtual 2
        AID dev1 = new AID("Dev1@192.168.18.173:1099/JADE", AID.ISGUID);
        dev1.addAddresses("http://192.168.18.173:7778/acc"); 

        AID dev2 = new AID("Dev2@192.168.18.173:1099/JADE", AID.ISGUID);
        dev2.addAddresses("http://192.168.18.173:7778/acc");

        developers.add(dev1);
        developers.add(dev2);

        registerService();

        addBehaviour(new RequestHandler());
        addBehaviour(new ProposalHandler());
        addBehaviour(new AcceptanceHandler());
    }

    private void registerService() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("scrum-master"); 
            sd.setName("scrum-service");
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println(getLocalName() + ": registrado en DF");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Scrum master recepciona nuevas tareas y se encarga de comunicarlas al equipo
    private class RequestHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);

            if (msg != null) {
                currentTask = msg.getContent();
                conversationId = msg.getConversationId();
                taskAgent = msg.getSender();
                proposals.clear();

                System.out.println("SCRUM: Task recibida → " + currentTask);
                sendCFPToDevelopers();

                myAgent.addBehaviour(new WakerBehaviour(myAgent, 2000) {
                    protected void onWake() {
                        sendProposalsToTaskAgent();
                    }
                });

            } else {
                block();
            }
        }
    }

    //Envio de tareas pendientes a todos los developers disponibles
    private void sendCFPToDevelopers() {
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (AID dev : developers) {
            cfp.addReceiver(dev);
        }
        cfp.setConversationId(conversationId);
        cfp.setContent(currentTask);
        send(cfp);
        System.out.println("SCRUM: CFP enviado a developers remotos");
    }

    //Recepcion de propuestas de cada developer disponible
    private class ProposalHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchConversationId(conversationId)
            );
            ACLMessage msg = receive(mt);

            if (msg != null) {
                int time = Integer.parseInt(msg.getContent());
                String devName = msg.getSender().getLocalName();
                proposals.put(devName, time);
                System.out.println("SCRUM: propuesta recibida → " + devName + " = " + time + "s");
            } else {
                block();
            }
        }
    }

    //Envio de propuestas a las tareas
    private void sendProposalsToTaskAgent() {
        if (proposals.isEmpty()) {
            System.out.println("SCRUM: Ningun developer mando propuesta. Ignorando.");
            return;
        }

        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, Integer> e : proposals.entrySet()) {
            if (content.length() > 0) content.append(";");
            content.append(e.getKey()).append("=").append(e.getValue());
        }

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(taskAgent);
        msg.setConversationId(conversationId);
        msg.setContent(content.toString());

        send(msg);
        System.out.println("SCRUM: Opciones enviadas al TaskAgent → " + content);
    }

    //Recepcionar mensaje de confirmacion de tarea seleccionada
    private class AcceptanceHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchConversationId(conversationId)
            );
            ACLMessage msg = receive(mt);

            if (msg != null) {
                System.out.println("SCRUM: TaskAgent eligio un ganador → " + msg.getContent());

                String[] parts = msg.getContent().split("=");
                if (parts.length == 2 && parts[0].equals("winner")) {
                    String winnerName = parts[1];
                    notifyWinnerDeveloper(winnerName);
                }
            } else {
                block();
            }
        }
    }
  
    //Informar sobre asignacion de tarea al developer
    private void notifyWinnerDeveloper(String winnerLocalName) {
        AID winnerAID = null;
        for (AID dev : developers) {
            if (dev.getLocalName().equals(winnerLocalName)) {
                winnerAID = dev;
                break;
            }
        }

        if (winnerAID != null) {
            ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            msg.addReceiver(winnerAID);
            msg.setConversationId(conversationId);
            msg.setContent("Adjudicado");
            send(msg);
            System.out.println("SCRUM: Asignacion notificada al developer remoto → " + winnerLocalName);
        } else {
            System.out.println("SCRUM: Error, no se encontro el AID remoto para " + winnerLocalName);
        }
    }
}
