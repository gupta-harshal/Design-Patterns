import java.util.Arrays;
import java.util.List;

class Notification {
    final String to;
    final String title;
    final String body;

    Notification(String to, String title, String body) {
        this.to = to;
        this.title = title;
        this.body = body;
    }
}

interface NotificationChannel {
    String name();
    void send(Notification notification);
}

class EmailChannel implements NotificationChannel {
    @Override public String name() { return "EMAIL"; }
    @Override public void send(Notification n) {
        System.out.println("EMAIL to=" + n.to + " subject=" + n.title + " body=" + n.body);
    }
}

class SmsChannel implements NotificationChannel {
    @Override public String name() { return "SMS"; }
    @Override public void send(Notification n) {
        System.out.println("SMS to=" + n.to + " text=" + n.title + ": " + n.body);
    }
}

class PushChannel implements NotificationChannel {
    @Override public String name() { return "PUSH"; }
    @Override public void send(Notification n) {
        System.out.println("PUSH to=" + n.to + " title=" + n.title);
    }
}

class NotificationService {
    void send(Notification notification, List<NotificationChannel> channels) {
        for (NotificationChannel channel : channels) {
            try {
                channel.send(notification);
            } catch (RuntimeException ex) {
                System.out.println("Channel " + channel.name() + " failed: " + ex.getMessage());
            }
        }
    }
}

public class Main {
    public static void main(String[] args) {
        NotificationService service = new NotificationService();
        Notification n = new Notification("user@example.com", "Order Shipped", "Your order #42 is on the way");
        service.send(n, Arrays.asList(new EmailChannel(), new SmsChannel(), new PushChannel()));
    }
}
