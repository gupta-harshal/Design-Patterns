# Comprehensive Guide: Abstract Factory Design Pattern in Java

## 📖 Introduction to the Pattern

The **Abstract Factory** is a creational design pattern that allows you to produce families of related or dependent objects without specifying their concrete classes. 

Instead of instantiating individual products directly using a concrete factory class, the client code interacts with an **Abstract Factory interface**, which acts as a top-level contract declaration. This structure enforces a strict architectural boundary, ensuring that an application handles entire suites of matching variants natively without risk of cross-contamination (e.g., mixing components from different vendor libraries or operational environments).

---

## 🏗️ Structural Architecture

The pattern relies on creating a "Factory of Factories." Below is the Unified Modeling Language (UML) structural layout mapping out the structural associations between interfaces, concrete implementations, and the calling client.

```mermaid
graph TD
    classDef interface fill:#e1f5fe,stroke:#01579b,stroke-width:2px;
    classDef concrete fill:#fff3e0,stroke:#e65100,stroke-width:2px;
    classDef client fill:#f3e5f5,stroke:#4a148c,stroke-width:2px;

    %% Factory Hierarchy
    UIFactory["&laquo;interface&raquo;<br>UIFactory"]:::interface
    WindowsFactory["WindowsFactory"]:::concrete
    MacFactory["MacFactory"]:::concrete
    
    UIFactory --> WindowsFactory
    UIFactory --> MacFactory

    %% Button Hierarchy
    Button["&laquo;interface&raquo;<br>Button"]:::interface
    WindowsButton["WindowsButton"]:::concrete
    MacButton["MacButton"]:::concrete
    
    Button --> WindowsButton
    Button --> MacButton

    %% Checkbox Hierarchy
    Checkbox["&laquo;interface&raquo;<br>Checkbox"]:::interface
    WindowsCheckbox["WindowsCheckbox"]:::concrete
    MacCheckbox["MacCheckbox"]:::concrete
    
    Checkbox --> WindowsCheckbox
    Checkbox --> MacCheckbox

    %% Client Relationship
    Application["Application"]:::client
    Main["Main (Driver)"]:::client

    Main --> UIFactory
    Main --> Application
    Application --> Button
    Application --> Checkbox
    
    WindowsFactory -.-> WindowsButton
    WindowsFactory -.-> WindowsCheckbox
    MacFactory -.-> MacButton
    MacFactory -.-> MacCheckbox
