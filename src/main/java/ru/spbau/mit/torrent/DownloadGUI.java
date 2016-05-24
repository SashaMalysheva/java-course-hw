package ru.spbau.mit.torrent;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class DownloadGUI extends JPanel {
    private static final String[] HEADERS = {"ID", "File's name", "Progress"};

    public DownloadGUI(Client client) throws IOException {
        super(new GridLayout(1, 0));

        TableModel model = new DownloadGUITableModel(client);
        JTable table = new JTable(model);

        table.getColumn("Progress").setCellRenderer((table1, value, isSelected, hasFocus, row, column) -> {
            FileState state = (FileState) value;
            int all = state.getPartsCount(), loaded = state.loadedPartsCount();
            JProgressBar bar =  new JProgressBar(0, all);
            bar.setValue(loaded);
            bar.setStringPainted(true);
            bar.setString((loaded * 100 / all) + "%");
            return bar;
        });
        add(new JScrollPane(table));
    }

    private static class DownloadGUITableModel extends AbstractTableModel {

        private final List<FileState> states;

        DownloadGUITableModel(Client client) {
            this.states = client.getArrayOfFiles();
            client.setOnFileStateAdded(this::addFileState);
            client.setOnFileStateChanged(this::updateFileProgress);
        }

        @Override
        public int getRowCount() {
            return states.size();
        }

        @Override
        public int getColumnCount() {
            return HEADERS.length;
        }

        @Override
        public String getColumnName(int index) {
            return HEADERS[index];
        }

        @Override
        public Object getValueAt(int row, int column) {
            FileState fileState = states.get(row);
            switch (column) {
                case 0 : return fileState.getID();
                case 1 : return fileState.getFileEntry().getName();
                case 2 : return fileState;
                default: return null;
           }
        }

        public void addFileState(FileState state) {
            states.add(state);
            fireTableRowsInserted(states.size() - 1, states.size() - 1);
        }

        public void updateFileProgress(FileState state) {
            int index = states.indexOf(state);
            if (index  != -1) {
                fireTableCellUpdated(index, 2);
            }
        }
    }
}
