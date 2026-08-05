import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

enum VehicleType { ECONOMY, SUV, LUXURY }

enum ReservationStatus { ACTIVE, CANCELLED }

class Vehicle {
    final String id;
    final VehicleType type;

    Vehicle(String id, VehicleType type) {
        this.id = id;
        this.type = type;
    }
}

class Reservation {
    final String id = UUID.randomUUID().toString().substring(0, 8);
    final Vehicle vehicle;
    final LocalDate start; // inclusive
    final LocalDate end;   // exclusive
    final double price;
    ReservationStatus status = ReservationStatus.ACTIVE;

    Reservation(Vehicle vehicle, LocalDate start, LocalDate end, double price) {
        this.vehicle = vehicle;
        this.start = start;
        this.end = end;
        this.price = price;
    }

    boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return otherStart.isBefore(end) && otherEnd.isAfter(start);
    }
}

class RentalService {
    private final List<Vehicle> fleet = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final Map<VehicleType, Double> dailyRate = new EnumMap<>(VehicleType.class);

    RentalService() {
        dailyRate.put(VehicleType.ECONOMY, 40.0);
        dailyRate.put(VehicleType.SUV, 70.0);
        dailyRate.put(VehicleType.LUXURY, 120.0);
    }

    void addVehicle(Vehicle v) { fleet.add(v); }

    private boolean isFree(Vehicle v, LocalDate start, LocalDate end) {
        for (Reservation r : reservations) {
            if (r.status != ReservationStatus.ACTIVE) continue;
            if (!r.vehicle.id.equals(v.id)) continue;
            if (r.overlaps(start, end)) return false;
        }
        return true;
    }

    List<Vehicle> search(LocalDate start, LocalDate end, VehicleType typeOrNull) {
        return fleet.stream()
                .filter(v -> typeOrNull == null || v.type == typeOrNull)
                .filter(v -> isFree(v, start, end))
                .collect(Collectors.toList());
    }

    Reservation book(String vehicleId, LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end");
        }
        Vehicle vehicle = fleet.stream().filter(v -> v.id.equals(vehicleId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown vehicle"));
        if (!isFree(vehicle, start, end)) {
            System.out.println("CONFLICT: " + vehicleId + " not free for " + start + ".." + end);
            return null;
        }
        long days = ChronoUnit.DAYS.between(start, end);
        double price = days * dailyRate.get(vehicle.type);
        Reservation res = new Reservation(vehicle, start, end, price);
        reservations.add(res);
        System.out.println("Booked " + vehicleId + " " + start + "->" + end
                + " days=" + days + " price=" + price + " id=" + res.id);
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
        System.out.println("Cancel failed for " + reservationId);
    }
}

public class Main {
    public static void main(String[] args) {
        RentalService service = new RentalService();
        service.addVehicle(new Vehicle("ECO-1", VehicleType.ECONOMY));
        service.addVehicle(new Vehicle("SUV-1", VehicleType.SUV));

        LocalDate s = LocalDate.of(2026, 8, 10);
        LocalDate e = LocalDate.of(2026, 8, 13);

        System.out.println("Available ECONOMY: " + service.search(s, e, VehicleType.ECONOMY));
        Reservation r1 = service.book("ECO-1", s, e);
        service.book("ECO-1", LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 14)); // conflict
        service.cancel(r1.id);
        service.book("ECO-1", LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 14)); // ok
    }
}
