import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;

public class AES {
    static int keySize = 128;
    SecretKey secretKey;

    public AES(byte[] a, byte[] b) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keySize);
        secretKey = createKey(a, b);
    }

    public AES() throws Exception {
    }

    SecretKey getSecretKey() {
        return secretKey;
    }

    public static byte[] createHalf() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] key = sha.digest();
        key = Arrays.copyOf(key, 8); // use only first 64 bits
        return key;
    }

    public static SecretKey createKey(byte[] a, byte[] b) {
        if (a.length != 8 || b.length != 8) {
            System.err.println("can't create key");
        }

        byte[] key = new byte[a.length + b.length];
        System.arraycopy(a, 0, key, 0, a.length);
        System.arraycopy(b, 0, key, a.length, b.length);

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;
    }

    public static byte[] encrypt(byte[] toEncrypt, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedByte = cipher.doFinal(toEncrypt);
        return encryptedByte;
    }

    public static byte[] decrypt(byte[] encryptedText, SecretKey secretKey) throws Exception {
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decryptedByte = cipher.doFinal(encryptedText);
        return decryptedByte;
    }
}
