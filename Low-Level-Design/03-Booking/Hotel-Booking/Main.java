import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

enum RoomType { STANDARD, DELUXE, SUITE }

enum ReservationStatus { ACTIVE, CANCELLED }

class Room {
    final String id;
    final RoomType type;
    final double nightlyRate;

    Room(String id, RoomType type, double nightlyRate) {
        this.id = id;
        this.type = type;
        this.nightlyRate = nightlyRate;
    }
}

class Reservation {
    final String id = UUID.randomUUID().toString().substring(0, 8);
    final Room room;
    final LocalDate checkIn;  // inclusive
    final LocalDate checkOut; // exclusive
    final double price;
    ReservationStatus status = ReservationStatus.ACTIVE;

    Reservation(Room room, LocalDate checkIn, LocalDate checkOut, double price) {
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.price = price;
    }

    boolean overlaps(LocalDate start, LocalDate end) {
        return start.isBefore(checkOut) && end.isAfter(checkIn);
    }
}

class Hotel {
    final String name;
    private final List<Room> rooms = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();

    Hotel(String name) { this.name = name; }

    void addRoom(Room room) { rooms.add(room); }

    private boolean isFree(Room room, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation r : reservations) {
            if (r.status != ReservationStatus.ACTIVE) continue;
            if (!r.room.id.equals(room.id)) continue;
            if (r.overlaps(checkIn, checkOut)) return false;
        }
        return true;
    }

    List<Room> search(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        return rooms.stream()
                .filter(r -> r.type == type)
                .filter(r -> isFree(r, checkIn, checkOut))
                .collect(Collectors.toList());
    }

    Reservation book(String roomId, LocalDate checkIn, LocalDate checkOut) {
        Room room = rooms.stream().filter(r -> r.id.equals(roomId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown room"));
        if (!isFree(room, checkIn, checkOut)) {
            System.out.println("CONFLICT on " + roomId + " for " + checkIn + ".." + checkOut);
            return null;
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double price = nights * room.nightlyRate;
        Reservation res = new Reservation(room, checkIn, checkOut, price);
        reservations.add(res);
        System.out.println("Booked " + roomId + " nights=" + nights + " price=" + price + " id=" + res.id);
        return res;
    }

    void cancel(String reservationId) {
        for (Reservation r : reservations) {
            if (r.id.equals(reservationId) && r.status == ReservationStatus.ACTIVE) {
                r.status = ReservationStatus.CANCELLED;
                System.out.println("Cancelled " + reservationId);
                return;
            }
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Hotel hotel = new Hotel("Oceanview");
        hotel.addRoom(new Room("101", RoomType.STANDARD, 100));
        hotel.addRoom(new Room("201", RoomType.DELUXE, 180));

        LocalDate in = LocalDate.of(2026, 8, 10);
        LocalDate out = LocalDate.of(2026, 8, 15);

        System.out.println("Available STANDARD: " + hotel.search(RoomType.STANDARD, in, out).size());
        Reservation r1 = hotel.book("101", in, out);

        // Overlap should fail
        hotel.book("101", LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 16));

        // Back-to-back should succeed (half-open)
        hotel.book("101", LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 17));

        hotel.cancel(r1.id);
        hotel.book("101", LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 14));
    }
}
