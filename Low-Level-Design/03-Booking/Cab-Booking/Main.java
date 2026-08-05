import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class Location {
    final double x;
    final double y;

    Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    double distanceTo(Location other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}

class Rider {
    final String id;
    final String name;

    Rider(String id, String name) {
        this.id = id;
        this.name = name;
    }
}

class Driver {
    final String id;
    final String name;
    Location location;
    boolean available = true;

    Driver(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }
}

enum TripStatus {
    REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
}

class Trip {
    final String id = UUID.randomUUID().toString().substring(0, 8);
    final Rider rider;
    Driver driver;
    final Location pickup;
    final Location drop;
    TripStatus status = TripStatus.REQUESTED;
    double fare;

    Trip(Rider rider, Location pickup, Location drop) {
        this.rider = rider;
        this.pickup = pickup;
        this.drop = drop;
    }
}

interface MatchingStrategy {
    Driver match(List<Driver> drivers, Location pickup);
}

class NearestDriverStrategy implements MatchingStrategy {
    @Override
    public Driver match(List<Driver> drivers, Location pickup) {
        Driver best = null;
        double bestDist = Double.MAX_VALUE;
        for (Driver d : drivers) {
            if (!d.available) continue;
            double dist = d.location.distanceTo(pickup);
            if (dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        return best;
    }
}

interface FareStrategy {
    double quote(Location pickup, Location drop);
}

class BasePlusPerKmFare implements FareStrategy {
    private final double base;
    private final double perKm;

    BasePlusPerKmFare(double base, double perKm) {
        this.base = base;
        this.perKm = perKm;
    }

    @Override
    public double quote(Location pickup, Location drop) {
        return base + perKm * pickup.distanceTo(drop);
    }
}

class RideService {
    private final List<Driver> drivers = new ArrayList<>();
    private final MatchingStrategy matching;
    private final FareStrategy fareStrategy;

    RideService(MatchingStrategy matching, FareStrategy fareStrategy) {
        this.matching = matching;
        this.fareStrategy = fareStrategy;
    }

    void addDriver(Driver d) { drivers.add(d); }

    Trip requestRide(Rider rider, Location pickup, Location drop) {
        Trip trip = new Trip(rider, pickup, drop);
        Driver driver = matching.match(drivers, pickup);
        if (driver == null) {
            trip.status = TripStatus.CANCELLED;
            System.out.println("No drivers for " + rider.name);
            return trip;
        }
        driver.available = false;
        trip.driver = driver;
        trip.status = TripStatus.ACCEPTED;
        trip.fare = fareStrategy.quote(pickup, drop);
        System.out.println("Matched " + rider.name + " -> " + driver.name
                + " fare≈" + String.format("%.2f", trip.fare));
        return trip;
    }

    void startTrip(Trip trip) {
        if (trip.status != TripStatus.ACCEPTED) {
            System.out.println("Cannot start from " + trip.status);
            return;
        }
        trip.status = TripStatus.IN_PROGRESS;
        System.out.println("Trip " + trip.id + " IN_PROGRESS");
    }

    void completeTrip(Trip trip) {
        if (trip.status != TripStatus.IN_PROGRESS) {
            System.out.println("Cannot complete from " + trip.status);
            return;
        }
        trip.status = TripStatus.COMPLETED;
        trip.driver.location = trip.drop;
        trip.driver.available = true;
        System.out.println("Trip " + trip.id + " COMPLETED. Charged "
                + String.format("%.2f", trip.fare));
    }

    void cancel(Trip trip) {
        if (trip.status == TripStatus.COMPLETED || trip.status == TripStatus.CANCELLED) {
            System.out.println("Cannot cancel " + trip.status);
            return;
        }
        trip.status = TripStatus.CANCELLED;
        if (trip.driver != null) {
            trip.driver.available = true;
        }
        System.out.println("Trip " + trip.id + " CANCELLED");
    }
}

public class Main {
    public static void main(String[] args) {
        RideService service = new RideService(
                new NearestDriverStrategy(),
                new BasePlusPerKmFare(50, 10));
        service.addDriver(new Driver("D1", "Anya", new Location(0, 0)));
        service.addDriver(new Driver("D2", "Bala", new Location(10, 10)));

        Rider r = new Rider("R1", "Dev");
        Trip trip = service.requestRide(r, new Location(1, 1), new Location(4, 5));
        service.startTrip(trip);
        service.completeTrip(trip);

        System.out.println("\n-- No driver left nearby test: mark both busy via second request mid-trip --");
        Trip t2 = service.requestRide(new Rider("R2", "Eve"), new Location(0, 0), new Location(2, 2));
        service.startTrip(t2);
        service.cancel(t2);
    }
}
