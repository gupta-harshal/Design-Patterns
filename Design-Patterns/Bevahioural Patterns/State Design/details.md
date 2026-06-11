# State Design Pattern in Java

A clean, production-ready implementation of the **State Design Pattern** in Java, modeled around a contextual finite state machine for an automated vending system.

---

## 📌 Overview

The **State Pattern** is a behavioral design pattern used to cleanly encapsulate varying behaviors for the same object based on its changing internal state variables. 

Instead of building massive, unmaintainable conditional matrices (`if-else` or `switch` statements) that grow continuously as features expand, the pattern extracts state-specific rules into dedicated standalone State classes. The original context object delegates execution routines dynamically to whatever State instance is currently active.

---

## 🏗️ Architecture Design

The interaction isolates localized mutation rules through strict state interfaces:

```mermaid
classDiagram
    class State {
        <<interface>>
        +insertQuarter(VendingMachine vm) void
        +ejectQuarter(VendingMachine vm) void
        +turnCrank(VendingMachine vm) void
    }

    class NoQuarterState {
        +insertQuarter(VendingMachine vm) void
        +ejectQuarter(VendingMachine vm) void
        +turnCrank(VendingMachine vm) void
    }

    class HasQuarterState {
        +insertQuarter(VendingMachine vm) void
        +ejectQuarter(VendingMachine vm) void
        +turnCrank(VendingMachine vm) void
    }

    class VendingMachine {
        -State currentState
        -State noQuarterState
        -State hasQuarterState
        +setState(State s) void
        +insertQuarter() void
        +turnCrank() void
    }

    NoQuarterState ..|> State : Implements
    HasQuarterState ..|> State : Implements
    VendingMachine --> State : Aggregates Current Target