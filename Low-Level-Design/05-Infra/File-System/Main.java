import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class Node {
    final String name;

    Node(String name) {
        this.name = name;
    }

    abstract boolean isDirectory();
}

class FileNode extends Node {
    String content;

    FileNode(String name, String content) {
        super(name);
        this.content = content;
    }

    @Override
    boolean isDirectory() {
        return false;
    }
}

class DirectoryNode extends Node {
    final Map<String, Node> children = new LinkedHashMap<>();

    DirectoryNode(String name) {
        super(name);
    }

    @Override
    boolean isDirectory() {
        return true;
    }
}

class FileSystem {
    private final DirectoryNode root = new DirectoryNode("");
    private DirectoryNode cwd = root;

    String pwd() {
        // Reconstruct path by walking — for demo keep a stack
        return pathOf(cwd);
    }

    private final Map<DirectoryNode, DirectoryNode> parent = new LinkedHashMap<>();

    FileSystem() {
        parent.put(root, root);
    }

    private String pathOf(DirectoryNode dir) {
        if (dir == root) return "/";
        List<String> parts = new ArrayList<>();
        DirectoryNode cur = dir;
        while (cur != root) {
            parts.add(0, cur.name);
            cur = parent.get(cur);
        }
        return "/" + String.join("/", parts);
    }

    private DirectoryNode resolveDir(String path, boolean createMissing) {
        DirectoryNode cur = path.startsWith("/") ? root : cwd;
        if (path.equals("/")) return root;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.isEmpty()) return cur;
        String[] parts = normalized.split("/");
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                cur = parent.getOrDefault(cur, root);
                continue;
            }
            Node next = cur.children.get(part);
            if (next == null) {
                if (!createMissing) {
                    throw new IllegalArgumentException("Not found: " + part);
                }
                DirectoryNode created = new DirectoryNode(part);
                cur.children.put(part, created);
                parent.put(created, cur);
                cur = created;
            } else if (!next.isDirectory()) {
                throw new IllegalArgumentException("Not a directory: " + part);
            } else {
                cur = (DirectoryNode) next;
            }
        }
        return cur;
    }

    void mkdir(String path) {
        resolveDir(path, true);
        System.out.println("mkdir " + path);
    }

    void createFile(String path, String content) {
        int slash = path.lastIndexOf('/');
        String dirPath = slash <= 0 ? (path.startsWith("/") ? "/" : ".") : path.substring(0, slash);
        String fileName = slash < 0 ? path : path.substring(slash + 1);
        DirectoryNode dir = dirPath.equals(".") ? cwd : resolveDir(dirPath.equals("") ? "/" : dirPath, true);
        dir.children.put(fileName, new FileNode(fileName, content));
        System.out.println("file " + path);
    }

    void ls(String path) {
        DirectoryNode dir = (path == null || path.isEmpty()) ? cwd : resolveDir(path, false);
        System.out.println("ls " + pathOf(dir) + ":");
        for (Node n : dir.children.values()) {
            System.out.println("  " + (n.isDirectory() ? "d " : "f ") + n.name);
        }
    }

    void cd(String path) {
        cwd = resolveDir(path, false);
        System.out.println("cd -> " + pwd());
    }
}

public class Main {
    public static void main(String[] args) {
        FileSystem fs = new FileSystem();
        fs.mkdir("/home");
        fs.mkdir("/home/dev");
        fs.createFile("/home/dev/readme.txt", "hello LLD");
        fs.mkdir("/var/log");
        fs.ls("/");
        fs.cd("/home/dev");
        fs.ls(".");
        fs.cd("..");
        fs.ls(".");
        System.out.println("pwd=" + fs.pwd());
    }
}
