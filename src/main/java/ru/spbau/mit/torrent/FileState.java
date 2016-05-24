package ru.spbau.mit.torrent;


import ru.spbau.mit.torrent.util.Collections;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileState {

    private static final int FULL_PART_SIZE = 1 * 1024 * 1024;
    private static final int BUFFER_SIZE = 4 * 1024;

    enum PartState {
        LOADED, LOADING, MISSED;
        boolean missed() {
            return this == MISSED;
        }
        boolean loaded() {
            return this == LOADED;
        }
    }

    private final List<PartState> partStateList;
    private final FileEntry fileEntry;
    private final Path local;

    public FileEntry getFileEntry() {
        return fileEntry;
    }

    private FileState(List<PartState> partStateList, FileEntry fileEntry) throws IOException {
        this(partStateList, fileEntry, Paths.get(fileEntry.getName()));
    }

    private FileState(List<PartState> partStateList, FileEntry fileEntry, Path local) throws IOException {
        this.partStateList = partStateList;
        this.fileEntry = fileEntry;
        this.local = local;
        if (!Files.exists(local)) {
            Files.createFile(local);
        }
    }

    private PartState getPartStateList(int i) {
        synchronized (partStateList) {
            return partStateList.get(i);
        }
    }

    private int realPartSize(int part) {
        if (part == partStateList.size() - 1) {
            int tmp = (int) fileEntry.getSize() % FULL_PART_SIZE;
            return tmp == 0 ? FULL_PART_SIZE : tmp;
        }
        return FULL_PART_SIZE;
    }

    public void readPart(int part, DataOutputStream dos) throws IOException {
        if (getPartStateList(part) != PartState.LOADED) {
            throw new IOException("Such part hasn't loaded");
        }
        try (RandomAccessFile file = new RandomAccessFile(local.toFile(), "r")) {
            file.seek(part * FULL_PART_SIZE);

            byte[] buffer = new byte[BUFFER_SIZE];
            int partSize = realPartSize(part);
            while (partSize > 0) {
                int read = file.read(buffer, 0, Math.min(partSize, BUFFER_SIZE));
                if (read == -1) {
                    throw new EOFException("File is shorter than recorded size.");
                }
                partSize -= read;
                dos.write(buffer, 0, read);
            }
        }
    }

    public void writePart(int part, DataInputStream dis) throws IOException {
        synchronized (partStateList) {
            if (partStateList.get(part) != PartState.MISSED) {
                throw new IOException("Such part has loaded or is loading");
            }
            partStateList.set(part, PartState.LOADING);
        }
        try (RandomAccessFile file = new RandomAccessFile(local.toFile(), "rw")) {
            file.seek(part * FULL_PART_SIZE);

            byte[] buffer = new byte[BUFFER_SIZE];
            int partSize = realPartSize(part);
            while (partSize > 0) {
                int read = dis.read(buffer, 0, Math.min(partSize, BUFFER_SIZE));
                if (read == -1) {
                    throw new EOFException("Cannot read the end of the file from socket.");
                }
                partSize -= read;
                file.write(buffer, 0, read);
            }
        } catch (final Exception e) {
            synchronized (partStateList) {
                partStateList.set(part, PartState.MISSED);
            }
            throw e;
        }
        synchronized (partStateList) {
            partStateList.set(part, PartState.LOADED);
        }
    }

    public int getID() {
        return fileEntry.getID();
    }


    public static FileState load(DataInputStream dis) throws IOException {
        FileEntry fileEntry = FileEntry.load(dis);
        List<PartState> list = new ArrayList<>();
        Collections.readFrom(dis, list, stream -> stream.readBoolean() ? PartState.LOADED : PartState.MISSED);
        Path dir = Paths.get(dis.readUTF());
        return new FileState(list, fileEntry, dir);
    }

    public void store(DataOutputStream dos) throws IOException {
        fileEntry.store(dos);
        Collections.writeTo(dos, partStateList, (stream, state) -> stream.writeBoolean(state == PartState.LOADED));
        dos.writeUTF(local.toString());
    }

    public boolean hasMissedParts() {
        return partStateList.stream().anyMatch(PartState::missed);
    }

    public boolean allLoaded() {
        return partStateList.stream().allMatch(PartState::loaded);
    }

    public List<Integer> loadedParts() {
        return Stream.iterate(0, (i) -> i + 1)
                .limit(partStateList.size())
                .filter(i -> partStateList.get(i).loaded())
                .collect(Collectors.toList());
    }

    public List<Integer> onlyMissed(List<Integer> parts) {
        return parts.stream().filter(i -> partStateList.get(i).missed()).collect(Collectors.toList());
    }

    public FileEntry entry() {
        return fileEntry;
    }

    public static FileState newFile(Path dir, FileEntry entry) throws IOException {
        int parts = parts(entry.getSize());
        List<PartState> partStateList = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            partStateList.add(PartState.MISSED);
        }
        return new FileState(partStateList, entry, dir.resolve(entry.getName()));
    }

    public static FileState ownFile(Path path, int id) throws IOException {
        long size = Files.size(path);
        int parts = parts(size);
        List<PartState> partStateList = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            partStateList.add(PartState.LOADED);
        }
        return new FileState(
                partStateList,
                new FileEntry(id, path.getFileName().toString(), size),
                path
        );
    }

    public int loadedPartsCount() {
        return partStateList.stream()
                .mapToInt((state) -> state == PartState.LOADED ? 1 : 0)
                .sum();
    }

    public  int getPartsCount() {
        return partStateList.size();
    }

    private static int parts(long size) {
        return (int) ((size + FULL_PART_SIZE - 1) / FULL_PART_SIZE);
    }


}
