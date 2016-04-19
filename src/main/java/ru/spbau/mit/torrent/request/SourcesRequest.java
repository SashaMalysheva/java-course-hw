package ru.spbau.mit.torrent.request;

import ru.spbau.mit.torrent.ClientInfo;
import ru.spbau.mit.torrent.Tracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;


public class SourcesRequest implements TrackerRequest {

    public static final int TYPE = 3;

    private final int id;
    private List<ClientInfo> clients;

    public SourcesRequest(int id) {
        this.id = id;
    }

    public SourcesRequest(Tracker tracker, int id) {
        this.id = id;
        this.clients = tracker.getClientInfoSet().stream()
                .filter((info) -> info.getIDs().contains(id))
                .collect(Collectors.toList());
    }

    public List<ClientInfo> getClients() {
        return clients;
    }

    @Override
    public void send(DataOutputStream dos) throws IOException {
        dos.writeByte(TYPE);
        dos.writeInt(id);
    }

    @Override
    public void readResponse(DataInputStream dis) throws IOException {
        int count = dis.readInt();
        clients = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            InetAddress seedAddress = InetAddress.getByAddress(new byte[]{
                            dis.readByte(),
                            dis.readByte(),
                            dis.readByte(),
                            dis.readByte()
                    });
            clients.add(new ClientInfo(seedAddress, dis.readUnsignedShort(), Collections.singleton(id)));
        }
    }

    @Override
    public void sendResponse(DataOutputStream dos) throws IOException {
        dos.writeInt(clients.size());
        for (ClientInfo client : clients) {
            for (byte aByte : client.getSeedAddress().getAddress()) {
                dos.writeByte(aByte);
            }
            dos.writeShort(client.getSeedPort());
        }
    }
}
