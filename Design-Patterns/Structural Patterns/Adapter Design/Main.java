// 1. TARGET INTERFACE (What your application expects)
interface JsonPrinter {
    void printJson(String jsonData);
}

// 2. ADAPTEE (The incompatible third-party library you want to use)
class AdvancedXmlPrinter {
    public void printXml(String xmlData) {
        System.out.println("Printing XML Data securely: " + xmlData);
    }
}

// 3. ADAPTER (The middleman translating JSON to XML)
class JsonToXmlAdapter implements JsonPrinter {
    private AdvancedXmlPrinter xmlPrinter;

    // Pass the incompatible object via constructor
    public JsonToXmlAdapter(AdvancedXmlPrinter xmlPrinter) {
        this.xmlPrinter = xmlPrinter;
    }

    @Override
    public void printJson(String jsonData) {
        // Step A: Convert JSON string to XML string (Simulated logic)
        String convertedXml = "<xml>" + jsonData.replace("{", "").replace("}", "") + "</xml>";
        
        // Step B: Delegate the work to the XML printer
        xmlPrinter.printXml(convertedXml);
    }
}

// 4. CLIENT (Your main application execution)
public class Main {
    public static void main(String[] args) {
        // Existing legacy setup or 3rd party tool
        AdvancedXmlPrinter thirdPartyPrinter = new AdvancedXmlPrinter();
        
        // Wrap it inside our Adapter
        JsonPrinter adapter = new JsonToXmlAdapter(thirdPartyPrinter);
        
        // The client code stays clean and uses the interface it expects
        String clientJsonData = "{'user': 'Harshal', 'status': 'Coding'}";
        adapter.printJson(clientJsonData);
    }
}