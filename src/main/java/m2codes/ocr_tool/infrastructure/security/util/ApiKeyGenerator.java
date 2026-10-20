package m2codes.ocr_tool.infrastructure.security.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyGenerator {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    private final KeyStoreUtil keyStoreUtil;

    @org.springframework.beans.factory.annotation.Value("${app.api-key.pepper:}")
    private String pepper;

    @PostConstruct
    void validatePepper() {
        if (pepper == null || pepper.length() < 32) {
            throw new IllegalStateException("APP_API_KEY_PEPPER must be set and contain at least 32 characters");
        }
    }

    public String generate() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "app_v2_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Unable to hash API key", e);
        }
    }

    public String encrypt(String teks) throws Exception {
        teks = getRandomCode().concat(teks);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey());
        byte[] encryptedBytes = cipher.doFinal(teks.getBytes());
        return "app_" + Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String encrpytedTeks) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey());
        encrpytedTeks = encrpytedTeks.substring(4);
        byte[] decodedBytes = Base64.getDecoder().decode(encrpytedTeks);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes).substring(4);
    }

    private SecretKey secretKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
        generator.init(KEY_SIZE);

        SecretKey secretKey = generator.generateKey();
        try {
            secretKey = keyStoreUtil.getSecretKey();
        } catch (Exception e) {
            keyStoreUtil.storeSecretKey(secretKey);
        }
        return secretKey;
    }

    private String getRandomCode() {
        SecureRandom random = new SecureRandom();
        int code = 100 + random.nextInt(900);
        return String.valueOf(code).concat("-");
    }

}
