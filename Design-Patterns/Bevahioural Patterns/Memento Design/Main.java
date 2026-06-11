import java.util.Stack;

// 1. The Memento Object (Immutable snapshot box containing the saved state)
class EditorMemento {
    private final String content; // Marked final to ensure immutability

    public EditorMemento(String content) {
        this.content = content;
    }

    // Package-private access so only the TextEditor can read this value
    protected String getContent() {
        return content;
    }
}

// 2. The Originator (The main object whose state needs tracking/restoring)
class TextEditor {
    private String content = "";

    public void type(String text) {
        content += text;
    }

    public String getContent() {
        return content;
    }

    // Creates a new snapshot checkpoint
    public EditorMemento save() {
        System.out.println("[Saving State]: \"" + content + "\"");
        return new EditorMemento(content);
    }

    // Restores state back from a specific memento instance
    public void restore(EditorMemento memento) {
        if (memento != null) {
            this.content = memento.getContent();
            System.out.println("[Restored State]: \"" + content + "\"");
        }
    }
}

// 3. The Caretaker (Manages the history stack of checkpoints; never alters snapshots)
class HistoryTracker {
    private final Stack<EditorMemento> history = new Stack<>();

    public void push(EditorMemento memento) {
        history.push(memento);
    }

    public EditorMemento pop() {
        if (!history.isEmpty()) {
            return history.pop();
        }
        System.out.println("No history states left to undo.");
        return null;
    }
}

// 4. Execution Driver
public class Main {
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        HistoryTracker history = new HistoryTracker();

        // 1. User starts typing text
        editor.type("Design ");
        history.push(editor.save()); // Save checkpoint 1

        // 2. User types more text
        editor.type("Patterns ");
        history.push(editor.save()); // Save checkpoint 2

        // 3. User makes a typing mistake
        editor.type("are completely boring errors!");
        System.out.println("Current Editor Text: " + editor.getContent());

        // 4. Hit Undo once (Pops the mistake state, falls back to checkpoint 2)
        System.out.println("\n--- Triggering First Undo ---");
        editor.restore(history.pop()); 
        
        // 5. Hit Undo again (Falls back to checkpoint 1)
        System.out.println("\n--- Triggering Second Undo ---");
        editor.restore(history.pop());

        System.out.println("\nFinal Screen Output: " + editor.getContent());
    }
}