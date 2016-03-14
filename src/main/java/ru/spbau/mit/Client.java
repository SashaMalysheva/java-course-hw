package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client implements AutoCloseable {
    private DataInputStream in;
    private DataOutputStream out;
    private final Socket socket;

    public static final class AnswerElement {
        public final String name;
        public final boolean isDirectory;

        AnswerElement(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof AnswerElement)) {
                return false;
            }
            AnswerElement other = (AnswerElement) obj;
            return name.equals(other.name) && isDirectory == other.isDirectory;
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + Boolean.hashCode(isDirectory);
        }
    }

    public Client(Socket socket) throws IOException {
        this.socket = socket;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            throw e;
        }
    }

    public AnswerElement[] list(String path) throws IOException {
        out.writeInt(1);
        out.writeUTF(path);
        int count = in.readInt();
        AnswerElement[] result = new AnswerElement[count];
        for (int i = 0; i < count; i++) {
            String name = in.readUTF();
            boolean isDirectory = in.readBoolean();
            result[i] = new AnswerElement(name, isDirectory);
        }
        return result;
    }

    public byte[] get(String path) throws IOException {
        out.writeInt(2);
        out.writeUTF(path);
        int length = in.readInt();
        byte[] result = new byte[length];
        int position = 0;
        while (position < result.length) {
            position += in.read(result, position, result.length - position);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        out.close();
        in.close();
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
    }
}
