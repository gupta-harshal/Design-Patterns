# 🏗️ Advanced Builder Design Pattern Implementation

A clean, production-grade implementation of the **Builder Creational Design Pattern** in Java. This architecture demonstrates how to construct complex objects step-by-step, eliminating bloated constructors while guaranteeing strict object immutability and thread safety in concurrent backend systems.

---

## 📐 Architectural Class Diagram

The relationship between the complex target product (`User`) and its nested static utility configuration layer (`UserBuilder`) is defined below. 

```mermaid
classDiagram
    direction LR
    class User {
        -String firstName
        -String lastName
        -String email
        -String phone
        -String age
        -User(UserBuilder builder)
        +getFirstName() String
        +getLastName() String
        +getEmail() String
        +getPhone() String
        +getAge() String
        +toString() String
    }

    class UserBuilder {
        -String firstName
        -String lastName
        -String email
        -String phone
        -String age
        +UserBuilder(String firstName, String lastName)
        +email(String email) UserBuilder
        +phone(String phone) UserBuilder
        +age(String age) UserBuilder
        +build() User
    }

    User +-- UserBuilder : Nested Static Class
    UserBuilder ..> User : Instantiates & Assembles