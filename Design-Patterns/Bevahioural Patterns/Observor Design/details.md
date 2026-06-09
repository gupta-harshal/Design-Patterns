# Observer Design Pattern in Java

A clean, production-ready implementation of the **Observer Design Pattern** in Java, modeled around a YouTube Channel and its Subscribers notification engine.

---

## 📌 Overview

The **Observer Pattern** is a behavioral design pattern that defines a **one-to-many dependency** between objects. When the state of one object (the **Subject**) changes, all its dependents (**Observers**) are notified and updated automatically.

This framework shifts your architecture from a wasteful **polling mechanism** (where dependencies constantly ask if an update is ready) to an efficient **push architecture** (where updates are broadcasted only when they occur).

---

## 🏗️ Architecture Design

The interaction between the components is decoupled through abstractions (`Subject` and `Observer` interfaces). Here is how the system is structured:

```mermaid
classDiagram
    class Subject {
        <<interface>>
        +registerObserver(Observer o) void
        +removeObserver(Observer o) void
        +notifyObservers() void
    }

    class Observer {
        <<interface>>
        +update(String eventData) void
    }

    class YoutubeChannel {
        -List<Observer> subscribers
        -String channelName
        -String latestVideoTitle
        +uploadVideo(String title) void
        +registerObserver(Observer o) void
        +removeObserver(Observer o) void
        +notifyObservers() void
    }

    class User {
        -String name
        +update(String videoTitle) void
    }

    Subject ..> Observer : Notifies
    YoutubeChannel ..|> Subject : Implements
    User ..|> Observer : Implements
    YoutubeChannel --> Observer : Maintains List of