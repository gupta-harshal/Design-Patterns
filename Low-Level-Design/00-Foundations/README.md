# 00 — Foundations

> The method behind every LLD problem in this repo.
> Read this section **before** attempting any problem. Re-read the checklist before every interview.

Foundations is not a pattern catalogue. It answers one question: *given a vague prompt and 45 minutes, how do you produce a defensible object model and talk about it convincingly?*

---

## Files in this section

| # | File | What it gives you | Read when |
|---|------|-------------------|-----------|
| 01 | [How to Approach LLD](./01-How-To-Approach-LLD.md) | The 7-step interview method + 45-minute timebox + common mistakes | First, and before every mock |
| 02 | [SOLID for LLD](./02-SOLID-For-LLD.md) | SOLID with real bad-vs-good Java from Parking Lot, Tic Tac Toe, Logger | Second — it explains *why* the method produces those classes |
| 03 | [Patterns Cheat Sheet](./03-Patterns-Cheat-Sheet.md) | GoF → LLD problem mapping, when **not** to use each, pattern-soup anti-pattern | While solving problems; skim before interviews |
| 04 | [UML and Diagrams](./04-UML-And-Diagrams.md) | Class / sequence / state diagram notation interviewers actually expect, in Mermaid | Before you draw anything |
| 05 | [Interview Checklist](./05-Interview-Checklist.md) | Printable before / during / after checklist | Night before + 5 minutes before the round |
| 06 | [Complexity and Tradeoffs](./06-Complexity-And-Tradeoffs.md) | Talk tracks for win-check, cache, rate limiter, booking conflicts, optimistic locking | When the interviewer says "how would this scale?" |

---

## How to study Foundations

**Pass 1 — understand (about 2 hours)**

1. Read `01` end to end. Write the 7 steps on an index card from memory.
2. Read `02`. For each principle, decide whether your last piece of code violated it.
3. Skim `03` and `04`. Do not memorise; you will return to them.

**Pass 2 — apply (spread over your problem practice)**

4. Solve [Tic Tac Toe](../01-Games/Tic-Tac-Toe/) *using the 7 steps explicitly*, out loud, on paper, with a timer.
5. Compare your class diagram to the one in `details.md`. Differences are the lesson — not the answer key.
6. After each problem, open `03` and ask: which patterns did I use, and did each earn its keep?

**Pass 3 — perform (interview week)**

7. Re-read `05` (checklist) and `06` (tradeoff talk tracks). These are the two files that change your *score*, not your *knowledge*.
8. Do two timed mocks with a 45-minute clock. If you finish the class diagram after minute 25, you are too slow — diagnose with `01`.

---

## The one-paragraph summary of this whole section

An LLD round scores **modelling judgment and communication**, not code volume. Spend the first 5 minutes narrowing scope in writing, the next 10 finding entities and their relationships, the next 15 on a class diagram with responsibilities and key method signatures, and the rest on one core flow, edge cases, and a named extension. Use a design pattern only when you can state the change it absorbs. Say complexity numbers out loud even when nobody asks. Finish with a short list of what you deliberately left out.

---

## What Foundations does *not* contain

- No Java files. This section is method and notation only; code lives in the problem folders.
- No exhaustive GoF explanations — that is the sibling [`Design-Patterns/`](../../Design-Patterns/) micro-lab.
- No distributed systems / HLD material. Sharding, load balancers and queues are out of scope unless the interviewer explicitly pulls you there.

---

[⬅ Back to the LLD index](../README.md)
