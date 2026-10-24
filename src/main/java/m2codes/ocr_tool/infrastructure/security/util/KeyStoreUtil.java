package m2codes.ocr_tool.infrastructure.security.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

@Slf4j
@Component
public class KeyStoreUtil {

    @Value("${keystore.path:/run/secrets/keystore.jks}")
    private String keyStoreFile;

    @Value("${keystore.password}")
    private String keyStorePassword;

    @Value("${keystore.alias}")
    private String keyStoreAlias;

    public void storeSecretKey(SecretKey secretKey) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(null, keyStorePassword.toCharArray());

        KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.ProtectionParameter passwordParam = new KeyStore.PasswordProtection(keyStorePassword.toCharArray());

        keyStore.setEntry(keyStoreAlias, secretKeyEntry, passwordParam);

        try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fos, keyStorePassword.toCharArray());
        }
    }

    public SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        try (FileInputStream fis = new FileInputStream(keyStoreFile)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }

        if (!keyStore.containsAlias(keyStoreAlias)) {
            log.info("secret key not found with alias: {}", keyStoreAlias);
            return null;
        }

        KeyStore.ProtectionParameter passwordParam = new KeyStore.PasswordProtection(keyStorePassword.toCharArray());
        KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(keyStoreAlias, passwordParam);

        return secretKeyEntry != null ? secretKeyEntry.getSecretKey() : null;
    }

}
