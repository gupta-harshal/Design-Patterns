# Decorator Design Pattern Implementation

A clean, production-grade Java implementation demonstrating the **Decorator Design Pattern**. This project models a flexible **Beverage Customizer System** showing how to attach new operational responsibilities (condiments) to an object dynamically at runtime without exploding the class inheritance tree.

## 📌 Overview

The Decorator Pattern is a structural design pattern that lets you attach new behaviors to objects by placing these objects inside special wrapper objects that contain the behaviors. It provides a flexible alternative to subclassing for extending functionality.

### 📐 Class Architecture Diagram

The structural wrapping mechanism is visualized below using Mermaid.js:

```mermaid
classDiagram
    class Coffee {
        <<interface>>
        +getDescription() String
        +getCost() double
    }

    class PlainCoffee {
        +getDescription() String
        +getCost() double
    }

    class CoffeeDecorator {
        <<abstract>>
        #decoratedCoffee: Coffee
        +CoffeeDecorator(coffee: Coffee)
        +getDescription() String
        +getCost() double
    }

    class MilkDecorator {
        +MilkDecorator(coffee: Coffee)
        +getDescription() String
        +getCost() double
    }

    class MochaDecorator {
        +MochaDecorator(coffee: Coffee)
        +getDescription() String
        +getCost() double
    }

    PlainCoffee ..|> Coffee : Implements (Component)
    CoffeeDecorator ..|> Coffee : Implements (Allows Nesting)
    CoffeeDecorator --> Coffee : Wraps (Composition Relationship)
    MilkDecorator --|> CoffeeDecorator : Extends (Concrete Decorator)
    MochaDecorator --|> CoffeeDecorator : Extends (Concrete Decorator)