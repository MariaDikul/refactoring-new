package ru.netology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import static java.lang.System.out;

public class Server {
    Map<String, Map<String, Handler>> handlers = new HashMap<>();
    private int port;

    public Server(int port) {
        this.port = port;
    }

    public Server() {
    }

    public void serverStart() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            out.println("Сервер запущен!");
            final ExecutorService threadPool = Executors.newFixedThreadPool(4);
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
                    final var socket = serverSocket.accept();
                    final var in = new BufferedInputStream(socket.getInputStream());
                    final var out = new BufferedOutputStream(socket.getOutputStream())
            ) {
                Request request = new Request(out, in);
                request.changeHandler(handlers).handle(request, out);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        return logic;
    }

    public static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.put(method, new HashMap<>());
        handlers.get(method).put(path, handler);
    }
}

