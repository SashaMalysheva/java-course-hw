package ru.spbau.mit.torrent.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public final class Sockets {

    private Sockets() {
    }

    public static DataInputStream socketInput(Socket socket) throws IOException {
        return new DataInputStream(socket.getInputStream());
    }

    public static DataOutputStream socketOutput(Socket socket) throws IOException {
        return new DataOutputStream(socket.getOutputStream());
    }
}
