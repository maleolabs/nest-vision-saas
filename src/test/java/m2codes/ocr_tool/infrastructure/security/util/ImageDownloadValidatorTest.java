package m2codes.ocr_tool.infrastructure.security.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test logika murni (tanpa Spring context, tanpa DNS/network):
 * ImageDownloadValidator#validate dan #isBlocked (S-07 / VIS-F08).
 *
 * NOTE: BELUM DIAKSEKUSI di sandbox (hanya JRE tanpa javac, MySQL tidak tersedia).
 * Jalankan via `mvn test` di dev env.
 */
class ImageDownloadValidatorTest {

    private ImageDownloadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ImageDownloadValidator();
        ReflectionTestUtils.setField(validator, "allowedHosts", "");
    }

    @Test
    void isBlockedCoversPrivateLoopbackLinkLocalAndMetadataRanges() throws Exception {
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("10.1.2.3")));        // 10/8
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("172.16.0.1")));      // 172.16/12
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("172.31.255.255")));  // batas atas 172.16/12
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("192.168.1.1")));     // 192.168/16
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("127.0.0.1")));       // 127/8
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("169.254.169.254"))); // metadata
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("0.0.0.0")));         // any-local
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("::1")));             // loopback IPv6
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("fc00::1")));         // unique-local IPv6
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("100.64.1.1")));      // CGNAT 100.64/10
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("100.127.255.255"))); // batas atas CGNAT
        assertTrue(ImageDownloadValidator.isBlocked(InetAddress.getByName("240.1.2.3")));       // reserved 240/4
    }

    @Test
    void isBlockedAllowsPublicAddresses() throws Exception {
        assertFalse(ImageDownloadValidator.isBlocked(InetAddress.getByName("8.8.8.8")));
        assertFalse(ImageDownloadValidator.isBlocked(InetAddress.getByName("172.32.0.1")));     // di luar 172.16/12
        assertFalse(ImageDownloadValidator.isBlocked(InetAddress.getByName("2607:f8b0:4004::1")));
        assertFalse(ImageDownloadValidator.isBlocked(InetAddress.getByName("100.20.0.1")));     // di luar CGNAT (oktet-2 < 64)
        assertFalse(ImageDownloadValidator.isBlocked(InetAddress.getByName("101.64.1.1")));     // di luar CGNAT (oktet-1 ≠ 100)
    }

    @Test
    void validateRejectsBlankAndNull() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("   "));
    }

    @Test
    void validateRejectsNonHttpScheme() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate("ftp://example.com/a.png"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("file:///etc/passwd"));
    }

    @Test
    void validateRejectsMalformedUrlWithoutHost() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http:///only-path.png"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("::::"));
    }

    @Test
    void validateRejectsPrivateIpLiteralEvenWithEmptyAllowlist() {
        // literal IP: resolusi tidak butuh DNS → aman untuk unit test offline
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://10.0.0.1/image.png"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://169.254.169.254/latest/meta-data"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://[fd00::1]/image.png"));
    }

    @Test
    void validateAllowsPublicIpLiteralWhenAllowlistEmpty() {
        validator.validate("http://8.8.8.8/image.png");
        validator.validate("https://203.0.113.10/image.png");
    }

    @Test
    void validateRejectsWhenHostNotInAllowlist() {
        ReflectionTestUtils.setField(validator, "allowedHosts", "trusted.example.com, other.example.com");
        assertThrows(IllegalArgumentException.class, () -> validator.validate("https://untrusted.example.com/a.png"));
        // public literal juga harus tunduk pada allowlist ketika non-kosong (cek sebelum resolusi)
        assertThrows(IllegalArgumentException.class, () -> validator.validate("http://8.8.8.8/image.png"));
    }

    @Test
    void validateAllowlistMatchingIsCaseInsensitive() {
        // allowlist entry UPPERCASE sedangkan URL host lowercase — LEBIH PENTING:
        // kedua-nya harus masuk domain .invalid sama supaya allowlist match dulu
        // (case-insensitive) lalu resolve gagal (NXDOMAIN, RFC 2606). Kalau matching
        // TIDAK case-insensitive, pesan-nya "host is not allowed" dan test merah.
        ReflectionTestUtils.setField(validator, "allowedHosts", "TRUSTED.EXAMPLE.INVALID");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://trusted.example.invalid/a.png"));
        assertEquals("Image URL host could not be resolved", e.getMessage());
    }

    @Test
    void validateAllowlistEntryLowercaseMatchesUppercaseHost() {
        // mirror H1: entry lowercase × host uppercase — kalau tidak dicocokkan
        // case-insensitive, penolakan-nya "not allowed" bukan "could not be resolved"
        ReflectionTestUtils.setField(validator, "allowedHosts", "trusted.example.invalid");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://TRUSTED.EXAMPLE.INVALID/a.png"));
        assertEquals("Image URL host could not be resolved", e.getMessage());
    }
}
