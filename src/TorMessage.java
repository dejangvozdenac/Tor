import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.nio.ByteBuffer;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by dejan on 5/6/16.
 */
public class TorMessage {
    final static int SERVER_NAME_MAX_LEN = 19;

    public enum Type {
        CREATE,
        CREATED,
        AES_REQUEST,
        AES_RESPONSE,
        EXTEND,
        EXTENDED,
        RELAY,
        RELAYED,
        BEGIN,
        DATA,
        TEARDOWN;
    }

    private int length;
    private Type type;
    private PublicKey publicKey;
    private String extendHost;
    private int extendPort;
    private byte[] payload;
    private String url;
    private byte[] bytes;

    //used to construct when sending
    // type CREATE, CREATED, EXTENDED
    public TorMessage(Type type, PublicKey publicKey) {
        this.length = 4 + 4 + publicKey.getEncoded().length;
        this.type = type;
        this.publicKey = publicKey;
        pack();
    }

    // type EXTEND
    public TorMessage(Type type, PublicKey publicKey, String extendHost, int extendPort) {
        this.length = 4 + 4 + publicKey.getEncoded().length + extendHost.getBytes().length + 4;
        this.type = type;
        this.publicKey = publicKey;
        this.extendHost = extendHost;
        this.extendPort = extendPort;
        pack();
    }

    // type DATA, AES_REQUEST, AES_RESPONSE, RELAY
    public TorMessage(Type type, byte[] payload) {
        this.length = 4 + 4 + payload.length;
        this.type = type;
        this.payload = payload;
        pack();
    }

    // type BEGIN
    public TorMessage(Type type, String url) {
        this.length = 4 + 4 + url.length();
        this.type = type;
        this.url = url;
        pack();
    }

    // type TEARDOWN
    public TorMessage(Type type) {
        this.length = 4 + 4;
        this.type = type;
        pack();
    }

    // used to construct when receiving
    public TorMessage(byte[] packedMessageBytes, int length) {
        ByteBuffer packedMessage = ByteBuffer.wrap(packedMessageBytes);

        type = packedMessage.getInt();

        switch (type) {
            case CREATE:
            case CREATED:
            case EXTENDED:
                byte[] publicKeyBytes = new byte[length];
                packedMessage.get(publicKeyBytes, 0, length);

                PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
                break;

            case BEGIN:
            case DATA:
                byte[] payloadBytes = new byte[length];
                packedMessage.get(payloadBytes, 0, length);
                break;

            case EXTEND:
                byte[] serverNameBytes = new byte[SERVER_NAME_MAX_LEN];
                packedMessage.get(serverNameBytes, 0, SERVER_NAME_MAX_LEN);
                
                String extendHostJunk = new String(serverNameBytes);
                extendHost = extendHostJunk.split(" ");

                extendPort = packedMessage.getInt();

                byte[] publicKeyBytes = new byte[length - 4 - SERVER_NAME_MAX_LEN];
                packedMessage.get(publicKeyBytes, 0, length - 4 - SERVER_NAME_MAX_LEN);
                PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
                break;
        }
    }

    private void pack() {
        ByteBuffer[] bytes = new ByteBuffer[length];

        bytes.putInt(length);
        bytes.putInt(type);

        switch (type) {
            case CREATE:
            case CREATED:
                bytes.put(publicKey.getEncoded());
                break;
            case EXTENDED:
                bytes.put(publicKey.getEncoded());
                break;
            case EXTEND:
                int start = bytes.position;

                bytes.put((extendHost + " ").getBytes());
                bytes.putInt(extendPort, start + SERVER_NAME_MAX_LEN + 1); // padding host
                bytes.put(publicKey.getEncoded());
                break;
            case BEGIN:
            case DATA:
                bytes.put(payload);
                break;
        }
    }

    public byte[] getBytes() {
        return ByteBuffer.wrap(bytes);
    }

    // for debugging purposes
    public String getString() {
        return "length: " + Integer.toString(length) +
               " type: " + type + 
               " publicKey (don't trust) " + publicKey.toString() + 
               " host: " + extendHost +
               " port: " + Integer.toString(extendPort) + 
               " payload: " + new String(payload) + 
               " url: " + url;
    }

    public Type getType() {
        return type;
    }

    public int getExtendPort() {
        return extendPort;
    }

    public String getExtendHost() {
        return extendHost;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getURL() {
        return url;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
