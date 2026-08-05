import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// 1. DOMAIN ENUMS
// ============================================================
enum VehicleType {
    MOTORCYCLE,
    CAR,
    BUS
}

enum SpotType {
    MOTORCYCLE,
    COMPACT,
    LARGE,
    HANDICAPPED
}

enum TicketStatus {
    ACTIVE,
    PAID
}

// ============================================================
// 2. VEHICLE
// ============================================================
class Vehicle {
    private final String plate;
    private final VehicleType type;
    private final boolean handicappedPermit;

    public Vehicle(String plate, VehicleType type) {
        this(plate, type, false);
    }

    public Vehicle(String plate, VehicleType type, boolean handicappedPermit) {
        this.plate = plate;
        this.type = type;
        this.handicappedPermit = handicappedPermit;
    }

    public String getPlate() {
        return plate;
    }

    public VehicleType getType() {
        return type;
    }

    public boolean hasHandicappedPermit() {
        return handicappedPermit;
    }

    @Override
    public String toString() {
        return type + "[" + plate + "]" + (handicappedPermit ? "(permit)" : "");
    }
}

// ============================================================
// 3. FIT POLICY — which vehicle may occupy which spot type
//    Kept out of Spot/Vehicle so the rule table has one home.
// ============================================================
final class SpotFitPolicy {
    private static final Map<VehicleType, List<SpotType>> ALLOWED =
            new EnumMap<VehicleType, List<SpotType>>(VehicleType.class);

    private static final Map<SpotType, Integer> SIZE_RANK =
            new EnumMap<SpotType, Integer>(SpotType.class);

    static {
        ALLOWED.put(VehicleType.MOTORCYCLE,
                Arrays.asList(SpotType.MOTORCYCLE, SpotType.COMPACT, SpotType.LARGE));
        ALLOWED.put(VehicleType.CAR,
                Arrays.asList(SpotType.COMPACT, SpotType.LARGE));
        ALLOWED.put(VehicleType.BUS,
                Arrays.asList(SpotType.LARGE));

        SIZE_RANK.put(SpotType.MOTORCYCLE, 0);
        SIZE_RANK.put(SpotType.COMPACT, 1);
        SIZE_RANK.put(SpotType.HANDICAPPED, 1);
        SIZE_RANK.put(SpotType.LARGE, 2);
    }

    private SpotFitPolicy() {
    }

    /** A handicapped spot is reserved: permit required, and never for a bus. */
    public static boolean canFit(Vehicle vehicle, SpotType spotType) {
        if (spotType == SpotType.HANDICAPPED) {
            return vehicle.hasHandicappedPermit() && vehicle.getType() != VehicleType.BUS;
        }
        return ALLOWED.get(vehicle.getType()).contains(spotType);
    }

    public static int sizeRank(SpotType spotType) {
        return SIZE_RANK.get(spotType);
    }
}

// ============================================================
// 4. PARKING SPOT — the only place occupancy is mutated
// ============================================================
class ParkingSpot {
    private final String id;
    private final int floorNumber;
    private final SpotType type;
    private final int distanceFromEntrance;
    private Vehicle occupant;

    public ParkingSpot(String id, int floorNumber, SpotType type, int distanceFromEntrance) {
        this.id = id;
        this.floorNumber = floorNumber;
        this.type = type;
        this.distanceFromEntrance = distanceFromEntrance;
    }

    public String getId() {
        return id;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public SpotType getType() {
        return type;
    }

    public int getDistanceFromEntrance() {
        return distanceFromEntrance;
    }

    public synchronized boolean isFree() {
        return occupant == null;
    }

    public synchronized Vehicle getOccupant() {
        return occupant;
    }

    /**
     * Guarded claim: returns false if the spot was taken between the search and
     * this call, or if the vehicle does not fit. Callers must retry on false.
     * This check-and-set is what makes double-parking impossible.
     */
    public synchronized boolean assign(Vehicle vehicle) {
        if (occupant != null) {
            return false;
        }
        if (!SpotFitPolicy.canFit(vehicle, type)) {
            return false;
        }
        occupant = vehicle;
        return true;
    }

    public synchronized boolean release() {
        if (occupant == null) {
            return false;
        }
        occupant = null;
        return true;
    }

    @Override
    public String toString() {
        return id + "(" + type + ")";
    }
}

// ============================================================
// 5. FLOOR + LOT
// ============================================================
class Floor {
    private final int number;
    private final List<ParkingSpot> spots = new ArrayList<ParkingSpot>();

    public Floor(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public List<ParkingSpot> getSpots() {
        return spots;
    }

    public Floor addSpots(SpotType type, int count, int baseDistance) {
        int existing = spots.size();
        for (int i = 0; i < count; i++) {
            String id = String.format("F%d-%s%02d", number, type.name().charAt(0), existing + i + 1);
            spots.add(new ParkingSpot(id, number, type, baseDistance + i));
        }
        return this;
    }

    public int countFree(SpotType type) {
        int free = 0;
        for (ParkingSpot spot : spots) {
            if (spot.getType() == type && spot.isFree()) {
                free++;
            }
        }
        return free;
    }
}

class ParkingLot {
    private final String name;
    private final List<Floor> floors = new ArrayList<Floor>();

    public ParkingLot(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Floor> getFloors() {
        return floors;
    }

    public Floor addFloor(Floor floor) {
        floors.add(floor);
        return floor;
    }

    public void printAvailability() {
        System.out.println("  Availability in " + name + ":");
        for (Floor floor : floors) {
            StringBuilder sb = new StringBuilder();
            for (SpotType type : SpotType.values()) {
                if (sb.length() > 0) {
                    sb.append("  ");
                }
                sb.append(type).append("=").append(floor.countFree(type));
            }
            System.out.println("    Floor " + floor.getNumber() + " -> " + sb);
        }
    }
}

// ============================================================
// 6. TICKET + RECEIPT
// ============================================================
class Ticket {
    private final String id;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TicketStatus status = TicketStatus.ACTIVE;
    private int feeCharged;

    public Ticket(String id, Vehicle vehicle, ParkingSpot spot, LocalDateTime entryTime) {
        this.id = id;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = entryTime;
    }

    public String getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public int getFeeCharged() {
        return feeCharged;
    }

    public void settle(LocalDateTime exitTime, int fee) {
        this.exitTime = exitTime;
        this.feeCharged = fee;
        this.status = TicketStatus.PAID;
    }

    @Override
    public String toString() {
        return "Ticket{" + id + ", " + vehicle + ", spot=" + spot.getId()
                + ", in=" + entryTime.toLocalTime() + ", " + status + "}";
    }
}

class Receipt {
    private final String ticketId;
    private final String plate;
    private final String spotId;
    private final long minutesParked;
    private final int amount;

    public Receipt(String ticketId, String plate, String spotId, long minutesParked, int amount) {
        this.ticketId = ticketId;
        this.plate = plate;
        this.spotId = spotId;
        this.minutesParked = minutesParked;
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Receipt{" + ticketId + ", " + plate + ", spot=" + spotId
                + ", " + minutesParked + " min, INR " + amount + "}";
    }
}

// ============================================================
// 7. TIME SOURCE (injected so the demo is deterministic)
// ============================================================
interface TimeProvider {
    LocalDateTime now();
}

class SimulatedClock implements TimeProvider {
    private LocalDateTime current;

    public SimulatedClock(LocalDateTime start) {
        this.current = start;
    }

    public LocalDateTime now() {
        return current;
    }

    public void advanceMinutes(long minutes) {
        current = current.plusMinutes(minutes);
    }
}

// ============================================================
// 8. STRATEGY — spot assignment
// ============================================================
interface SpotAssignmentStrategy {
    String name();

    /** Returns a candidate free spot, or null when nothing fits. */
    ParkingSpot findSpot(ParkingLot lot, Vehicle vehicle);
}

/** First fitting free spot in scan order — O(n), cheapest to reason about. */
class AnyFreeSpotStrategy implements SpotAssignmentStrategy {
    public String name() {
        return "ANY_FREE";
    }

    public ParkingSpot findSpot(ParkingLot lot, Vehicle vehicle) {
        for (Floor floor : lot.getFloors()) {
            for (ParkingSpot spot : floor.getSpots()) {
                if (spot.isFree() && SpotFitPolicy.canFit(vehicle, spot.getType())) {
                    return spot;
                }
            }
        }
        return null;
    }
}

/** Lowest floor first, then closest to the entrance. */
class NearestFirstSpotStrategy implements SpotAssignmentStrategy {
    public String name() {
        return "NEAREST_FIRST";
    }

    public ParkingSpot findSpot(ParkingLot lot, Vehicle vehicle) {
        ParkingSpot best = null;
        for (Floor floor : lot.getFloors()) {
            for (ParkingSpot spot : floor.getSpots()) {
                if (!spot.isFree() || !SpotFitPolicy.canFit(vehicle, spot.getType())) {
                    continue;
                }
                if (best == null || isCloser(spot, best)) {
                    best = spot;
                }
            }
        }
        return best;
    }

    private boolean isCloser(ParkingSpot candidate, ParkingSpot best) {
        if (candidate.getFloorNumber() != best.getFloorNumber()) {
            return candidate.getFloorNumber() < best.getFloorNumber();
        }
        return candidate.getDistanceFromEntrance() < best.getDistanceFromEntrance();
    }
}

/** Smallest spot that still fits, then nearest — stops bikes eating bus bays. */
class BestFitSpotStrategy implements SpotAssignmentStrategy {
    public String name() {
        return "BEST_FIT";
    }

    public ParkingSpot findSpot(ParkingLot lot, Vehicle vehicle) {
        ParkingSpot best = null;
        for (Floor floor : lot.getFloors()) {
            for (ParkingSpot spot : floor.getSpots()) {
                if (!spot.isFree() || !SpotFitPolicy.canFit(vehicle, spot.getType())) {
                    continue;
                }
                if (best == null || isBetter(spot, best)) {
                    best = spot;
                }
            }
        }
        return best;
    }

    private boolean isBetter(ParkingSpot candidate, ParkingSpot best) {
        int candidateRank = SpotFitPolicy.sizeRank(candidate.getType());
        int bestRank = SpotFitPolicy.sizeRank(best.getType());
        if (candidateRank != bestRank) {
            return candidateRank < bestRank;
        }
        if (candidate.getFloorNumber() != best.getFloorNumber()) {
            return candidate.getFloorNumber() < best.getFloorNumber();
        }
        return candidate.getDistanceFromEntrance() < best.getDistanceFromEntrance();
    }
}

// ============================================================
// 9. STRATEGY — pricing
// ============================================================
interface PricingStrategy {
    String name();

    int calculateFee(Ticket ticket, LocalDateTime exitTime);
}

/** Per-started-hour rate that depends on the spot occupied, with a free grace window. */
class HourlyPricingStrategy implements PricingStrategy {
    private final Map<SpotType, Integer> ratePerHour;
    private final long graceMinutes;

    public HourlyPricingStrategy(Map<SpotType, Integer> ratePerHour, long graceMinutes) {
        this.ratePerHour = ratePerHour;
        this.graceMinutes = graceMinutes;
    }

    public String name() {
        return "HOURLY";
    }

    public int calculateFee(Ticket ticket, LocalDateTime exitTime) {
        long minutes = minutesParked(ticket, exitTime);
        if (minutes <= graceMinutes) {
            return 0;
        }
        long startedHours = (minutes + 59) / 60;
        return (int) (startedHours * ratePerHour.get(ticket.getSpot().getType()));
    }

    static long minutesParked(Ticket ticket, LocalDateTime exitTime) {
        long minutes = Duration.between(ticket.getEntryTime(), exitTime).toMinutes();
        return minutes < 0 ? 0 : minutes;
    }
}

/** Day-pass style: one flat charge no matter the duration. */
class FlatRatePricingStrategy implements PricingStrategy {
    private final int flatAmount;

    public FlatRatePricingStrategy(int flatAmount) {
        this.flatAmount = flatAmount;
    }

    public String name() {
        return "FLAT";
    }

    public int calculateFee(Ticket ticket, LocalDateTime exitTime) {
        return flatAmount;
    }
}

/** Context that owns the pricing policy; swapping the policy never touches callers. */
class FeeCalculator {
    private PricingStrategy strategy;

    public FeeCalculator(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public String strategyName() {
        return strategy.name();
    }

    public int calculate(Ticket ticket, LocalDateTime exitTime) {
        return strategy.calculateFee(ticket, exitTime);
    }
}

// ============================================================
// 10. SERVICE — the only public entry/exit API
// ============================================================
class ParkingService {
    private static final int MAX_ASSIGN_ATTEMPTS = 5;

    private final ParkingLot lot;
    private final FeeCalculator feeCalculator;
    private final TimeProvider clock;
    private SpotAssignmentStrategy assignmentStrategy;

    private final Map<String, Ticket> activeTickets = new LinkedHashMap<String, Ticket>();
    private final Map<String, String> plateToTicket = new HashMap<String, String>();
    private int ticketSequence = 0;

    public ParkingService(ParkingLot lot,
                          SpotAssignmentStrategy assignmentStrategy,
                          FeeCalculator feeCalculator,
                          TimeProvider clock) {
        this.lot = lot;
        this.assignmentStrategy = assignmentStrategy;
        this.feeCalculator = feeCalculator;
        this.clock = clock;
    }

    public void setAssignmentStrategy(SpotAssignmentStrategy strategy) {
        this.assignmentStrategy = strategy;
    }

    public Ticket park(Vehicle vehicle) {
        if (plateToTicket.containsKey(vehicle.getPlate())) {
            throw new IllegalStateException("Vehicle " + vehicle.getPlate()
                    + " already holds active ticket " + plateToTicket.get(vehicle.getPlate()));
        }
        for (int attempt = 0; attempt < MAX_ASSIGN_ATTEMPTS; attempt++) {
            ParkingSpot spot = assignmentStrategy.findSpot(lot, vehicle);
            if (spot == null) {
                throw new IllegalStateException("Parking full: no spot fits " + vehicle);
            }
            if (!spot.assign(vehicle)) {
                continue; // another entry gate won the race — search again
            }
            ticketSequence++;
            Ticket ticket = new Ticket("T" + String.format("%03d", ticketSequence),
                    vehicle, spot, clock.now());
            activeTickets.put(ticket.getId(), ticket);
            plateToTicket.put(vehicle.getPlate(), ticket.getId());
            return ticket;
        }
        throw new IllegalStateException("Could not claim a spot after "
                + MAX_ASSIGN_ATTEMPTS + " attempts (contention)");
    }

    public Receipt unpark(String ticketId) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Unknown or already settled ticket: " + ticketId);
        }
        LocalDateTime exitTime = clock.now();
        int fee = feeCalculator.calculate(ticket, exitTime);
        ticket.settle(exitTime, fee);
        ticket.getSpot().release();
        activeTickets.remove(ticketId);
        plateToTicket.remove(ticket.getVehicle().getPlate());
        return new Receipt(ticket.getId(), ticket.getVehicle().getPlate(), ticket.getSpot().getId(),
                HourlyPricingStrategy.minutesParked(ticket, exitTime), fee);
    }

    public Map<String, Ticket> getActiveTickets() {
        return activeTickets;
    }
}

// ============================================================
// 11. DEMO
// ============================================================
public class Main {

    public static void main(String[] args) {
        SimulatedClock clock = new SimulatedClock(LocalDateTime.of(2026, 1, 1, 9, 0));

        ParkingLot lot = new ParkingLot("City Center Lot");
        lot.addFloor(new Floor(1))
                .addSpots(SpotType.MOTORCYCLE, 2, 1)
                .addSpots(SpotType.COMPACT, 2, 10)
                .addSpots(SpotType.HANDICAPPED, 1, 5)
                .addSpots(SpotType.LARGE, 1, 20);
        lot.addFloor(new Floor(2))
                .addSpots(SpotType.COMPACT, 2, 10)
                .addSpots(SpotType.LARGE, 1, 20);

        Map<SpotType, Integer> rates = new EnumMap<SpotType, Integer>(SpotType.class);
        rates.put(SpotType.MOTORCYCLE, 20);
        rates.put(SpotType.COMPACT, 40);
        rates.put(SpotType.HANDICAPPED, 40);
        rates.put(SpotType.LARGE, 80);

        FeeCalculator feeCalculator = new FeeCalculator(new HourlyPricingStrategy(rates, 15));
        ParkingService service = new ParkingService(lot, new NearestFirstSpotStrategy(),
                feeCalculator, clock);

        banner("SETUP");
        System.out.println("  Strategy: NEAREST_FIRST   Pricing: " + feeCalculator.strategyName()
                + " (15 min grace)");
        lot.printAvailability();

        // ---------------------------------------------------- happy path
        banner("HAPPY PATH — park three vehicles");
        Vehicle bike = new Vehicle("KA-01-BK-1111", VehicleType.MOTORCYCLE);
        Vehicle car = new Vehicle("KA-02-CR-2222", VehicleType.CAR);
        Vehicle bus = new Vehicle("KA-03-BS-3333", VehicleType.BUS);

        Ticket bikeTicket = service.park(bike);
        Ticket carTicket = service.park(car);
        Ticket busTicket = service.park(bus);
        System.out.println("  " + bikeTicket);
        System.out.println("  " + carTicket);
        System.out.println("  " + busTicket);
        lot.printAvailability();

        // ---------------------------------------------------- double-park guard
        banner("VERIFY — a claimed spot cannot be claimed twice");
        ParkingSpot carSpot = carTicket.getSpot();
        Vehicle intruder = new Vehicle("KA-09-XX-9999", VehicleType.CAR);
        System.out.println("  spot " + carSpot.getId() + " occupied by " + carSpot.getOccupant());
        System.out.println("  intruder assign(" + carSpot.getId() + ") -> "
                + carSpot.assign(intruder) + "   (expected false)");
        System.out.println("  occupant unchanged: " + carSpot.getOccupant());

        banner("VERIFY — same plate cannot hold two tickets");
        try {
            service.park(car);
        } catch (IllegalStateException e) {
            System.out.println("  rejected: " + e.getMessage());
        }

        banner("VERIFY — handicapped spot needs a permit");
        Vehicle permitted = new Vehicle("KA-04-CR-4444", VehicleType.CAR, true);
        System.out.println("  car without permit fits HANDICAPPED? "
                + SpotFitPolicy.canFit(car, SpotType.HANDICAPPED));
        System.out.println("  car with permit fits HANDICAPPED?    "
                + SpotFitPolicy.canFit(permitted, SpotType.HANDICAPPED));

        // ---------------------------------------------------- failure: lot full for buses
        banner("FAILURE PATH — no LARGE spot left for a second bus");
        Vehicle bus2 = new Vehicle("KA-05-BS-5555", VehicleType.BUS);
        Vehicle bus3 = new Vehicle("KA-06-BS-6666", VehicleType.BUS);
        Ticket bus2Ticket = service.park(bus2);
        System.out.println("  parked " + bus2 + " at " + bus2Ticket.getSpot().getId()
                + " (last LARGE spot)");
        try {
            service.park(bus3);
        } catch (IllegalStateException e) {
            System.out.println("  rejected: " + e.getMessage());
        }

        // ---------------------------------------------------- exit + pricing
        banner("EXIT — hourly pricing, spot is freed");
        clock.advanceMinutes(135);
        System.out.println("  clock advanced 135 min -> " + clock.now().toLocalTime());
        Receipt carReceipt = service.unpark(carTicket.getId());
        System.out.println("  " + carReceipt + "   (3 started hours x INR 40 = 120)");
        System.out.println("  spot " + carSpot.getId() + " free again? " + carSpot.isFree());

        System.out.println("  re-parking the intruder into the freed spot...");
        Ticket intruderTicket = service.park(intruder);
        System.out.println("  " + intruderTicket);

        banner("EXIT — grace window keeps a short stay free");
        clock.advanceMinutes(0);
        Vehicle quickBike = new Vehicle("KA-07-BK-7777", VehicleType.MOTORCYCLE);
        Ticket quickTicket = service.park(quickBike);
        clock.advanceMinutes(10);
        System.out.println("  " + service.unpark(quickTicket.getId()) + "   (<= 15 min grace)");

        banner("FAILURE PATH — reusing a settled ticket");
        try {
            service.unpark(carTicket.getId());
        } catch (IllegalArgumentException e) {
            System.out.println("  rejected: " + e.getMessage());
        }

        // ---------------------------------------------------- strategy swap
        banner("STRATEGY SWAP — BEST_FIT keeps big bays for big vehicles");
        service.unpark(bikeTicket.getId());
        service.setAssignmentStrategy(new BestFitSpotStrategy());
        Vehicle bike2 = new Vehicle("KA-08-BK-8888", VehicleType.MOTORCYCLE);
        System.out.println("  BEST_FIT parks a bike at "
                + service.park(bike2).getSpot() + " (motorcycle bay, not a car bay)");

        feeCalculator.setStrategy(new FlatRatePricingStrategy(150));
        System.out.println("  pricing switched to " + feeCalculator.strategyName());
        clock.advanceMinutes(20);
        System.out.println("  " + service.unpark(busTicket.getId()) + "   (flat, duration ignored)");

        banner("FINAL STATE");
        lot.printAvailability();
        System.out.println("  active tickets: " + service.getActiveTickets().keySet());
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
