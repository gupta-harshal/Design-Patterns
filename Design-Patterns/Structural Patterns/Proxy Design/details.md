# Proxy Design Pattern Implementation

A clean, production-grade Java implementation demonstrating the **Proxy Design Pattern**. This project models a **Corporate Network Firewall Proxy** that acts as an access control intermediary to intercept network connections, filtering blacklisted domains before passing traffic onto the concrete internal internet routing service.

## 📌 Overview

The Proxy Design Pattern is a structural pattern that lets you provide a substitute or placeholder for another object. A proxy controls access to the original object, allowing you to perform something either before or after the request gets through to the original object.

### 📐 Class Architecture Diagram

The structural intercept mechanics are mapped out below using Mermaid.js:

```mermaid
classDiagram
    class Internet {
        <<interface>>
        +connectTo(serverUrl: String) void
    }

    class RealInternet {
        +connectTo(serverUrl: String) void
    }

    class ProxyInternet {
        -realInternet: Internet
        -bannedSites: List~String~
        +connectTo(serverUrl: String) void
    }

    class Main {
        +main(args: String[]) void
    }

    RealInternet ..|> Internet : Implements Core Service
    ProxyInternet ..|> Internet : Implements Proxy Interface
    ProxyInternet --> RealInternet : Wraps & Controls (Composition)
    Main ..> Internet : Interacts via Common Contract