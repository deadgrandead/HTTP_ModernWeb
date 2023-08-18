package com.deadgrandead;

public class Main {
    public static void main(String[] args) {
        int threads = 64;
        int port = 9999;

        Server server = new Server(threads, port);
        server.start();
    }
}
