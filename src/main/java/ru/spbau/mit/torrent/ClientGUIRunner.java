package ru.spbau.mit.torrent;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ClientGUIRunner {
    private static final Path PATH = Paths.get("");

    private static Client client;
    private static JFileChooser fileChooser;
    private static DownloadGUI downloadGUI;

    private ClientGUIRunner() {
    }

    public static void main(String[] args) throws IOException {
        client = new Client("localhost", PATH);
        Server seed = client.seed();
        Thread seedThread = new Thread(seed);
        seedThread.start();

        final JFrame frame = new JFrame("Torrent");
        final JMenuBar menuBar = buildMenuBar();

        fileChooser = new JFileChooser(PATH.toAbsolutePath().toFile());
        downloadGUI = new DownloadGUI(client);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    seedThread.interrupt();
                    seed.close();
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        });
        frame.setJMenuBar(menuBar);
        frame.add(downloadGUI);

        frame.pack();
        frame.setVisible(true);
    }

    private static JMenuBar buildMenuBar() {
        JMenuItem itemUpload = new JMenuItem("Upload files");
        JMenuItem itemDownload = new JMenuItem("Download files");

        itemUpload.addActionListener(e -> {
            System.err.println("Choosing file");
            int returnVal = fileChooser.showOpenDialog(new JPanel());

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                System.err.println("File chosen");
                File file = fileChooser.getSelectedFile();
                try {
                    FileEntry entry = client.upload(file.getName(), Files.size(file.toPath()));
                    FileState state = FileState.ownFile(client.getPath().relativize(file.toPath()), entry.getID());
                    client.saveFileState(state);
                } catch (IOException er) {
                    er.printStackTrace();
                }
            }
        });

        itemDownload.addActionListener(e -> {
            final JFrame frame = new JFrame("Choose files");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            try {
                frame.add(new ServerFilesGUI(client, frame));
            } catch (IOException er) {
                er.printStackTrace();
            }

            frame.pack();
            frame.setVisible(true);
        });

        JMenu menu = new JMenu("Menu");
        menu.add(itemDownload);
        menu.add(itemUpload);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menu);

        return menuBar;
    }
}