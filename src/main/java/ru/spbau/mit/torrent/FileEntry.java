package ru.spbau.mit.torrent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FileEntry {
    private final int id;
    private final String name;
    private final long size;

    public FileEntry(int id, String name, long size) {
        this.id = id;
        this.name = name;
        this.size = size;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public static FileEntry load(DataInputStream dis) throws IOException {
        int id = dis.readInt();
        String name = dis.readUTF();
        long size = dis.readLong();
        return new FileEntry(id, name, size);
    }

    public void store(DataOutputStream dos) throws IOException {
        dos.writeInt(id);
        dos.writeUTF(name);
        dos.writeLong(size);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileEntry fileEntry = (FileEntry) o;
        return id == fileEntry.id && size == fileEntry.size && name.equals(fileEntry.name);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return id + ": " + name + "(" + size + " bytes)";
    }
}
