import java.util.ArrayList;
import java.util.List;

// 1. The Iterator Interface
interface Iterator<T> {
    boolean hasNext();
    T next();
}

// 2. The Aggregate Interface
interface Aggregate<T> {
    Iterator<T> createIterator();
}

// 3. The Concrete Aggregate (A custom Channel Playlist)
class Playlist implements Aggregate<String> {
    private List<String> videos = new ArrayList<>();

    public void addVideo(String title) {
        videos.add(title);
    }

    @Override
    public Iterator<String> createIterator() {
        return new PlaylistIterator(this.videos);
    }
}

// 4. The Concrete Iterator
class PlaylistIterator implements Iterator<String> {
    private List<String> videos;
    private int position = 0;

    public PlaylistIterator(List<String> videos) {
        this.videos = videos;
    }

    @Override
    public boolean hasNext() {
        return position < videos.size();
    }

    @Override
    public String next() {
        if (!hasNext()) {
            return null;
        }
        return videos.get(position++);
    }
}

// 5. Execution Demo
public class Main {
    public static void main(String[] args) {
        Playlist designPatternsPlaylist = new Playlist();
        designPatternsPlaylist.addVideo("1. Introduction to Creational Patterns");
        designPatternsPlaylist.addVideo("2. Deep Dive into Observer Pattern");
        designPatternsPlaylist.addVideo("3. Mastering Iterator Pattern");

        // Obtain the iterator
        Iterator<String> iterator = designPatternsPlaylist.createIterator();

        // Traverse the collection without knowing how it is stored under the hood
        System.out.println("Streaming Playlist Videos:");
        while (iterator.hasNext()) {
            String video = iterator.next();
            System.out.println("- Watch: " + video);
        }
    }
}