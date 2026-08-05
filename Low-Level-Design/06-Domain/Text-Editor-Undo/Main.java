import java.util.ArrayDeque;
import java.util.Deque;

class Editor {
    private final StringBuilder content = new StringBuilder();

    String getContent() {
        return content.toString();
    }

    void insert(int index, String text) {
        content.insert(index, text);
    }

    String delete(int index, int length) {
        String removed = content.substring(index, index + length);
        content.delete(index, index + length);
        return removed;
    }
}

interface EditorCommand {
    void execute();
    void undo();
}

class InsertCommand implements EditorCommand {
    private final Editor editor;
    private final int index;
    private final String text;

    InsertCommand(Editor editor, int index, String text) {
        this.editor = editor;
        this.index = index;
        this.text = text;
    }

    @Override
    public void execute() {
        editor.insert(index, text);
    }

    @Override
    public void undo() {
        editor.delete(index, text.length());
    }
}

class DeleteCommand implements EditorCommand {
    private final Editor editor;
    private final int index;
    private final int length;
    private String removed;

    DeleteCommand(Editor editor, int index, int length) {
        this.editor = editor;
        this.index = index;
        this.length = length;
    }

    @Override
    public void execute() {
        removed = editor.delete(index, length);
    }

    @Override
    public void undo() {
        editor.insert(index, removed);
    }
}

class History {
    private final Deque<EditorCommand> undo = new ArrayDeque<>();
    private final Deque<EditorCommand> redo = new ArrayDeque<>();

    void execute(EditorCommand cmd) {
        cmd.execute();
        undo.push(cmd);
        redo.clear();
    }

    void undo() {
        if (undo.isEmpty()) {
            System.out.println("Nothing to undo");
            return;
        }
        EditorCommand cmd = undo.pop();
        cmd.undo();
        redo.push(cmd);
    }

    void redo() {
        if (redo.isEmpty()) {
            System.out.println("Nothing to redo");
            return;
        }
        EditorCommand cmd = redo.pop();
        cmd.execute();
        undo.push(cmd);
    }
}

public class Main {
    public static void main(String[] args) {
        Editor editor = new Editor();
        History history = new History();

        history.execute(new InsertCommand(editor, 0, "Hello"));
        history.execute(new InsertCommand(editor, 5, " World"));
        System.out.println(editor.getContent()); // Hello World

        history.execute(new DeleteCommand(editor, 5, 6));
        System.out.println(editor.getContent()); // Hello

        history.undo();
        System.out.println("undo -> " + editor.getContent()); // Hello World

        history.undo();
        System.out.println("undo -> " + editor.getContent()); // Hello

        history.redo();
        System.out.println("redo -> " + editor.getContent()); // Hello World
    }
}
