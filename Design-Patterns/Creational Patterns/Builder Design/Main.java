// 1. The Complex Target Product Class
class User {
    // Immundable fields (all marked final - excellent for thread safety)
    private final String firstName; // Required
    private final String lastName;  // Required
    private final String email;     // Optional
    private final String phone;     // Optional
    private final String age;       // Optional

    // The main class constructor is PRIVATE. 
    // This forces developers to use the Builder; they cannot bypass it using 'new User(...)'.
    private User(UserBuilder builder) {
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.phone = builder.phone;
        this.age = builder.age;
    }

    // Getters only, no setters (Immutable Object Design)
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAge() { return age; }

    @Override
    public String toString() {
        return "User Profile [Name=" + firstName + " " + lastName + 
               ", Email=" + email + ", Phone=" + phone + ", Age=" + age + "]";
    }

    // 2. The Static Inner Builder Class
    public static class UserBuilder {
        // Mirror fields from the outer class
        private final String firstName; 
        private final String lastName;  
        private String email = "N/A";    // Default values if omitted
        private String phone = "N/A";
        private String age = "Not Disclosed";

        // Builder Constructor enforces the mandatory fields
        public UserBuilder(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        // Fluent Setters: Crucial step. Every method returns 'this' (the builder instance itself)
        public UserBuilder email(String email) {
            this.email = email;
            return this; // Allows method chaining
        }

        public UserBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public UserBuilder age(String age) {
            this.age = age;
            return this;
        }

        // The final assembly point
        public User build() {
            // Optional: You can place validation logic here before object birth
            // e.g., if (!email.contains("@")) throw new IllegalArgumentException();
            return new User(this);
        }
    }
}

// 3. Execution Test Driver
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Builder Pattern Demo ===\n");

        // Example A: Creating a fully populated user profile via method chaining
        User fullProfile = new User.UserBuilder("John", "Doe")
                                .email("john.doe@example.com")
                                .phone("+1-555-0199")
                                .age("28")
                                .build();

        // Example B: Creating a basic profile (skipping optional fields cleanly)
        User basicProfile = new User.UserBuilder("Alice", "Smith")
                                 .email("alice@example.com")
                                 // phone and age are cleanly ignored
                                 .build();

        System.out.println(fullProfile);
        System.out.println(basicProfile);
    }
}