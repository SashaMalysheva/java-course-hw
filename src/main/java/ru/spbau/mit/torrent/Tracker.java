package ru.spbau.mit.torrent;

import java.io.*;
import java.net.Socket;
import java.util.*;

public final class Tracker extends Server {

    public static final int SERVER_PORT = 8081;
    public static final long UPDATE_TIMEOUT = 60000;

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
