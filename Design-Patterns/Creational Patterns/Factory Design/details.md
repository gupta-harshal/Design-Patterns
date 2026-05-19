# Java Creational Design Patterns Repository 🚀

This repository is a dedicated, production-ready implementation of core **Creational Design Patterns** in Java. Creational patterns focus on abstracting the object instantiation process, ensuring that a system remains independent of how its objects are created, composed, and represented.

By decoupling creation logic from business logic, this codebase adheres to core architectural principles like **KISS** (Keep It Simple, Stupid), **DRY** (Don't Repeat Yourself), and the **Open/Closed Principle**.

---

## 📂 Repository Contents & Architecture

### 1. Simple Factory Structure
The Simple Factory pattern encapsulates conditional creation logic (`switch` or `if-else` blocks) into a single concrete class. It shields the client runner from having to explicitly call individual constructors.

* **Domain Example:** Vehicle Rental System (Car, Bike, Truck instantiation).
* **Location:** `Creational Patterns/Factory Design/Simple-Factory/`

#### 🏗️ Class Diagram
```mermaid
classDiagram
    class VehicleFactory {
        +createVehicle(String type) Static Vehicle
    }
    class Vehicle {
        <<interface>>
        +rent() void
    }
    class Car {
        +rent() void
    }
    class Bike {
        +rent() void
    }
    class Truck {
        +rent() void
    }

    VehicleFactory ..> Vehicle : Instantiates
    Vehicle <|.. Car
    Vehicle <|.. Bike
    Vehicle <|.. Truck
