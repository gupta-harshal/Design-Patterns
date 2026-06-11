// 1. The Abstract Class defining the Template Method
abstract class DataMiner {
    
    // This is the Template Method. It defines the rigid skeleton of the algorithm.
    // Making it 'final' prevents subclasses from changing the execution order.
    public final void mineData(String filePath) {
        openFile(filePath);
        extractData();
        parseData();
        if (hookEnableReport()) { // A hook to conditionally run steps
            generateReport();
        }
        closeFile();
    }

    // Concrete methods implemented natively by the base class
    protected void openFile(String path) {
        System.out.println("Opening file stream from: " + path);
    }

    protected void closeFile() {
        System.out.println("Closing file stream safely.");
    }

    // Abstract methods that MUST be implemented by individual subclasses
    protected abstract void extractData();
    protected abstract void parseData();

    // A Hook: Subclasses can optionally override this to alter execution flow
    protected boolean hookEnableReport() {
        return true; 
    }
    
    protected void generateReport() {
        System.out.println("Default: Generating baseline analytical summary PDF.");
    }
}

// 2. Concrete Implementation 1: PDF Miner
class PdfDataMiner extends DataMiner {
    @Override
    protected void extractData() {
        System.out.println("Extracting raw byte elements using PDFBox parser...");
    }

    @Override
    protected void parseData() {
        System.out.println("Parsing unstructured PDF strings into keyword matrix tables.");
    }
}

// 3. Concrete Implementation 2: CSV Miner
class CsvDataMiner extends DataMiner {
    @Override
    protected void extractData() {
        System.out.println("Reading comma-separated rows using BufferedReader stream...");
    }

    @Override
    protected void parseData() {
        System.out.println("Mapping CSV strings directly to programmatic Key-Value columns.");
    }

    // Overriding the hook to turn off reporting for raw CSV data transfers
    @Override
    protected boolean hookEnableReport() {
        return false;
    }
}

// 4. Execution Driver
public class Main {
    public static void main(String[] args) {
        System.out.println("--- Processing PDF Invoice ---");
        DataMiner pdfMiner = new PdfDataMiner();
        pdfMiner.mineData("/docs/invoice.pdf");

        System.out.println("\n--- Processing Stock CSV ---");
        DataMiner csvMiner = new CsvDataMiner();
        csvMiner.mineData("/data/ticks.csv");
    }
}