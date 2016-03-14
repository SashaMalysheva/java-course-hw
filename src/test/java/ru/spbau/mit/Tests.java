package ru.spbau.mit;


import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

public final class Tests {

    private final static String[] dirs = new String[]{"dir1", "dir2", "dir3", "dir4"};
    private final static String[] files = new String[]{"file1", "file2", "file3", "file4"};

    private static final int MAX_PORT = 20000;
    private static final int MIN_PORT = 10000;

    File rootDirectory;

    @Before
    public void initDirectories() throws IOException{
        rootDirectory = Files.createTempDirectory("root").toFile();
        for (String dir : dirs) {
            File directory = new File(rootDirectory, dir);
            directory.mkdir();
            for (String file : files) {
                new File(directory, file).createNewFile();
            }
        }

    }

    @Test
    public void testGet() throws IOException {
        int port;
        Random rnd = new Random();
        port = rnd.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;

        Server server = new Server(port, rootDirectory.toPath());
        Client client = new Client(new Socket("localhost", port));

        try {

            File file = new File(new File(rootDirectory, dirs[0]), files[0]);
            PrintWriter writer = new PrintWriter(file);
            String fileString = "abcdefgh";
            writer.print(fileString);
            writer.close();

            byte[] text = client.get(file.toString());

            assertArrayEquals(text, fileString.getBytes());

        } finally {
            try {
                server.shutdown();
            } catch (Exception ignored) {
            }
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testList() throws IOException {
        int port;
        Random rnd = new Random();
        port = rnd.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;

        Server server = new Server(port, rootDirectory.toPath());
        Client client = new Client(new Socket("localhost", port));

        try {
            Client.AnswerElement[] answers = client.list("");
            assertEquals(4, answers.length);
            for (Client.AnswerElement answer : answers) {
                assertTrue(answer.isDirectory);
            }
            for (String dirName : dirs) {
                answers = client.list(dirName);
                assertEquals(4, answers.length);
                for (Client.AnswerElement answer : answers) {
                    assertFalse(answer.isDirectory);
                }
            }

        } finally {
            try {
                server.shutdown();
            } catch (Exception ignored) {
            }
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }
}
