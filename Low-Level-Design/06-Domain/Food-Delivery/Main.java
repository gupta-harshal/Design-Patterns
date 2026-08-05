import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class Location {
    final double x, y;
    Location(double x, double y) { this.x = x; this.y = y; }
    double distanceTo(Location o) {
        double dx = x - o.x, dy = y - o.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}

class MenuItem {
    final String id;
    final String name;
    final double price;
    int stock;

    MenuItem(String id, String name, double price, int stock) {
        this.id = id; this.name = name; this.price = price; this.stock = stock;
    }
}

class Restaurant {
    final String id;
    final String name;
    final Location location;
    final List<MenuItem> menu = new ArrayList<>();

    Restaurant(String id, String name, Location location) {
        this.id = id; this.name = name; this.location = location;
    }

    MenuItem find(String itemId) {
        for (MenuItem m : menu) if (m.id.equals(itemId)) return m;
        return null;
    }
}

class Customer {
    final String id;
    final String name;
    final Location location;
    Customer(String id, String name, Location location) {
        this.id = id; this.name = name; this.location = location;
    }
}

class DeliveryAgent {
    final String id;
    final String name;
    Location location;
    boolean available = true;
    DeliveryAgent(String id, String name, Location location) {
        this.id = id; this.name = name; this.location = location;
    }
}

enum OrderStatus {
    PLACED, ACCEPTED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
}

class OrderLine {
    final MenuItem item;
    final int qty;
    OrderLine(MenuItem item, int qty) { this.item = item; this.qty = qty; }
}

class Order {
    final String id = UUID.randomUUID().toString().substring(0, 8);
    final Customer customer;
    final Restaurant restaurant;
    final List<OrderLine> lines;
    final double total;
    OrderStatus status = OrderStatus.PLACED;
    DeliveryAgent agent;

    Order(Customer customer, Restaurant restaurant, List<OrderLine> lines, double total) {
        this.customer = customer;
        this.restaurant = restaurant;
        this.lines = lines;
        this.total = total;
    }
}

class DeliveryService {
    private final List<DeliveryAgent> agents = new ArrayList<>();

    void addAgent(DeliveryAgent a) { agents.add(a); }

    Order placeOrder(Customer customer, Restaurant restaurant, String itemId, int qty) {
        MenuItem item = restaurant.find(itemId);
        if (item == null || item.stock < qty) {
            System.out.println("Cannot place: stock/item issue");
            return null;
        }
        item.stock -= qty;
        List<OrderLine> lines = new ArrayList<>();
        lines.add(new OrderLine(item, qty));
        Order order = new Order(customer, restaurant, lines, item.price * qty);
        System.out.println("PLACED " + order.id + " total=" + order.total);
        return order;
    }

    private boolean transition(Order order, OrderStatus from, OrderStatus to) {
        if (order.status != from) {
            System.out.println("Illegal " + order.status + " -> " + to);
            return false;
        }
        order.status = to;
        System.out.println("Order " + order.id + " -> " + to);
        return true;
    }

    void accept(Order o) { transition(o, OrderStatus.PLACED, OrderStatus.ACCEPTED); }
    void preparing(Order o) { transition(o, OrderStatus.ACCEPTED, OrderStatus.PREPARING); }

    void outForDelivery(Order o) {
        if (o.status != OrderStatus.PREPARING) {
            System.out.println("Illegal " + o.status + " -> OUT_FOR_DELIVERY");
            return;
        }
        DeliveryAgent best = null;
        double bestDist = Double.MAX_VALUE;
        for (DeliveryAgent a : agents) {
            if (!a.available) continue;
            double d = a.location.distanceTo(o.restaurant.location);
            if (d < bestDist) { bestDist = d; best = a; }
        }
        if (best == null) {
            System.out.println("No agents available");
            return;
        }
        best.available = false;
        o.agent = best;
        o.status = OrderStatus.OUT_FOR_DELIVERY;
        System.out.println("Order " + o.id + " -> OUT_FOR_DELIVERY by " + best.name);
    }

    void deliver(Order o) {
        if (!transition(o, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)) return;
        o.agent.location = o.customer.location;
        o.agent.available = true;
    }
}

public class Main {
    public static void main(String[] args) {
        Restaurant r = new Restaurant("R1", "Spice Hub", new Location(0, 0));
        r.menu.add(new MenuItem("M1", "Paneer", 200, 5));
        Customer c = new Customer("C1", "Nisha", new Location(3, 4));
        DeliveryService svc = new DeliveryService();
        svc.addAgent(new DeliveryAgent("A1", "Ravi", new Location(1, 1)));
        svc.addAgent(new DeliveryAgent("A2", "Omar", new Location(10, 10)));

        Order order = svc.placeOrder(c, r, "M1", 2);
        svc.accept(order);
        svc.preparing(order);
        svc.outForDelivery(order);
        svc.deliver(order);

        System.out.println("\n-- Bad transition --");
        svc.accept(order);
    }
}
