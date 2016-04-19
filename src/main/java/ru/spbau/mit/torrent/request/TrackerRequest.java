package ru.spbau.mit.torrent.request;


import ru.spbau.mit.torrent.Tracker;
import ru.spbau.mit.torrent.util.Collections;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public interface TrackerRequest extends Request {

    static TrackerRequest read(Tracker tracker, Socket socket, DataInputStream dis)
            throws IOException, IllegalRequestFormatException {
        byte type = dis.readByte();
        TrackerRequest trackerRequest;
        switch (type) {
            case ListRequest.TYPE :
                trackerRequest = new ListRequest(tracker);
                break;
            case UploadRequest.TYPE :
                trackerRequest = new UploadRequest(tracker, dis.readUTF(), dis.readLong());
                break;
            case SourcesRequest.TYPE :
                trackerRequest = new SourcesRequest(tracker, dis.readInt());
                break;
            case UpdateRequest.TYPE :
                trackerRequest = new UpdateRequest(
                        tracker,
                        dis.readShort(),
                        socket.getInetAddress(),
                        (Set<Integer>) Collections.readFrom(dis, new HashSet<>(), DataInputStream::readInt)
                );
                break;
            default:
                throw new IllegalRequestFormatException("No such type of tracker request " + Byte.toString(type));
        }
        return trackerRequest;
    }
}
