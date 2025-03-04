package m2codes.perizinan_ocr_tool.infrastructure.security.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyGenerator {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    private final KeyStoreUtil keyStoreUtil;

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