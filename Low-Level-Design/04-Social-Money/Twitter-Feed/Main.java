import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class Tweet {
    final String authorId;
    final String text;
    final long timestamp;

    Tweet(String authorId, String text, long timestamp) {
        this.authorId = authorId;
        this.text = text;
        this.timestamp = timestamp;
    }
}

class User {
    final String id;
    final String name;
    final Set<String> following = new HashSet<>();
    final List<Tweet> tweets = new ArrayList<>();

    User(String id, String name) {
        this.id = id;
        this.name = name;
        following.add(id); // see own tweets
    }
}

class SocialService {
    private final Map<String, User> users = new HashMap<>();
    private long clock = 1;

    User register(String id, String name) {
        User u = new User(id, name);
        users.put(id, u);
        return u;
    }

    void follow(String followerId, String followeeId) {
        users.get(followerId).following.add(followeeId);
        System.out.println(followerId + " follows " + followeeId);
    }

    void unfollow(String followerId, String followeeId) {
        if (followerId.equals(followeeId)) return;
        users.get(followerId).following.remove(followeeId);
    }

    void post(String userId, String text) {
        Tweet t = new Tweet(userId, text, clock++);
        users.get(userId).tweets.add(t);
        System.out.println(userId + " tweeted: " + text);
    }

    List<Tweet> getFeed(String userId, int limit) {
        User user = users.get(userId);
        List<Tweet> feed = new ArrayList<>();
        for (String id : user.following) {
            feed.addAll(users.get(id).tweets);
        }
        return feed.stream()
                .sorted(Comparator.comparingLong((Tweet t) -> t.timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}

public class Main {
    public static void main(String[] args) {
        SocialService svc = new SocialService();
        svc.register("u1", "Ada");
        svc.register("u2", "Lin");
        svc.register("u3", "Grace");

        svc.follow("u1", "u2");
        svc.follow("u1", "u3");
        svc.post("u2", "hello from Lin");
        svc.post("u3", "systems!");
        svc.post("u1", "my own tweet");
        svc.post("u2", "second from Lin");

        System.out.println("\nAda feed:");
        for (Tweet t : svc.getFeed("u1", 10)) {
            System.out.println("  @" + t.authorId + ": " + t.text);
        }
    }
}
