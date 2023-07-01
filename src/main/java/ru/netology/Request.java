package ru.netology;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ru.netology.Server.badRequest;


public class Request {
    private BufferedOutputStream out;
    private BufferedInputStream in;
    private String method, path;
    private String[] requestLine;
    private List<String> headers;
    private List<NameValuePair> params, body;
    private final int read;
    final int limit = 4096;
    final byte[] buffer = new byte[limit];
    private byte[] requestLineDelimiter, headersDelimiter;
    private int requestLineEnd, headersStart, headersEnd;

    public Request(BufferedOutputStream out, BufferedInputStream in) throws IOException {
        this.out = out;
        this.in = in;
        in.mark(limit);
        this.read = in.read(buffer);
        setMethod();
        setPathAndParams();
        setHeaders();
        setBody();
    }

    private void setMethod() throws IOException {
        requestLineDelimiter = new byte[]{'\r', '\n'};
        requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) badRequest(out);

        requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) badRequest(out);
        this.method = requestLine[0];
        this.path = requestLine[1].split("\\?")[0];
        }

        private void setPathAndParams() {
            if(requestLine[1].contains("?")) {
                this.path = requestLine[1].split("\\?")[0];
                this.params = URLEncodedUtils.parse(requestLine[1].split("\\?")[1], StandardCharsets.UTF_8);
                System.out.println(params);
                System.out.println(path);
            }
        }


    private void setHeaders() throws IOException {
        headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        headersStart = requestLineEnd + requestLineDelimiter.length;
        headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) badRequest(out);
        in.reset();
        in.skip(headersStart);
        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        this.headers = Arrays.asList(new String(headersBytes).split("\r\n"));

    }

    private void setBody() throws IOException {
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);
                this.body = URLEncodedUtils.parse(new String(bodyBytes), StandardCharsets.UTF_8);
                System.out.println(body);
            }
        }
    }

    public Handler changeHandler(Map<String, Map<String, Handler>> methodMap) throws IOException {
        if (methodMap.containsKey(this.method) && methodMap.get(this.method).containsKey(this.path)) {
            return methodMap.get(this.method).get(this.path);
        } else {
            badRequest(out);
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
