package ru.spbau.mit;

import java.util.Arrays;

public class Address {
    private static final int CNT_BYTE_IN_IP = 4;
    private short port;
    private byte[] ip;

    public Address() {
        ip = new byte[CNT_BYTE_IN_IP];
    }

    public short getPort() {
        return port;
    }

    public byte[] getIp() {
        return ip;
    }

    public void setPort(short port) {
        this.port = port;
    }

    public void setIp(byte[] ip) {
        this.ip = ip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address that = (Address) o;
        return port == that.port && Arrays.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        int result = (int) port;
        result = 31 * result + Arrays.hashCode(ip);
        return result;
    }
}
