// 1. PRODUCT LAYER (Abstractions & Concrete)
// The unified interface all payment providers must implement
interface PaymentGateway {
    void processPayment(double amount);
}

// Concrete Product A
class StripeGateway implements PaymentGateway {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing $" + amount + " securely via Stripe API.");
    }
}

// Concrete Product B
class RazorpayGateway implements PaymentGateway {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing ₹" + amount + " securely via Razorpay API.");
    }
}


// 2. CREATOR LAYER (The Factory Method Setup)

// The abstract creator declaration containing the core factory method
interface GatewayFactory {
    // This is the official Factory Method
    PaymentGateway createGateway();
}

// Concrete Creator A - Responsible ONLY for Stripe instantiation
class StripeFactory implements GatewayFactory {
    @Override
    public PaymentGateway createGateway() {
        return new StripeGateway();
    }
}

// Concrete Creator B - Responsible ONLY for Razorpay instantiation
// We create this factory because we want to decouple the client code from the specific payment gateway implementations. This allows us to switch out payment providers without changing the business logic, adhering to the Open/Closed Principle.
class RazorpayFactory implements GatewayFactory {
    @Override
    public PaymentGateway createGateway() {
        return new RazorpayGateway();
    }
}


// 3. CLIENT RUNNER LAYER

// The core business service that uses our decoupled architecture
class OrderService {
    private final GatewayFactory gatewayFactory;

    // Dependency Injection: The service accepts ANY factory implementation
    public OrderService(GatewayFactory gatewayFactory) {
        this.gatewayFactory = gatewayFactory;
    }

    public void checkout(double amount) {
        // The business logic executes cleanly without knowing the concrete class
        PaymentGateway gateway = gatewayFactory.createGateway();
        gateway.processPayment(amount);
    }
}

// Main class execution block
public class Main {
    public static void main(String[] args) {
        System.out.println("--- Scenario 1: Customer chooses Razorpay ---");
        // 1. Create the specific factory
        GatewayFactory razorpayFactory = new RazorpayFactory();
        // 2. Inject it into the business service
        OrderService razorpayService = new OrderService(razorpayFactory);
        // 3. Process checkout
        razorpayService.checkout(1500.00);

        System.out.println("\n--- Scenario 2: System switches to Stripe ---");
        // The exact same client execution process, but with a different provider factory
        GatewayFactory stripeFactory = new StripeFactory();
        OrderService stripeService = new OrderService(stripeFactory);
        stripeService.checkout(85.50);
    }
}