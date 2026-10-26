package m2codes.ocr_tool.infrastructure.security.util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher; import javax.crypto.spec.GCMParameterSpec; import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets; import java.security.SecureRandom; import java.util.Base64;
@Component public class ApiKeyEncryption {
 private final SecretKeySpec key; private final SecureRandom random=new SecureRandom();
 public ApiKeyEncryption(@Value("${app.api-key.encryption-secret:}") String secret){ if(secret==null||secret.isBlank()) throw new IllegalStateException("APP_API_KEY_ENCRYPTION_SECRET must be set"); byte[] b; try{b=Base64.getDecoder().decode(secret);}catch(IllegalArgumentException e){throw new IllegalStateException("APP_API_KEY_ENCRYPTION_SECRET must be base64",e);} if(b.length!=32)throw new IllegalStateException("APP_API_KEY_ENCRYPTION_SECRET must decode to 32 bytes"); key=new SecretKeySpec(b,"AES"); }
 public Encrypted encrypt(String value){try{byte[] n=new byte[12];random.nextBytes(n);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,n));return new Encrypted(Base64.getEncoder().encodeToString(n),Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8))));}catch(Exception e){throw new IllegalStateException("API key encryption failed",e);}}
  public String decrypt(String nonce,String value){try{byte[] n=Base64.getDecoder().decode(nonce); byte[] ciphertext=Base64.getDecoder().decode(value); if(n.length!=12||ciphertext.length<16) throw new IllegalArgumentException("invalid AES-GCM payload"); Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,n));return new String(c.doFinal(ciphertext),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("API key decryption failed",e);}}
 public record Encrypted(String nonce,String ciphertext){}
}
