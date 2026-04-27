import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class ProductOwnerAgent extends Agent {

    protected void setup() {
        System.out.println("Product Owner iniciado...");

        createTaskAgents();
    }

    private void createTaskAgents() {

        ContainerController cc = getContainerController();

        for (int i = 1; i <= 4; i++) {
            try {

                String agentName = "TaskAgent-" + i;

                AgentController agent = cc.createNewAgent(
                        agentName,
                        "TaskAgent",
                        null
                );

                agent.start();

                System.out.println("Creado: " + agentName);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}