package ru.spbau.mit.torrent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerFilesGUI extends JPanel implements ActionListener {
    private static final String DOWNLOAD_FILES_STRING = "Ok";
    private static final Path RELATIVE_ROOT_PATH = Paths.get("");

    private Client client;
    private JFrame frame;

    private JList<FileEntry> list;
    private DefaultListModel<FileEntry> listModel;

    public ServerFilesGUI(Client client, JFrame frame) throws IOException {
        super(new BorderLayout());

        this.client = client;
        this.frame = frame;
        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setSelectedIndex(0);

        JButton okButton = new JButton(DOWNLOAD_FILES_STRING);
        okButton.setActionCommand(DOWNLOAD_FILES_STRING);
        okButton.addActionListener(this);

        update();
        JScrollPane scrollPane = new JScrollPane(list);
        add(scrollPane, BorderLayout.CENTER);
        add(okButton, BorderLayout.SOUTH);
    }

    public void update() throws IOException {
        listModel.clear();
        client.list().forEach(listModel::addElement);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        list.getSelectedValuesList().forEach((chosenFile) -> {
            try {
                FileState state = FileState.newFile(RELATIVE_ROOT_PATH, chosenFile);
                client.saveFileState(state);
                client.downloader().submitToDownload(state);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        frame.dispose();
    }
}
