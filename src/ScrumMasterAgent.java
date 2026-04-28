import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ScrumMasterAgent extends Agent {

    protected void setup() {
        System.out.println("--- [SCRUM MASTER] --- Agente de monitoreo activo.");

        // Comportamiento para recibir reportes de TaskAgents y Developers
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String sender = msg.getSender().getLocalName();
                    String content = msg.getContent();

                    // Auditoría de logs (logging centralizado)
                    System.out.println("\n[AUDITORÍA] Mensaje de: " + sender);
                    System.out.println("[CONTENIDO] " + content);

                    // Lógica de monitoreo: detección de bloqueos
                    if (content.contains("status=NO_ASSIGNMENT")) {
                        System.err.println("!!! ALERTA SCRUM MASTER: Tarea bloqueada. Ningún Developer aceptó.");
                    } else if (content.contains("status=IN_PROGRESS")) {
                        System.out.println(">>> Seguimiento: Tarea en desarrollo por " + sender);
                    }
                } else {
                    block();
                }
            }
        });

        // Comportamiento para auditar quiénes están en las páginas amarillas cada 10 seg
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                auditarDesarrolladores();
            }
        });
    }

    private void auditarDesarrolladores() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("developer");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            System.out.println("--- [ESTADO EQUIPO] Developers activos en DF: " + result.length);
            for (DFAgentDescription dev : result) {
                System.out.println("  - " + dev.getName().getLocalName() + " (Disponible)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}