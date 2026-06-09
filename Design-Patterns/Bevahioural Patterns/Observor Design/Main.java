import java.util.ArrayList;
import java.util.List;

// 1. The Observer Interface
interface Observer {
    void update(String videoTitle);
}

// 2. The Subject Interface
interface Subject {
    void registerObserver(Observer observer);
    void removeObserver(Observer observer);
    void notifyObservers();
}

// 3. The Concrete Subject (The YouTube Channel)
class YoutubeChannel implements Subject {
    private List<Observer> subscribers = new ArrayList<>();
    private String channelName;
    private String latestVideoTitle;

    public YoutubeChannel(String channelName) {
        this.channelName = channelName;
    }

    public void uploadVideo(String title) {
        this.latestVideoTitle = title;
        System.out.println("\n[" + channelName + "] Uploaded a new video: " + title);
        notifyObservers(); // Trigger notification automatically
    }

    @Override
    public void registerObserver(Observer observer) {
        subscribers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        subscribers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer subscriber : subscribers) {
            subscriber.update(latestVideoTitle);
        }
    }
}

// 4. Concrete Observers (The Subscribers)
class User implements Observer {
    private String name;

    public User(String name) {
        this.name = name;
    }

    @Override
    public void update(String videoTitle) {
        System.out.println("Notification to " + name + ": New video out! Watch '" + videoTitle + "'");
    }
}

// 5. Execution Demo
public class Main {
    public static void main(String[] args) {
        // Create the channel (Subject)
        YoutubeChannel techChannel = new YoutubeChannel("TechWithGemini");

        // Create users (Observers)
        User alice = new User("Alice");
        User bob = new User("Bob");

        // Users subscribe to the channel
        techChannel.registerObserver(alice);
        techChannel.registerObserver(cbob);

        // Channel uploads a video -> Notifications fire automatically
        techChannel.uploadVideo("Mastering System Design in 2026");

        // Bob decides to unsubscribe
        techChannel.removeObserver(bob);

        // Channel uploads another video -> Only Alice gets notified
        techChannel.uploadVideo("Java Observer Pattern Explained!");
    }
}