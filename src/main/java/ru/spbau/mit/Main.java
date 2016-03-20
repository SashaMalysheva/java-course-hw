package ru.spbau.mit;

import java.io.IOException;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Expected arguments: <port>");
            System.exit(1);
        }
        try {
            final Server server = new Server(Integer.parseInt(args[0]));
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        server.shutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
