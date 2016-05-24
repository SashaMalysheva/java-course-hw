package ru.spbau.mit.torrent.util;

import java.io.DataInputStream;
import java.io.IOException;

@FunctionalInterface
public interface Reader<R> {

    R read(DataInputStream dis) throws IOException;
}
