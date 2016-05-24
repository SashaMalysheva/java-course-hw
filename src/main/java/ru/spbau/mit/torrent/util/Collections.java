package ru.spbau.mit.torrent.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Collection;

public final class Collections {

    private Collections() {
    }

    public static <R> void writeTo(DataOutputStream dos, Collection<R> c, Writer<R> writer) throws IOException {
        dos.writeInt(c.size());
        for (R elem : c) {
            writer.write(dos, elem);
        }
    }

    public static <R> Collection<R> readFrom(DataInputStream dis, Collection<R> c, Reader<R> reader)
            throws IOException {
        int size;
        try {
            size = dis.readInt();
        } catch (final EOFException e) {
            return c;
        }
        for (int i = 0; i < size; i++) {
            c.add(reader.read(dis));
        }
        return c;
    }
}
