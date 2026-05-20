# Comprehensive Guide: Abstract Factory Design Pattern in Java

## 📖 Introduction to the Pattern

The **Abstract Factory** is a creational design pattern that allows you to produce families of related or dependent objects without specifying their concrete classes. 

Instead of instantiating individual products directly using a concrete factory class, the client code interacts with an **Abstract Factory interface**, which acts as a top-level contract declaration. This structure enforces a strict architectural boundary, ensuring that an application handles entire suites of matching variants natively without risk of cross-contamination (e.g., mixing components from different vendor libraries or operational environments).

---

## 🗂️ Class Diagram

Below is the structured Unified Modeling Language (UML) Class Diagram showing the methods, properties, interfaces, and concrete associations within the system.

```mermaid
classDiagram
    %% Factory Hierarchy
    class UIFactory {
        <<interface>>
        +createButton() Button
        +createCheckbox() Checkbox
    }
    class WindowsFactory {
        +createButton() Button
        +createCheckbox() Checkbox
    }
    class MacFactory {
        +createButton() Button
        +createCheckbox() Checkbox
    }

    UIFactory <|.. WindowsFactory
    UIFactory <|.. MacFactory

    %% Button Hierarchy
    class Button {
        <<interface>>
        +render() void
    }
    class WindowsButton {
        +render() void
    }
    class MacButton {
        +render() void
    }

    Button <|.. WindowsButton
    Button <|.. MacButton

    %% Checkbox Hierarchy
    class Checkbox {
        <<interface>>
        +check() void
    }
    class WindowsCheckbox {
        +check() void
    }
    class MacCheckbox {
        +check() void
    }

    Checkbox <|.. WindowsCheckbox
    Checkbox <|.. MacCheckbox

    %% Client Layer
    class Application {
        -Button button
        -Checkbox checkbox
        +Application(UIFactory factory)
        +paint() void
    }

    class Main {
        +main(String[] args) static
    }

    %% Structural Creation Dependencies
    Main ..> UIFactory : Configures
    Main ..> Application : Instantiates
    Application --> Button : Uses
    Application --> Checkbox : Uses
    
    WindowsFactory ..> WindowsButton : Instantiates
    WindowsFactory ..> WindowsCheckbox : Instantiates
    MacFactory ..> MacButton : Instantiates
    MacFactory ..> MacCheckbox : Instantiates
