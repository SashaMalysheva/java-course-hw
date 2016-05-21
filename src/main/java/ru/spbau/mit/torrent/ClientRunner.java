package ru.spbau.mit.torrent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ClientRunner {

    private static final int ARG_TRACKER = 0;
    private static final int ARG_TYPE = 1;
    private static final int ARG_UPLOADED_PATH = 2;

    public static final String TYPE_LIST = "list";
    public static final String TYPE_UPLOAD = "upload";
    public static final String TYPE_RUN = "run";

    private final Client client;

    private ClientRunner(Client client) {
        this.client = client;
    }

    private void doRun() throws IOException {
        ExecutorService loaders = Executors.newCachedThreadPool();
        List<FileEntry> entries = client.list();
        for (FileEntry entry : entries) {
            FileState state = client.getFileState(entry.getID());
            if (state == null) {
                state = FileState.newFile(Paths.get(""), entry);
                client.saveFileState(state);
            }
            final FileState fState = state;
            if (state.hasMissedParts()) {
                loaders.submit(() -> {
                    try {
                        download(fState);
                        System.out.printf("File %s with %d ID %s downloaded\n",
                                fState.entry().getName(),
                                fState.getID(),
                                fState.hasMissedParts() ? "partly" : "fully");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        try (Seed seed = client.seed()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    seed.close();
                } catch (IOException ignored) {
                }
            }));
            seed.run();
        }
    }

    private void download(FileState state) throws IOException {
        int id = state.getID();
        List<ClientInfo> seeds = client.sources(id);
        for (ClientInfo seed : seeds) {
            List<Integer> parts = client.stat(seed.openSocket(), id);
            parts = state.onlyMissed(parts);
            for (int part : parts) {
                client.get(seed.openSocket(), state, part);
            }
        }
    }

    private void doUpload(String pathName) throws IOException {
        Path path = Paths.get(pathName);
        if (!Files.isRegularFile(path)) {
            System.out.printf("Invalid path %s has provided\n", path.toString());
            return;
        }
        String name = path.getFileName().toString();
        long size = Files.size(path);
        FileEntry entry = client.upload(name, size);
        FileState state = FileState.ownFile(client.getPath().relativize(path), entry.getID());
        client.saveFileState(state);
        System.out.printf("File uploaded to tracker and assigned by %d ID\n", entry.getID());
    }

    private void doList() throws IOException {
        System.out.println("Available files:");
        client.list().forEach(fileEntry -> System.out.printf(
                "%d: %s (%d bytes).\n",
                fileEntry.getID(),
                fileEntry.getName(),
                fileEntry.getSize()
        ));
    }

    public static void main(String[] args) {
        Objects.requireNonNull(args);
        if (args.length <= ARG_TYPE) {
            throw new IllegalArgumentException("Too less arguments");
        }
        try (Client client = new Client(args[ARG_TRACKER], Paths.get(""))) {
            ClientRunner clientRunner = new ClientRunner(client);
            switch (args[ARG_TYPE]) {
                case TYPE_LIST:
                    clientRunner.doList();
                    break;
                case TYPE_UPLOAD:
                    clientRunner.doUpload(args[ARG_UPLOADED_PATH]);
                    break;
                case TYPE_RUN:
                    clientRunner.doRun();
                    break;
                default:
                    throw new IllegalArgumentException("No such command");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
