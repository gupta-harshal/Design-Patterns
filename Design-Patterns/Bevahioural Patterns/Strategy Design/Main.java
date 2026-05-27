// THE STRATEGY INTERFACE
interface RouteStrategy {
    void buildRoute(String start, String end);
}

// CONCRETE STRATEGIES
class DrivingStrategy implements RouteStrategy {
    @Override
    public void buildRoute(String start, String end) {
        System.out.println("🚗 Driving: Found fastest route from " + start + " to " + end + " via highways factoring in real-time traffic.");
    }
}

class WalkingStrategy implements RouteStrategy {
    @Override
    public void buildRoute(String start, String end) {
        System.out.println("🚶 Walking: Found shortest pedestrian path from " + start + " to " + end + " utilizing sidewalks and crosswalks.");
    }
}

class TransitStrategy implements RouteStrategy {
    @Override
    public void buildRoute(String start, String end) {
        System.out.println("🚌 Transit: Found optimal route from " + start + " to " + end + " aligned with bus and subway timetables.");
    }
}

class CyclingStrategy implements RouteStrategy {
    @Override
    public void buildRoute(String start, String end) {
        System.out.println("🚲 Cycling: Found green route from " + start + " to " + end + " prioritizing dedicated bike lanes and avoiding steep hills.");
    }
}

// THE CONTEXT (THE APP CORE)
class Navigator {
    private RouteStrategy routeStrategy;

    // Dynamically swap the routing algorithm at runtime
    public void setStrategy(RouteStrategy routeStrategy) {
        this.routeStrategy = routeStrategy;
    }

    public void executeRouting(String start, String end) {
        if (routeStrategy == null) {
            System.out.println("❌ Error: Please select a travel mode first!");
            return;
        }
        // Delegate the computation to the active concrete strategy
        routeStrategy.buildRoute(start, end);
    }
}

// CLIENT EXECUTION
public class Main {
    public static void main(String[] args) {
        Navigator mapsApp = new Navigator();
        
        String startPoint = "Electronic City";
        String endPoint = "Indiranagar";
        
        System.out.println("--- Scenario 1: User wants to drive to work ---");
        mapsApp.setStrategy(new DrivingStrategy());
        mapsApp.executeRouting(startPoint, endPoint);
        
        System.out.println("\n--- Scenario 2: Heavy traffic detected! User switches to Public Transit ---");
        mapsApp.setStrategy(new TransitStrategy());
        mapsApp.executeRouting(startPoint, endPoint);
        
        System.out.println("\n--- Scenario 3: Weekend exploring, user chooses to cycle ---");
        mapsApp.setStrategy(new CyclingStrategy());
        mapsApp.executeRouting(startPoint, endPoint);
    }
}