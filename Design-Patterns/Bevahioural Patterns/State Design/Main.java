// 1. The State Interface (Defines actions available across all states)
interface State {
    void insertQuarter(VendingMachine machine);
    void ejectQuarter(VendingMachine machine);
    void turnCrank(VendingMachine machine);
}

// 2. The Context Class (The primary object whose behavior changes)
class VendingMachine {
    private State noQuarterState;
    private State hasQuarterState;
    private State soldState;

    private State currentState;
    private int snackCount = 0;

    public VendingMachine(int snackCount) {
        this.noQuarterState = new NoQuarterState();
        this.hasQuarterState = new HasQuarterState();
        this.soldState = new SoldState();

        this.snackCount = snackCount;
        if (snackCount > 0) {
            this.currentState = noQuarterState;
        } else {
            this.currentState = soldState; // Out of stock initially
        }
    }

    public void setState(State state) {
        this.currentState = state;
    }

    public void insertQuarter() {
        currentState.insertQuarter(this);
    }

    public void ejectQuarter() {
        currentState.ejectQuarter(this);
    }

    public void turnCrank() {
        currentState.turnCrank(this);
    }

    public void releaseSnack() {
        if (snackCount > 0) {
            System.out.println("A snack rolls out of the slot...");
            snackCount--;
        }
    }

    public int getSnackCount() {
        return snackCount;
    }

    public State getNoQuarterState() { return noQuarterState; }
    public State getHasQuarterState() { return hasQuarterState; }
    public State getSoldState() { return soldState; }
}

// 3. Concrete State A: No Quarter Present
class NoQuarterState implements State {
    @Override
    public void insertQuarter(VendingMachine machine) {
        System.out.println("You inserted a quarter.");
        machine.setState(machine.getHasQuarterState()); // State Transition
    }

    @Override
    public void ejectQuarter(VendingMachine machine) {
        System.out.println("You haven't inserted a quarter.");
    }

    @Override
    public void turnCrank(VendingMachine machine) {
        System.out.println("You turned, but there's no quarter.");
    }
}

// 4. Concrete State B: Quarter Present
class HasQuarterState implements State {
    @Override
    public void insertQuarter(VendingMachine machine) {
        System.out.println("You can't insert another quarter.");
    }

    @Override
    public void ejectQuarter(VendingMachine machine) {
        System.out.println("Quarter returned.");
        machine.setState(machine.getNoQuarterState()); // State Transition
    }

    @Override
    public void turnCrank(VendingMachine machine) {
        System.out.println("You turned the crank...");
        machine.setState(machine.getSoldState()); // State Transition
    }
}

// 5. Concrete State C: Dispensing Item
class SoldState implements State {
    @Override
    public void insertQuarter(VendingMachine machine) {
        System.out.println("Please wait, we're already giving you a snack.");
    }

    @Override
    public void ejectQuarter(VendingMachine machine) {
        System.out.println("Sorry, you already turned the crank.");
    }

    @Override
    public void turnCrank(VendingMachine machine) {
        System.out.println("Turning twice doesn't get you another snack!");
    }
}

// 6. Execution Driver
public class Main {
    public static void main(String[] args) {
        VendingMachine machine = new VendingMachine(2);

        // Attempting to use the machine step-by-step
        machine.turnCrank();   // Fails: No money
        machine.insertQuarter(); // Success
        machine.turnCrank();   // Success -> Triggers Sold State
        
        // Context handles internal logic delegation safely
        machine.releaseSnack(); 
        machine.setState(machine.getNoQuarterState()); // Reset down to default loop
    }
}