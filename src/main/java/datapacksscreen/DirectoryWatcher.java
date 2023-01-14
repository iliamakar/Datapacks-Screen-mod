package datapacksscreen;

import com.sun.nio.file.ExtendedWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectoryWatcher implements Runnable {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final List<Path> changed;
    private final List<Path> deleted;
    private boolean isDirDeleted = false;
    private Thread thread;

    public DirectoryWatcher(Path dir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.changed = new ArrayList<Path>();
        this.deleted = new ArrayList<Path>();
        register(dir);
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY}, ExtendedWatchEventModifier.FILE_TREE);
        keys.put(key, dir);
    }

    private void registerAll(Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void start() {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        try {
            watcher.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        thread.interrupt();
    }

    @Override
    public void run() {
        for (;;) {

            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                return;
            }
;
            Path dir = keys.get(key);

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                WatchEvent<Path> event2 = (WatchEvent<Path>) event;
                Path name = event2.context();
                Path path = dir.resolve(name);

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    onEntryCreate(path);
                }

                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    onEntryDelete(path);
                }

                if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    onEntryModify(path);
                }

                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);
                }
            }
        }
    }

    private void onEntryCreate(Path path) {
        if (path.getFileName().toString().contains(".")) {
            if (!changed.contains(path)) {
                //System.out.println("ADD " + path);
                changed.add(path);
                deleted.remove(path);
            }
        }
    }

    private void onEntryDelete(Path path) {
        if (path.getFileName().toString().contains(".")) {
            if (!deleted.contains(path)) {
               // System.out.println("DEL " + path);
                deleted.add(path);
                changed.remove(path);
            }
        } else {
            isDirDeleted = true;
        }
    }

    private void onEntryModify(Path path) {
        if (path.getFileName().toString().contains(".")) {
            if (!changed.contains(path)) {
                // System.out.println("MOD " + path);
                changed.add(path);
                deleted.remove(path);
            }
        }
    }

    public List<Path> getChanged() {
        return changed;
    }

    public List<Path> getDeleted() {
        return deleted;
    }

    public boolean isDirDeleted() {
        return isDirDeleted;
    }

    public void reset() {
        changed.clear();
        deleted.clear();
        isDirDeleted = false;
    }
}