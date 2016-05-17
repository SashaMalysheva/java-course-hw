package ru.spbau.mit.torrent;

import org.apache.log4j.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public class DownloadGUI extends JPanel {
    private static final Logger LOG = Logger.getLogger(DownloadGUI.class);
    private static final int TIME_BETWEEN_UPDATE = 1000;
    private static final int PROGRESS_BAR_SIZE = 100;

    private Client client;
    private Server seed;
    private JTable table;
    private DefaultTableModel model;

    private Thread updateThread;

    private ArrayList<JProgressBar> progressBars = new ArrayList<>();

    public DownloadGUI(Client client) throws IOException {
        super(new GridLayout(1, 0));

        model = new DefaultTableModel() {
            private String[] header = {"File's name", "Progress"};

            @Override
            public int getColumnCount() {
                return header.length;
            }

            @Override
            public String getColumnName(int index) {
                return header[index];
            }
        };

        this.client = client;
        this.seed = client.seed();
        table = new JTable(model);

        table.getColumn("Progress").setCellRenderer((table1, value, isSelected, hasFocus, row, column) -> {
            if (progressBars.size() <= row) {
                progressBars.add(new JProgressBar(0, PROGRESS_BAR_SIZE));
            }
            progressBars.get(row).setStringPainted(true);
            return progressBars.get(row);
        });
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    update();
                    try {
                        Thread.sleep(TIME_BETWEEN_UPDATE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        updateThread.start();


    }


    public void update() {
        ArrayList<FileState> files = client.getArrayOfFiles();

        int rowsCount = model.getRowCount();
        for (int i = rowsCount - 1; i >= 0; --i) {
            model.removeRow(i);
        }

        for (FileState file : files) {
            String fileName = file.getFileEntry().getName();
            model.addRow(new Object[]{fileName});
            int cntPartHave = file.numOfExistingParts();
            int allPart = file.getPartsCount();

            int row = model.getRowCount() - 1;
            int column = 1;
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            JProgressBar progressBar = (JProgressBar) table.prepareRenderer(renderer, row, column);

            if (allPart == 0) {
                allPart = 1;
            }

            progressBar.setValue(cntPartHave * PROGRESS_BAR_SIZE / allPart);
        }
    }
}
