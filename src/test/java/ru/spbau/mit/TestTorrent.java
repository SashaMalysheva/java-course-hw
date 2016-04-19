package ru.spbau.mit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.spbau.mit.torrent.*;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class TestTorrent {

    private static final Path EXAMPLE_PATH = Paths.get("src", "test", "resources", "checkstyle.xml");
    private static final Path CLIENT1_DIR = Paths.get("torrent", "client-01");
    private static final Path CLIENT2_DIR = Paths.get("torrent", "client-02");

    private FileEntry upload(Client client) throws IOException {
        return client.upload(
                TestTorrent.EXAMPLE_PATH.getFileName().toString(),
                Files.size(TestTorrent.EXAMPLE_PATH)
        );
    }

    @Test
    public void testListAndUpload() throws IOException {
        try (
                Tracker tracker = new Tracker();
                Client client1 = new Client("localhost", CLIENT1_DIR);
                Client client2 = new Client("localhost", CLIENT2_DIR)
        ) {
            Thread trackerThread = new Thread(tracker);
            trackerThread.start();

            assertEquals(Collections.emptyList(), client1.list());
            assertEquals(Collections.emptyList(), client2.list());

            FileEntry entry1 = upload(client1);
            FileEntry entry2 = upload(client2);
            assertNotEquals(entry1.getID(), entry2.getID());

            assertEquals(Arrays.asList(entry1, entry2), client1.list());
            assertEquals(Arrays.asList(entry1, entry2), client2.list());

            trackerThread.interrupt();
            trackerThread.join();
        } catch (InterruptedException ignored) {
        }
    }


    @Test
    public void testDownload() throws IOException {
        FileEntry entry;
        try (
                Tracker tracker = new Tracker();
                Client client2 = new Client("localhost", CLIENT2_DIR);
                Client client1 = new Client("localhost", CLIENT1_DIR)) {
            Thread trackerThread = new Thread(tracker);
            trackerThread.start();

            entry = upload(client1);

            FileState state = FileState.ownFile(EXAMPLE_PATH, entry.getID());
            client1.saveFileState(state);

            final int id = entry.getID();

            assertNotNull(client2
                            .list()
                            .stream()
                            .filter(a -> a.getID() == id)
                            .findAny()
                            .orElse(null)
            );

            // seeding
            try (Seed seed = client1.seed()) {
                Thread seedThread = new Thread(seed);
                seedThread.start();

            //downloading
                Thread.sleep(500);

                List<ClientInfo> seeds = client2.sources(id);
                ClientInfo seedInfo = seeds.get(0);

                List<Integer> parts = client2.stat(seedInfo.openSocket(), id);

                FileState newState = FileState.newFile(CLIENT2_DIR, entry);
                client2.saveFileState(newState);

                for (int part : parts) {
                    client2.get(seedInfo.openSocket(), newState, part);
                }

                seedThread.interrupt();
                seedThread.join();

                trackerThread.interrupt();
                trackerThread.join();

                assertTrue("Something missed", newState.allLoaded());
            }

            assertTrue("Files are different", FileUtils.contentEquals(
                    EXAMPLE_PATH.toFile(),
                    CLIENT2_DIR.resolve(EXAMPLE_PATH.getFileName().toString()).toFile()
            ));
        } catch (InterruptedException ignored) {
        }
    }

    @Before
    public void init() throws IOException {
        Files.createDirectories(CLIENT1_DIR);
        Files.createDirectories(CLIENT2_DIR);
    }

    @After
    public void clear() throws IOException {
        Path path = Paths.get("torrent");
        if (Files.exists(path)) {
            Files.walkFileTree(path, new DirectoryRemover());
        }
    }

    private static class DirectoryRemover extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return super.postVisitDirectory(dir, exc);
        }
    }
}
