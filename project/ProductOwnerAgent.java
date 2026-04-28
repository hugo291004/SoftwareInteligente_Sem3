import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

public class ProductOwnerAgent extends Agent {

    private volatile boolean isProcessing = false;

    protected void setup() {
        System.out.println(getLocalName() + " iniciado.");

        Object[] args = getArguments();

        SequentialBehaviour seq = new SequentialBehaviour();

        if (args != null && args.length > 0) {
            String csvPath = (String) args[0];

            seq.addSubBehaviour(new OneShotBehaviour() {
                public void action() {
                    processCSV(csvPath);
                }
            });
        }

        seq.addSubBehaviour(new OneShotBehaviour() {
            public void action() {
                startCLI();
            }
        });

        addBehaviour(seq);
    }


    private void startCLI() {
        new Thread(() -> {
            //Esperar la entrada de nuevas rutas csv por procesar
            Scanner sc = new Scanner(System.in);

            System.out.println("Ingrese rutas de CSV:");

            while (true) {
                try {
                    String input = sc.nextLine().trim();

                    if (input.isEmpty()) continue;

                    if (!isProcessing) {
                        isProcessing = true;
                        addBehaviour(new OneShotBehaviour() {
                            public void action() {
                                processCSV(input);
                                isProcessing = false;
                                System.out.println("Ingrese rutas de CSV:");
                            }
                        });
                    } else {
                        System.out.println("Aun se estan procesando tareas...");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void processCSV(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                Task task = parseTask(line);

                if (task != null) {
                    createTaskAgent(task);
                }
            }

        } catch (Exception e) {
            System.out.println("Error leyendo CSV: " + filePath);
            e.printStackTrace();
        }
    }


    private Task parseTask(String line) {
        String[] parts = line.split(",");

        if (parts.length != 3) return null;

        return new Task(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim()
        );
    }


    private void createTaskAgent(Task task) {
        try {
            String agentName = "Task-" + System.currentTimeMillis();

            Object[] args = new Object[]{task.toString()};

            AgentContainer container = getContainerController();
            getContainerController()
                    .createNewAgent(agentName, "TaskAgent", args)
                    .start();

            System.out.println("Creado: " + agentName + " → " + task);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
