package ru.spbau.mit;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Client {
    private static final int SLEEP_TIME = 1000;
    private File stateFile;
    private ServerSocket serverSocket;
    private int port;
    private Map<Integer, FInfo> files;
    private String host;

    public static final int SERVER_PORT = 8081;
    public static final int LIST_QUERY = 1;
    public static final int SOURCES_QUERY = 3;
    public static final int UPDATE_QUERY = 4;
    public static final int STAT_QUERY = 1;
    public static final int GET_QUERY = 2;
    public static final int UPDATE_INTERVAL = 1000;

    public Client(String host, String pathInfo) throws IOException {
        this.host = host;

        stateFile = new File(pathInfo);

        files = new HashMap<>();

        loadState();
    }

    public Thread startSendUpdateQuery() throws IOException {
        Thread thread = new Thread(() -> {
            try {
                try {
                    sendUpdateQuery();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        return thread;
    }

    private void catheSocket() throws IOException {
        while (true) {
            Socket socket = this.serverSocket.accept();
            if (socket != null) {
                handlingQuery(socket);
            } else {
                return;
            }
        }
    }

    private void sendUpdateQuery() throws IOException, InterruptedException {
        while (true) {
            Socket socket = new Socket(host, SERVER_PORT);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            dos.writeByte(UPDATE_QUERY);
            dos.writeShort(port);
            dos.writeInt(files.size());

            for (Integer id : files.keySet()) {
                dos.writeInt(id);
            }

            socket.close();
            Thread.sleep(UPDATE_INTERVAL);
        }
    }

    private ArrayList<FInfo> sendListQuery() throws IOException {
        Socket socket = new Socket(host, SERVER_PORT);
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        dos.writeByte(LIST_QUERY);
        int count = dis.readInt();

        ArrayList<FInfo> filesOnServer = new ArrayList<>();

        for (int i = 0; i < count; ++i) {
            filesOnServer.add(FInfo.fromServerInfo(dis.readInt(), dis.readUTF(), dis.readLong()));
        }

        socket.close();
        return filesOnServer;
    }

    public void get(int id, String name) throws FileNotFoundException {
        FInfo fInfo = FInfo.fromServerInfo(id, name, -1);
        files.put(id, fInfo);
    }

    public int newFile(String name) throws IOException {
        Socket socket = new Socket(host, SERVER_PORT);
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        FInfo fInfo = FInfo.fromLocalFile(dis, dos, name);
        files.put(fInfo.getId(), fInfo);

        socket.close();
        return fInfo.getId();
    }

    public ArrayList<FInfo> list() throws IOException {
        return sendListQuery();
    }

    public void run(int port) throws IOException, InterruptedException {
        this.port = port;
        startSendUpdateQuery();
        startSeedingThread();
        ArrayList<FInfo> fis = list();

        for (FInfo fi : fis) {
            if (files.containsKey(fi.getId()) && files.get(fi.getId()).getSize() == -1) {
                files.put(fi.getId(),
                        FInfo.fromServerInfo(fi.getId(), files.get(fi.getId()).getName(), fi.getSize()));
            }
        }
        while (true) {
            for (Map.Entry<Integer, FInfo> entry : files.entrySet()) {
                if (entry.getValue().getSize() != -1) {
                    download(entry.getValue().getId(), entry.getValue().getName(), entry.getValue().getSize());
                }
            }
            Thread.sleep(SLEEP_TIME);
        }
    }

    private void startSeedingThread() throws IOException {
        serverSocket = new ServerSocket(port);
        Thread thread = new Thread(() -> {
            try {
                catheSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private void download(int id, String name, long size) throws IOException {
        ArrayList<Address> clientsWithFile = sendSourcesQuery(id);

        FInfo file = files.get(id);

        Collections.shuffle(clientsWithFile);
        for (Address currentClient : clientsWithFile) {
            Socket socket = new Socket(InetAddress.getByAddress(currentClient.getIp()), currentClient.getPort());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            ArrayList<Integer> parts = sendStatQuery(dis, dos, id);

            for (Integer partNum : parts) {
                if (file.needPart(partNum)) {
                    byte[] partEntry = sendGetQuery(dis, dos, file, partNum);
                    file.savePart(partEntry, partNum);
                    System.err.println("Save part " + partNum + " " + id);
                }
            }
            socket.close();
        }
    }

    private byte[] sendGetQuery(DataInputStream dis, DataOutputStream dos, FInfo file, int partNum)
            throws IOException {
        dos.writeByte(GET_QUERY);
        dos.writeInt(file.getId());
        dos.writeInt(partNum);

        int partLen = file.getPartLength(partNum);

        byte[] partEntry = new byte[partLen];
        if (dis.read(partEntry) == partLen) {
            return partEntry;
        } else {
            return null;
        }
    }

    private ArrayList<Integer> sendStatQuery(DataInputStream dis, DataOutputStream dos, int id)
            throws IOException {
        dos.writeByte(STAT_QUERY);
        dos.writeInt(id);

        ArrayList<Integer> parts = new ArrayList<>();

        int count = dis.readInt();
        for (int i = 0; i < count; ++i) {
            int partNum = dis.readInt();
            parts.add(partNum);
        }
        return parts;
    }

    private ArrayList<Address> sendSourcesQuery(int id) throws IOException {
        Socket socket = new Socket(host, SERVER_PORT);
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        dos.writeByte(SOURCES_QUERY);
        dos.writeInt(id);

        ArrayList<Address> clients = new ArrayList<>();
        int cnt = dis.readInt();

        for (int i = 0; i < cnt; ++i) {
            Address address = new Address();
            dis.read(address.getIp());
            address.setPort(dis.readShort());
            clients.add(address);
        }

        socket.close();
        return clients;
    }

    private void handlingQuery(Socket socket) {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            while (!socket.isClosed()) {
                int operation = dis.readByte();
                if (operation == STAT_QUERY) {
                    handlingStatQuery(dis, dos);
                } else if (operation == GET_QUERY) {
                    handlingGetQuery(dis, dos);
                } else {
                    System.err.println("Wrong query " + String.format("%x", operation));
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

    private void handlingStatQuery(DataInputStream dis, DataOutputStream dos) throws IOException {
        int id = dis.readInt();
        if (!files.containsKey(id)) {
            dos.writeInt(0);
        } else {
            ArrayList<Integer> parts = files.get(id).getExistingParts();

            dos.writeInt(parts.size());

            for (Integer part : parts) {
                dos.writeInt(part);
            }
        }
    }

    private boolean handlingGetQuery(DataInputStream dis, DataOutputStream dos) throws IOException {
        int id = dis.readInt();
        int part = dis.readInt();

        return files.containsKey(id) && files.get(id).sendFilePart(part, dos);

    }

    public void saveState() throws FileNotFoundException {
        PrintWriter out = new PrintWriter(stateFile);
        out.print(files.size());
        out.print("\n");
        for (Map.Entry<Integer, FInfo> entry : files.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().writeInfo(out);
            } else {
                out.print(entry.getKey());
                out.print(" -1\n");
            }
        }
        out.close();
    }

    private void loadState() {
        try {
            Scanner in = new Scanner(stateFile);
            int cnt = in.nextInt();
            for (int i = 0; i < cnt; ++i) {
                FInfo fi = FInfo.fromStateFile(in);
                if (fi.getSize() < 0) {
                    files.put(fi.getId(), null);
                } else {
                    files.put(fi.getId(), fi);
                }
            }
            in.close();
        } catch (FileNotFoundException ignored) {
        }
    }
}
