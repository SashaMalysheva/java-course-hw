package ru.spbau.mit.torrent.request;

import ru.spbau.mit.torrent.Seed;

import java.io.DataInputStream;
import java.io.IOException;


public interface SeedRequest extends Request {

    static SeedRequest read(Seed seed, DataInputStream dis) throws IOException, IllegalRequestFormatException {
        byte type = dis.readByte();
        SeedRequest seedRequest;
        switch (type) {
            case StatRequest.TYPE:
                seedRequest = new StatRequest(seed, dis.readInt());
                break;
            case GetRequest.TYPE:
                seedRequest = new GetRequest(seed, dis.readInt(), dis.readInt());
                break;
            default:
                throw new IllegalRequestFormatException("No such type of seed request " + Byte.toString(type));
        }
        return seedRequest;
    }
}
