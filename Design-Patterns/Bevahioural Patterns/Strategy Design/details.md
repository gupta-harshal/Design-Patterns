# Navigation Routing Engine (Strategy Design Pattern)

A clean, production-ready Java implementation demonstrating the **Strategy Design Pattern** modeled after real-world routing engines like Google Maps or Uber. 

This project illustrates how to decouple a core application context from a family of interchangeable routing algorithms, fulfilling the **Open/Closed Principle** of object-oriented design by eliminating fragile conditional (`if/else`) blocks.

---

## Architecture Overview

Instead of bundling multiple travel mode logics inside a single class, this system breaks down each algorithm into its own isolated strategy class. The core client application (`Navigator`) maintains a reference to the generic strategy interface and delegates work to it dynamically at runtime.

### System Class Diagram (Mermaid)

```mermaid
classDiagram
    direction TB
    class Navigator {
        -RouteStrategy routeStrategy
        +setStrategy(RouteStrategy strategy)
        +executeRouting(String start, String end)
    }
    
    class RouteStrategy {
        <<interface>>
        +buildRoute(String start, String end)*
    }
    
    class DrivingStrategy {
        +buildRoute(String start, String end)
    }
    
    class WalkingStrategy {
        +buildRoute(String start, String end)
    }
    
    class TransitStrategy {
        +buildRoute(String start, String end)
    }
    
    class CyclingStrategy {
        +buildRoute(String start, String end)
    }

    Navigator --> RouteStrategy : composes
    DrivingStrategy ..|> RouteStrategy : implements
    WalkingStrategy ..|> RouteStrategy : implements
    TransitStrategy ..|> RouteStrategy : implements
    CyclingStrategy ..|> RouteStrategy : implements