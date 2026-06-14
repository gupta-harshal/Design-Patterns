# Flyweight Design Pattern Implementation

A clean, production-grade Java implementation demonstrating the **Flyweight Design Pattern**. This project models a high-performance **Gaming Particle System (Bullet Renderer)** optimized to eliminate high-volume memory overhead by extracting immutable state properties into shared cache pools.

## 📌 Overview

The Flyweight Design Pattern is a structural pattern that lets you fit more objects into the available amount of RAM by sharing common parts of state between multiple objects instead of keeping all of the data in each object.

### 📐 Class Architecture Diagram

The architectural boundary separating cached structural properties from transient execution states is visualized below via Mermaid.js:

```mermaid
classDiagram
    class Bullet {
        <<interface>>
        +render(x: int, y: int, speed: int) void
    }

    class ConcreteBulletType {
        -color: String
        -texture: String
        +ConcreteBulletType(color: String, texture: String)
        +render(x: int, y: int, speed: int) void
    }

    class BulletFactory {
        -bulletCache: Map~String, Bullet~
        +getBulletType(color: String, texture: String) Bullet
    }

    class Main {
        +main(args: String[]) void
    }

    ConcreteBulletType ..|> Bullet : Implements Intrinsic Contract
    BulletFactory --> Bullet : Manages & Caches
    Main ..> BulletFactory : Requests Flyweights From
    Main ..> Bullet : Provides Extrinsic Data To