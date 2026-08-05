# Low Level Design (LLD) Systems

This folder holds **end-to-end system designs** built on top of the GoF patterns in `Design-Patterns/`.

While the patterns folder answers *“how do I structure this collaboration?”*, this folder answers *“how do I design a complete product/feature?”* — requirements, classes, relationships, flows, and working Java code.

---

## What lives here

Each subfolder is one system:

| System | Description |
|--------|-------------|
| [Tic-Tac-Toe](./Tic-Tac-Toe/) | Classic 2-player game LLD with Strategy-based win detection |

Add new systems as siblings of `Tic-Tac-Toe/` using the same layout.

---

## Suggested layout for a new system

```text
Low-Level-Design/
└── <System-Name>/
    ├── Main.java     # Runnable Java implementation
    └── README.md     # Requirements, class diagram, design explanation
```

A good system README usually covers:

1. Problem statement & requirements  
2. Entities / class responsibilities  
3. Class diagram (Mermaid)  
4. Key design decisions & patterns  
5. SOLID mapping  
6. Sequence / flow for the main use case  
7. How to compile and run  

---

## How this relates to Design Patterns

| Concern | Folder |
|---------|--------|
| Isolated pattern demos (Factory, Strategy, State, …) | `Design-Patterns/` |
| Full systems that *compose* those patterns | `Low-Level-Design/` |

Example: Tic-Tac-Toe uses the **Strategy Pattern** for win checking — the same idea as in `Design-Patterns/Bevahioural Patterns/Strategy Design`, applied inside a real game model.
