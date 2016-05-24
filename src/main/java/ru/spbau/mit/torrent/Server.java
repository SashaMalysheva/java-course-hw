package ru.spbau.mit.torrent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public abstract class Server implements Runnable, AutoCloseable {

    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final ScheduledExecutorService scheduler;

    protected Server(int port) throws IOException {
        System.err.println("Server created");

        this.serverSocket = new ServerSocket(port);
        this.threadPool = Executors.newCachedThreadPool();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void run() {
        System.err.println("Server is running...");
        while (!Thread.interrupted()) {
            Socket clientSocket;
            try {
                Future<Socket> futureSocket = threadPool.submit(serverSocket::accept);
                clientSocket = futureSocket.get();
                threadPool.submit(() -> handleConnection(clientSocket));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void close() throws IOException {
        threadPool.shutdown();
        scheduler.shutdown();
        serverSocket.close();

        System.err.println("Server closed");
    }

    protected abstract void handleConnection(Socket socket);

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
