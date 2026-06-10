# Mediator Design Pattern in Java

A clean, production-ready implementation of the **Mediator Design Pattern** in Java, modeled around a decoupled chatroom communication network.

---

## 📌 Overview

The **Mediator Pattern** is a behavioral design pattern that stops components from communicating directly with each other. Instead, components call a central mediator object, which routes the communication data to the proper destinations. 

This changes the system dependencies from a complex, interconnected **many-to-many mesh** network topology into a clean **one-to-many star** topology.

---

## 🏗️ Architecture Design

The pattern abstracts communication logic into a centralized router, isolating individual colleague nodes from knowing about the existence of neighboring nodes.

```mermaid
classDiagram
    class ChatMediator {
        <<interface>>
        +sendMessage(String msg, User u) void
        +addUser(User u) void
    }

    class ChatRoom {
        -List~User~ users
        +sendMessage(String msg, User u) void
        +addUser(User u) void
    }

    class User {
        <<abstract>>
        #ChatMediator mediator
        #String name
        +send(String msg)* void
        +receive(String msg)* void
    }

    class PremiumUser {
        +send(String msg) void
        +receive(String msg) void
    }

    class BasicUser {
        +send(String msg) void
        +receive(String msg) void
    }

    ChatRoom ..|> ChatMediator : Implements
    PremiumUser --|> User : Inherits
    BasicUser --|> User : Inherits
    User --> ChatMediator : Keeps Reference to
    ChatRoom --> User : Tracks List of