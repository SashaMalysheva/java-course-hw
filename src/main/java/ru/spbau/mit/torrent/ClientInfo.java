package ru.spbau.mit.torrent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;

public class ClientInfo {
    private final InetAddress seedAddress;
    private final int seedPort;
    private final Set<Integer> ids;

    public ClientInfo(InetAddress seedAddress, int seedPort, Set<Integer> ids) {
        this.seedAddress = seedAddress;
        this.ids = ids;
        this.seedPort = seedPort;
    }

    public InetAddress getSeedAddress() {
        return seedAddress;
    }

    public int getSeedPort() {
        return seedPort;
    }

    public Set<Integer> getIDs() {
        return ids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClientInfo that = (ClientInfo) o;
        return seedPort == that.seedPort && seedAddress.equals(that.seedAddress);
    }

    @Override
    public int hashCode() {
        int result = seedAddress.hashCode();
        result = 31 * result + seedPort;
        return result;
    }

    public Socket openSocket() throws IOException {
        return new Socket(seedAddress, seedPort);
    }
}
