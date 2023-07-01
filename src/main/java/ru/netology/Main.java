package ru.netology;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(9999);

        server.addHandler("GET", "/spring.svg", (request, out) -> {
            Path filePath = Path.of(".", "public", "/spring.svg");
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + Files.probeContentType(filePath) + "\r\n" +
                            "Content-Length: " + Files.size(filePath) + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        });

        server.addHandler("GET", "/classic.html", (request, out) -> {
            Path filePath = Path.of(".", "public", "/classic.html");
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + Files.probeContentType(filePath) + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
        });

        server.addHandler("GET", "/index.html", (request, out) -> {
            Path filePath = Path.of(".", "public", "/index.html");
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + Files.probeContentType(filePath) + "\r\n" +
                            "Content-Length: " + Files.size(filePath) + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        });

        server.addHandler("POST", "/default.html", (request, out) -> {
            System.out.println("Данные отправлены");
        });

        server.serverStart();
    }
}
