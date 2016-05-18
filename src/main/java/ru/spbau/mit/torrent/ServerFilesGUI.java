package ru.spbau.mit.torrent;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ServerFilesGUI extends JPanel implements ActionListener {
    private static final Logger LOG = Logger.getLogger(ServerFilesGUI.class);
    private static final String DOWNLOAD_FILES_STRING = "Okey";

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

        JButton okeyButton = new JButton(DOWNLOAD_FILES_STRING);
        okeyButton.setActionCommand(DOWNLOAD_FILES_STRING);
        okeyButton.addActionListener(this);

        update();
        JScrollPane scrollPane = new JScrollPane(list);
        add(scrollPane, BorderLayout.CENTER);
        add(okeyButton, BorderLayout.SOUTH);
    }


    public void update() throws IOException {
        listModel.clear();

        client.list().forEach(listModel::addElement);
    }



    @Override
    public void actionPerformed(ActionEvent event) {
        list.getSelectedValuesList().forEach((chosenFile) -> {
            try {
                FileState state = FileState.newFile(client.getPath(), chosenFile);
                client.saveFileState(state);
                client.downloader().submitToDownload(state);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        frame.dispose();
    }
}
