package ru.spbau.mit;


import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class Tests {

    private static final int MAX_PORT = 20000;
    private static final int MIN_PORT = 10000;

    Path root;

    public static final Random RND = new Random();

    public static final FileVisitor<Path> RANDOM_FILE_CREATOR = new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            int count = RND.nextInt(10);
            for (int i = 0; i < count; i++) {
                String name = "file" + Integer.toString(i);
                Files.createFile(dir.resolve(name)).toFile().deleteOnExit();
                BufferedWriter writer = Files.newBufferedWriter(dir.resolve(name));
                writer.write(name);
            }
            return FileVisitResult.CONTINUE;
        }
    };

    public static final FileVisitor<Path> RANDOM_DIR_CREATOR = new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            int count = RND.nextInt(10);
            int tag = RND.nextInt();
            for (int i = 0; i < count; i++) {
                String name = "dir" + Integer.toString(state) + "_" + Integer.toString(i);
                Files.createDirectory(dir.resolve(name)).toFile().deleteOnExit();
            }
            return FileVisitResult.CONTINUE;
        }
    };

    static int state = 0;

    public void genDirectories() throws IOException {
        Files.walkFileTree(root, RANDOM_DIR_CREATOR);
        state++;
    }

    public void genFiles() throws IOException {
        Files.walkFileTree(root, RANDOM_FILE_CREATOR);
    }

    @Before
    public void root() throws IOException{
        root = Files.createTempDirectory(".");
        root.toFile().deleteOnExit();
    }

    @Test
    public void test01_empty_root() throws IOException {
        test();
    }

    @Test
    public void test02_files_only() throws IOException {
        genFiles();
        test();
    }

    @Test
    public void test03_directories() throws IOException {
        genDirectories();
        genFiles();
        test();
    }

    @Test
    public void test04_nested_directories() throws IOException {
        genDirectories();
        genDirectories();
        genFiles();
        test();
    }

    private void test() {
        int port;
        Random rnd = new Random();
        port = rnd.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;

        Server server = null;
        Client client = null;

        try {
            server = new Server(port, root);
            client = new Client(new Socket("localhost", port));

            testList(client, Paths.get(""));
        } catch (Exception e) {
            fail("Unexpected exception caught: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (server != null) server.shutdown();
            } catch (Exception ignored) {
            }
            try {
                if (client != null) client.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void testList(Client client, Path path) throws IOException{
        Client.AnswerElement[] answers = client.list(path.toString());
        File dir = root.resolve(path).toFile();
        File[] files  = dir.listFiles();
        if (files == null) {
            throw new IllegalArgumentException("expected path to file");
        }
        assertEquals(files.length, answers.length);

        Arrays.sort(answers, Comparator.comparing((x) -> x.name));
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (int i = 0; i < answers.length; i++) {
            assertEquals(files[i].isDirectory(), answers[i].isDirectory);
            if (answers[i].isDirectory) {
                testList(client, path.resolve(answers[i].name));
            } else {
                testGet(client, path.resolve(answers[i].name));
            }
        }
    }

    private void testGet(Client client, Path path) throws IOException {
        byte[] serverBytes = client.get(path.toString());
        byte[] realBytes = new byte[serverBytes.length];
        try {
            IOUtils.readFully(Files.newInputStream(root.resolve(path)), realBytes);
        } catch (EOFException e) {
            fail("Real and loaded files have different sizes");
        }
        for (int i = 0; i < serverBytes.length; i++) {
            assertEquals("Files have different " + i + " byte", realBytes[i], serverBytes[i]);
        }
    }
}
