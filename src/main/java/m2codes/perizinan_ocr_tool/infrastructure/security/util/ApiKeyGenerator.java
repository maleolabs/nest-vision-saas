package m2codes.perizinan_ocr_tool.infrastructure.security.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

public class ApiKeyGenerator {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    public static String encrypt(String teks) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey());
        byte[] encryptedBytes = cipher.doFinal(teks.getBytes());
        return "app_" + Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encrpytedTeks) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey());
        encrpytedTeks = encrpytedTeks.substring(4);
        byte[] decodedBytes = Base64.getDecoder().decode(encrpytedTeks);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }

    private static SecretKey secretKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
        generator.init(KEY_SIZE);
        return generator.generateKey();
    }

}