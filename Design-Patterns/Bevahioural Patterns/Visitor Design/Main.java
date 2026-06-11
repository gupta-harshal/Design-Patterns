// 1. The Visitor Interface (Defines operation entry points for each concrete element)
interface ReportVisitor {
    void visit(TextFile file);
    void visit(VideoFile file);
}

// 2. The Element Interface (Enables components to accept any visitor dynamically)
interface DocumentElement {
    void accept(ReportVisitor visitor);
}

// 3. Concrete Element A: Text File
class TextFile implements DocumentElement {
    private String fileName;
    private int wordCount;

    public TextFile(String fileName, int wordCount) {
        this.fileName = fileName;
        this.wordCount = wordCount;
    }

    public String getFileName() { return fileName; }
    public int getWordCount() { return wordCount; }

    @Override
    public void accept(ReportVisitor visitor) {
        // Double Dispatch: Element hands control over to the visitor, passing itself
        visitor.visit(this);
    }
}

// 4. Concrete Element B: Video File
class VideoFile implements DocumentElement {
    private String title;
    private int durationInSeconds;

    public VideoFile(String title, int durationInSeconds) {
        this.title = title;
        this.durationInSeconds = durationInSeconds;
    }

    public String getTitle() { return title; }
    public int getDurationInSeconds() { return durationInSeconds; }

    @Override
    public void accept(ReportVisitor visitor) {
        visitor.visit(this);
    }
}

// 5. Concrete Visitor 1: Generates a plain text metadata summary
class MetadataExtractor implements ReportVisitor {
    @Override
    public void visit(TextFile file) {
        System.out.println("Text Metadata -> File: " + file.getFileName() + ", Words: " + file.getWordCount());
    }

    @Override
    public void visit(VideoFile file) {
        System.out.println("Video Metadata -> Title: " + file.getTitle() + ", Runtime: " + file.getDurationInSeconds() + "s");
    }
}

// 6. Concrete Visitor 2: Calculates disk storage or cloud hosting costs
class HostingCostCalculator implements ReportVisitor {
    @Override
    public void visit(TextFile file) {
        // Low cost for text
        double cost = file.getWordCount() * 0.001;
        System.out.println("Text Storage Cost estimate for " + file.getFileName() + ": $" + String.format("%.4f", cost));
    }

    @Override
    public void visit(VideoFile file) {
        // High cost for video streaming bandwidth
        double cost = file.getDurationInSeconds() * 0.15;
        System.out.println("Video Bandwidth Cost estimate for " + file.getTitle() + ": $" + String.format("%.2f", cost));
    }
}

// 7. Execution Driver
public class Main {
    public static void main(String[] args) {
        // A diverse collection of object elements
        DocumentElement[] library = new DocumentElement[] {
            new TextFile("readme.txt", 450),
            new VideoFile("tutorial.mp4", 1200),
            new TextFile("essay.docx", 2300)
        };

        // Instantiate completely separate business operations (Visitors)
        ReportVisitor extractor = new MetadataExtractor();
        ReportVisitor costCalculator = new HostingCostCalculator();

        System.out.println("--- Operation 1: Extracting Metadata ---");
        for (DocumentElement element : library) {
            element.accept(extractor);
        }

        System.out.println("\n--- Operation 2: Calculating Financial Costs ---");
        for (DocumentElement element : library) {
            element.accept(costCalculator);
        }
    }
}