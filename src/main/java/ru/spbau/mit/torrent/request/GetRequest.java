package ru.spbau.mit.torrent.request;

import ru.spbau.mit.torrent.FileState;
import ru.spbau.mit.torrent.Seed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GetRequest implements SeedRequest {

    public static final int TYPE = 2;

    private final int part;
    private final FileState fileState;

    public GetRequest(int part, FileState fileState) {
        this.part = part;
        this.fileState = fileState;
    }

    public GetRequest(Seed seed, int id, int part) {
        this.part = part;
        this.fileState = seed.getClient().getFileState(id);
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeByte(TYPE);
        dos.writeInt(fileState.getID());
        dos.writeInt(part);
    }

    @Override
    public void readResponse(DataInputStream dis) throws IOException {
        fileState.writePart(part, dis);
    }

    @Override
    public void sendResponse(DataOutputStream dos) throws IOException {
        fileState.readPart(part, dos);
    }
}
