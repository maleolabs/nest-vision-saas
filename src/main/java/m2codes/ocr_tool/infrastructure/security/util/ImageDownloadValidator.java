package m2codes.ocr_tool.infrastructure.security.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;

/**
 * S-07 / VIS-F08 — SSRF guard untuk setiap fetch gambar by URL.
 *
 * Satu choke point untuk SEMUA jalur fetch by URL (saat ini PyTesseractOcrService dan
 * TesseractOcrService): validasi scheme/allowlist/IP-private dilakukan SEBELUM fetch,
 * dan fetch-nya dijalankan di method yang sama (download).
 *
 * RESIDUAL DNS-REBINDING (jujur, jangan klaim berlebihan): validate() memblokir
 * berdasarkan resolusi DNS PERTAMA; HttpClient menyambung ke host string yang sama dan
 * memicu resolusi KEDUA pada connect — jadi tetap ada TOCTOU window antara dua
 * resolusi (bukan "satu resolusi oleh kode yang sama"); pinning-IP sengaja TIDAK
 * dilakukan karena merusak SNI/TLS handshake. Mitigasi ops (WAJIB di prod):
 * set env OCR_FETCH_IMAGE_URL_ALLOWED_HOSTS non-empty — allowlist match pada NAMA HOST
 * sehingga rebinding tidak berguna bagi attacker.
 *
 * Env opsional: OCR_FETCH_IMAGE_URL_ALLOWED_HOSTS (comma separated). Kosong = hanya
 * blokir target private. Tidak ditambahkan ke application.properties; @Value membaca
 * langsung dari Environment, dan file properties di-commit tidak diutak-atik.
 */
@Slf4j
@Component
public class ImageDownloadValidator {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_IMAGE_BYTES = 12 * 1024 * 1024; // 12 MB — jangkauan aman di atas file 3-6 MB
    private static final int READ_BUFFER_SIZE = 8 * 1024;
    /** budget total download = connect + request; dipakai sebagai deadline streaming body */
    private static final long DOWNLOAD_MAX_MILLIS = 20_000;

    /** klien tunggal; JDK built-in (java.net.http), tanpa dependency baru */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            // JANGAN follow redirect: redirect bisa mengarah ke target internal tanpa re-validasi
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Value("${OCR_FETCH_IMAGE_URL_ALLOWED_HOSTS:}")
    private String allowedHosts;

    /**
     * Validasi murni (tanpa fetch): scheme http/https, allowlist opsional, resolusi DNS
     * + blokir IP private/loopback/link-local/metadata (169.254, 10/8, 172.16/12,
     * 192.168/16, 127/8, 0.0.0.0, multicast, IPv6 unique-local fc00::/7).
     * IllegalArgumentException = URL ditolak; message selalu generik (tanpa detail internal).
     */
    public void validate(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Image URL is required");
        }

        URI uri;
        try {
            uri = URI.create(imageUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Image URL is not valid");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("Image URL scheme must be http or https");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Image URL host is not valid");
        }

        if (allowedHosts != null && !allowedHosts.isBlank() && !isHostAllowed(host)) {
            throw new IllegalArgumentException("Image URL host is not allowed");
        }

        for (InetAddress address : resolve(host)) {
            // cek SETIAP alamat hasil resolusi (round-robin DNS tetap ter-cover)
            if (isBlocked(address)) {
                throw new IllegalArgumentException("Image URL host is not allowed");
            }
        }
    }

    /**
     * validate + fetch dalam satu call → dipakai engine OCR sebelum parse gambar.
     * Timeouts, deadline, dan ukuran dibatasi supaya fetch by URL tidak mem-blokir
     * worker pool (slowloris: trickling dibatasi deadline per-iterasi di readCapped;
     * handshake/connect ter-batas CONNECT_TIMEOUT).
     * Catatan residual: timeout HttpRequest java.net.http mencakup penerimaan HEADERS,
     * bukan streaming body — satu read() yang macet total tanpa satu byte pun tidak
     * bisa diinterupsi deadline; ini trade-off tanpa thread watchdog (tanpa dep baru).
     */
    public byte[] download(String imageUrl) {
        validate(imageUrl);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(imageUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            // URI.create internal (kegagalan tak terduga karena validate sudah lolos)
            throw new IllegalArgumentException("Image URL is not valid");
        }

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Image URL fetch interrupted");
        } catch (IOException e) {
            // jangan teruskan e kutip: "Failed to fetch image from URL" generik
            log.warn("Image URL fetch failed: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to fetch image from URL");
        }

        // 3xx TIDAK difollow (Redirect.NEVER) — redirect dianggap failure
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Image URL fetch rejected, HTTP status {}", response.statusCode());
            closeQuietly(response);
            throw new IllegalArgumentException("Failed to fetch image from URL");
        }

        // Content-Length kalau server jujur; hard cap tetap diberlakukan saat streaming
        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        if (contentLength > MAX_IMAGE_BYTES) {
            log.warn("Image URL response too large: {} bytes", contentLength);
            closeQuietly(response);
            throw new IllegalArgumentException("Image URL response is too large");
        }

        try (InputStream in = response.body()) {
            // deadline streaming body: `HttpRequest.timeout` sudah habis efek-nya setelah
            // headers terbaca (ofInputStream complete saat headers) — trickle tanpa cap
            // bisa menahan worker unlimited (slowloris)
            long readDeadlineNanos = System.nanoTime() + DOWNLOAD_MAX_MILLIS * 1_000_000L;
            return readCapped(in, readDeadlineNanos);
        } catch (IOException e) {
            log.warn("Image URL read failed: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to fetch image from URL");
        }
    }

    private byte[] readCapped(InputStream in, long readDeadlineNanos) throws IOException {
        int total = 0;
        int read;
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            while ((read = in.read(buffer)) != -1) {
                if (System.nanoTime() > readDeadlineNanos) {
                    // exception generik sama seperti failure fetch lain (tanpa detail internal)
                    throw new IllegalArgumentException("Failed to fetch image from URL");
                }
                total += read;
                if (total > MAX_IMAGE_BYTES) {
                    throw new IllegalArgumentException("Image URL response is too large");
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private InetAddress[] resolve(String host) {
        try {
            return InetAddress.getAllByName(host);
        } catch (IOException e) {
            // tidak disebut host-nya ke client (jangan memudahkan probing DNS)
            log.warn("Image URL host could not be resolved: {}", e.getMessage());
            throw new IllegalArgumentException("Image URL host could not be resolved");
        }
    }

    private boolean isHostAllowed(String host) {
        return Arrays.stream(allowedHosts.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .anyMatch(entry -> entry.equalsIgnoreCase(host));
    }

    /**
     * Dipakai juga untuk unit test statis (tanpa DNS). Blokir alamat menurut kategori:
     * any-local, loopback (127/8), link-local/metadata (169.254/16), site-local
     * (10/8, 172.16/12, 192.168/16), multicast (224/4), CGNAT/shared (100.64.0.0/10),
     * reserved 240.0.0.0/4 termasuk broadcast 255..., dan IPv6 unique-local (fc00::/7).
     */
    public static boolean isBlocked(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] octets = address.getAddress();
        if (address instanceof Inet6Address) {
            // Java isSiteLocalAddress tidak mencakup fc00::/7 (unique-local modern)
            return (octets[0] & 0xFE) == 0xFC;
        }
        // Inet4 tambahan: CGNAT 100.64.0.0/10 → oktet-1 tepat 100 (bit /10), oktet-2 64..127
        if ((octets[0] & 0xFF) == 100 && (octets[1] & 0xFF) >= 64 && (octets[1] & 0xFF) <= 127) {
            return true;
        }
        // reserved 240.0.0.0/4 (multicast 224-239 sudah kebetulan di atas)
        return (octets[0] & 0xFF) >= 240;
    }

    private void closeQuietly(HttpResponse<InputStream> response) {
        try {
            response.body().close();
        } catch (IOException ignored) {
        }
    }
}
