# Composite Design Pattern Implementation

A clean, production-grade Java implementation demonstrating the **Composite Design Pattern**. This project models a hierarchical **File System** tree structure where individual items (`File`) and container items (`Directory`) are manipulated seamlessly through a unified interface.

## 📌 Overview

The Composite Design Pattern is a structural pattern that allows you to compose objects into tree structures to represent part-whole hierarchies. It lets clients treat individual objects and compositions of objects uniformly.

### 📐 Class Architecture Diagram

The structural tree relationship of the pattern is mapped out below using Mermaid.js:

```mermaid
classDiagram
    class FileSystemComponent {
        <<interface>>
        +showDetails(indent: String) void
        +getSize() long
    }

    class File {
        -name: String
        -size: long
        +File(name: String, size: long)
        +showDetails(indent: String) void
        +getSize() long
    }

    class Directory {
        -name: String
        -components: List~FileSystemComponent~
        +Directory(name: String)
        +addComponent(component: FileSystemComponent) void
        +removeComponent(component: FileSystemComponent) void
        +showDetails(indent: String) void
        +getSize() long
    }

    class Main {
        +main(args: String[]) void
    }

    File ..|> FileSystemComponent : Implements (Leaf)
    Directory ..|> FileSystemComponent : Implements (Composite)
    Directory --> FileSystemComponent : Aggregates (1 to Many)
    Main ..> FileSystemComponent : Interacts with Uniformly