// 1. IMPLEMENTOR INTERFACE (The low-level platform-dependent interface)
interface Device {
    boolean isEnabled();
    void enable();
    void disable();
    int getVolume();
    void setVolume(int percent);
}

// 2. CONCRETE IMPLEMENTORS (The platform-specific hardware logic)
class TV implements Device {
    private boolean on = false;
    private int volume = 30;

    @Override public boolean isEnabled() { return on; }
    @Override public void enable() { on = true; System.out.println("TV: Powered ON"); }
    @Override public void disable() { on = false; System.out.println("TV: Powered OFF"); }
    @Override public int getVolume() { return volume; }
    @Override public void setVolume(int percent) { this.volume = percent; System.out.println("TV: Volume adjusted to " + percent + "%"); }
}

class Radio implements Device {
    private boolean on = false;
    private int volume = 15;

    @Override public boolean isEnabled() { return on; }
    @Override public void enable() { on = true; System.out.println("Radio: Powered ON"); }
    @Override public void disable() { on = false; System.out.println("Radio: Powered OFF"); }
    @Override public int getVolume() { return volume; }
    @Override public void setVolume(int percent) { this.volume = percent; System.out.println("Radio: Volume adjusted to " + percent + "%"); }
}

// 3. ABSTRACTION (The high-level control logic containing the "bridge" reference to an Implementor)
class RemoteControl {
    protected Device device; // The Bridge reference

    public RemoteControl(Device device) {
        this.device = device;
    }

    public void togglePower() {
        if (device.isEnabled()) {
            device.disable();
        } else {
            device.enable();
        }
    }

    public void volumeUp() {
        device.setVolume(device.getVolume() + 10);
    }
}

// 4. REFINED ABSTRACTION (An extended control interface without changing the hardware layer)
class AdvancedRemoteControl extends RemoteControl {
    public AdvancedRemoteControl(Device device) {
        super(device);
    }

    public void mute() {
        System.out.println("Advanced Remote: Activating Mute Mode");
        device.setVolume(0);
    }
}

// 5. CLIENT CODE
public class Main {
    public static void main(String[] args) {
        // Instantiate the hardware components
        Device sonyTV = new TV();
        Device phillipsRadio = new Radio();

        System.out.println("=== Testing Basic Remote with a TV ===");
        RemoteControl basicRemote = new RemoteControl(sonyTV);
        basicRemote.togglePower();
        basicRemote.volumeUp();

        System.out.println("\n=== Testing Advanced Remote with a Radio ===");
        AdvancedRemoteControl advancedRemote = new AdvancedRemoteControl(phillipsRadio);
        advancedRemote.togglePower();
        advancedRemote.volumeUp();
        advancedRemote.mute(); // Access specialized abstraction feature
    }
}