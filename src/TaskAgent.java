import java.util.Date;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPANames;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class TaskAgent extends Agent {

    private static final long RETRY_INTERVAL = 5000;
    private int attempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private TickerBehaviour ticker;

    private AID scrumMasterAID = new AID("ScrumMaster", AID.ISLOCALNAME);

    protected void setup() {

        ticker = new TickerBehaviour(this, RETRY_INTERVAL) {

            protected void onTick() {

                DFAgentDescription[] developers = searchDevelopers();

                if (developers.length == 0) {
                    incrementAttempts();
                    return;
                }

                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (DFAgentDescription dev : developers) {
                    cfp.addReceiver(dev.getName());
                }
                cfp.setContent("taskId=1;effort=5;priority=high");
                cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                cfp.setReplyByDate(new Date(System.currentTimeMillis() + 3000));

                addBehaviour(new TaskCNPInitiator(myAgent, cfp));
            }
        };
        addBehaviour(ticker);
    }

    private void notifyScrumMaster(String reason) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(scrumMasterAID);
        msg.setContent("taskId=1;status=" + reason);
        send(msg);
    }

    private DFAgentDescription[] searchDevelopers() {

        DFAgentDescription template = new DFAgentDescription();

        ServiceDescription sd = new ServiceDescription();
        sd.setType("developer");
        template.addServices(sd);

        try {
            return DFService.search(this, template);
        } catch (Exception e) {
            e.printStackTrace();
            return new DFAgentDescription[0];
        }
    }

    public void incrementAttempts() {
        attempts++;
        if (attempts >= MAX_ATTEMPTS) {
            notifyScrumMaster("NO_ASSIGNMENT");
            doDelete();
        }
    }

    public void resetAttempts() {
        attempts = 0;
    }

    public void stopTicker() {
        if (ticker != null) {
            ticker.stop();
        }
    }

}