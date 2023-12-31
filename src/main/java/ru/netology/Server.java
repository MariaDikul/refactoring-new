package ru.netology;

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

public class Server {
    final List<String> validPaths = List.of
            ("/index.html", "/spring.svg", "/spring.png", "/resources.html",
                    "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html",
                    "/events.html", "/events.js");
    private int port;
    private Path filePath;
    private String path;
    private String mimeType;
    private long length;

    public Server(int port) {
        this.port = port;
    }

    public void serverStart() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Сервер запущен!");
            final ExecutorService threadPool = Executors.newFixedThreadPool(64);
            while (true) {
                threadPool.execute(run(serverSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Runnable run(ServerSocket serverSocket) throws IOException {
        Runnable logic = () -> {
            try (
                    final Socket socket = serverSocket.accept();
                    final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final var out = new BufferedOutputStream(socket.getOutputStream());
            ) {
                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");
                path = parts[1];
                filePath = Path.of(".", "public", path);
                mimeType = Files.probeContentType(filePath);
                length = Files.size(filePath);
                if (parts.length == 3) {
                    serverResponse(in, out);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        return logic;
    }

    private void serverResponse(BufferedReader in, BufferedOutputStream out) throws IOException {
        if (!validPaths.contains(path)) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }

        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
        }

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

