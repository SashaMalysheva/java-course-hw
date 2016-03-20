package ru.spbau.mit;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class ClientRunnable implements Runnable {
    private final Path path;
    private final Socket client;

    ClientRunnable(Path path, Socket client) {
        this.path = path;
        this.client = client;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream())){
            while (true) {
                int command = in.readInt();
                switch (command) {
                    case 1:
                        Path dir = path.resolve(in.readUTF());
                        if (!Files.isDirectory(dir)) {
                            out.writeInt(0);
                            return;
                        }
                        List<Path> content =
                                Files.list(dir)
                                        .sorted(Comparator.comparing(Path::getFileName))
                                        .collect(Collectors.toList());
                        out.writeInt(content.size());
                        for (Path inside : content) {
                            out.writeUTF(inside.getFileName().toString());
                            out.writeBoolean(Files.isDirectory(inside));
                        }
                        break;
                    case 2:
                        Path file = path.resolve(in.readUTF());
                        try (InputStream fin = Files.newInputStream(file)) {
                            out.writeInt((int) Files.size(file));
                            IOUtils.copyLarge(fin, out);
                        } catch (NoSuchFileException | UnsupportedOperationException e) {
                            out.writeInt(0);
                        }
                        break;
                    default:
                        throw new RuntimeException("Invalid command received from client");
                }
            }
        } catch (EOFException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (!client.isInputShutdown() && !client.isClosed()) {
                    client.shutdownInput();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (!client.isOutputShutdown() && !client.isClosed()) {
                    client.shutdownOutput();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
