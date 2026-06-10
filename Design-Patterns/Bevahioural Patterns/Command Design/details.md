# Command Design Pattern in Java

A clean, production-ready implementation of the **Command Design Pattern** in Java, modeled around an extensible smart home remote control system.

---

## 📌 Overview

The **Command Pattern** is a behavioral design pattern that turns a request into a **stand-alone object** containing all information about the request. 

This conversion allows you to parameterize clients with different requests, queue or log requests, and support undoable operations. By wrapping business logic requests inside individual objects, you decouple the object invoking the operation from the object that actually knows how to execute it.

---

## 🏗️ Architecture Design

The pattern isolates the components of a request into four distinct roles: the **Client**, the **Invoker**, the **Command**, and the **Receiver**. Here is how the system is structured:

```mermaid
classDiagram
    class Command {
        <<interface>>
        +execute() void
        +undo() void
    }

    class LightOnCommand {
        -SmartLight light
        +execute() void
        +undo() void
    }

    class LightOffCommand {
        -SmartLight light
        +execute() void
        +undo() void
    }

    class SmartLight {
        +turnOn() void
        +turnOff() void
    }

    class RemoteControl {
        -Command slot
        -Command lastCommand
        +setCommand(Command c) void
        +pressButton() void
        +pressUndo() void
    }

    LightOnCommand ..|> Command : Implements
    LightOffCommand ..|> Command : Implements
    LightOnCommand --> SmartLight : Aggregates (Receiver)
    LightOffCommand --> SmartLight : Aggregates (Receiver)
    RemoteControl --> Command : Uses (Invoker)