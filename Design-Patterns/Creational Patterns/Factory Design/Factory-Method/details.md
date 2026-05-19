# Factory Method Pattern — Payment Gateway Integration 💳

This module provides a production-ready implementation of the official **GoF Factory Method Design Pattern**, applied to a dynamic multi-vendor payment processing system (supporting Stripe and Razorpay).

---

## 🎯 The Problem & Architectural Goal

In a traditional application, instantiating objects directly via the `new` keyword creates **tight coupling**. For example, writing `PaymentGateway gateway = new RazorpayGateway();` inside your core business logic makes it fragile. 

Worse, different payment vendors require entirely unique parameters to initialize:
* **Stripe** might require an `ApiKey` and an `ApiVersion`.
* **Razorpay** might require a `KeyId`, `KeySecret`, and a `DatabaseConnection`.

If you use a simple central switch-case statement to handle this creation, you violate the **Open/Closed Principle (OCP)** because you have to alter tested framework code every time a new payment vendor is introduced.

### The Solution
The **Factory Method Pattern** interfaces object creation out of the business logic. It defines a generic creator interface (`GatewayFactory`), delegating the complex, vendor-specific construction parameters to isolated concrete sub-factories (`StripeFactory`, `RazorpayFactory`). 

---

## 🏗️ Architecture Diagram

```mermaid
classDiagram
    class GatewayFactory {
        <<interface>>
        +createGateway()* PaymentGateway
    }
    class StripeFactory {
        +createGateway() PaymentGateway
    }
    class RazorpayFactory {
        +createGateway() PaymentGateway
    }
    class PaymentGateway {
        <<interface>>
        +processPayment(double amount) void
    }
    class StripeGateway {
        +processPayment(double amount) void
    }
    class RazorpayGateway {
        +processPayment(double amount) void
    }

    GatewayFactory <|.. StripeFactory
    GatewayFactory <|.. RazorpayFactory
    PaymentGateway <|.. StripeGateway
    PaymentGateway <|.. RazorpayGateway
    StripeFactory ..> StripeGateway : Instantiates
    RazorpayFactory ..> RazorpayGateway : Instantiates