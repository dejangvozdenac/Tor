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
        if (a.length != 64 || b.length != 64) {
            System.err.println("can't create key");
        }

        byte[] key = new byte[a.length + b.length];
        System.arraycopy(a, 0, key, 0, a.length);
        System.arraycopy(b, 0, key, a.length, b.length);

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;
    }

    public static byte[] encrypt(String plainText, SecretKey secretKey) throws Exception {
        byte[] plainTextBytes = plainText.getBytes();

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedByte = cipher.doFinal(plainTextBytes);
        return encryptedByte;
    }

    public static String decrypt(byte[] encryptedText, SecretKey secretKey) throws Exception {
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decryptedByte = cipher.doFinal(encryptedText);
        return new String(decryptedByte);
    }
}
