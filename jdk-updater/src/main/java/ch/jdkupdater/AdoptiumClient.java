package ch.jdkupdater;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks up the latest Eclipse Temurin JDK for Windows through the Adoptium API.
 *
 * <p>The few fields needed are read with regular expressions, so the tool needs no JSON library.</p>
 *
 * @see <a href="https://api.adoptium.net/q/swagger-ui/">Adoptium API</a>
 */
public final class AdoptiumClient {

    /** Base URI of the public Adoptium API. */
    public static final URI DEFAULT_API = URI.create("https://api.adoptium.net/v3/");

    private static final String RELEASES_PATH = "info/available_releases";
    private static final String ASSETS_PATH =
            "assets/latest/%d/hotspot?architecture=%s&image_type=jdk&os=windows&vendor=eclipse";
    private static final Pattern LTS_VERSION = Pattern.compile("\"most_recent_lts\"\\s*:\\s*(\\d+)");
    private static final Pattern NEWEST_VERSION = Pattern.compile("\"most_recent_feature_release\"\\s*:\\s*(\\d+)");
    private static final Pattern RELEASE_NAME = Pattern.compile("\"release_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ARCHIVE_LINK = Pattern.compile("\"link\"\\s*:\\s*\"([^\"]+\\.zip)\"");
    private static final Pattern CHECKSUM_LINK =
            Pattern.compile("\"checksum_link\"\\s*:\\s*\"([^\"]+\\.zip\\.sha256\\.txt)\"");
    private static final String ARM64 = "ARM64";

    private final WebClient webClient;
    private final URI api;

    /**
     * Creates a client for an Adoptium API.
     *
     * @param webClient the client for the requests
     * @param api       base URI of the API ending with a slash, normally {@link #DEFAULT_API}
     */
    public AdoptiumClient(final WebClient webClient, final URI api) {
        this.webClient = webClient;
        this.api = api;
    }

    /**
     * Returns the latest Windows JDK of a release channel.
     *
     * <p>Adoptium announces a new feature version before its builds are published. In this case the previous
     * version is returned for {@link ReleaseChannel#NEWEST}.</p>
     *
     * @param channel the release channel
     * @return the latest release
     * @throws IOException          if the API cannot be reached or offers no Windows build
     * @throws InterruptedException if the thread is interrupted while waiting for the API
     */
    public Release findLatest(final ReleaseChannel channel) throws IOException, InterruptedException {
        int version = latestVersion(channel);
        Optional<String> assets = fetchAssets(version);
        if (assets.isEmpty() && channel == ReleaseChannel.NEWEST) {
            assets = fetchAssets(version - 1);
        }
        String json = assets.orElseThrow(() -> new IOException("No Windows JDK found for Java " + version));
        return new Release(find(json, RELEASE_NAME), URI.create(find(json, ARCHIVE_LINK)),
                URI.create(find(json, CHECKSUM_LINK)));
    }

    private int latestVersion(final ReleaseChannel channel) throws IOException, InterruptedException {
        String json = webClient.fetchText(api.resolve(RELEASES_PATH));
        return Integer.parseInt(find(json, versionPattern(channel)));
    }

    private static Pattern versionPattern(final ReleaseChannel channel) {
        return switch (channel) {
            case LTS -> LTS_VERSION;
            case NEWEST -> NEWEST_VERSION;
        };
    }

    private Optional<String> fetchAssets(final int version) throws IOException, InterruptedException {
        URI uri = api.resolve(ASSETS_PATH.formatted(version, architecture()));
        try {
            return Optional.of(webClient.fetchText(uri)).filter(json -> RELEASE_NAME.matcher(json).find());
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private static String architecture() {
        boolean arm = ARM64.equals(System.getenv("PROCESSOR_ARCHITECTURE"))
                || ARM64.equals(System.getenv("PROCESSOR_ARCHITEW6432"));
        if (arm) {
            return "aarch64";
        }
        return "x64";
    }

    private static String find(final String json, final Pattern pattern) throws IOException {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new IOException("Unexpected answer from the Adoptium API");
        }
        return matcher.group(1);
    }
}
