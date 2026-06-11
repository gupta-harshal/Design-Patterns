# Visitor Design Pattern in Java

A clean, production-ready implementation of the **Visitor Design Pattern** in Java, modeled around an extensible multi-format media processing system.

---

## 📌 Overview

The **Visitor Pattern** is a behavioral design pattern that enables you to separate data structures from the operations or algorithms that act upon them. 

Instead of cluttering multiple structural data classes with unrelated business methodologies (e.g., placing export logic, statistics calculations, or rendering engines inside core data objects), the Visitor pattern extracts these operations into separate visitor classes. When a new operation is needed, you create a new visitor instance without altering the underlying target structure.

---

## 🏗️ Architecture Design

The interaction relies on an execution mechanism known as **Double Dispatch** to resolve polymorphic methods at runtime:

```mermaid
classDiagram
    class ReportVisitor {
        <<interface>>
        +visit(TextFile f) void
        +visit(VideoFile v) void
    }

    class MetadataExtractor {
        +visit(TextFile f) void
        +visit(VideoFile v) void
    }

    class DocumentElement {
        <<interface>>
        +accept(ReportVisitor v)* void
    }

    class TextFile {
        -String fileName
        -int wordCount
        +accept(ReportVisitor v) void
    }

    MetadataExtractor ..|> ReportVisitor : Implements
    TextFile ..|> DocumentElement : Implements
    TextFile --> ReportVisitor : Passes self context to