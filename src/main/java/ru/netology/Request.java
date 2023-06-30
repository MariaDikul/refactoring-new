package ru.netology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class Request {
    private Server server = new Server();
    private BufferedOutputStream out;
    private BufferedInputStream in;
    private String method, path, body;
    private List<String> headers;
    private int read;
    final int limit = 4096;
    final byte[] buffer = new byte[limit];
    private byte[] requestLineDelimiter, headersDelimiter;
    private int requestLineEnd, headersStart, headersEnd;


    public Request(BufferedOutputStream out, BufferedInputStream in) throws IOException {
        this.out = out;
        this.in = in;
        in.mark(limit);
        this.read = in.read(buffer);
        setMethodAndPath();
        setHeaders();
        setBody();
    }

    private void setMethodAndPath() throws IOException {
        requestLineDelimiter = new byte[]{'\r', '\n'};
        requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            server.badRequest(out);
        }

        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            server.badRequest(out);
        }
        this.method = requestLine[0];
        this.path = requestLine[1];
        if (!path.startsWith("/")) {
            server.badRequest(out);
        }
    }

    private void setHeaders() throws IOException {
        headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        headersStart = requestLineEnd + requestLineDelimiter.length;
        headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            server.badRequest(out);
        }
        in.reset();
        in.skip(headersStart);
        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        this.headers = Arrays.asList(new String(headersBytes).split("\r\n"));

    }

    private void setBody() throws IOException {
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);
                this.body = new String(bodyBytes);
            }
        }
    }

    public Handler changeHandler(Map<String, Map<String, Handler>> methodMap) throws IOException {
        if (methodMap.containsKey(this.method) && methodMap.get(this.method).containsKey(this.path)) {
            Handler handler = methodMap.get(this.method).get(this.path);
            return handler;
        } else {
            server.badRequest(out);
        }
        return null;
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}
