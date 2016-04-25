package ru.spbau.mit.torrent;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Tracker extends Server {

    public static final int SERVER_PORT = 8081;

    private final List<FileEntry> files = new ArrayList<>();
    private final Set<ClientInfo> clientInfoSet = new HashSet<>();

    public Tracker() throws IOException {
        super(SERVER_PORT);
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
}
