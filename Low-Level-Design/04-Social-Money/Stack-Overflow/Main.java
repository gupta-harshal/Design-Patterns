import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// =====================================================================
// 0. REPUTATION RULE TABLE — one place, named constants, no magic numbers.
//    Every rule is reversible: undoing an action applies the negation.
// =====================================================================
final class Reputation {

    private Reputation() {
    }

    static final int MINIMUM = 1;

    static final int QUESTION_UPVOTE = +5;
    static final int QUESTION_DOWNVOTE = -2;
    static final int ANSWER_UPVOTE = +10;
    static final int ANSWER_DOWNVOTE = -2;

    /** Downvoting an ANSWER costs the voter 1 rep. Downvoting a question is free. */
    static final int ANSWER_DOWNVOTER_COST = -1;
    static final int QUESTION_DOWNVOTER_COST = 0;

    static final int ANSWER_ACCEPTED_AUTHOR = +15;
    static final int ANSWER_ACCEPTED_ASKER = +2;

    /** Privilege gates (a small slice of the real SO privilege ladder). */
    static final int MIN_REP_TO_DOWNVOTE = 125;
    static final int MIN_REP_TO_COMMENT = 50;
}

// =====================================================================
// 1. CLOCK — a monotonic counter instead of wall-clock time so the demo
//    output is deterministic and diffable.
// =====================================================================
final class Clock {
    private static long counter = 0;

    private Clock() {
    }

    static long tick() {
        return ++counter;
    }
}

// =====================================================================
// 2. USER
//    Reputation is stored as a RAW running total and clamped only on read.
//    Why: the floor of 1 would otherwise make deltas non-reversible
//    (apply -2 at rep 1, then revert +2, and you have invented 2 rep).
// =====================================================================
class User {
    private final String id;
    private final String name;
    private int rawReputation;
    private final List<String> reputationLog = new ArrayList<String>();

    User(String id, String name, int startingReputation) {
        this.id = id;
        this.name = name;
        this.rawReputation = startingReputation;
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    int getReputation() {
        return Math.max(Reputation.MINIMUM, rawReputation);
    }

    void applyReputation(int delta, String reason) {
        if (delta == 0) {
            return;
        }
        rawReputation += delta;
        reputationLog.add(String.format("%+4d  %-46s -> %d", delta, reason, getReputation()));
    }

    void printReputationLog() {
        System.out.println("  " + name + " (rep " + getReputation() + ")");
        if (reputationLog.isEmpty()) {
            System.out.println("        (no reputation events)");
        }
        for (String entry : reputationLog) {
            System.out.println("        " + entry);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        return id.equals(((User) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}

// =====================================================================
// 3. TAG (Flyweight — one canonical instance per name, shared by questions)
// =====================================================================
class Tag {
    private final String name;

    Tag(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Tag)) {
            return false;
        }
        return name.equals(((Tag) other).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}

class TagRegistry {
    private final Map<String, Tag> tags = new LinkedHashMap<String, Tag>();

    Tag get(String rawName) {
        String name = rawName.trim().toLowerCase();
        Tag tag = tags.get(name);
        if (tag == null) {
            tag = new Tag(name);
            tags.put(name, tag);
        }
        return tag;
    }

    Set<Tag> getAll(List<String> names) {
        Set<Tag> result = new LinkedHashSet<Tag>();
        for (String name : names) {
            result.add(get(name));
        }
        return result;
    }
}

// =====================================================================
// 4. COMMENT
// =====================================================================
class Comment {
    private final User author;
    private final String text;
    private final long createdAt;

    Comment(User author, String text) {
        this.author = author;
        this.text = text;
        this.createdAt = Clock.tick();
    }

    User getAuthor() {
        return author;
    }

    long getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "\"" + text + "\" — " + author;
    }
}

// =====================================================================
// 5. POST — the shared base for Question and Answer.
//    Votes and comments behave identically on both; only the reputation
//    weights differ, so those are abstract hooks (Template Method).
// =====================================================================
enum VoteType {
    UPVOTE(+1),
    DOWNVOTE(-1);

    private final int scoreDelta;

    VoteType(int scoreDelta) {
        this.scoreDelta = scoreDelta;
    }

    int getScoreDelta() {
        return scoreDelta;
    }
}

abstract class Post {
    private final String id;
    private final User author;
    private final String body;
    private final long createdAt;
    private final Map<User, VoteType> votes = new LinkedHashMap<User, VoteType>();
    private final List<Comment> comments = new ArrayList<Comment>();

    protected Post(String id, User author, String body) {
        this.id = id;
        this.author = author;
        this.body = body;
        this.createdAt = Clock.tick();
    }

    String getId() {
        return id;
    }

    User getAuthor() {
        return author;
    }

    String getBody() {
        return body;
    }

    long getCreatedAt() {
        return createdAt;
    }

    int getScore() {
        int score = 0;
        for (VoteType vote : votes.values()) {
            score += vote.getScoreDelta();
        }
        return score;
    }

    VoteType voteOf(User user) {
        return votes.get(user);
    }

    void setVote(User user, VoteType type) {
        votes.put(user, type);
    }

    void clearVote(User user) {
        votes.remove(user);
    }

    void addComment(Comment comment) {
        comments.add(comment);
    }

    List<Comment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /** Reputation weights — the only thing that differs between post kinds. */
    abstract int upvoteRewardForAuthor();

    abstract int downvotePenaltyForAuthor();

    abstract int downvoteCostForVoter();

    abstract String kind();
}

// =====================================================================
// 6. QUESTION
// =====================================================================
class Question extends Post {
    private final String title;
    private final Set<Tag> tags;
    private final List<Answer> answers = new ArrayList<Answer>();
    private Answer acceptedAnswer;

    Question(String id, User author, String title, String body, Set<Tag> tags) {
        super(id, author, body);
        this.title = title;
        this.tags = new LinkedHashSet<Tag>(tags);
    }

    String getTitle() {
        return title;
    }

    Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }

    void addAnswer(Answer answer) {
        answers.add(answer);
    }

    List<Answer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }

    Answer getAcceptedAnswer() {
        return acceptedAnswer;
    }

    void setAcceptedAnswer(Answer answer) {
        this.acceptedAnswer = answer;
    }

    @Override
    int upvoteRewardForAuthor() {
        return Reputation.QUESTION_UPVOTE;
    }

    @Override
    int downvotePenaltyForAuthor() {
        return Reputation.QUESTION_DOWNVOTE;
    }

    @Override
    int downvoteCostForVoter() {
        return Reputation.QUESTION_DOWNVOTER_COST;
    }

    @Override
    String kind() {
        return "question";
    }

    void print() {
        System.out.println("  [" + getId() + "] " + title
                + "  (score " + getScore() + ", tags " + tags + ", by " + getAuthor() + ")");
        for (Comment comment : getComments()) {
            System.out.println("        comment: " + comment);
        }
        for (Answer answer : answers) {
            System.out.println("      " + (answer.isAccepted() ? "[ACCEPTED] " : "           ")
                    + "[" + answer.getId() + "] score " + answer.getScore()
                    + " by " + answer.getAuthor() + ": " + answer.getBody());
            for (Comment comment : answer.getComments()) {
                System.out.println("                     comment: " + comment);
            }
        }
    }
}

// =====================================================================
// 7. ANSWER
// =====================================================================
class Answer extends Post {
    private final Question question;
    private boolean accepted;

    Answer(String id, User author, String body, Question question) {
        super(id, author, body);
        this.question = question;
    }

    Question getQuestion() {
        return question;
    }

    boolean isAccepted() {
        return accepted;
    }

    void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    @Override
    int upvoteRewardForAuthor() {
        return Reputation.ANSWER_UPVOTE;
    }

    @Override
    int downvotePenaltyForAuthor() {
        return Reputation.ANSWER_DOWNVOTE;
    }

    @Override
    int downvoteCostForVoter() {
        return Reputation.ANSWER_DOWNVOTER_COST;
    }

    @Override
    String kind() {
        return "answer";
    }
}

// =====================================================================
// 8. VOTING SERVICE
//    The three cases that must be handled, in order:
//      a) same vote again  -> toggle OFF (revert the old effect)
//      b) opposite vote    -> revert the old effect, then apply the new one
//      c) no existing vote -> apply
//    Reverting is literally "apply the negated deltas" — that symmetry is
//    what keeps reputation consistent, and it is why the floor of 1 is
//    applied on READ rather than on write.
// =====================================================================
class VotingService {

    void vote(User voter, Post post, VoteType type) {
        if (post.getAuthor().equals(voter)) {
            throw new IllegalStateException(voter + " cannot vote on their own " + post.kind());
        }
        if (type == VoteType.DOWNVOTE && voter.getReputation() < Reputation.MIN_REP_TO_DOWNVOTE) {
            throw new IllegalStateException(voter + " needs "
                    + Reputation.MIN_REP_TO_DOWNVOTE + " reputation to downvote (has "
                    + voter.getReputation() + ")");
        }

        VoteType existing = post.voteOf(voter);

        if (existing == type) {
            post.clearVote(voter);
            applyEffect(voter, post, type, -1);
            System.out.println("  " + voter + " removed their " + type + " on " + post.getId());
            return;
        }

        if (existing != null) {
            applyEffect(voter, post, existing, -1);
        }
        post.setVote(voter, type);
        applyEffect(voter, post, type, +1);
        System.out.println("  " + voter + " cast " + type + " on " + post.getId()
                + (existing != null ? " (changed from " + existing + ")" : "")
                + " — score now " + post.getScore());
    }

    /** {@code sign} is +1 to apply the vote's effects, -1 to undo them. */
    private void applyEffect(User voter, Post post, VoteType type, int sign) {
        String label = (sign > 0 ? "" : "undo ") + type + " on " + post.kind() + " " + post.getId();
        if (type == VoteType.UPVOTE) {
            post.getAuthor().applyReputation(sign * post.upvoteRewardForAuthor(), label);
        } else {
            post.getAuthor().applyReputation(sign * post.downvotePenaltyForAuthor(), label);
            voter.applyReputation(sign * post.downvoteCostForVoter(), label + " (voter cost)");
        }
    }
}

// =====================================================================
// 9. SEARCH — a plain in-memory filter. The real thing is an inverted
//    index; the shape of the API is identical, so swapping it is cheap.
// =====================================================================
class SearchService {

    private static final Comparator<Question> BY_SCORE_THEN_RECENCY = new Comparator<Question>() {
        @Override
        public int compare(Question a, Question b) {
            int byScore = Integer.compare(b.getScore(), a.getScore());
            return byScore != 0 ? byScore : Long.compare(b.getCreatedAt(), a.getCreatedAt());
        }
    };

    List<Question> byTag(List<Question> questions, Tag tag) {
        List<Question> hits = new ArrayList<Question>();
        for (Question question : questions) {
            if (question.hasTag(tag)) {
                hits.add(question);
            }
        }
        Collections.sort(hits, BY_SCORE_THEN_RECENCY);
        return hits;
    }

    List<Question> byKeyword(List<Question> questions, String keyword) {
        String needle = keyword.toLowerCase();
        List<Question> hits = new ArrayList<Question>();
        for (Question question : questions) {
            if (question.getTitle().toLowerCase().contains(needle)
                    || question.getBody().toLowerCase().contains(needle)) {
                hits.add(question);
            }
        }
        Collections.sort(hits, BY_SCORE_THEN_RECENCY);
        return hits;
    }
}

// =====================================================================
// 10. FACADE
// =====================================================================
class StackOverflowService {

    private final Map<String, User> users = new LinkedHashMap<String, User>();
    private final List<Question> questions = new ArrayList<Question>();
    private final TagRegistry tagRegistry = new TagRegistry();
    private final VotingService votingService = new VotingService();
    private final SearchService searchService = new SearchService();
    private int questionSequence = 0;
    private int answerSequence = 0;

    User registerUser(String id, String name, int startingReputation) {
        User user = new User(id, name, startingReputation);
        users.put(id, user);
        return user;
    }

    TagRegistry getTagRegistry() {
        return tagRegistry;
    }

    Question postQuestion(User author, String title, String body, String... tagNames) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("A question needs a title");
        }
        if (tagNames.length == 0 || tagNames.length > 5) {
            throw new IllegalArgumentException("A question needs between 1 and 5 tags");
        }
        Question question = new Question("q" + (++questionSequence), author, title, body,
                tagRegistry.getAll(Arrays.asList(tagNames)));
        questions.add(question);
        return question;
    }

    Answer postAnswer(User author, Question question, String body) {
        Answer answer = new Answer("a" + (++answerSequence), author, body, question);
        question.addAnswer(answer);
        return answer;
    }

    void addComment(User author, Post post, String text) {
        if (author.getReputation() < Reputation.MIN_REP_TO_COMMENT) {
            throw new IllegalStateException(author + " needs "
                    + Reputation.MIN_REP_TO_COMMENT + " reputation to comment");
        }
        post.addComment(new Comment(author, text));
    }

    void vote(User voter, Post post, VoteType type) {
        votingService.vote(voter, post, type);
    }

    /**
     * Only the question owner may accept, and a question has at most one
     * accepted answer. Calling this on the already-accepted answer un-accepts it.
     */
    void acceptAnswer(User actor, Answer answer) {
        Question question = answer.getQuestion();
        if (!question.getAuthor().equals(actor)) {
            throw new IllegalStateException("Only " + question.getAuthor()
                    + " (the asker) can accept an answer on " + question.getId()
                    + "; " + actor + " cannot");
        }

        Answer current = question.getAcceptedAnswer();
        if (current != null) {
            current.setAccepted(false);
            question.setAcceptedAnswer(null);
            current.getAuthor().applyReputation(-Reputation.ANSWER_ACCEPTED_AUTHOR,
                    "answer " + current.getId() + " un-accepted");
            question.getAuthor().applyReputation(-Reputation.ANSWER_ACCEPTED_ASKER,
                    "un-accepted an answer on " + question.getId());
            System.out.println("  " + current.getId() + " un-accepted");
            if (current == answer) {
                return; // Toggle off; nothing new to accept.
            }
        }

        answer.setAccepted(true);
        question.setAcceptedAnswer(answer);
        answer.getAuthor().applyReputation(Reputation.ANSWER_ACCEPTED_AUTHOR,
                "answer " + answer.getId() + " accepted");
        question.getAuthor().applyReputation(Reputation.ANSWER_ACCEPTED_ASKER,
                "accepted an answer on " + question.getId());
        System.out.println("  " + actor + " accepted " + answer.getId()
                + " by " + answer.getAuthor());
    }

    List<Question> searchByTag(String tagName) {
        return searchService.byTag(questions, tagRegistry.get(tagName));
    }

    List<Question> searchByKeyword(String keyword) {
        return searchService.byKeyword(questions, keyword);
    }

    List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    List<User> getUsers() {
        return new ArrayList<User>(users.values());
    }
}

// =====================================================================
// 11. CLIENT
// =====================================================================
public class Main {

    public static void main(String[] args) {
        StackOverflowService so = new StackOverflowService();

        // Seeded reputations so the privilege gates (downvote needs 125) are meaningful.
        User alice = so.registerUser("u1", "Alice", 100);
        User bob = so.registerUser("u2", "Bob", 50);
        User charlie = so.registerUser("u3", "Charlie", 40);
        User dave = so.registerUser("u4", "Dave", 500);
        User erin = so.registerUser("u5", "Erin", 300);

        System.out.println("=== Posting ===");
        Question q1 = so.postQuestion(alice,
                "How does HashMap resolve collisions in Java?",
                "I understand hashing but not what happens when two keys collide.",
                "java", "collections", "hashmap");
        Question q2 = so.postQuestion(charlie,
                "How do I merge two DataFrames in pandas?",
                "Looking for the equivalent of a SQL join.",
                "python", "pandas");
        Question q3 = so.postQuestion(bob,
                "When should I use ConcurrentHashMap over synchronized Map?",
                "Trying to understand the locking differences.",
                "java", "concurrency");

        Answer a1 = so.postAnswer(bob, q1,
                "Separate chaining: buckets hold a linked list, which becomes a red-black tree past 8 entries.");
        Answer a2 = so.postAnswer(charlie, q1,
                "It just overwrites the old value.");

        so.addComment(dave, q1, "Which Java version are you on? The treeify threshold changed in 8.");
        so.addComment(alice, a1, "The treeify detail is exactly what I was missing, thanks.");
        System.out.println("  posted 3 questions, 2 answers, 2 comments");

        // -----------------------------------------------------------------
        // VOTING — starting reputations: Alice 100, Bob 50, Charlie 40,
        //                                Dave 500, Erin 300
        // -----------------------------------------------------------------
        System.out.println();
        System.out.println("=== Voting ===");

        so.vote(dave, q1, VoteType.UPVOTE);      // Alice +5  -> 105
        so.vote(erin, q1, VoteType.UPVOTE);      // Alice +5  -> 110
        so.vote(charlie, a1, VoteType.UPVOTE);   // Bob   +10 -> 60
        so.vote(dave, a1, VoteType.UPVOTE);      // Bob   +10 -> 70
        so.vote(dave, a2, VoteType.DOWNVOTE);    // Charlie -2 -> 38, Dave -1 -> 499

        // Change of mind: revert +5 then apply -2  => Alice 110 - 5 - 2 = 103
        so.vote(erin, q1, VoteType.DOWNVOTE);
        // Same vote again toggles it off: revert -2 => Alice 105
        so.vote(erin, q1, VoteType.DOWNVOTE);

        System.out.println();
        System.out.println("=== Rejected actions ===");
        expectRejected("Alice upvotes her own question", new Runnable() {
            public void run() {
                so.vote(alice, q1, VoteType.UPVOTE);
            }
        });
        expectRejected("Bob (rep 70) downvotes — below the 125 threshold", new Runnable() {
            public void run() {
                so.vote(bob, q2, VoteType.DOWNVOTE);
            }
        });
        expectRejected("Bob accepts an answer on Alice's question", new Runnable() {
            public void run() {
                so.acceptAnswer(bob, a1);
            }
        });
        expectRejected("Charlie (rep 38) comments — below the 50 threshold", new Runnable() {
            public void run() {
                so.addComment(charlie, q1, "me too");
            }
        });

        // -----------------------------------------------------------------
        // ACCEPTING
        // -----------------------------------------------------------------
        System.out.println();
        System.out.println("=== Accepting ===");
        so.acceptAnswer(alice, a1);   // Bob +15 -> 85,  Alice +2 -> 107
        so.acceptAnswer(alice, a2);   // un-accept a1: Bob -15 -> 70, Alice -2 -> 105
                                      // accept    a2: Charlie +15 -> 53, Alice +2 -> 107

        // -----------------------------------------------------------------
        // FINAL STATE
        // -----------------------------------------------------------------
        System.out.println();
        System.out.println("=== Question thread ===");
        q1.print();

        System.out.println();
        System.out.println("=== Search ===");
        printHits("tag:java", so.searchByTag("java"));
        printHits("tag:python", so.searchByTag("python"));
        printHits("tag:rust (no hits)", so.searchByTag("rust"));
        printHits("keyword:'collide'", so.searchByKeyword("collide"));

        System.out.println();
        System.out.println("=== Reputation ledger ===");
        for (User user : so.getUsers()) {
            user.printReputationLog();
        }

        System.out.println();
        System.out.println("=== Checks ===");
        expect("Alice reputation", 107, alice.getReputation());
        expect("Bob reputation", 70, bob.getReputation());
        expect("Charlie reputation", 53, charlie.getReputation());
        expect("Dave reputation", 499, dave.getReputation());
        expect("Erin reputation", 300, erin.getReputation());
        expect("q1 score", 1, q1.getScore());
        expect("a1 score", 2, a1.getScore());
        expect("a2 score", -1, a2.getScore());
        expect("java tag hits", 2, so.searchByTag("java").size());
        expect("accepted answer is a2", 1, q1.getAcceptedAnswer() == a2 ? 1 : 0);
        expect("a1 no longer accepted", 0, a1.isAccepted() ? 1 : 0);
        expect("q3 exists", 1, so.getQuestions().contains(q3) ? 1 : 0);
    }

    private static void printHits(String label, List<Question> hits) {
        System.out.println("  " + label + " -> " + hits.size() + " result(s)");
        for (Question question : hits) {
            System.out.println("        [" + question.getId() + "] score "
                    + question.getScore() + "  " + question.getTitle());
        }
    }

    private static void expectRejected(String label, Runnable action) {
        try {
            action.run();
            System.out.println("  " + label + " -> UNEXPECTEDLY ALLOWED");
        } catch (RuntimeException e) {
            System.out.println("  " + label + " -> rejected: " + e.getMessage());
        }
    }

    private static void expect(String label, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("CHECK FAILED [" + label + "] expected "
                    + expected + " but was " + actual);
        }
        System.out.println("  check OK: " + label + " = " + actual);
    }
}
