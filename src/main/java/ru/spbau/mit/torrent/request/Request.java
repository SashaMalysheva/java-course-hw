package ru.spbau.mit.torrent.request;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Request {

    void send(DataOutputStream dos) throws IOException;

    void readResponse(DataInputStream dis) throws IOException;

    void sendResponse(DataOutputStream dos) throws IOException;
}
