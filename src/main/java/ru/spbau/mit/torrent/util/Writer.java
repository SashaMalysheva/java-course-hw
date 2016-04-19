package ru.spbau.mit.torrent.util;

import java.io.DataOutputStream;
import java.io.IOException;

@FunctionalInterface
public interface Writer<R> {

    void write(DataOutputStream dos, R resource) throws IOException;
}
