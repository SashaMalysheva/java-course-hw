package ru.spbau.mit.torrent.request;

final class IllegalRequestFormatException extends RuntimeException {

    IllegalRequestFormatException(String message) {
        super(message);
    }
}
