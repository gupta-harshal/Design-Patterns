# Memento Design Pattern in Java

A clean, production-ready implementation of the **Memento Design Pattern** in Java, modeled around an undo-history transaction engine for text processing.

---

## 📌 Overview

The **Memento Pattern** is a behavioral design pattern used to capture and externalize an object's internal state configurations without violating object-oriented **encapsulation rules**. 

In standard object engineering, implementing a historical rollback function would force you to expose the private variables of an object to an external tracking module, breaking class boundaries. The Memento pattern resolves this by introducing a dual-interface wrapper object that protects snapshot states while allowing the creator object complete restoration rights.

---

## 🏗️ Architecture Design

The interaction divides data management tasks across three distinct roles:



```mermaid
classDiagram
    class EditorMemento {
        -String content
        #getContent() String
    }

    class TextEditor {
        -String content
        +type(String text) void
        +save() EditorMemento
        +restore(EditorMemento m) void
    }

    class HistoryTracker {
        -Stack~EditorMemento~ history
        +push(EditorMemento m) void
        +pop() EditorMemento
    }

    TextEditor ..> EditorMemento : Instantiates & Restores
    HistoryTracker --> EditorMemento : Aggregates History Stack