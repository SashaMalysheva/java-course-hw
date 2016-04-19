package ru.spbau.mit.torrent;

import ru.spbau.mit.torrent.request.*;
import ru.spbau.mit.torrent.util.Collections;
import ru.spbau.mit.torrent.util.Sockets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Client implements AutoCloseable {

    private static final String STATE_FILE = "client-state.dat";

    private final Map<Integer, FileState> files;

    private final Path path;

    private final String host;

    public Client(String host, Path path) throws IOException {
        this.path = path;
        this.host = host;

        Path state = path.resolve(STATE_FILE);
        if (!Files.exists(state)) {
            Files.createFile(state);
        }
        DataInputStream dis = new DataInputStream(Files.newInputStream(state));
        ArrayList<FileState> list = new ArrayList<>();
        Collections.readFrom(dis, list, FileState::load);

        dis.close();
        this.files = list.stream().collect(Collectors.toMap(FileState::getID, Function.<FileState>identity()));
    }

    private void store() throws IOException {
        DataOutputStream dos = new DataOutputStream(Files.newOutputStream(path.resolve(STATE_FILE)));
        Collections.writeTo(dos, files.values(), (stream, elem) -> elem.store(dos));
        dos.close();
    }

    public List<FileEntry> list() throws IOException {
        return fetchResponse(tracker(), new ListRequest()).getEntries();
    }

    public FileEntry upload(String name, long size) throws IOException {
        return fetchResponse(tracker(), new UploadRequest(name, size)).getEntry();
    }

    public List<ClientInfo> sources(int id) throws IOException {
        return fetchResponse(tracker(), new SourcesRequest(id)).getClients();
    }

    public boolean update(int seedPort, Set<Integer> availableFiles) throws IOException {
        return fetchResponse(tracker(), new UpdateRequest(seedPort, availableFiles)).isRes();
    }

    public List<Integer> stat(Socket seed, int id) throws IOException {
        return fetchResponse(seed, new StatRequest(id)).getAvailableParts();
    }

    public void get(Socket seed, FileState fileState, int part) throws IOException {
        fetchResponse(seed, new GetRequest(part, fileState));
    }

    public Seed seed() throws IOException {
        return new Seed(this);
    }

    private <R extends Request> R fetchResponse(Socket server, R request) throws IOException {
        try (Socket s = server) {
            request.send(Sockets.socketOutput(s));
            Sockets.socketOutput(s).flush();
            request.readResponse(Sockets.socketInput(s));
            return request;
        }
    }

    private Socket tracker() throws IOException {
        return new Socket(host, Tracker.SERVER_PORT);
    }

    public Set<Integer> availableFiles() {
        return files.keySet();
    }

    public FileState getFileState(int id) {
        return files.get(id);
    }

    public void saveFileState(FileState fileState) {
        files.put(fileState.getID(), fileState);
    }

    @Override
    public void close() throws IOException {
        store();
        System.err.println("Client closed");
    }
}
