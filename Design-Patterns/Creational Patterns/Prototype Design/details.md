# NPC Spawning Engine (Prototype Design Pattern)

A clean, production-ready Java implementation demonstrating the **Prototype Design Pattern** modeled after game engine asset-loading and entity-spawning systems.

This project illustrates how to create new objects by cloning an existing configured instance (the prototype) rather than using the `new` keyword, effectively skipping expensive asset-loading computations and bypassing subclass coupling.

---

## Architecture Overview

Instead of hitting the disk storage or database to reload graphic meshes, textures, and hitboxes every time an NPC spawns, the system creates a single "Base Prototype" instance. When duplicates are required, the client simply calls `.clone()`. 

The concrete object handles its own cloning logic via a private constructor to perform a **Deep Copy**, ensuring that mutable nested objects (like ability lists) are completely independent.

### System Class Diagram (Mermaid)

```mermaid
classDiagram
    direction TB
    class Prototype {
        <<interface>>
        +clone() Prototype*
    }
    
    class Monster {
        -String type
        -int health
        -String weapon
        -List~String~ specialAbilities
        +Monster(String, int, String, List)
        -Monster(Monster target)
        +clone() Prototype
        +setHealth(int health)
        +setWeapon(String weapon)
        +getSpecialAbilities() List~String~
        +toString() String
    }
    
    class Main {
        +main(String[] args)$
    }

    Monster ..|> Prototype : implements
    Main --> Prototype : client uses
    Monster --> Monster : instantiates copy via private constructor