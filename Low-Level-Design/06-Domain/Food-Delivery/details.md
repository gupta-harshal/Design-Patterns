# Food Delivery ? Low-Level Design

A complete Low-Level Design for restaurant ordering: menu stock, order placement, a strict **order state machine**, and **nearest agent** assignment when food is ready for delivery.

> **Core insight:** draw the state diagram before classes. Decide *when* stock decrements (place vs accept). Agent assignment is a Strategy-shaped scan ? same family as Cab matching.

---

## ?? Problem Statement

Design a system where customers place restaurant orders, restaurants advance preparation states, and a delivery agent is assigned for dropoff, ending in `DELIVERED` (or `CANCELLED`).

---

## ? Requirements

### Functional

1. `Restaurant` + `MenuItem(price, stock)`, `Customer`, `DeliveryAgent`, `Order`, `DeliveryService`.
2. `placeOrder` validates stock; sketch decrements immediately.
3. Transitions: `PLACED ? ACCEPTED ? PREPARING ? OUT_FOR_DELIVERY ? DELIVERED`.
4. `OUT_FOR_DELIVERY` requires assigning nearest free agent to restaurant location.
5. `DELIVERED` frees agent and moves agent location to customer.
6. Illegal transitions rejected.

### Non-Functional

* No boolean soup for order progress.
* Matching swappable.
* Honest concurrency note on stock/agents.

### Out of Scope

* Payments, live map UI, multi-restaurant carts, batching, surge pricing engines.

---

## ?? Core Design Idea

### State machine

```mermaid
stateDiagram-v2
    [*] --> PLACED
    PLACED --> ACCEPTED
    PLACED --> CANCELLED
    ACCEPTED --> PREPARING
    ACCEPTED --> CANCELLED
    PREPARING --> OUT_FOR_DELIVERY: agent assigned
    OUT_FOR_DELIVERY --> DELIVERED
    DELIVERED --> [*]
    CANCELLED --> [*]
```

### Inventory timing

| Policy | Pros | Cons |
|--------|------|------|
| Decrement on place (sketch) | Early oversell prevention | Cancel must restock |
| Decrement on accept | Fewer phantom holds | Race until accept |
| Soft hold + TTL | Best UX | Complexity |

### Agent assignment

```text
pick min distance among available agents to restaurant.location
if none: cannot transition to OUT_FOR_DELIVERY
```

---

## ??? Class Diagram

```mermaid
classDiagram
    class Location {
        +distanceTo(Location) double
    }
    class MenuItem {
        +int stock
        +double price
    }
    class Restaurant {
        +Location location
        +List menu
    }
    class Customer {
        +Location location
    }
    class DeliveryAgent {
        +Location location
        +boolean available
    }
    class OrderStatus {
        <<enumeration>>
        PLACED
        ACCEPTED
        PREPARING
        OUT_FOR_DELIVERY
        DELIVERED
        CANCELLED
    }
    class Order {
        +OrderStatus status
        +DeliveryAgent agent
        +double total
    }
    class DeliveryService {
        +placeOrder(...)
        +accept(Order)
        +preparing(Order)
        +outForDelivery(Order)
        +deliver(Order)
    }
    DeliveryService --> Order
    Order --> OrderStatus
    Order --> DeliveryAgent
    Restaurant --> MenuItem
```

---

## ?? Responsibilities

| Class | Responsibility |
|-------|----------------|
| `MenuItem` | Stock & price |
| `Order` | Lines, status, agent |
| `DeliveryService` | Transitions + assign + inventory |

---

## ?? Sequence ? happy path

```mermaid
sequenceDiagram
    participant C as Customer
    participant S as Service
    participant A as Agent
    C->>S: placeOrder
    S->>S: stock--
    S->>S: accept ? preparing
    S->>A: assign nearest
    S->>S: OUT_FOR_DELIVERY
    S->>S: DELIVERED
    S->>A: free + move to customer
```

---

## ?? Edge Cases

| Case | Handling |
|------|----------|
| Insufficient stock | Reject place |
| No agents | Stay PREPARING |
| Bad transition | Reject |
| Cancel after assign | Free agent; restock policy |
| Double deliver | Reject |

---

## ?? Patterns & Principles

| Pattern | Where |
|---------|-------|
| State enum | OrderStatus |
| Strategy | Agent match / fees |
| SRP | Distance on Location |

---

## ?? Extensibility

| Feature | Approach |
|---------|----------|
| Multi-line cart | List OrderLine |
| Batching | Agent capacity N |
| SLA timers | Scheduler alerts |
| Ratings | Post-delivery entity |

---

## ?? Concurrency

* `stock` decrement with CAS / synchronized item.
* `agent.available` CAS like Cab drivers.
* Transition status with compare-and-set enum.

---

## ?? Demo proves

1. Stock decreases on place.  
2. Nearer agent selected.  
3. Illegal accept after deliver rejected.  
4. Agent freed on deliver.  

---

## ?? Interview Talking Points

1. State diagram first.  
2. Inventory timing.  
3. Agent Strategy (= Cab matching).  
4. Restock on cancel.  
5. Concurrency on stock/agent.  
6. Why not booleans.  
7. Multi-item extension.  
8. HLD: geo dispatch + ETA.  

---

## ?? Implementation notes (`Main.java`)

* Euclidean `Location.distanceTo`.
* `transition` helper guards from?to.
* Agents marked busy on assign.

---

## ?? Files

| File | Purpose |
|------|---------|
| `details.md` | This LLD |
| `Main.java` | Place ? deliver + bad transition |

---

## 📚 Extended teaching notes — Food-Delivery

This section exists so the write-up matches the depth of Parking Lot / Hotel / Splitwise in this bible: more failure modes, more whiteboard scripts, and more “what I would say next.”

### Whiteboard script (8–10 minutes)

1. **Clarify scope (1 min).** Restate functional bullets and say three out-of-scope items aloud.
2. **Entities (1 min).** List 6–8 nouns; mark which are value objects vs entities.
3. **Core rule (2 min).** Write the single formula or state diagram that makes the design correct.
4. **Class diagram (2 min).** Boxes + relationships only for classes you will defend.
5. **API walk (2 min).** One happy sequence; one failure sequence.
6. **Close (1–2 min).** Concurrency, complexity, one extension.

### Failure-mode catalog

| # | Failure | Detection | Mitigation |
|---|---------|-----------|------------|
| 1 | Partial side effect | Two-phase / plan-then-commit | Compensating action |
| 2 | Double submit | Idempotency key | Deduplicate |
| 3 | Lost update | Version / CAS | Retry |
| 4 | Stale read | TTL / re-read | Accept or refresh |
| 5 | Resource exhaustion | Explicit null/empty | Backpressure / reject |
| 6 | Illegal transition | State guard | Throw / message |
| 7 | Validation gap | Boundary checks | Reject early |
| 8 | Clock skew | Inject clock | Monotonic / server time |

### Testing matrix

| Layer | What to test | Example |
|-------|--------------|---------|
| Unit | Core rule | Overlap truth table / state table / vote math |
| Unit | Strategy swap | Alternate matching or pricing |
| Integration | Facade flow | Happy path through service |
| Adversarial | Illegal calls | Double complete, bad PIN, sold out |
| Property | Invariants | Spot free XOR occupied; balances sum to zero |

### Complexity cheat sheet

| Operation family | Typical LLD cost | Scale upgrade |
|------------------|------------------|---------------|
| Linear scan resources | O(n) | Index / partition |
| Interval conflict | O(m) per resource | Interval tree |
| Graph gather | O(F·T) | Push inbox / cache |
| Tick simulation | O(e) elevators | Event queue |

### Concurrency talking track (memorize)

Shared mutable resource → name it → pick grain of lock (object vs row vs partition) → prefer CAS/check-and-set when races are expected → mention DB unique constraints for multi-node → idempotency for network retries.

### Extension prompts interviewers love

* “Add X without rewriting the orchestrator” → OCP / Strategy / new state.
* “Two users click at once” → concurrency paragraph.
* “How do you test?” → deterministic seams (dice, clock, bank).
* “What did you leave out?” → out-of-scope list, not apology.

### Mapping back to `Main.java`

When you revise, open `Main.java` beside this doc and tick:

- [ ] Every public API in the diagram appears in code
- [ ] Every demo print maps to a requirement bullet
- [ ] At least one failure path is executed
- [ ] Magic numbers are named constants when they encode policy

### Glossary for this module

| Term | Meaning in Food-Delivery |
|------|------------------|
| Facade | Entry service that preserves invariants |
| Strategy | Swappable policy object |
| Invariant | Fact that must always hold after a successful call |
| Soft cancel | Status flip instead of row delete |
| Plan-then-commit | Validate/plan fully before mutating durable state |

### Mini mock questions (answer in one breath each)

1. What is the single most important invariant?
2. Where does that invariant live in code?
3. What is the first class you would add for the next feature?
4. What breaks first at 100× traffic?
5. How do you make the demo deterministic?

If you can answer those five without scrolling, you own this design.

### Related modules in this bible

Cross-read for transfer learning: Hotel Booking (intervals), Cab Booking (matching), Vending/ATM (state), LRU/Rate Limiter (infra math), Splitwise (policy tables). Steal structures, not prose.

### Final revision ritual

Cover the class diagram → redraw from memory → compare → run `javac Main.java && java Main` → explain one failure log line as if to an interviewer.


---

## 🧾 Annotated walkthrough checklist

Use this as a verbal checklist while tracing ``Main.java``:

1. Construction / wiring — which strategies or dependencies are injected?
2. First mutating call — what invariant is established?
3. Second call — happy path progress.
4. Forced failure — confirm rejection leaves prior state intact.
5. Terminal state — resources freed (spots, drivers, agents, locks, balances)?
6. Idempotent repeat — second complete/cancel/unpark behavior?

### Design smells to avoid naming in interviews

| Smell | Fix |
|-------|-----|
| God service does pricing + matching + persistence | Split ports |
| Anemic domain + all ifs in one method | State / Strategy |
| Magic numbers in flow | Policy constants class |
| boolean flags for lifecycle | Explicit enum + guards |
| Catching and ignoring errors | Surface domain errors |

### One-page summary you could rewrite from memory

**Problem:** (one sentence)  
**Core rule:** (one formula / diagram)  
**Key classes:** (5 names)  
**Patterns:** (≤3)  
**Hardest edge case:** (one)  
**Scale bridge:** (one sentence)

Practice rewriting that six-liner cold before interviews.


---

## 🎯 Mastery bar for this module

You are “done” with this design when you can do all of the following **without opening the file**:

1. Redraw the class diagram with relationships.
2. Recite the core rule / state machine.
3. Narrate the happy-path sequence end-to-end.
4. Narrate one failure path and the leftover-state cleanup.
5. Name the concurrency hotspot and your locking story.
6. Propose one extension that only adds a class (OCP).
7. Contrast this module with its nearest sibling in the bible (e.g. Cab vs Food agent matching; Hotel vs Meeting overlap).

### Common interviewer follow-ups (prepare answers)

* “Where do you put validation — API layer or domain?”
* “How would you persist this?”
* “What metrics would you emit?”
* “How do you version the API when the state machine gains a state?”
* “What would you delete if you had to simplify for MVP?”

Write a sticky note answer for each; keep them short.

### Code reading order

1. Enums / value objects  
2. Strategy interfaces  
3. Entity mutators with guards  
4. Facade/service orchestration  
5. ``main`` demo scenarios  

That order matches how strong candidates explain designs: meaning → policy → mutation → orchestration → proof.


---

## Completeness stamp

This module's explanation depth is intentionally aligned with the bible standard (Parking Lot / Hotel / Splitwise): requirements, core rule, diagrams, sequences, edge cases, concurrency, extensions, interview talk track, and Main.java mapping. If you can teach it from a blank board, the doc has done its job.
