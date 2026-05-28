import java.util.ArrayList;
import java.util.List;

// THE PROTOTYPE INTERFACE
interface Prototype {
    Prototype clone(); 
}

// CONCRETE PROTOTYPE
class Monster implements Prototype {
    private String type;
    private int health;
    private String weapon;
    private List<String> specialAbilities; // Reference type to demonstrate deep copy

    // Standard Constructor (Simulating an expensive operational load)
    public Monster(String type, int health, String weapon, List<String> abilities) {
        this.type = type;
        this.health = health;
        this.weapon = weapon;
        this.specialAbilities = abilities;
        System.out.println("⏳ Expensive Loading: Graphics assets and hitboxes loaded for " + type);
    }

    // Private Constructor used specifically for Cloning (Handles Deep Copy)
    private Monster(Monster target) {
        if (target != null) {
            this.type = target.type;
            this.health = target.health;
            this.weapon = target.weapon;
            
            // DEEP COPY: Create a brand new list instead of copying the reference pointer
            this.specialAbilities = new ArrayList<>(target.specialAbilities);
        }
    }

    // Implementing the clone contract
    @Override
    public Prototype clone() {
        return new Monster(this); 
    }
    public void setHealth(int health) { this.health = health; }
    public void setWeapon(String weapon) { this.weapon = weapon; }
    public List<String> getSpecialAbilities() { return this.specialAbilities; }

    @Override
    public String toString() {
        return "Monster [Type=" + type + ", Health=" + health + ", Weapon=" + weapon + ", Abilities=" + specialAbilities + "]";
    }
}

// CLIENT CODE & EXECUTION
public class Main {
    public static void main(String[] args) {
        // Step 1: Create the baseline base prototype (Costs heavy processing time)
        List<String> orcAbilities = new ArrayList<>();
        orcAbilities.add("Night Vision");
        orcAbilities.add("Berserk Rage");
        
        Monster baseOrc = new Monster("Elite Orc", 200, "Battleaxe", orcAbilities);
        System.out.println("Original Base: " + baseOrc + "\n");

        // Step 2: Spawn an army of duplicates instantly using the Prototype pattern
        System.out.println("--- Spawning Clones (Zero asset-loading latency) ---");
        
        Monster orcSolider1 = (Monster) baseOrc.clone();
        orcSolider1.setHealth(150); // Tweak the state slightly for variety
        
        Monster orcCaptain = (Monster) baseOrc.clone();
        orcCaptain.setHealth(400);
        orcCaptain.setWeapon("Enchanted Greatsword");
        orcCaptain.getSpecialAbilities().add("Commanding Shout"); // Verifying Deep Copy

        // Step 3: Print states to verify independent objects
        System.out.println("Clone 1 (Soldier): " + orcSolider1);
        System.out.println("Clone 2 (Captain): " + orcCaptain);
        System.out.println("Original Base remains unaffected: " + baseOrc);
    }
}