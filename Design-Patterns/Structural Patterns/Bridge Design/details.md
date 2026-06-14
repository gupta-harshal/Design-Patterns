# Bridge Design Pattern Implementation

A clean, production-grade Java implementation demonstrating the **Bridge Design Pattern**. This project models a modular architecture separating high-level UI controls (`RemoteControl`) from low-level infrastructure hardware implementations (`Device`), decoupling them completely to avoid geometric class explosions.

## 📌 Overview

The Bridge Design Pattern is a structural pattern that lets you split a large class or a set of closely related classes into two separate hierarchies—Abstraction and Implementation—which can be developed independently of each other.

### 📐 Class Architecture Diagram

The structural decouple and connection via the aggregation "bridge" is mapped out below using Mermaid.js:

```mermaid
classDiagram
    class RemoteControl {
        #device: Device
        +RemoteControl(device: Device)
        +togglePower() void
        +volumeUp() void
    }

    class AdvancedRemoteControl {
        +AdvancedRemoteControl(device: Device)
        +mute() void
    }

    class Device {
        <<interface>>
        +isEnabled() boolean
        +enable() void
        +disable() void
        +getVolume() int
        +setVolume(percent: int) void
    }

    class TV {
        -on: boolean
        -volume: int
    }

    class Radio {
        -on: boolean
        -volume: int
    }

    AdvancedRemoteControl --|> RemoteControl : Extends (Abstraction Hierarchy)
    RemoteControl --> Device : Contains Bridge Link (Composition)
    TV ..|> Device : Implements (Implementation Hierarchy)
    Radio ..|> Device : Implements (Implementation Hierarchy)