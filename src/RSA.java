import java.security.*;
import javax.crypto.Cipher;

public class RSA {
    static int keySize = 1024;
    static PublicKey publicKey;
    static PrivateKey privateKey;

    public RSA() throws Exception {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keySize);

        final KeyPair key = keyGen.generateKeyPair();
        publicKey = key.getPublic();
        privateKey = key.getPrivate();
    }

    public static PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static String getPublicKey() {
        return new String(publicKey.getEncoded());
    }

    public static byte[] encrypt(String text, PublicKey key) {
        byte[] cipherText = null;

        try {
          // get an RSA cipher object and print the provider
          final Cipher cipher = Cipher.getInstance("RSA");

          // encrypt the plain text using the public key
          cipher.init(Cipher.ENCRYPT_MODE, key);
          cipherText = cipher.doFinal(text.getBytes());
        } catch (Exception e) {
          e.printStackTrace();
        }

        return cipherText;
      }

    public static String decrypt(byte[] text, PrivateKey key) {
        byte[] dectyptedText = null;

        try {
          // get an RSA cipher object and print the provider
          final Cipher cipher = Cipher.getInstance("RSA");

          // decrypt the text using the private key
          cipher.init(Cipher.DECRYPT_MODE, key);
          dectyptedText = cipher.doFinal(text);

        } catch (Exception ex) {
          ex.printStackTrace();
        }

        return new String(dectyptedText);
    }
}