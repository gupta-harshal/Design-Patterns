# 05 — LLD Interview Checklist

> Printable. Read the "Before" section the night before, the "During" section in the first minute of the round, and the "After" section within an hour of finishing.

---

## ✅ BEFORE the round

### The night before (30–45 min, not more)

- [ ] Re-read [How to Approach LLD](./01-How-To-Approach-LLD.md) — the 7 steps and the 45-minute timebox
- [ ] Skim the [Patterns Cheat Sheet](./03-Patterns-Cheat-Sheet.md) trigger table (one line per pattern)
- [ ] Re-read the "common mistakes" list — most lost points are there, not in missing knowledge
- [ ] Skim two solved problems you already did; do **not** learn a new problem the night before
- [ ] Sleep. Cramming a new pattern buys less than being alert enough to listen carefully.

### Recall check — can you say these cold?

- [ ] The 7 steps in order
- [ ] Composition vs aggregation, with the delete test
- [ ] Strategy vs State: varying *algorithm* vs varying *lifecycle behaviour*
- [ ] Decorator vs Proxy: *adds behaviour* vs *controls access*
- [ ] Two reasons Singleton is risky (hidden dependency, untestable/global mutable state)
- [ ] Optimistic vs pessimistic locking, and when each wins
- [ ] Why booleans-for-status is worse than an enum

### 10 minutes before

- [ ] Tools ready: whiteboard tool open, or paper + 2 pens; IDE/editor open if it's a coding round
- [ ] A blank page with the four headers already written: `SCOPE` / `ENTITIES` / `CLASSES` / `EDGE CASES`
- [ ] Timer or clock visible — you must be able to check the minute mark without hunting
- [ ] Water; camera framed so your diagram is legible if you're drawing on paper
- [ ] One deep breath and a reminder: **narrate everything**

---

## 🎯 DURING the round

### Minute 0–5 — Clarify and bound

- [ ] Restate the problem in your own words; get agreement
- [ ] Ask 3–5 sharp questions (scale, actors, top use cases, constraints, concurrency)
- [ ] Write and show: **in scope / out of scope / assumptions**
- [ ] Confirm what the interviewer wants: design-only, design + one coded class, or full code
- [ ] Ask whether they want breadth (many features) or depth (one feature, done well)

> 🚫 Don't start naming classes yet. 🚫 Don't ask 15 questions.

### Minute 5–15 — Entities and relationships

- [ ] List candidate nouns; mark each as **entity / value object / enum / service**
- [ ] Replace status booleans with enums, and type strings with enums
- [ ] Draw relationships with **multiplicities** (`1`, `0..1`, `1..*`, `*`)
- [ ] Decide composition vs aggregation for each containment
- [ ] Flag many-to-many relationships that need their own class

### Minute 15–30 — Class diagram

- [ ] Entry-point class first, top-left
- [ ] Each class: fields → 2–4 key methods → one-sentence responsibility
- [ ] If a responsibility sentence needs "and", split the class
- [ ] Put an interface at each **expected point of change**; say what change it absorbs
- [ ] No god class; no anemic getter-only classes with a `Manager` doing everything
- [ ] Keep narrating design decisions and the alternative you rejected

### Minute 30–35 — APIs

- [ ] Write real signatures with parameter and return types on the main service
- [ ] Decide `Optional` vs exception for each failure; never return `null`
- [ ] State who validates what
- [ ] State idempotency for repeat calls (double cancel, double unpark)

### Minute 35–40 — Core flow

- [ ] One sequence diagram: happy path + one failure branch
- [ ] Every participant already exists in the class diagram
- [ ] Mark locks / transactions / TTL / async boundaries with a note
- [ ] Draw a state diagram if the entity has a real lifecycle

### Minute 40–45 — Edge cases, complexity, extensions

- [ ] Volunteer 4–6 edge cases (empty, full, duplicate, expired, out-of-order, invalid input)
- [ ] Raise **concurrency** yourself if the problem has shared resources — seats, spots, inventory, counters
- [ ] State complexity of the hottest operation and the improvement path
- [ ] Name 2 extensions and point at the seam that absorbs each
- [ ] Close with a 30-second summary: core classes, the two seams for change, what you deliberately left out

### Continuous — behaviours that score

- [ ] **Think out loud.** Silence is unscorable.
- [ ] **Take hints.** A question like "what if there are two lots?" is a hint, not curiosity. Act on it.
- [ ] **Handle challenges with a tradeoff, not a defence.** "Fair — the alternative is X, which wins on Y and costs Z. I'd switch if throughput matters more than clarity."
- [ ] **Correct yourself openly** when you spot a flaw. Self-review is a senior signal.
- [ ] **Manage the clock.** If you're at minute 30 without a diagram, cut scope and say you're cutting it.
- [ ] **Prefer depth over breadth** unless told otherwise.

### Red flags to catch in yourself

- [ ] Am I coding before I've drawn anything? → stop, draw
- [ ] Have I gone quiet for more than ~20 seconds? → narrate
- [ ] Have I named more than 3 patterns? → justify or drop them
- [ ] Am I still on requirements past minute 15? → assume and move
- [ ] Am I arguing rather than exploring? → restate the tradeoff and offer the alternative
- [ ] Did I say any complexity number this round? → if not, say one now

---

## 💻 If it's a coding LLD round

- [ ] Agree on scope of *code* vs *design* before typing
- [ ] Write enums and interfaces first — they're cheap and show structure fast
- [ ] Implement the class with real logic fully (`hasWinner`, `allow`, `get/put`, `allocate`)
- [ ] Leave supporting classes as signatures with `// ...` bodies, and say so
- [ ] Use meaningful names; no `data`, `temp`, `obj`, `flag`
- [ ] Constructor injection for dependencies — no `new ConcreteThing()` inside business methods
- [ ] Add a tiny `main` or a couple of assertions to demo the happy path
- [ ] Mention thread-safety explicitly: which fields are shared, what guards them
- [ ] Don't return internal mutable collections; return copies or unmodifiable views

---

## 📝 AFTER the round

### Immediately (within the hour, while it's fresh)

- [ ] Write down the exact prompt and any follow-up questions asked
- [ ] Note where you stalled, and *why* — missing knowledge, or missing method?
- [ ] Note every hint the interviewer gave and whether you acted on it
- [ ] Note the questions you couldn't answer well

### Same day

- [ ] Redo the problem properly, untimed, with the full 7 steps
- [ ] Answer the questions you fumbled; write the answer in your own words
- [ ] Add the problem to this repo if it isn't there, with `details.md` + `Main.java`
- [ ] Update your personal mistake list — patterns of failure repeat across rounds

### Weekly

- [ ] Review your mistake list; pick the top recurring one and drill it specifically
- [ ] Do one timed 45-minute mock on an unfamiliar problem
- [ ] If the same weakness appears three times (e.g. always late to the diagram, always silent on concurrency), make it the *only* thing you practise that week

---

## One-page cheat card

```text
MINUTES
 0-5   scope        in/out/assumptions written down
 5-15  entities     nouns → entity | value | enum | service ; relationships + multiplicity
15-30  classes      fields, key methods, one responsibility each ; interfaces at change points
30-35  APIs         real signatures ; Optional vs exception ; idempotency
35-40  flow         one sequence diagram, happy + one failure ; state diagram if lifecycle
40-45  close        edge cases, concurrency, complexity, 2 extensions, 30s summary

ALWAYS               NEVER
 narrate              silent drawing
 write assumptions    guessing scope
 enums over booleans  status flag pairs
 justify each pattern pattern soup
 raise concurrency    ignoring shared-resource races
 state complexity     waiting to be asked
 take hints           defending instead of trading off
```

---

[⬅ UML and Diagrams](./04-UML-And-Diagrams.md) · [Foundations index](./README.md) · [Next: Complexity and Tradeoffs ➡](./06-Complexity-And-Tradeoffs.md)
