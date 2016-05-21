package ru.spbau.mit.torrent;

import java.io.IOException;
import java.nio.file.Paths;

public final class TrackerRunner {

    private TrackerRunner() {
    }

    public static void main(String[] args) {
        try (final Tracker tracker = new Tracker(Paths.get(""))) {
            Thread trackerThread = new Thread(tracker);
            trackerThread.start();

            System.out.println("Press any <Enter> to close");
            System.in.read();

            trackerThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
