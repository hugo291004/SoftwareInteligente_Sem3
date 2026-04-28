import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Random;

public class DeveloperAgent extends Agent {

    private int maxCapacity = 2; 
    private int currentTasks = 0;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " iniciado con capacidad maxima de " + maxCapacity + " tareas.");

        addBehaviour(new CFPHandler());
        addBehaviour(new AcceptanceHandler());
    }

    // Recibir propuestar para realizar nuevas tareas
    private class CFPHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = receive(mt);

            if (msg != null) {
                String taskData = msg.getContent();
                System.out.println(getLocalName() + ": Evaluando tarea " + taskData);

                if (currentTasks < maxCapacity) {
                    int estimatedTime = calculateEstimatedTime(taskData);
                    
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(estimatedTime));
                    send(reply);
                    
                    System.out.println(getLocalName() + ": Propone " + estimatedTime + "s para la tarea.");
                } else {

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Capacidad llena");
                    send(reply);
                    
                    System.out.println(getLocalName() + ": Falta de capacidad para hacer mas taraeas.");
                }
            } else {
                block();
            }
        }
    }

    //Recibir confirmarcion para empezar el trabajo
    private class AcceptanceHandler extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = receive(mt);

            if (msg != null && currentTasks < maxCapacity) {
                currentTasks++;
                
                int workDuration = (int) (Math.random() * 20 + 20);
                
                System.out.println(getLocalName() + ": Aceptado. Trabajando por " + workDuration + " segundos... (Capacidad: " + currentTasks + "/" + maxCapacity + ")");

                addBehaviour(new WakerBehaviour(myAgent, workDuration * 1000L) {
                    protected void onWake() {
                        currentTasks--;
                        System.out.println(getLocalName() + ": Tarea finalizada. Liberando espacio. (Capacidad: " + currentTasks + "/" + maxCapacity + ")");
                    }
                });
            } else {
                block();
            }
        }
    }

    //Estimar de manera aleatorio el tiempo aproximado en levantar la tarea
    private int calculateEstimatedTime(String taskData) {

        Random random = new Random();

        if (taskData.contains("priority=hight")) {
            return random.nextInt(10) + 30;  
        } else if (taskData.contains("priority=medium")) {
            return random.nextInt(10) + 50;  
        } else {
            return random.nextInt(10) + 70; 
        }
    }
}
