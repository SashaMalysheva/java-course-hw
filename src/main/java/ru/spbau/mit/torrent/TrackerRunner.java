package ru.spbau.mit.torrent;

import java.io.IOException;

public final class TrackerRunner {

    private TrackerRunner() {
    }

    public static void main(String[] args) {
        try (final Tracker tracker = new Tracker()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    tracker.close();
                } catch (final IOException ignored) {
                }
            }));
            tracker.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
