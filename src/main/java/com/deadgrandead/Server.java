package com.deadgrandead;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Server {
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final ExecutorService threadPool;
    private final int port;

    public Server(int threads, int port) {
        this.threadPool = Executors.newFixedThreadPool(threads);
        this.port = port;
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(this.port)) {
            while (true) {
                final var socket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleConnection(Socket socket) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");
            if (parts.length != 3) {
                return;
            }

            final var path = parts[1];
            if (!validPaths.contains(path)) {
                sendNotFound(out);
                return;
            }

            if (path.equals("/classic.html")) {
                sendClassicHtml(out);
                return;
            }

            sendFile(path, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNotFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void sendClassicHtml(BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "public", "/classic.html");
        final var template = Files.readString(filePath);
        final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();

        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }

    private void sendFile(String path, BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);
        final var length = Files.size(filePath);

        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }
}
