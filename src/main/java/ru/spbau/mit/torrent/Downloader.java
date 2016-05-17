package ru.spbau.mit.torrent;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Downloader implements AutoCloseable {
    private final Client client;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public Downloader(Client client) {
        this.client = client;
    }

    void submitToDownload(FileState state) {
        threadPool.execute(() -> {
            System.err.println("Loading file " + state.getFileEntry());
            while (state.hasMissedParts()) {
                try {
                    System.err.println("Getting seeds for " + state.getFileEntry());
                    List<ClientInfo> clients = client.sources(state.getID());
                    for (ClientInfo cl : clients) {
                        for (int part : state.onlyMissed(client.stat(cl.openSocket(), state.getID()))) {
                            System.err.println("Loading " + part + " part for " + state.getFileEntry());
                            client.get(cl.openSocket(), state, part);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void close() {
        threadPool.shutdown();
    }
}
