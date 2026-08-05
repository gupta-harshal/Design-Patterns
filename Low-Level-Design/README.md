# Low-Level Design Bible

> Your personal, forever revision resource for **Low-Level Design (LLD)** interviews and real-world object modeling.
> Every module has a deep explanation (`details.md`) and a runnable Java sketch (`Main.java`).

---

## How to use this bible

1. **Read [Foundations](./00-Foundations/) first** — especially *How to Approach LLD*. That is the method.
2. Work problems **in section order** (Games → Machines → Booking → …).
3. For each problem: read `details.md` → draw the class diagram yourself → only then open `Main.java`.
4. Before interviews: Foundations + the **Must-do 10**.
5. Treat every `Main.java` as a **teaching sketch**, not production code.

### Study loop (per problem)

```text
Cover the details.md APIs
    → Whiteboard entities from memory (10 min)
        → Check against the class diagram
            → Trace one happy path + one failure path
                → Answer: "How would you extend this?"
```

---

## Must-do 10 (if time is short)

| # | Problem | Section | Why it matters |
|---|---------|---------|----------------|
| 1 | [Parking Lot](./02-Machines/Parking-Lot/) | Machines | Spots, tickets, strategy |
| 2 | [Elevator](./02-Machines/Elevator/) | Machines | Scheduling + state |
| 3 | [Movie Ticket Booking](./03-Booking/Movie-Ticket-Booking/) | Booking | Locks, double-booking |
| 4 | [Splitwise](./04-Social-Money/Splitwise/) | Social | Balance math |
| 5 | [Snake & Ladder](./01-Games/Snake-And-Ladder/) | Games | Board + dice strategy |
| 6 | [Chess](./01-Games/Chess/) | Games | Polymorphism / moves |
| 7 | [Logging Framework](./05-Infra/Logging-Framework/) | Infra | Levels, appenders |
| 8 | [LRU Cache](./05-Infra/LRU-Cache/) | Infra | O(1) structure |
| 9 | [Rate Limiter](./05-Infra/Rate-Limiter/) | Infra | Token bucket |
| 10 | [Cab Booking](./03-Booking/Cab-Booking/) | Booking | Matching + lifecycle |

---

## Full catalog

### 00 — Foundations

| Doc | Focus |
|-----|--------|
| [How to Approach LLD](./00-Foundations/01-How-To-Approach-LLD.md) | Interview method & timebox |
| [SOLID for LLD](./00-Foundations/02-SOLID-For-LLD.md) | Principles with system examples |
| [Patterns Cheat Sheet](./00-Foundations/03-Patterns-Cheat-Sheet.md) | Pattern → problem map |
| [UML & Diagrams](./00-Foundations/04-UML-And-Diagrams.md) | Class / sequence / state |
| [Interview Checklist](./00-Foundations/05-Interview-Checklist.md) | Before / during / after |
| [Complexity & Tradeoffs](./00-Foundations/06-Complexity-And-Tradeoffs.md) | Perf & consistency talk tracks |

### 01 — Games

| Problem | Patterns / skills |
|---------|-------------------|
| [Tic Tac Toe](./01-Games/Tic-Tac-Toe/) | Board, turns, win detection |
| [Snake & Ladder](./01-Games/Snake-And-Ladder/) | Dice Strategy, board jumps |
| [Chess](./01-Games/Chess/) | Piece polymorphism, path rules |

### 02 — Machines

| Problem | Patterns / skills |
|---------|-------------------|
| [Parking Lot](./02-Machines/Parking-Lot/) | Factory/Strategy, tickets, fees |
| [Elevator](./02-Machines/Elevator/) | State, SCAN dispatch |
| [Vending Machine](./02-Machines/Vending-Machine/) | State machine |
| [ATM](./02-Machines/ATM/) | State + cash chain |

### 03 — Booking

| Problem | Patterns / skills |
|---------|-------------------|
| [Movie Ticket Booking](./03-Booking/Movie-Ticket-Booking/) | Seat locks, payments |
| [Hotel Booking](./03-Booking/Hotel-Booking/) | Date-range overlap |
| [Cab Booking](./03-Booking/Cab-Booking/) | Matching, trip lifecycle |
| [Car Rental](./03-Booking/Car-Rental/) | Inventory + pricing |

### 04 — Social & Money

| Problem | Patterns / skills |
|---------|-------------------|
| [Splitwise](./04-Social-Money/Splitwise/) | Split strategies, balances |
| [Stack Overflow](./04-Social-Money/Stack-Overflow/) | Voting, reputation |
| [Twitter Feed](./04-Social-Money/Twitter-Feed/) | Follow graph, pull feed |
| [Notification System](./04-Social-Money/Notification-System/) | Channel Strategy |

### 05 — Infra building blocks

| Problem | Patterns / skills |
|---------|-------------------|
| [Logging Framework](./05-Infra/Logging-Framework/) | Levels, appenders |
| [LRU Cache](./05-Infra/LRU-Cache/) | HashMap + DLL |
| [Rate Limiter](./05-Infra/Rate-Limiter/) | Token bucket |
| [Pub-Sub](./05-Infra/Pub-Sub/) | Broker, subscribers |
| [File System](./05-Infra/File-System/) | Composite tree |

### 06 — Domain systems

| Problem | Patterns / skills |
|---------|-------------------|
| [Library Management](./06-Domain/Library-Management/) | Copies, loans, fines |
| [Online Shopping](./06-Domain/Online-Shopping/) | Cart, inventory, order states |
| [Meeting Scheduler](./06-Domain/Meeting-Scheduler/) | Room conflicts |
| [Text Editor Undo](./06-Domain/Text-Editor-Undo/) | Command / Memento |
| [Food Delivery](./06-Domain/Food-Delivery/) | Order + agent assign |

---

## Pattern → problem map

| Pattern | Practice on |
|---------|-------------|
| **State** | Vending, ATM, Elevator, Order lifecycles |
| **Strategy** | Dice, pricing, matching, splits, notifications |
| **Observer / Pub-Sub** | Notifications, message broker |
| **Factory** | Vehicles/spots, notification channels |
| **Command + Memento** | Text editor undo |
| **Composite** | File system |
| **Chain of Responsibility** | ATM cash, logger levels |
| **Singleton** (sparingly) | Logger — discuss testability tradeoffs |

---

## Module standard

Every problem folder:

| File | Purpose |
|------|---------|
| `details.md` | Requirements → design → diagrams → flows → edge cases → extensions → interview talk track |
| `Main.java` | Single-file runnable sketch (`javac Main.java && java Main`) |

### Universal LLD method

```text
Clarify requirements (functional + NFR + out of scope)
    → List entities & relationships
        → Assign responsibilities (SRP)
            → Add patterns only when they remove pain
                → Define core APIs
                    → Happy path sequence
                        → Edge cases + extensions
```

---

## Linked GoF micro-lab

Sibling folder `../Design-Patterns/` (Creational / Structural / Behavioural) drills **individual patterns**.  
This bible drills **systems that compose those patterns** — what interviews actually ask.

---

## Quick start

```text
Low-Level-Design/
  README.md              ← you are here
  00-Foundations/        ← method first
  01-Games/ … 06-Domain/ ← 25 full designs
```

Each problem:

```bash
cd <problem-folder>
javac Main.java && java Main
```
