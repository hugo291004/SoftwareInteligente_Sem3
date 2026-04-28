import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPANames;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.proto.ContractNetInitiator;

import java.util.*;

public class TaskCNPInitiator extends Agent {

    private int taskCounter = 1;

    protected void setup() {
        System.out.println(getLocalName() + " iniciado - enviando tareas...");

        // Cada 10 segundos envía una nueva tarea
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {

                List<AID> developers = searchDevelopers();

                if (developers.isEmpty()) {
                    System.out.println("No hay developers disponibles.");
                    return;
                }

                int effort = new Random().nextInt(5) + 1;
                String[] priorities = {"high", "medium", "low"};
                String priority = priorities[new Random().nextInt(priorities.length)];

                String content = "taskId=" + taskCounter +
                                 ";effort=" + effort +
                                 ";priority=" + priority;

                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                for (AID dev : developers) {
                    cfp.addReceiver(dev);
                }

                cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                cfp.setContent(content);

                System.out.println("\n Enviando tarea: " + content);

                addBehaviour(new ContractNetInitiator(myAgent, cfp) {

                    protected void handleAllResponses(Vector responses, Vector acceptances) {

                        int bestTime = Integer.MAX_VALUE;
                        ACLMessage bestProposal = null;

                        for (Object obj : responses) {
                            ACLMessage response = (ACLMessage) obj;

                            if (response.getPerformative() == ACLMessage.PROPOSE) {

                                String content = response.getContent();
                                int time = extractInt(content, "time");

                                System.out.println("📩 Propuesta de " +
                                    response.getSender().getLocalName() +
                                    " -> " + content);

                                if (time < bestTime) {
                                    bestTime = time;
                                    bestProposal = response;
                                }
                            } else if (response.getPerformative() == ACLMessage.REFUSE) {
                                System.out.println("X " +
                                    response.getSender().getLocalName() +
                                    " rechazó la tarea");
                            }
                        }

                        for (Object obj : responses) {
                            ACLMessage response = (ACLMessage) obj;
                            ACLMessage reply = response.createReply();

                            if (response.equals(bestProposal)) {
                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                System.out.println("Aceptando a " +
                                    response.getSender().getLocalName());
                            } else {
                                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            }

                            acceptances.add(reply);
                        }
                    }

                    protected void handleInform(ACLMessage inform) {
                        System.out.println("📌 " +
                            inform.getSender().getLocalName() +
                            " -> " + inform.getContent());
                    }
                });

                taskCounter++;
            }
        });
    }

    // Buscar developers en el DF
    private List<AID> searchDevelopers() {
        List<AID> result = new ArrayList<>();

        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("developer");
            template.addServices(sd);

            DFAgentDescription[] found = DFService.search(this, template);

            for (DFAgentDescription dfd : found) {
                result.add(dfd.getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // Extraer enteros del formato key=value
    private int extractInt(String content, String key) {
        try {
            for (String part : content.split(";")) {
                if (part.startsWith(key + "=")) {
                    return Integer.parseInt(part.split("=")[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}