package ru.spbau.mit.torrent;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;


public final class Seed extends Server {

    private final Client client;

    public Seed(Client client) throws IOException {
        super(0);
        this.client = client;
        getScheduler().scheduleWithFixedDelay(() -> {
            try {
                System.err.print("Seed updating...");
                client.update(getPort(), client.availableFiles());
                System.err.println("done");
            } catch (IOException e) {
                System.err.println("failed");
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    protected void handleConnection(Socket socket) {
        try (Connection connection = Connection.withSeed(this, socket)) {
            connection.handleRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Client getClient() {
        return client;
    }
}
