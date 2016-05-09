import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.nio.ByteBuffer;

/**
 * Created by dejan on 5/6/16.
 */
public class TorMessage {
    final static SERVER_NAME_MAX_LEN = 19;

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

    // type DATA, AES_REQUEST, AES_RESPONSE
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
    public TorMessage(byte[] packedMessage, int length) {
        packedMessage = ByteBuffer.wrap(packedMessage);

        type = packedMessage.getInt();

        switch (type) {
            case Type.CREATE:
            case Type.CREATED:
            case Type.EXTENDED:
                byte[] publicKeyBytes = new byte[length];
                packedMessage.get(publicKeyBytes, 0, length);

                // TODO: how to change publickey back from bytes?
                // PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
                break;

            case Type.BEGIN:
            case Type.DATA:
                byte[] payloadBytes = new byte[length];
                packedMessage.get(payloadBytes, 0, length);
                break;

            case Type.EXTEND:
                byte[] serverNameBytes = new byte[SERVER_NAME_MAX_LEN];
                packedMessage.get(serverNameBytes, 0, SERVER_NAME_MAX_LEN);
                
                String extendHostJunk = new String(serverNameBytes);
                extendHost = extendHostJunk.split(" ");

                extendPort = packedMessage.getInt();

                byte[] publicKeyBytes = new byte[length - 4 - SERVER_NAME_MAX_LEN];
                packedMessage.get(publicKeyBytes, 0, length - 4 - SERVER_NAME_MAX_LEN);
                // TODO: how to change publickey back from bytes?
                // PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
                break;
        }
    }

    private void pack() {
        ByteBuffer[] bytes = new ByteBuffer[length];

        bytes.putInt(length);
        bytes.putInt(type);

        switch (type) {
            case Type.CREATE:
            case Type.CREATED:
                bytes.put(publicKey.getEncoded());
                break;
            case Type.EXTENDED:
                bytes.put(publicKey.getEncoded());
                break;
            case Type.EXTEND:
                int start = bytes.position;

                bytes.put((extendHost + " ").getBytes());
                bytes.putInt(extendPort, start + SERVER_NAME_MAX_LEN + 1); // padding host
                bytes.put(publicKey.getEncoded());
                break;
            case Type.BEGIN:
            case Type.DATA:
                bytes.put(payload);
                break;
        }
    }

    public ByteBuffer[] getBytes() {
        return bytes;
    }

    public String getString() {
        return type + SEPARATOR + body + "\n";
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
