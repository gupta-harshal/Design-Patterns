import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

enum Direction {
    UP, DOWN, IDLE
}

enum ElevatorState {
    IDLE, MOVING, DOOR_OPEN, MAINTENANCE
}

class HallRequest {
    final int floor;
    final Direction direction; // UP or DOWN only

    HallRequest(int floor, Direction direction) {
        if (direction == Direction.IDLE) {
            throw new IllegalArgumentException("Hall call must be UP or DOWN");
        }
        this.floor = floor;
        this.direction = direction;
    }
}

class Elevator {
    private final int id;
    private final int minFloor;
    private final int maxFloor;
    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private ElevatorState state = ElevatorState.IDLE;
    // SCAN: ascending targets while going up; descending while going down
    private final NavigableSet<Integer> upTargets = new TreeSet<>();
    private final NavigableSet<Integer> downTargets = new TreeSet<>();

    Elevator(int id, int minFloor, int maxFloor, int startFloor) {
        this.id = id;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.currentFloor = startFloor;
    }

    int getId() { return id; }
    int getCurrentFloor() { return currentFloor; }
    Direction getDirection() { return direction; }
    ElevatorState getState() { return state; }

    boolean isIdle() {
        return state != ElevatorState.MAINTENANCE && upTargets.isEmpty() && downTargets.isEmpty();
    }

    void takeOffline() {
        state = ElevatorState.MAINTENANCE;
        upTargets.clear();
        downTargets.clear();
        direction = Direction.IDLE;
    }

    void restore() {
        if (state == ElevatorState.MAINTENANCE) {
            state = ElevatorState.IDLE;
        }
    }

    void addDestination(int floor) {
        if (state == ElevatorState.MAINTENANCE) {
            return;
        }
        if (floor < minFloor || floor > maxFloor) {
            throw new IllegalArgumentException("Floor out of range: " + floor);
        }
        if (floor == currentFloor) {
            state = ElevatorState.DOOR_OPEN;
            return;
        }
        if (floor > currentFloor) {
            upTargets.add(floor);
        } else {
            downTargets.add(floor);
        }
        if (direction == Direction.IDLE) {
            direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
            state = ElevatorState.MOVING;
        }
    }

    /** Advance one simulation tick: move one floor or close door and pick next. */
    void step() {
        if (state == ElevatorState.MAINTENANCE) {
            return;
        }
        if (state == ElevatorState.DOOR_OPEN) {
            // Door closes; decide next direction
            if (!upTargets.isEmpty() && (direction == Direction.UP || direction == Direction.IDLE || downTargets.isEmpty())) {
                direction = Direction.UP;
                state = ElevatorState.MOVING;
            } else if (!downTargets.isEmpty()) {
                direction = Direction.DOWN;
                state = ElevatorState.MOVING;
            } else if (!upTargets.isEmpty()) {
                direction = Direction.UP;
                state = ElevatorState.MOVING;
            } else {
                direction = Direction.IDLE;
                state = ElevatorState.IDLE;
            }
            return;
        }

        if (direction == Direction.UP) {
            if (upTargets.isEmpty()) {
                if (!downTargets.isEmpty()) {
                    direction = Direction.DOWN;
                } else {
                    direction = Direction.IDLE;
                    state = ElevatorState.IDLE;
                }
                return;
            }
            currentFloor++;
            state = ElevatorState.MOVING;
            if (upTargets.contains(currentFloor)) {
                upTargets.remove(currentFloor);
                state = ElevatorState.DOOR_OPEN;
                System.out.println("  Elevator#" + id + " opened at " + currentFloor + " (UP queue)");
            }
        } else if (direction == Direction.DOWN) {
            if (downTargets.isEmpty()) {
                if (!upTargets.isEmpty()) {
                    direction = Direction.UP;
                } else {
                    direction = Direction.IDLE;
                    state = ElevatorState.IDLE;
                }
                return;
            }
            currentFloor--;
            state = ElevatorState.MOVING;
            if (downTargets.contains(currentFloor)) {
                downTargets.remove(currentFloor);
                state = ElevatorState.DOOR_OPEN;
                System.out.println("  Elevator#" + id + " opened at " + currentFloor + " (DOWN queue)");
            }
        }
    }

    /** Rough cost for dispatch: lower is better. Integer.MAX_VALUE = skip. */
    int estimateCost(HallRequest req) {
        if (state == ElevatorState.MAINTENANCE) {
            return Integer.MAX_VALUE;
        }
        int floor = req.floor;
        if (isIdle()) {
            return Math.abs(currentFloor - floor);
        }
        // Prefer elevators already moving toward the caller in the same direction
        if (direction == Direction.UP && req.direction == Direction.UP && floor >= currentFloor) {
            return floor - currentFloor;
        }
        if (direction == Direction.DOWN && req.direction == Direction.DOWN && floor <= currentFloor) {
            return currentFloor - floor;
        }
        // Moving away / opposite — pessimistic cost
        return Math.abs(currentFloor - floor) + (maxFloor - minFloor);
    }

    @Override
    public String toString() {
        return "Elevator#" + id + "{floor=" + currentFloor + ", dir=" + direction
                + ", state=" + state + ", up=" + upTargets + ", down=" + downTargets + "}";
    }
}

interface DispatchStrategy {
    Elevator choose(List<Elevator> elevators, HallRequest request);
}

class NearestSuitableStrategy implements DispatchStrategy {
    @Override
    public Elevator choose(List<Elevator> elevators, HallRequest request) {
        Elevator best = null;
        int bestCost = Integer.MAX_VALUE;
        for (Elevator e : elevators) {
            int cost = e.estimateCost(request);
            if (cost < bestCost) {
                bestCost = cost;
                best = e;
            }
        }
        return best;
    }
}

class ElevatorController {
    private final List<Elevator> elevators = new ArrayList<>();
    private final DispatchStrategy strategy;

    ElevatorController(DispatchStrategy strategy) {
        this.strategy = strategy;
    }

    void addElevator(Elevator e) {
        elevators.add(e);
    }

    void requestHall(int floor, Direction dir) {
        HallRequest req = new HallRequest(floor, dir);
        Elevator chosen = strategy.choose(elevators, req);
        if (chosen == null) {
            System.out.println("No elevator available for hall " + floor + " " + dir);
            return;
        }
        System.out.println("Hall " + dir + "@" + floor + " -> Elevator#" + chosen.getId());
        chosen.addDestination(floor);
    }

    void requestCabin(int elevatorId, int floor) {
        for (Elevator e : elevators) {
            if (e.getId() == elevatorId) {
                System.out.println("Cabin Elevator#" + elevatorId + " -> " + floor);
                e.addDestination(floor);
                return;
            }
        }
        throw new IllegalArgumentException("Unknown elevator " + elevatorId);
    }

    void tick() {
        for (Elevator e : elevators) {
            e.step();
        }
    }

    void printStatus() {
        for (Elevator e : elevators) {
            System.out.println("  " + e);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        ElevatorController controller = new ElevatorController(new NearestSuitableStrategy());
        controller.addElevator(new Elevator(1, 0, 10, 0));
        controller.addElevator(new Elevator(2, 0, 10, 5));

        System.out.println("=== Initial ===");
        controller.printStatus();

        controller.requestHall(3, Direction.UP);
        controller.requestCabin(1, 8);
        controller.requestHall(9, Direction.DOWN);

        for (int t = 1; t <= 20; t++) {
            System.out.println("-- tick " + t + " --");
            controller.tick();
            controller.printStatus();
            boolean allIdle = true;
            // stop early if quiet (simple demo)
        }
        System.out.println("=== Done ===");
    }
}
