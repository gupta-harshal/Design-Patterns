# Facade Design Pattern Implementation

A clean, production-grade Java implementation demonstrating the **Facade Design Pattern**. This project models a real-world **Home Theater System** configuration where multiple standalone, complex hardware subsystems are controlled through a single, highly simplified unified wrapper interface.

## 📌 Overview

The Facade Design Pattern is a structural pattern that provides a simplified interface to a library, a framework, or any other complex set of classes. It reduces coupling between clients and subsystems by introducing an entry-point layer that manages low-level system orchestrations.

### 📐 Class Architecture Diagram

The architectural separation between the Client, the Facade, and the hidden subsystems is illustrated below using Mermaid.js:

```mermaid
classDiagram
    class Client {
        +main(args: String[]) void
    }

    class HomeTheaterFacade {
        -amp: Amplifier
        -projector: Projector
        -sound: SoundSystem
        -streaming: StreamingService
        +HomeTheaterFacade(amp, projector, sound, streaming)
        +watchMovie(movie: String) void
        +endMovie() void
    }

    class Amplifier {
        +turnOn() void
        +setVolume(level: int) void
        +turnOff() void
    }

    class Projector {
        +turnOn() void
        +setWideScreenMode() void
        +turnOff() void
    }

    class SoundSystem {
        +enableSurroundSound() void
    }

    class StreamingService {
        +playMovie(movie: String) void
    }

    Client --> HomeTheaterFacade : Interacts Only With
    HomeTheaterFacade --> Amplifier : Orchestrates (Composition)
    HomeTheaterFacade --> Projector : Orchestrates (Composition)
    HomeTheaterFacade --> SoundSystem : Orchestrates (Composition)
    HomeTheaterFacade --> StreamingService : Orchestrates (Composition)