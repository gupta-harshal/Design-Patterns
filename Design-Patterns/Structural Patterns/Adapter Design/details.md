# Adapter Design Pattern Implementation

A clean, production-grade Java implementation demonstrating the **Adapter Design Pattern**. This project models a real-world integration scenario where an application expecting JSON input needs to work seamlessly with a third-party XML analytics library without modification to either codebase.

## 📌 Overview

The Adapter Pattern is a structural design pattern that allows objects with incompatible interfaces to collaborate. It acts as a wrapper between two objects, catching calls for one object and transforming them into a format and interface recognizable by the second object.

### 📐 Class Architecture Diagram

The structural relationship between the Client, Target, Adapter, and Adaptee is mapped out below using Mermaid.js:

```mermaid
classDiagram
    class Client {
        +main(args: String[]) void
    }

    class JsonPrinter {
        <<interface>>
        +printJson(jsonData: String) void
    }

    class JsonToXmlAdapter {
        -xmlPrinter: AdvancedXmlPrinter
        +JsonToXmlAdapter(xmlPrinter: AdvancedXmlPrinter)
        +printJson(jsonData: String) void
    }

    class AdvancedXmlPrinter {
        +printXml(xmlData: String) void
    }

    Client ..> JsonPrinter : Uses
    JsonToXmlAdapter ..|> JsonPrinter : Implements
    JsonToXmlAdapter --> AdvancedXmlPrinter : Wraps (Composition)