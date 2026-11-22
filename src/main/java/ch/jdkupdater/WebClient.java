package ch.jdkupdater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.DoubleConsumer;

/**
 * Reads text and downloads files over HTTP.
 */
public final class WebClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(10);
    private static final String USER_AGENT = "jdk-updater";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final int BUFFER_SIZE = 65_536;
    private static final int END_OF_STREAM = -1;

    private final HttpClient httpClient;

    /**
     * Creates a client that follows redirects and uses the default proxy settings.
     */
    public WebClient() {
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * Returns the content of a resource as text.
     *
     * @param uri the resource
     * @return the content
     * @throws HttpStatusException  if the server does not answer with 200 OK
     * @throws IOException          if the server cannot be reached
     * @throws InterruptedException if the thread is interrupted while waiting for the server
     */
    public String fetchText(final URI uri) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request(uri), HttpResponse.BodyHandlers.ofString());
        requireOk(uri, response.statusCode());
        return response.body();
    }

    /**
     * Downloads a resource into a file and reports the progress from 0.0 to 1.0.
     *
     * <p>No progress is reported if the server does not send the size of the resource.</p>
     *
     * @param uri      the resource
     * @param target   the file to write, an existing file is replaced
     * @param progress receives the progress
     * @throws HttpStatusException  if the server does not answer with 200 OK
     * @throws IOException          if the server cannot be reached or the file cannot be written
     * @throws InterruptedException if the thread is interrupted while waiting for the server
     */
    public void download(final URI uri, final Path target, final DoubleConsumer progress)
            throws IOException, InterruptedException {
        HttpResponse<InputStream> response = httpClient.send(request(uri), HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = response.body()) {
            requireOk(uri, response.statusCode());
            long totalBytes = response.headers().firstValueAsLong(CONTENT_LENGTH).orElse(0);
            try (OutputStream out = Files.newOutputStream(target)) {
                copy(in, out, totalBytes, progress);
            }
        }
    }

    private static void copy(final InputStream in, final OutputStream out, final long totalBytes,
            final DoubleConsumer progress) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long copiedBytes = 0;
        int read = in.read(buffer);
        while (read != END_OF_STREAM) {
            out.write(buffer, 0, read);
            copiedBytes += read;
            if (totalBytes > 0) {
                progress.accept((double) copiedBytes / totalBytes);
            }
            read = in.read(buffer);
        }
    }

    private static HttpRequest request(final URI uri) {
        return HttpRequest.newBuilder(uri)
                .header("User-Agent", USER_AGENT)
                .timeout(REQUEST_TIMEOUT)
                .build();
    }

    private static void requireOk(final URI uri, final int statusCode) throws HttpStatusException {
        if (statusCode != HttpURLConnection.HTTP_OK) {
            throw new HttpStatusException(uri, statusCode);
        }
    }
}
