# Iterator Design Pattern in Java

A clean, production-ready implementation of the **Iterator Design Pattern** in Java, modeled around a sequential video playlist engine.

---

## 📌 Overview

The **Iterator Pattern** is a behavioral design pattern that allows you to sequentially access elements of a collection object (like a list, stack, tree, or graph) **without exposing its underlying representation**.

Whether your data structure is backed by a standard array, a dynamic array list, a linked list, or a complex multi-dimensional tree, the consumer interacts with it using the exact same generic interface loop (`hasNext()` and `next()`).

---

## 🏗️ Architecture Design

The interaction between the client and the data store is completely decoupled through abstractions (`Aggregate` and `Iterator` interfaces). Here is how the system is structured:

```mermaid
classDiagram
    class Aggregate~T~ {
        <<interface>>
        +createIterator() Iterator~T~
    }

    class Iterator~T~ {
        <<interface>>
        +hasNext() boolean
        +next() T
    }

    class Playlist {
        -List~String~ videos
        +addVideo(String title) void
        +createIterator() Iterator~String~
    }

    class PlaylistIterator {
        -List~String~ videos
        -int position
        +hasNext() boolean
        +next() String
    }

    Playlist ..|> Aggregate : Implements
    PlaylistIterator ..|> Iterator : Implements
    Playlist --> PlaylistIterator : Creates