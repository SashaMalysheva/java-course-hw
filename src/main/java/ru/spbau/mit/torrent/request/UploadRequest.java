package ru.spbau.mit.torrent.request;

import ru.spbau.mit.torrent.FileEntry;
import ru.spbau.mit.torrent.Tracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class UploadRequest implements TrackerRequest {

    public static final int TYPE = 2;


    private final long size;
    private final String name;
    private int id;

    public UploadRequest(String name, long size) {
        this.size = size;
        this.name = name;
    }

    public UploadRequest(Tracker tracker, String name, long size) {
        this.name = name;
        this.size = size;
        List<FileEntry> entries = tracker.getFiles();
        synchronized (entries) {
            id = entries.size();
            entries.add(new FileEntry(id, name, size));
        }
    }
    
    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeByte(TYPE);
        dos.writeUTF(name);
        dos.writeLong(size);
    }

    @Override
    public void readResponse(DataInputStream dis) throws IOException {
        id = dis.readInt();
    }

    @Override
    public void sendResponse(DataOutputStream dos) throws IOException {
        dos.writeInt(id);
    }

    public FileEntry getEntry() {
        return new FileEntry(id, name, size);
    }
}
