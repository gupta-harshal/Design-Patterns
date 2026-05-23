class DangerousSingleton {
    private static DangerousSingleton instance;
    private DangerousSingleton() {}

    // NO VALIDATION LAYER / NO LOCKING
    public static DangerousSingleton getInstance() {
        if (instance == null) {
            // Artificial delay to force threads to overlap and cause a bug
            try { Thread.sleep(10); } catch (Exception e) {}
            instance = new DangerousSingleton();
        }
        return instance;
    }
}

class DoubleValidationSingleton {
    private static volatile DoubleValidationSingleton instance;
    private DoubleValidationSingleton() {}

    // DOUBLE VALIDATION WITH SYNCHRONIZED
    public static DoubleValidationSingleton getInstance() {
        if (instance == null) {                         // Validation 1
            synchronized (DoubleValidationSingleton.class) {
                if (instance == null) {                 // Validation 2
                    instance = new DoubleValidationSingleton();
                }
            }
        }
        return instance;
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        
        System.out.println("TESTING DANGEROUS SINGLETON (NO DOUBLE-CHECK)");
        for (int i = 1; i <= 3; i++) {
            new Thread(() -> {
                DangerousSingleton obj = DangerousSingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " address: " + System.identityHashCode(obj));
            }, "Thread-" + i).start();
        }

        // Wait for the first test to finish printing
        Thread.sleep(200);
        
        System.out.println("\n TESTING DOUBLE-VALIDATION SINGLETON (SAFE) ---");
        for (int i = 1; i <= 3; i++) {
            new Thread(() -> {
                DoubleValidationSingleton obj = DoubleValidationSingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " address: " + System.identityHashCode(obj));
            }, "Thread-" + i).start();
        }
    }
}