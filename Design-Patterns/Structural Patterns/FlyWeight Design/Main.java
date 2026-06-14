import java.util.HashMap;
import java.util.Map;

// 1. FLYWEIGHT INTERFACE (Defines how the flyweight accepts extrinsic state)
interface Bullet {
    void render(int x, int y, int speed); // x, y, speed are extrinsic states
}

// 2. CONCRETE FLYWEIGHT (Stores intrinsic state: immutable, shared data)
class ConcreteBulletType implements Bullet {
    private final String color;
    private final String texture; // Representing a heavy 3D asset/image reference

    public ConcreteBulletType(String color, String texture) {
        this.color = color;
        this.texture = texture;
        // Simulate a heavy asset loading process once
        System.out.println("System: [Heavy Asset Loaded] Created bullet type with color " + color);
    }

    @Override
    public void render(int x, int y, int speed) {
        System.out.println("Rendering " + color + " bullet at (" + x + ", " + y + ") traveling at speed " + speed + "mph.");
    }
}

// 3. FLYWEIGHT FACTORY (Manages caching and reuse of Flyweight objects)
class BulletFactory {
    private static final Map<String, Bullet> bulletCache = new HashMap<>();

    public static Bullet getBulletType(String color, String texture) {
        String key = color + "_" + texture;
        
        // Reuse existing flyweight instance if available
        if (!bulletCache.containsKey(key)) {
            bulletCache.put(key, new ConcreteBulletType(color, texture));
        }
        return bulletCache.get(key);
    }
}

// 4. CLIENT (Maintains unique extrinsic coordinates and triggers operations)
public class Main {
    public static void main(String[] args) {
        // We want to fire 5 bullets, but we will only allocate memory for 2 distinct types!
        
        System.out.println("=== Simulation Initiated ===");
        
        // Player 1 fires 3 red bullets
        Bullet redBullet = BulletFactory.getBulletType("Red", "HighRes_Laser_Texture.png");
        redBullet.render(10, 20, 300);
        redBullet.render(12, 22, 300);
        redBullet.render(15, 25, 300);

        // Player 2 fires 2 blue armor-piercing bullets
        Bullet blueBullet = BulletFactory.getBulletType("Blue", "Heavy_AP_Texture.png");
        blueBullet.render(100, 200, 450);
        blueBullet.render(105, 205, 450);

        System.out.println("\n=== Memory Report ===");
        System.out.println("Total bullets fired: 5");
        System.out.println("Total distinct bullet objects stored in memory heap: " + bulletCache.size());
    }
    
    // Simulating access to factory cache size for display
    private static final Map<String, Bullet> bulletCache = BulletFactory.getBulletType("Red", "HighRes_Laser_Texture.png") != null ? reflectCache() : null;
    private static Map<String, Bullet> reflectCache() {
        try {
            java.lang.reflect.Field field = BulletFactory.class.getDeclaredField("bulletCache");
            field.setAccessible(true);
            return (Map<String, Bullet>) field.get(null);
        } catch(Exception e) { return new HashMap<>(); }
    }
}