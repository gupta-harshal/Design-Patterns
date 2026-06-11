# Chain of Responsibility Design Pattern in Java

A clean, production-ready implementation of the **Chain of Responsibility Design Pattern** in Java, modeled around a decoupled support ticket escalation workflow.

---

## 📌 Overview

The **Chain of Responsibility** is a behavioral design pattern that decouples the sender of a request from its receivers by giving multiple objects a chance to handle the request. This pattern links the receiving objects together in a sequential chain, allowing the request to pass implicitly along the line until an eligible handler processes it.

Instead of hardcoding massive conditional logic blocks inside a single controller to route tasks, individual handlers are configured to focus strictly on a isolated criteria layer, creating highly modular, testable processing pipelines.

---

## 🏗️ Architecture Design

The interaction isolates localized processing steps by converting concrete processors into linked nodes:

```mermaid
classDiagram
    class SupportTicket {
        -String description
        -int severityLevel
        +getDescription() String
        +getSeverityLevel() int
    }

    class SupportHandler {
        <<abstract>>
        #SupportHandler nextHandler
        +setNextHandler(SupportHandler nh) void
        +handleRequest(SupportTicket t) void
        #canHandle(SupportTicket t)* boolean
        #process(SupportTicket t)* void
    }

    class AutomatedBotHandler {
        #canHandle(SupportTicket t) boolean
        #process(SupportTicket t) void
    }

    class Level1AgentHandler {
        #canHandle(SupportTicket t) boolean
        #process(SupportTicket t) void
    }

    AutomatedBotHandler --|> SupportHandler : Inherits
    Level1AgentHandler --|> SupportHandler : Inherits
    SupportHandler --> SupportHandler : References Next Node (Pointer)