package ru.spbau.mit.torrent.request;

import ru.spbau.mit.torrent.Seed;
import ru.spbau.mit.torrent.util.Collections;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatRequest implements SeedRequest {

    public static final int TYPE = 1;


    private final int id;
    private final List<Integer> availableParts;

    public List<Integer> getAvailableParts() {
        return availableParts;
    }

    public StatRequest(int id) {
        this.id = id;
        this.availableParts = new ArrayList<>();
    }

    public StatRequest(Seed seed, int id) {
        this.id = id;
        this.availableParts = seed.getClient().getFileState(id).loadedParts();
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeByte(TYPE);
        dos.writeInt(id);
    }

    @Override
    public void readResponse(DataInputStream dis) throws IOException {
        Collections.readFrom(dis, availableParts, DataInputStream::readInt);
    }

    @Override
    public void sendResponse(DataOutputStream dos) throws IOException {
        Collections.writeTo(dos, availableParts, DataOutputStream::writeInt);
    }
}
