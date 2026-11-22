package ch.jdkupdater;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;

/**
 * Signals that a server answered with an HTTP status other than 200 OK.
 */
public final class HttpStatusException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** HTTP status code of the answer. */
    private final int statusCode;

    /**
     * Creates an exception for a failed request.
     *
     * @param uri        the requested URI
     * @param statusCode the HTTP status code of the answer
     */
    public HttpStatusException(final URI uri, final int statusCode) {
        super("HTTP " + statusCode + " for " + uri);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code of the answer.
     *
     * @return the status code, for example 404
     */
    public int getStatusCode() {
        return statusCode;
    }
}
