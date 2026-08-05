import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class Product {
    final String code;
    final String name;
    final int priceCents;
    int quantity;

    Product(String code, String name, int priceCents, int quantity) {
        this.code = code;
        this.name = name;
        this.priceCents = priceCents;
        this.quantity = quantity;
    }
}

interface VendingState {
    void insertCoin(VendingMachine machine, int amountCents);
    void selectProduct(VendingMachine machine, String code);
    void cancel(VendingMachine machine);
}

class IdleState implements VendingState {
    @Override
    public void insertCoin(VendingMachine machine, int amountCents) {
        if (amountCents <= 0) {
            System.out.println("Rejected: non-positive coin");
            return;
        }
        machine.addBalance(amountCents);
        machine.setState(machine.getHasMoneyState());
        System.out.println("Balance: " + machine.getBalanceCents() + " cents");
    }

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        System.out.println("Insert money first");
    }

    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("Nothing to cancel");
    }
}

class HasMoneyState implements VendingState {
    @Override
    public void insertCoin(VendingMachine machine, int amountCents) {
        if (amountCents <= 0) {
            System.out.println("Rejected: non-positive coin");
            return;
        }
        machine.addBalance(amountCents);
        System.out.println("Balance: " + machine.getBalanceCents() + " cents");
    }

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        Product p = machine.getProduct(code);
        if (p == null) {
            System.out.println("Unknown product: " + code);
            return;
        }
        if (p.quantity <= 0) {
            System.out.println(p.name + " is sold out");
            return;
        }
        if (machine.getBalanceCents() < p.priceCents) {
            System.out.println("Need " + (p.priceCents - machine.getBalanceCents()) + " more cents");
            return;
        }
        machine.setSelected(p);
        machine.setState(machine.getDispensingState());
        machine.completeDispense();
    }

    @Override
    public void cancel(VendingMachine machine) {
        int refund = machine.getBalanceCents();
        machine.clearBalance();
        machine.setState(machine.getIdleState());
        System.out.println("Refunded " + refund + " cents");
    }
}

class DispensingState implements VendingState {
    @Override
    public void insertCoin(VendingMachine machine, int amountCents) {
        System.out.println("Busy dispensing");
    }

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        System.out.println("Busy dispensing");
    }

    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("Too late to cancel");
    }
}

class VendingMachine {
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final VendingState idleState = new IdleState();
    private final VendingState hasMoneyState = new HasMoneyState();
    private final VendingState dispensingState = new DispensingState();
    private VendingState state = idleState;
    private int balanceCents;
    private Product selected;

    void addProduct(Product p) {
        products.put(p.code, p);
    }

    void setState(VendingState state) { this.state = state; }
    VendingState getIdleState() { return idleState; }
    VendingState getHasMoneyState() { return hasMoneyState; }
    VendingState getDispensingState() { return dispensingState; }

    void addBalance(int cents) { balanceCents += cents; }
    int getBalanceCents() { return balanceCents; }
    void clearBalance() { balanceCents = 0; }

    Product getProduct(String code) { return products.get(code); }
    void setSelected(Product p) { selected = p; }

    void insertCoin(int amountCents) { state.insertCoin(this, amountCents); }
    void selectProduct(String code) { state.selectProduct(this, code); }
    void cancel() { state.cancel(this); }

    void completeDispense() {
        Product p = selected;
        p.quantity--;
        int change = balanceCents - p.priceCents;
        balanceCents = 0;
        selected = null;
        state = idleState;
        System.out.println("Dispensed: " + p.name + " | change: " + change + " cents | left: " + p.quantity);
    }

    void printMenu() {
        System.out.println("Menu:");
        for (Product p : products.values()) {
            System.out.println("  " + p.code + " " + p.name + " " + p.priceCents + "c qty=" + p.quantity);
        }
    }

    Map<String, Product> snapshot() {
        return Collections.unmodifiableMap(products);
    }
}

public class Main {
    public static void main(String[] args) {
        VendingMachine vm = new VendingMachine();
        vm.addProduct(new Product("A1", "Chips", 150, 2));
        vm.addProduct(new Product("B2", "Soda", 125, 1));
        vm.printMenu();

        System.out.println("\n-- Happy path --");
        vm.insertCoin(100);
        vm.insertCoin(50);
        vm.selectProduct("A1"); // exact 150

        System.out.println("\n-- Insufficient funds --");
        vm.insertCoin(100);
        vm.selectProduct("B2"); // needs 125
        vm.insertCoin(50);
        vm.selectProduct("B2"); // change 25

        System.out.println("\n-- Sold out + cancel --");
        vm.insertCoin(200);
        vm.selectProduct("B2"); // sold out
        vm.cancel();

        System.out.println("\n-- Select without money --");
        vm.selectProduct("A1");
    }
}
