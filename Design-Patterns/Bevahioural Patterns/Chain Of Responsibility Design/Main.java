// 1. The Request Object
class SupportTicket {
    private String description;
    private int severityLevel; // 1: Low, 2: Medium, 3: High

    public SupportTicket(String description, int severityLevel) {
        this.description = description;
        this.severityLevel = severityLevel;
    }

    public String getDescription() { return description; }
    public int getSeverityLevel() { return severityLevel; }
}

// 2. The Handler Interface (Abstract Class to enforce chaining logic)
abstract class SupportHandler {
    protected SupportHandler nextHandler;

    // Sets the next element in the pipeline
    public void setNextHandler(SupportHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    // Template-style method to handle execution delegation
    public final void handleRequest(SupportTicket ticket) {
        if (canHandle(ticket)) {
            process(ticket);
        } else if (nextHandler != null) {
            System.out.println(getClass().getSimpleName() + " cannot handle. Escalating...");
            nextHandler.handleRequest(ticket);
        } else {
            System.out.println("Ticket [" + ticket.getDescription() + "] could not be resolved by any department.");
        }
    }

    protected abstract boolean canHandle(SupportTicket ticket);
    protected abstract void process(SupportTicket ticket);
}

// 3. Concrete Handler 1: Front Desk Bot
class AutomatedBotHandler extends SupportHandler {
    @Override
    protected boolean canHandle(SupportTicket ticket) {
        return ticket.getSeverityLevel() == 1;
    }

    @Override
    protected void process(SupportTicket ticket) {
        System.out.println("AutomatedBot: Resolved ticket -> " + ticket.getDescription());
    }
}

// 4. Concrete Handler 2: Level 1 Agent
class Level1AgentHandler extends SupportHandler {
    @Override
    protected boolean canHandle(SupportTicket ticket) {
        return ticket.getSeverityLevel() == 2;
    }

    @Override
    protected void process(SupportTicket ticket) {
        System.out.println("Level1Agent: Resolved ticket -> " + ticket.getDescription());
    }
}

// 5. Concrete Handler 3: Tech Lead
class TechLeadHandler extends SupportHandler {
    @Override
    protected boolean canHandle(SupportTicket ticket) {
        return ticket.getSeverityLevel() == 3;
    }

    @Override
    protected void process(SupportTicket ticket) {
        System.out.println("TechLead: Resolved critical ticket -> " + ticket.getDescription());
    }
}

// 6. Execution Driver
public class Main {
    public static void main(String[] args) {
        // Create individual chain segments
        SupportHandler bot = new AutomatedBotHandler();
        SupportHandler agent = new Level1AgentHandler();
        SupportHandler techLead = new TechLeadHandler();

        // Configure the linking order: Bot -> Agent -> Tech Lead
        bot.setNextHandler(agent);
        agent.setNextHandler(techLead);

        // Injecting distinct tickets into the head of the chain
        SupportTicket t1 = new SupportTicket("Check account balance", 1);
        SupportTicket t2 = new SupportTicket("Router configurations mismatch", 2);
        SupportTicket t3 = new SupportTicket("Database cluster server downtime", 3);

        System.out.println("--- Ticket 1 Entering Chain ---");
        bot.handleRequest(t1);

        System.out.println("\n--- Ticket 2 Entering Chain ---");
        bot.handleRequest(t2);

        System.out.println("\n--- Ticket 3 Entering Chain ---");
        bot.handleRequest(t3);
    }
}