import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

// ============================================================
// 0. DOMAIN ERRORS
// ============================================================

class SchedulingException extends RuntimeException {
    public SchedulingException(String message) {
        super(message);
    }
}

// ============================================================
// 1. TIME SLOT — the half-open interval [start, end)
// ============================================================

class TimeSlot {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalDateTime start;
    private final LocalDateTime end;

    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new SchedulingException("Slot start must be strictly before end: "
                    + start + " -> " + end);
        }
        this.start = start;
        this.end = end;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    // THE rule. Half-open intervals: back-to-back slots (10-11, 11-12) do NOT overlap.
    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && end.isAfter(other.start);
    }

    public long durationMinutes() {
        return Duration.between(start, end).toMinutes();
    }

    @Override
    public String toString() {
        return FMT.format(start) + "-" + FMT.format(end);
    }
}

// ============================================================
// 2. USER + CALENDAR
// ============================================================

class User {
    private final String id;
    private final String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

// One calendar per user: the list of meetings that person is committed to.
class Calendar {
    private final String ownerId;
    private final List<Meeting> meetings = new ArrayList<>();

    public Calendar(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public boolean isFree(TimeSlot slot) {
        return conflictAt(slot) == null;
    }

    public Meeting conflictAt(TimeSlot slot) {
        for (Meeting meeting : meetings) {
            if (meeting.getSlot().overlaps(slot)) {
                return meeting;
            }
        }
        return null;
    }

    public void add(Meeting meeting) {
        meetings.add(meeting);
    }

    public void remove(Meeting meeting) {
        meetings.remove(meeting);
    }

    public List<Meeting> getMeetings() {
        return Collections.unmodifiableList(meetings);
    }
}

// ============================================================
// 3. MEETING ROOM
// ============================================================

class MeetingRoom {
    private final String id;
    private final int capacity;
    private final List<TimeSlot> bookings = new ArrayList<>();

    public MeetingRoom(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isAvailable(TimeSlot slot) {
        for (TimeSlot booked : bookings) {
            if (booked.overlaps(slot)) {
                return false;
            }
        }
        return true;
    }

    public void book(TimeSlot slot) {
        if (!isAvailable(slot)) {
            throw new SchedulingException("Room " + id + " is already booked at " + slot);
        }
        bookings.add(slot);
    }

    public void release(TimeSlot slot) {
        bookings.removeIf(booked -> booked.getStart().equals(slot.getStart())
                && booked.getEnd().equals(slot.getEnd()));
    }

    public List<TimeSlot> getBookings() {
        return Collections.unmodifiableList(bookings);
    }

    @Override
    public String toString() {
        return id + "(cap " + capacity + ")";
    }
}

// ============================================================
// 4. MEETING
// ============================================================

class Meeting {
    private final String id;
    private final String title;
    private final User organizer;
    private final Set<User> attendees;
    private final MeetingRoom room;
    private final TimeSlot slot;

    public Meeting(String id, String title, User organizer, Set<User> attendees,
                   MeetingRoom room, TimeSlot slot) {
        this.id = id;
        this.title = title;
        this.organizer = organizer;
        this.attendees = attendees;
        this.room = room;
        this.slot = slot;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public User getOrganizer() {
        return organizer;
    }

    public Set<User> getAttendees() {
        return Collections.unmodifiableSet(attendees);
    }

    public MeetingRoom getRoom() {
        return room;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    @Override
    public String toString() {
        return "\"" + title + "\" " + slot + " in " + room.getId()
                + " with " + attendees;
    }
}

// ============================================================
// 5. NOTIFICATION (Observer)
// ============================================================

interface MeetingObserver {
    void onScheduled(Meeting meeting);

    void onCancelled(Meeting meeting);
}

class ConsoleInviteNotifier implements MeetingObserver {
    @Override
    public void onScheduled(Meeting meeting) {
        System.out.println("   [invite] " + meeting.getAttendees() + " -> \""
                + meeting.getTitle() + "\" " + meeting.getSlot()
                + " @ " + meeting.getRoom().getId());
    }

    @Override
    public void onCancelled(Meeting meeting) {
        System.out.println("   [cancel] " + meeting.getAttendees() + " -> \""
                + meeting.getTitle() + "\" " + meeting.getSlot());
    }
}

// ============================================================
// 6. ROOM SELECTION (Strategy)
// ============================================================

interface RoomSelectionStrategy {
    MeetingRoom select(List<MeetingRoom> candidates);
}

// Best fit: the smallest room that still holds everyone, so big rooms stay
// free for big meetings.
class SmallestSufficientRoomStrategy implements RoomSelectionStrategy {
    @Override
    public MeetingRoom select(List<MeetingRoom> candidates) {
        MeetingRoom best = null;
        for (MeetingRoom room : candidates) {
            if (best == null || room.getCapacity() < best.getCapacity()) {
                best = room;
            }
        }
        return best;
    }
}

// ============================================================
// 7. SCHEDULER (orchestration)
// ============================================================

class Scheduler {
    private final List<MeetingRoom> rooms = new ArrayList<>();
    private final Map<String, Calendar> calendarsByUserId = new HashMap<>();
    private final Map<String, Meeting> meetingsById = new HashMap<>();
    private final List<MeetingObserver> observers = new ArrayList<>();
    private final RoomSelectionStrategy roomSelection;
    private final AtomicInteger sequence = new AtomicInteger(100);

    public Scheduler(RoomSelectionStrategy roomSelection) {
        this.roomSelection = roomSelection;
    }

    public void addRoom(MeetingRoom room) {
        rooms.add(room);
    }

    public void register(User user) {
        calendarsByUserId.put(user.getId(), new Calendar(user.getId()));
    }

    public void addObserver(MeetingObserver observer) {
        observers.add(observer);
    }

    public Calendar calendarOf(User user) {
        Calendar calendar = calendarsByUserId.get(user.getId());
        if (calendar == null) {
            throw new SchedulingException("User not registered: " + user.getId());
        }
        return calendar;
    }

    // Rooms that are both free for the slot and big enough for the party.
    public List<MeetingRoom> findAvailableRooms(TimeSlot slot, int requiredCapacity) {
        List<MeetingRoom> available = new ArrayList<>();
        for (MeetingRoom room : rooms) {
            if (room.getCapacity() >= requiredCapacity && room.isAvailable(slot)) {
                available.add(room);
            }
        }
        return available;
    }

    public Meeting schedule(String title, User organizer, List<User> invitees, TimeSlot slot) {
        Set<User> attendees = new LinkedHashSet<>();
        attendees.add(organizer);
        attendees.addAll(invitees);

        // 1. Attendee conflicts first — a busy person cannot be fixed by another room.
        for (User attendee : attendees) {
            Meeting conflict = calendarOf(attendee).conflictAt(slot);
            if (conflict != null) {
                throw new SchedulingException(attendee.getName() + " is busy at " + slot
                        + " with \"" + conflict.getTitle() + "\"");
            }
        }

        // 2. Then room availability.
        List<MeetingRoom> candidates = findAvailableRooms(slot, attendees.size());
        if (candidates.isEmpty()) {
            throw new SchedulingException("No room free at " + slot
                    + " for " + attendees.size() + " attendee(s)");
        }
        MeetingRoom room = roomSelection.select(candidates);

        // 3. Commit: book the room, then write every calendar.
        Meeting meeting = new Meeting("MTG-" + sequence.incrementAndGet(), title,
                organizer, attendees, room, slot);
        room.book(slot);
        for (User attendee : attendees) {
            calendarOf(attendee).add(meeting);
        }
        meetingsById.put(meeting.getId(), meeting);

        for (MeetingObserver observer : observers) {
            observer.onScheduled(meeting);
        }
        return meeting;
    }

    public void cancel(String meetingId) {
        Meeting meeting = meetingsById.remove(meetingId);
        if (meeting == null) {
            throw new SchedulingException("Unknown meeting: " + meetingId);
        }
        meeting.getRoom().release(meeting.getSlot());
        for (User attendee : meeting.getAttendees()) {
            calendarOf(attendee).remove(meeting);
        }
        for (MeetingObserver observer : observers) {
            observer.onCancelled(meeting);
        }
    }

    public List<MeetingRoom> getRooms() {
        return Collections.unmodifiableList(rooms);
    }
}

// ============================================================
// 8. CLIENT DEMO
// ============================================================

public class Main {
    private static final LocalDate DAY = LocalDate.of(2024, 6, 3);

    private static TimeSlot slot(int startHour, int startMin, int endHour, int endMin) {
        return new TimeSlot(LocalDateTime.of(DAY, LocalTime.of(startHour, startMin)),
                LocalDateTime.of(DAY, LocalTime.of(endHour, endMin)));
    }

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler(new SmallestSufficientRoomStrategy());
        scheduler.addRoom(new MeetingRoom("Huddle-A", 3));
        scheduler.addRoom(new MeetingRoom("Focus-B", 6));
        scheduler.addRoom(new MeetingRoom("Boardroom-C", 12));
        scheduler.addObserver(new ConsoleInviteNotifier());

        User alice = new User("U1", "Alice");
        User bob = new User("U2", "Bob");
        User carol = new User("U3", "Carol");
        User dave = new User("U4", "Dave");
        User erin = new User("U5", "Erin");
        User frank = new User("U6", "Frank");
        List<User> staff = List.of(alice, bob, carol, dave, erin, frank);
        for (User user : staff) {
            scheduler.register(user);
        }

        System.out.println("=== 1. OVERLAP RULE: start < other.end && end > other.start ===");
        TimeSlot tenToEleven = slot(10, 0, 11, 0);
        System.out.println("10:00-11:00 vs 10:30-11:30 -> " + tenToEleven.overlaps(slot(10, 30, 11, 30))
                + "  (partial overlap)");
        System.out.println("10:00-11:00 vs 11:00-12:00 -> " + tenToEleven.overlaps(slot(11, 0, 12, 0))
                + "  (back-to-back, allowed)");
        System.out.println("10:00-11:00 vs 10:15-10:45 -> " + tenToEleven.overlaps(slot(10, 15, 10, 45))
                + "  (fully contained)");
        System.out.println("10:00-11:00 vs 09:00-09:30 -> " + tenToEleven.overlaps(slot(9, 0, 9, 30))
                + "  (disjoint)");

        System.out.println();
        System.out.println("=== 2. AVAILABLE ROOMS FOR 10:00-11:00, 3 people ===");
        System.out.println("   " + scheduler.findAvailableRooms(tenToEleven, 3));

        System.out.println();
        System.out.println("=== 3. SCHEDULE (best-fit room wins) ===");
        Meeting standup = scheduler.schedule("Standup", alice, List.of(bob, carol), tenToEleven);
        System.out.println("   booked: " + standup);
        System.out.println("   rooms still free 10:00-11:00 for 3: "
                + scheduler.findAvailableRooms(tenToEleven, 3));

        System.out.println();
        System.out.println("=== 4. BACK-TO-BACK IS NOT A CONFLICT ===");
        Meeting review = scheduler.schedule("Design Review", alice, List.of(bob), slot(11, 0, 12, 0));
        System.out.println("   booked: " + review);

        System.out.println();
        System.out.println("=== 5. ATTENDEE BUSY -> CONFLICT ===");
        try {
            scheduler.schedule("1:1", dave, List.of(bob), slot(10, 30, 11, 0));
        } catch (SchedulingException e) {
            System.out.println("   Rejected: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== 6. FREE ATTENDEES STILL GET A DIFFERENT ROOM ===");
        Meeting sync = scheduler.schedule("Vendor Sync", dave, List.of(), slot(10, 30, 11, 0));
        System.out.println("   booked: " + sync);

        System.out.println();
        System.out.println("=== 7. ALL ROOMS TAKEN -> NO ROOM AVAILABLE ===");
        Meeting roadmap = scheduler.schedule("Roadmap", erin, List.of(), tenToEleven);
        System.out.println("   booked: " + roadmap);
        System.out.println("   rooms free 10:30-10:45 for 1: "
                + scheduler.findAvailableRooms(slot(10, 30, 10, 45), 1));
        try {
            scheduler.schedule("Coffee Chat", frank, List.of(), slot(10, 30, 10, 45));
        } catch (SchedulingException e) {
            System.out.println("   Rejected: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== 8. CAPACITY FILTER ===");
        System.out.println("   rooms free 14:00-15:00 for 10 people: "
                + scheduler.findAvailableRooms(slot(14, 0, 15, 0), 10));
        System.out.println("   rooms free 14:00-15:00 for 20 people: "
                + scheduler.findAvailableRooms(slot(14, 0, 15, 0), 20));

        System.out.println();
        System.out.println("=== 9. CANCEL FREES THE ROOM AND THE CALENDARS ===");
        scheduler.cancel(standup.getId());
        System.out.println("   Bob's calendar now: " + scheduler.calendarOf(bob).getMeetings());
        System.out.println("   rooms free 10:00-11:00 for 3: "
                + scheduler.findAvailableRooms(tenToEleven, 3));
        Meeting retro = scheduler.schedule("Retro", frank, List.of(bob), tenToEleven);
        System.out.println("   rebooked into the freed room: " + retro);

        System.out.println();
        System.out.println("=== 10. FINAL CALENDARS ===");
        for (User user : staff) {
            System.out.println("   " + user + ": " + scheduler.calendarOf(user).getMeetings());
        }
    }
}
