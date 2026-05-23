# 🔒 Thread-Safe Singleton Design Pattern Analysis

A comparative architectural study analyzing the implementation variations of the **Singleton Creational Design Pattern** in Java. This codebase provides a deep-dive comparison between a naive concurrent-unsafe model and an production-standard multi-threaded implementation leveraging **Double-Checked Locking**.

---

## 📐 Concurrency Execution Blueprint

The sequence diagram below illustrates how the **Double-Checked Locking** mechanism coordinates incoming execution threads at the exact same millisecond boundary, protecting the global heap from duplicate memory footprints:

```mermaid
sequenceDiagram
    autonumber
    actor T1 as Thread 1 (High Priority)
    actor T2 as Thread 2 (Concurrent Client)
    participant Zone1 as Layer 1: Read Gate (instance == null)
    participant Lock as Turnstile: synchronized block
    participant Zone2 as Layer 2: Security Guard (instance == null)
    participant Heap as Shared Memory Heap

    T1->>Zone1: Evaluate instance state
    Note over T1,Zone1: State is NULL
    T2->>Zone1: Evaluate instance state (simultaneously)
    Note over T2,Zone1: State is NULL
    
    T1->>Lock: Request JVM Lock Allocation
    activate Lock
    Note over T1,Lock: Lock ACQUIRED
    T2->>Lock: Request JVM Lock Allocation
    Note over T2,Lock: Blocked & Enqueued
    
    T1->>Zone2: Re-evaluate instance state safely inside lock
    Note over T1,Zone2: State is STILL NULL
    T1->>Heap: Allocate new DoubleValidationSingleton() memory block
    Note over Heap: Memory Address Loaded
    deactivate Lock
    Note over T1: Releases JVM Lock
    
    activate Lock
    Note over T2,Lock: Lock DELEGATED to Thread 2
    T2->>Zone2: Re-evaluate instance state inside lock
    Note over T2,Zone2: State is NOT NULL (Thread 1 created it)
    Note over T2: Skips Instantiation Sequence Safely
    deactivate Lock
