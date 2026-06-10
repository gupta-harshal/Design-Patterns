// 1. The Command Interface
interface Command {
    void execute();
    void undo(); // Command objects easily support undo/redo mechanics
}

// 2. The Receiver (The object that actually knows how to perform the work)
class SmartLight {
    public void turnOn() {
        System.out.println("The smart light is ON.");
    }
    public void turnOff() {
        System.out.println("The smart light is OFF.");
    }
}

// 3. Concrete Command 1: To turn on the light
class LightOnCommand implements Command {
    private SmartLight light;

    public LightOnCommand(SmartLight light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.turnOn();
    }

    @Override
    public void undo() {
        light.turnOff();
    }
}

// 4. Concrete Command 2: To turn off the light
class LightOffCommand implements Command {
    private SmartLight light;

    public LightOffCommand(SmartLight light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.turnOff();
    }

    @Override
    public void undo() {
        light.turnOn();
    }
}

// 5. The Invoker (The remote control that triggers the command)
class RemoteControl {
    private Command slot;
    private Command lastCommand; // Tracked for undo functionality

    public void setCommand(Command command) {
        this.slot = command;
    }

    public void pressButton() {
        if (slot != null) {
            slot.execute();
            lastCommand = slot;
        }
    }

    public void pressUndo() {
        if (lastCommand != null) {
            System.out.print("[Undo Action] -> ");
            lastCommand.undo();
        }
    }
}

// 6. Execution Driver
public class Main {
    public static void main(String[] args) {
        // Create the Receiver
        SmartLight livingRoomLight = new SmartLight();

        // Create the Commands and pass the Receiver to them
        Command lightOn = new LightOnCommand(livingRoomLight);
        Command lightOff = new LightOffCommand(livingRoomLight);

        // Create the Invoker
        RemoteControl remote = new RemoteControl();

        // Turn the light ON
        remote.setCommand(lightOn);
        remote.pressButton();

        // Turn the light OFF
        remote.setCommand(lightOff);
        remote.pressButton();

        // Undo the last action (Turns the light back ON)
        remote.pressUndo();
    }
}