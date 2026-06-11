# Template Method Design Pattern in Java

A clean, production-ready implementation of the **Template Method Design Pattern** in Java, modeled around an extensible data-mining pipeline.

---

## 📌 Overview

The **Template Method Pattern** is a behavioral design pattern that defines the core structural sequence of an algorithm inside a base class method while leaving individual operational steps open to implementation variants by subclasses.

It addresses a common architectural anti-pattern: duplication of structural code. When multiple classes execute identical structural processes but differ only in minor low-level mechanics, the Template pattern extracts the invariant structure upwards, enforcing standardized execution flow while supporting polymorphic customization.

---

## 🏗️ Architecture Design

The pattern isolates algorithmic invariant workflows inside a rigid abstract scaffold, allowing structural extensions downwards:

```mermaid
classDiagram
    class DataMiner {
        <<abstract>>
        +mineData(String path) void
        #openFile(String path) void
        #closeFile() void
        #extractData()* void
        #parseData()* void
        #hookEnableReport() boolean
        #generateReport() void
    }

    class PdfDataMiner {
        #extractData() void
        #parseData() void
    }

    class CsvDataMiner {
        #extractData() void
        #parseData() void
        #hookEnableReport() boolean
    }

    PdfDataMiner --|> DataMiner : Inherits & Implements
    CsvDataMiner --|> DataMiner : Inherits & Implements