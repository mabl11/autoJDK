package ch.jdkupdater;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class TestServer implements AutoCloseable {

    private static final int NO_BODY = -1;

    private final Map<String, byte[]> responses = new ConcurrentHashMap<>();
    private final HttpServer server;

    TestServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    void serve(final String path, final byte[] body) {
        responses.put(path, body);
    }

    void serve(final String path, final String body) {
        serve(path, body.getBytes(StandardCharsets.UTF_8));
    }

    URI uri(final String path) {
        InetSocketAddress address = server.getAddress();
        return URI.create("http://" + address.getHostString() + ":" + address.getPort() + path);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(final HttpExchange exchange) throws IOException {
        byte[] body = responses.get(exchange.getRequestURI().getPath());
        if (body == null) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, NO_BODY);
        } else {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
        exchange.close();
    }
}
