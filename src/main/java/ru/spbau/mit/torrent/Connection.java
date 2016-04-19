package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.request.SeedRequest;
import ru.spbau.mit.torrent.request.TrackerRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public abstract class Connection implements AutoCloseable {
    private final Socket client;

    protected final DataInputStream dis;
    protected final DataOutputStream dos;

    public Connection(Socket client) throws IOException {
        this.client = client;
        this.dis = new DataInputStream(client.getInputStream());
        this.dos = new DataOutputStream(client.getOutputStream());
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    public abstract void handleRequest() throws IOException;

    public static  Connection withTracker(Tracker tracker, Socket client) throws IOException {
        return new Connection(client) {

            @Override
            public void handleRequest() throws IOException {
                TrackerRequest.read(tracker, client, dis).sendResponse(dos);
                dos.flush();
            }
        };
    }

    public static Connection withSeed(Seed seed, Socket client) throws IOException {
        return new Connection(client) {

            @Override
            public void handleRequest() throws IOException {
                SeedRequest.read(seed, dis).sendResponse(dos);
                dos.flush();
            }
        };
    }
}
