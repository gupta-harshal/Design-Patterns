// 1. COMPONENT INTERFACE (The base blueprint for core objects and decorators)
interface Coffee {
    String getDescription();
    double getCost();
}

// 2. CONCRETE COMPONENT (The basic object that can have responsibilities added to it)
class PlainCoffee implements Coffee {
    @Override
    public String getDescription() {
        return "Plain Coffee";
    }

    @Override
    public double getCost() {
        return 50.0; // Base Price in INR
    }
}

// 3. BASE DECORATOR (Implements Component and wraps a Component instance via Composition)
abstract class CoffeeDecorator implements Coffee {
    protected Coffee decoratedCoffee; // The wrapped object

    public CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription();
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost();
    }
}

// 4. CONCRETE DECORATORS (Adding custom features/behaviors dynamically)
class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + ", with Milk";
    }

    @Override
    public double getCost() {
        return super.getCost() + 15.0; // Adds price of milk
    }
}

class MochaDecorator extends CoffeeDecorator {
    public MochaDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + ", with Mocha (Chocolate)";
    }

    @Override
    public double getCost() {
        return super.getCost() + 25.0; // Adds price of mocha
    }
}

// 5. CLIENT CODE
public class Main {
    public static void main(String[] args) {
        // Step A: Order a plain coffee
        Coffee myCoffee = new PlainCoffee();
        System.out.println("Order 1: " + myCoffee.getDescription() + " | Cost: ₹" + myCoffee.getCost());

        // Step B: Wrap it with Milk at runtime
        myCoffee = new MilkDecorator(myCoffee);
        System.out.println("Order 2: " + myCoffee.getDescription() + " | Cost: ₹" + myCoffee.getCost());

        // Step C: Wrap it again with Mocha chocolate layer
        myCoffee = new MochaDecorator(myCoffee);
        System.out.println("Order 3: " + myCoffee.getDescription() + " | Cost: ₹" + myCoffee.getCost());
        
        // Notice how we didn't create a complex "PlainCoffeeWithMilkAndMocha" class!
    }
}