import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

interface Subscriber {
    String name();
    void onMessage(String topic, String message);
}

class PrintingSubscriber implements Subscriber {
    private final String name;

    PrintingSubscriber(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void onMessage(String topic, String message) {
        System.out.println("[" + name + "] " + topic + " => " + message);
    }
}

class MessageBroker {
    private final Map<String, Set<Subscriber>> topics = new LinkedHashMap<>();

    void createTopic(String topic) {
        topics.computeIfAbsent(topic, t -> new LinkedHashSet<>());
        System.out.println("Topic ready: " + topic);
    }

    void subscribe(String topic, Subscriber subscriber) {
        Set<Subscriber> set = topics.get(topic);
        if (set == null) {
            throw new IllegalArgumentException("Unknown topic: " + topic);
        }
        set.add(subscriber);
        System.out.println(subscriber.name() + " subscribed to " + topic);
    }

    void unsubscribe(String topic, Subscriber subscriber) {
        Set<Subscriber> set = topics.get(topic);
        if (set != null) {
            set.remove(subscriber);
        }
    }

    void publish(String topic, String message) {
        Set<Subscriber> set = topics.get(topic);
        if (set == null) {
            throw new IllegalArgumentException("Unknown topic: " + topic);
        }
        System.out.println("PUBLISH " + topic + ": " + message);
        // Copy to avoid CME if a subscriber unsubscribes in callback
        for (Subscriber s : new LinkedHashSet<>(set)) {
            s.onMessage(topic, message);
        }
    }

    Set<String> listTopics() {
        return Collections.unmodifiableSet(topics.keySet());
    }
}

public class Main {
    public static void main(String[] args) {
        MessageBroker broker = new MessageBroker();
        broker.createTopic("orders");
        broker.createTopic("alerts");

        Subscriber email = new PrintingSubscriber("EmailWorker");
        Subscriber sms = new PrintingSubscriber("SmsWorker");
        Subscriber audit = new PrintingSubscriber("Audit");

        broker.subscribe("orders", email);
        broker.subscribe("orders", audit);
        broker.subscribe("alerts", sms);

        broker.publish("orders", "order-42 created");
        broker.publish("alerts", "high CPU");

        broker.unsubscribe("orders", audit);
        broker.publish("orders", "order-42 paid");
    }
}
