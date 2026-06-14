import java.util.ArrayList;
import java.util.List;

// 1. COMPONENT (The uniform interface for both individual and group objects)
interface FileSystemComponent {
    void showDetails(String indent);
    long getSize();
}

// 2. LEAF (Individual atomic objects that have no children)
class File implements FileSystemComponent {
    private String name;
    private long size;

    public File(String name, long size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public void showDetails(String indent) {
        System.out.println(indent + "- File: " + name + " (" + size + " KB)");
    }

    @Override
    public long getSize() {
        return this.size;
    }
}

// 3. COMPOSITE (Group objects that store references to other Components)
class Directory implements FileSystemComponent {
    private String name;
    private List<FileSystemComponent> components = new ArrayList<>();

    public Directory(String name) {
        this.name = name;
    }

    public void addComponent(FileSystemComponent component) {
        components.add(component);
    }

    public void removeComponent(FileSystemComponent component) {
        components.remove(component);
    }

    @Override
    public void showDetails(String indent) {
        System.out.println(indent + "+ Directory: " + name);
        for (FileSystemComponent component : components) {
            // Recursively trigger the same method down the tree structure
            component.showDetails(indent + "  ");
        }
    }

    @Override
    public long getSize() {
        long totalSize = 0;
        for (FileSystemComponent component : components) {
            totalSize += component.getSize();
        }
        return totalSize;
    }
}

// 4. CLIENT
public class Main {
    public static void main(String[] args) {
        // Individual files (Leafs)
        File file1 = new File("SOP.pdf", 465);
        File file2 = new File("AdapterPattern.java", 12);
        File file3 = new File("Avatar.mp4", 2500000);

        // Sub-directory
        Directory subDir = new Directory("College_Prep");
        subDir.addComponent(file1);
        subDir.addComponent(file2);

        // Root directory containing files and the sub-directory
        Directory rootDir = new Directory("Root");
        rootDir.addComponent(file3);
        rootDir.addComponent(subDir); // Composite inside Composite

        // Treat individual and composite objects identically via the interface
        System.out.println("=== Displaying Directory Tree ===");
        rootDir.showDetails("");

        System.out.println("\n=== Total Size Calculation ===");
        System.out.println("Total Space Used: " + rootDir.getSize() + " KB");
    }
}