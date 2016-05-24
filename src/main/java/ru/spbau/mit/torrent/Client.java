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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Client implements AutoCloseable {

    private static final String STATE_FILE = "client-state.dat";

    private final Map<Integer, FileState> files;
    private final Path path;
    private final String host;

    private volatile Downloader downloader;

    private Consumer<FileState> onFileStateAdded;
    private Consumer<FileState> onFileStateChanged;

    public Client(String host, Path path) throws IOException {
        this.path = path.toAbsolutePath();
        this.host = host;

        Path state = path.resolve(STATE_FILE);
        if (!Files.exists(state)) {
            Files.createFile(state);
        }

        ArrayList<FileState> list = new ArrayList<>();
        try (DataInputStream dis = new DataInputStream(Files.newInputStream(state))) {
            Collections.readFrom(dis, list, FileState::load);
        }
        this.files = list.stream()
                .collect(Collectors.toConcurrentMap(FileState::getID, Function.<FileState>identity()));
    }

    public ArrayList<FileState> getArrayOfFiles() {
        return new ArrayList<>(files.values());
    }

    private void store() throws IOException {
        DataOutputStream dos = new DataOutputStream(Files.newOutputStream(path.resolve(STATE_FILE)));
        Collections.writeTo(dos, files.values(), (stream, elem) -> elem.store(dos));
        dos.close();
    }

    public List<FileEntry> list() throws IOException {
        return fetchResponse(tracker(), new ListRequest()).getEntries();
    }

    public Path getPath() {
        return path;
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
        if (onFileStateChanged != null) {
            onFileStateChanged.accept(fileState);
        }
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
        if (onFileStateAdded != null) {
            onFileStateAdded.accept(fileState);
        }
    }

    public Downloader downloader() {
        if (downloader == null) {
            synchronized (this) {
                if (downloader == null) {
                    downloader = new Downloader(this);
                }
            }
        }
        return downloader;
    }

    public void setOnFileStateAdded(Consumer<FileState> onFileStateAdded) {
        this.onFileStateAdded = onFileStateAdded;
    }

    public void setOnFileStateChanged(Consumer<FileState> onFileStateChanged) {
        this.onFileStateChanged = onFileStateChanged;
    }

    @Override
    public void close() throws IOException {
        if (downloader != null) {
            synchronized (this) {
                if (downloader != null) {
                    downloader.close();
                }
            }
        }
        store();
        System.err.println("Client closed");
    }
}
