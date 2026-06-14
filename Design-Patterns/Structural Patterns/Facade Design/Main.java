// 1. COMPLEX SUBSYSTEMS (Classes with varied, low-level operational methods)
class Amplifier {
    public void turnOn() { System.out.println("Amplifier: Powering ON."); }
    public void setVolume(int level) { System.out.println("Amplifier: Volume set to " + level); }
    public void turnOff() { System.out.println("Amplifier: Powering OFF."); }
}

class Projector {
    public void turnOn() { System.out.println("Projector: Powering ON."); }
    public void setWideScreenMode() { System.out.println("Projector: Screen aspect ratio set to 16:9."); }
    public void turnOff() { System.out.println("Projector: Powering OFF."); }
}

class SoundSystem {
    public void enableSurroundSound() { System.out.println("Sound System: Dolby Atmos Surround enabled."); }
}

class StreamingService {
    public void playMovie(String movie) { System.out.println("Streaming Service: Buffering and streaming '" + movie + "'..."); }
}

// 2. FACADE (The unified class that wraps complex subsystems into simple methods)
class HomeTheaterFacade {
    private Amplifier amp;
    private Projector projector;
    private SoundSystem sound;
    private StreamingService streaming;

    // Subsystems are passed into the facade via composition
    public HomeTheaterFacade(Amplifier amp, Projector projector, SoundSystem sound, StreamingService streaming) {
        this.amp = amp;
        this.projector = projector;
        this.sound = sound;
        this.streaming = streaming;
    }

    // A single, high-level method hiding massive internal multi-step logic
    public void watchMovie(String movie) {
        System.out.println("\n--- Initializing Movie Mode ---");
        projector.turnOn();
        projector.setWideScreenMode();
        amp.turnOn();
        amp.setVolume(12);
        sound.enableSurroundSound();
        streaming.playMovie(movie);
        System.out.println("--- System Ready. Enjoy your show! ---\n");
    }

    public void endMovie() {
        System.out.println("\n--- Shutting Down System ---");
        streaming.playMovie(""); // Stops streaming
        amp.turnOff();
        projector.turnOff();
        System.out.println("--- System Safely Powered Down ---\n");
    }
}

// 3. CLIENT
public class Main {
    public static void main(String[] args) {
        // Instantiate the complex subsystem parts
        Amplifier amp = new Amplifier();
        Projector projector = new Projector();
        SoundSystem sound = new SoundSystem();
        StreamingService streaming = new StreamingService();

        // Pass them into the Facade
        HomeTheaterFacade homeTheater = new HomeTheaterFacade(amp, projector, sound, streaming);

        // The client interacts only with the simplified Facade interface
        homeTheater.watchMovie("Inception");
        
        // Easily shut everything down with one call
        homeTheater.endMovie();
    }
}