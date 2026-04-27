/*
    Para ejecutar desde la carpeta base en POWERSHELL
    Compilar los .class desde src a out
    • javac -cp ".;..\lib\jade.jar" -d ..\out *.java

    Desde la carpeta base
    • java -cp ".;lib\jade.jar;out" jade.Boot -gui ProductOwner:ProductOwnerAgent
*/

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.util.ArrayList;
import java.util.List;

public class ProductOwnerAgent extends Agent {

    // Backlog simulado
    private List<Object[]> backlog = new ArrayList<>();

    protected void setup() {
        System.out.println("Product Owner iniciado...");

        initBacklog();
        createTaskAgents();
    }

    // Inicializa tareas
    private void initBacklog() {
        backlog.add(new Object[]{1, 5, "high"});
        backlog.add(new Object[]{2, 3, "medium"});
        backlog.add(new Object[]{3, 8, "high"});
        backlog.add(new Object[]{4, 2, "low"});
        backlog.add(new Object[]{5, 4, "medium"});
    }

    private void createTaskAgents() {

        ContainerController cc = getContainerController();

        for (Object[] task : backlog) {
            try {

                int taskId = (int) task[0];
                int effort = (int) task[1];
                String priority = (String) task[2];

                String agentName = "TaskAgent-" + taskId;

                AgentController agent = cc.createNewAgent(
                        agentName,
                        "TaskAgent",
                        new Object[]{taskId, effort, priority}
                );

                agent.start();

                System.out.println(
                    "Creado: " + agentName +
                    " -> effort=" + effort +
                    ", priority=" + priority
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}