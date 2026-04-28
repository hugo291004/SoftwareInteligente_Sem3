/*
    Para ejecutar desde la carpeta agent_developer en POWERSHELL
    Compilar los .class desde agent_developer a out
    • javac -cp ".;..\lib\jade.jar" -d ..\out *.java

    Desde la carpeta base
    • java -cp ".;lib\jade.jar;out" jade.Boot -gui Developer1:DeveloperAgent
*/

import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPANames;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.proto.ContractNetResponder;

public class DeveloperAgent extends Agent {

    // Carga de trabajo actual: número de tareas en ejecución simultánea
    private int currentLoad = 0;

    // Máximo de tareas simultáneas que el agente puede manejar
    private static final int MAX_LOAD = 2;

    protected void setup() {
        System.out.println(getLocalName() + " iniciado - listo para recibir tareas.");

        // Registrar este agente en el DF como proveedor del servicio "developer"
        registerInDF();

        // Plantilla para capturar solo mensajes CFP del protocolo Contract Net
        MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
            MessageTemplate.MatchPerformative(ACLMessage.CFP)
        );

        // Comportamiento responder: gestiona todo el ciclo del protocolo Contract Net
        addBehaviour(new ContractNetResponder(this, mt) {

            // Evalúa la oferta de trabajo y decide si propone o rechaza
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) {

                String content  = cfp.getContent();
                int effort      = extractInt(content, "effort");
                String priority = extractString(content, "priority");

                System.out.println(getLocalName() + " | CFP recibido: " + content);
                System.out.println(getLocalName() + " | Carga actual: " + currentLoad + "/" + MAX_LOAD);

                ACLMessage reply = cfp.createReply();

                // Si está sobrecargado, rechaza la tarea sin calcular propuesta
                if (currentLoad >= MAX_LOAD) {
                    System.out.println(getLocalName() + " | REFUSE - agente sobrecargado");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("reason=overloaded");
                    return reply;
                }

                // Calcula tiempo estimado según esfuerzo, prioridad y carga actual
                int estimatedTime = calculateEstimatedTime(effort, priority);

                System.out.println(getLocalName() + " | PROPOSE - tiempo estimado: " + estimatedTime + "s");
                reply.setPerformative(ACLMessage.PROPOSE);
                // El contenido incluye "time=" para que TaskCNPInitiator pueda comparar propuestas
                reply.setContent("time=" + estimatedTime + ";load=" + currentLoad);
                return reply;
            }

            // Maneja la aceptación: incrementa carga y simula ejecución de la tarea
            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {

                String content = cfp.getContent();
                int taskId = extractInt(content, "taskId");
                int effort = extractInt(content, "effort");

                // Actualiza disponibilidad interna al aceptar la asignación
                currentLoad++;
                System.out.println(getLocalName() + " | ACCEPTED tarea " + taskId +
                    " | Carga: " + currentLoad + "/" + MAX_LOAD);

                // Simula el tiempo de desarrollo: libera la carga tras effort*1000 ms
                long simulationMs = effort * 1000L;
                addBehaviour(new WakerBehaviour(myAgent, simulationMs) {
                    protected void onWake() {
                        // Libera la carga al terminar la simulación de desarrollo
                        currentLoad--;
                        System.out.println(getLocalName() + " | Tarea " + taskId +
                            " completada | Carga: " + currentLoad + "/" + MAX_LOAD);
                    }
                });

                // Informa al iniciador que la tarea fue tomada y está en progreso
                ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                inform.setContent("taskId=" + taskId + ";status=IN_PROGRESS");
                return inform;
            }

            // Maneja el rechazo: otro developer fue seleccionado, no hay acción requerida
            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                System.out.println(getLocalName() + " | Propuesta rechazada para: " + cfp.getContent());
            }
        });
    }

    // Registra el agente en el DF con tipo de servicio "developer" para ser descubierto
    private void registerInDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("developer");
            sd.setName(getLocalName() + "-service");
            dfd.addServices(sd);

            DFService.register(this, dfd);
            System.out.println(getLocalName() + " | Registrado en DF como 'developer'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Calcula el tiempo estimado en segundos: mayor carga o menor prioridad = más tiempo
    private int calculateEstimatedTime(int effort, String priority) {
        int priorityFactor;
        switch (priority.toLowerCase()) {
            case "high":   priorityFactor = 1; break; // Alta prioridad: propone tiempo mínimo
            case "medium": priorityFactor = 2; break;
            case "low":    priorityFactor = 3; break;
            default:       priorityFactor = 2; break;
        }
        // A mayor carga actual, el tiempo estimado crece linealmente
        return effort * priorityFactor * (1 + currentLoad);
    }

    // Extrae un valor entero de un contenido con formato "key=value;key=value"
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

    // Extrae un valor string de un contenido con formato "key=value;key=value"
    private String extractString(String content, String key) {
        try {
            for (String part : content.split(";")) {
                if (part.startsWith(key + "=")) {
                    return part.split("=")[1];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "medium"; // valor por defecto si no se encuentra la clave
    }

    // Desregistra el agente del DF cuando es eliminado para liberar el registro
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println(getLocalName() + " | Desregistrado del DF.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
