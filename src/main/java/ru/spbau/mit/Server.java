package ru.spbau.mit;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.*;

public class Server {
    private ServerSocket serverSocket;
    private Map<Integer, FileEntry> filesByID = new HashMap<>();
    private Map<Address, ClientInfo> activeClient = new HashMap<>();
    private Random rnd = new SecureRandom();
    private File stateFile;
    private PrintWriter stateWriter;

    public static final int SERVER_PORT = 8081;
    public static final int LIST_QUERY = 1;
    public static final int UPLOAD_QUERY = 2;
    public static final int SOURCES_QUERY = 3;
    public static final int UPDATE_QUERY = 4;
    public static final long UPDATE_TIMEOUT = 60000;

    public Server(File file) throws FileNotFoundException, UnsupportedEncodingException {
        stateFile = file;
        loadState();
        stateWriter = new PrintWriter(new FileOutputStream(file, true));
    }

    private void loadState() {
        Scanner in;
        try {
            in = new Scanner(stateFile);
            while (in.hasNext()) {
                FileEntry entry = new FileEntry();
                entry.clients = new HashSet<>();
                entry.id = in.nextInt();
                entry.name = in.next();
                entry.size = in.nextLong();

                filesByID.put(entry.id, entry);
            }
            in.close();
        } catch (FileNotFoundException ignored) {
        }
    }

    public static void main(String[] args) throws IOException {
        new Server(new File(args[0])).start();
    }

    public Thread start() throws IOException {
        serverSocket = new ServerSocket(SERVER_PORT);

        Thread thread = new Thread(() -> {
            try {
                catchSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        return thread;
    }

    public synchronized void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket accept() throws IOException {
        try {
            return serverSocket.accept();
        } catch (SocketException e) {
            return null;
        }

    }

    private void catchSocket() throws IOException {
        while (true) {
            Socket socket = accept();
            if (socket != null) {
                handlingQuery(socket);
            } else {
                return;
            }
        }
    }

    private void handlingQuery(Socket socket) {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            while (!socket.isClosed()) {
                byte operation = dis.readByte();
                if (operation == LIST_QUERY) {
                    handlingListQuery(dos);
                } else if (operation == UPLOAD_QUERY) {
                    handlingUploadQuery(dis, dos);
                } else if (operation == SOURCES_QUERY) {
                    handlingSourcesQuery(dis, dos);
                } else if (operation == UPDATE_QUERY) {
                    handlingUpdateQuery(dis, dos, socket);
                } else {
                    System.err.printf("Wrong query\n");
                }
            }
        } catch (IOException ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handlingUploadQuery(DataInputStream dis, DataOutputStream dos) throws IOException {
        String name = dis.readUTF();
        Long size = dis.readLong();

        FileEntry newFile = new FileEntry();
        newFile.name = name;
        newFile.size = size;
        newFile.clients = new HashSet<>();
        newFile.id = rnd.nextInt();
        while (filesByID.containsKey(newFile.id)) {
            newFile.id = rnd.nextInt();
        }

        filesByID.put(newFile.id, newFile);

        stateWriter.println(newFile.id + " " + newFile.name + " " + newFile.size);
        stateWriter.flush();
        dos.writeInt(newFile.id);
    }

    private void handlingSourcesQuery(DataInputStream dis, DataOutputStream dos) throws IOException {
        int id = dis.readInt();

        FileEntry file = filesByID.get(id);

        ArrayList<Address> del = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (Address client : file.clients) {
            if (activeClient.get(client).lastUpdateTime < currentTime - UPDATE_TIMEOUT) {
                del.add(client);
            }
        }

        for (Address client : del) {
            deleteClient(client);
        }

        dos.writeInt(file.clients.size());

        for (Address client : file.clients) {
            dos.write(client.getIp());
            dos.writeShort(client.getPort());
        }
    }

    private void deleteClient(Address client) {
        if (activeClient.containsKey(client)) {
            ArrayList<Integer> oldClientsFiles = activeClient.get(client).files;
            activeClient.remove(client);

            for (Integer oldFiles : oldClientsFiles) {
                filesByID.get(oldFiles).clients.remove(client);
            }
        }
    }

    private void handlingUpdateQuery(DataInputStream dis, DataOutputStream dos, Socket socket) throws IOException {
        short seedPort = dis.readShort();
        byte[] ip = socket.getInetAddress().getAddress();

        Address newClient = new Address();
        newClient.setIp(ip);
        newClient.setPort(seedPort);

        deleteClient(newClient);

        int count = dis.readInt();
        ArrayList<Integer> clientsFilesId = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            Integer fileId = dis.readInt();
            clientsFilesId.add(fileId);

            if (filesByID.containsKey(fileId)) {
                filesByID.get(fileId).clients.add(newClient);
            } else {
                socket.close();
                return;
            }
        }
        activeClient.put(newClient, new ClientInfo(clientsFilesId, System.currentTimeMillis()));

        dos.writeBoolean(true);
    }

    private void handlingListQuery(DataOutputStream dos) throws IOException {
        dos.writeInt(filesByID.size());
        for (FileEntry entry : filesByID.values()) {
            dos.writeInt(entry.id);
            dos.writeUTF(entry.name);
            dos.writeLong(entry.size);
        }
    }

    public static class FileEntry {
        private int id;
        private String name;
        private long size;

        private Set<Address> clients = new HashSet<>();
    }

    private class ClientInfo {
        private ArrayList<Integer> files = new ArrayList<>();
        private long lastUpdateTime;

        ClientInfo(ArrayList<Integer> files, long lastUpdateTime) {
            this.files.addAll(files);
            this.lastUpdateTime = lastUpdateTime;
        }
    }
}
