package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.util.Collections;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class Tracker extends Server {

    public static final String TRACKER_STATE_FILE = "tracker-state.dat";
    public static final int SERVER_PORT = 8081;

    private final List<FileEntry> files = new ArrayList<>();
    private final Set<ClientInfo> clientInfoSet = ConcurrentHashMap.newKeySet();

    private final Path stateFile;

    public Tracker(Path root) throws IOException {
        super(SERVER_PORT);

        stateFile = root.resolve(TRACKER_STATE_FILE);
        if (!Files.exists(stateFile)) {
            Files.createFile(stateFile);
        }
        Collections.readFrom(new DataInputStream(Files.newInputStream(stateFile)), files, FileEntry::load);

        getScheduler().scheduleWithFixedDelay(
                () -> {
                    clientInfoSet.removeIf(clientInfo -> !clientInfo.updated);
                    clientInfoSet.forEach(clientInfo -> clientInfo.updated = false);
                    System.err.println("Tracker: now tracking - " + clientInfoSet.toString());
                },
                0, 1, TimeUnit.MINUTES
        );
    }

    @Override
    public void handleConnection(Socket socket) {
        try (Connection connection = Connection.withTracker(this, socket)) {
            connection.handleRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<FileEntry> getFiles() {
        return files;
    }

    public Set<ClientInfo> getClientInfoSet() {
        return clientInfoSet;
    }

    @Override
    public void close() throws IOException {
        Collections.writeTo(
                new DataOutputStream(Files.newOutputStream(stateFile)), files,
                (dos, resource) -> resource.store(dos)
        );
        super.close();
    }
}
