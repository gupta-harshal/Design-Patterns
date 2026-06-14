// 1. SUBJECT INTERFACE (The common blueprint shared by both the real object and the proxy)
interface Internet {
    void connectTo(String serverUrl) throws Exception;
}

// 2. REAL SUBJECT (The heavy, core service doing the actual network lifting)
class RealInternet implements Internet {
    @Override
    public void connectTo(String serverUrl) {
        System.out.println("Network: Establishing secure connection to -> " + serverUrl);
    }
}

// 3. PROXY (The gateway containing smart interception logic before hitting the real service)
class ProxyInternet implements Internet {
    private Internet realInternet = new RealInternet();
    private static java.util.List<String> bannedSites;

    static {
        bannedSites = new java.util.ArrayList<>();
        bannedSites.add("facebook.com");
        bannedSites.add("instagram.com");
        bannedSites.add("twitter.com");
    }

    @Override
    public void connectTo(String serverUrl) throws Exception {
        // Step A: Perform Access Control / Security Check
        if (bannedSites.contains(serverUrl.toLowerCase())) {
            throw new SecurityException("Access Denied: '" + serverUrl + "' is blacklisted on this corporate network!");
        }

        // Step B: If validations clear, forward the execution to the real object
        realInternet.connectTo(serverUrl);
    }
}

// 4. CLIENT CODE
public class Main {
    public static void main(String[] args) {
        // The client code interacts strictly via the abstract interface type
        Internet officeNetwork = new ProxyInternet();

        try {
            System.out.println("=== Test Case 1: Accessing educational resource ===");
            officeNetwork.connectTo("leetcode.com");

            System.out.println("\n=== Test Case 2: Accessing blacklisted domain ===");
            officeNetwork.connectTo("instagram.com");
            
        } catch (Exception e) {
            System.out.println("Proxy Interception Rule Triggered: " + e.getMessage());
        }
    }
}