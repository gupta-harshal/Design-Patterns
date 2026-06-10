import java.util.ArrayList;
import java.util.List;

// 1. The Mediator Interface
interface ChatMediator {
    void sendMessage(String msg, User user);
    void addUser(User user);
}

// 2. The Abstract Colleague (Entities that communicate through the mediator)
abstract class User {
    protected ChatMediator mediator;
    protected String name;

    public User(ChatMediator mediator, String name) {
        this.mediator = mediator;
        this.name = name;
    }

    public abstract void send(String msg);
    public abstract void receive(String msg);
}

// 3. Concrete Mediator (The Central Control Tower)
class ChatRoom implements ChatMediator {
    private List<User> users;

    public ChatRoom() {
        this.users = new ArrayList<>();
    }

    @Override
    public void addUser(User user) {
        this.users.add(user);
    }

    @Override
    public void sendMessage(String msg, User sender) {
        // Route the message to everyone EXCEPT the sender
        for (User u : users) {
            if (u != sender) {
                u.receive(msg);
            }
        }
    }
}

// 4. Concrete Colleague 1
class PremiumUser extends User {
    public PremiumUser(ChatMediator mediator, String name) {
        super(mediator, name);
    }

    @Override
    public void send(String msg) {
        System.out.println("\n>>> " + this.name + " (Premium) sends: " + msg);
        mediator.sendMessage(msg, this);
    }

    @Override
    public void receive(String msg) {
        System.out.println("[" + this.name + " received]: " + msg);
    }
}

// 5. Concrete Colleague 2
class BasicUser extends User {
    public BasicUser(ChatMediator mediator, String name) {
        super(mediator, name);
    }

    @Override
    public void send(String msg) {
        System.out.println("\n>>> " + this.name + " (Basic) sends: " + msg);
        mediator.sendMessage(msg, this);
    }

    @Override
    public void receive(String msg) {
        System.out.println("[" + this.name + " received]: " + msg);
    }
}

// 6. Execution Driver
public class Main {
    public static void main(String[] args) {
        // Create the central mediator
        ChatMediator chatRoom = new ChatRoom();

        // Create users and link them to the mediator
        User alice = new PremiumUser(chatRoom, "Alice");
        User bob = new BasicUser(chatRoom, "Bob");
        User charlie = new BasicUser(chatRoom, "Charlie");

        chatRoom.addUser(alice);
        chatRoom.addUser(bob);
        chatRoom.addUser(charlie);

        // Users communicate blindly through the mediator
        alice.send("Hey everyone! The project is live.");
        bob.send("Awesome news!");
    }
}