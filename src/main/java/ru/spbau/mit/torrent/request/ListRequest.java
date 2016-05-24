package ru.spbau.mit.torrent.request;

import ru.spbau.mit.torrent.FileEntry;
import ru.spbau.mit.torrent.Tracker;
import ru.spbau.mit.torrent.util.Collections;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListRequest implements TrackerRequest {

    public static final int TYPE = 1;

    private final List<FileEntry> entries;

    public ListRequest() {
        this.entries = new ArrayList<>();
    }

    public ListRequest(Tracker tracker) {
        this.entries = tracker.getFiles();
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeByte(TYPE);
    }

    @Override
    public void readResponse(DataInputStream dis) throws IOException {
        entries.clear();
        Collections.readFrom(dis, entries, (stream) ->
                        new FileEntry(stream.readInt(), stream.readUTF(), stream.readLong())
        );
    }

    @Override
    public void sendResponse(DataOutputStream dos) throws IOException {
        Collections.writeTo(dos, entries, (stream, entry) -> {
            stream.writeInt(entry.getID());
            stream.writeUTF(entry.getName());
            stream.writeLong(entry.getSize());
        });
    }

    public List<FileEntry> getEntries() {
        return entries;
    }
}
