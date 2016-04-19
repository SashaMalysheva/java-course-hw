package ru.spbau.mit.torrent.request;

import ru.spbau.mit.torrent.ClientInfo;
import ru.spbau.mit.torrent.Tracker;
import ru.spbau.mit.torrent.util.Collections;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class UpdateRequest implements TrackerRequest {

    public static final int TYPE = 4;

    private final int seedPort;
    private final Set<Integer> availableFiles;
    private boolean res;

    public UpdateRequest(int seedPort, Set<Integer> availableFiles) {
        this.seedPort = seedPort;
        this.availableFiles = availableFiles;
    }

    public UpdateRequest(Tracker tracker, int seedPort, InetAddress seedAddress, Set<Integer> availableFiles) {
        this.seedPort = seedPort;
        this.availableFiles = availableFiles;
        ClientInfo updatedClientInfo = new ClientInfo(seedAddress, seedPort, availableFiles);
        Set<ClientInfo> infoSet = tracker.getClientInfoSet();
        System.err.println("Tracker: seed info updated");
        //updating info
        infoSet.remove(updatedClientInfo);
        infoSet.add(updatedClientInfo);

        //delete after a minute
        tracker.getScheduler().schedule(() -> {
            System.err.println("Tracker: seed info removed");
            infoSet.remove(updatedClientInfo);
        }, 1, TimeUnit.MINUTES);
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeByte(TYPE);
        dos.writeShort(seedPort);
        Collections.writeTo(dos, availableFiles, DataOutputStream::writeInt);
    }

    @Override
    public void readResponse(DataInputStream dis) throws IOException {
        res = dis.readBoolean();
    }

    @Override
    public void sendResponse(DataOutputStream dos) throws IOException {
        dos.writeBoolean(res);
    }

    public boolean isRes() {
        return res;
    }
}
