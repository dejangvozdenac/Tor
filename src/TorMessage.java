import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.nio.ByteBuffer;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;

/**
 * Created by dejan on 5/6/16.
 */
public class TorMessage {
    final static int SERVER_NAME_MAX_LEN = 19;

    public enum Type {
        CREATE(0),
        CREATED(1),
        AES_REQUEST(2),
        AES_RESPONSE(3),
        EXTEND(4),
        EXTENDED(5),
        RELAY(6),
        RELAYED(7),
        BEGIN(8),
        DATA(9),
        TEARDOWN(10);

        private final int id;

        Type(int id) {
            this.id = id;
        }

        int toInt() {
            return id;
        }

        static Type toEnum(int id) {
            switch (id) {
                case 0:
                    return Type.CREATE;
                case 1:
                    return Type.CREATED;
                case 2:
                    return Type.AES_REQUEST;
                case 3:
                    return Type.AES_RESPONSE;
                case 4:
                    return Type.EXTEND;
                case 5:
                    return Type.EXTENDED;
                case 6:
                    return Type.RELAY;
                case 7:
                    return Type.RELAYED;
                case 8:
                    return Type.BEGIN;
                case 9:
                    return Type.DATA;
                case 10:
                    return Type.TEARDOWN;
                default:
                    System.out.println("BAD: " + Integer.toString(id));
                    return Type.BEGIN; // TODO: BAD
            }
        }
    }

    private int length;
    private Type type;
    private PublicKey publicKey = null;
    private String extendHost = null;
    private int extendPort = 0;
    private byte[] payload = null;
    private String url = null;
    private ByteBuffer bytes = null;

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
        this.length = 4 + 4 + publicKey.getEncoded().length + SERVER_NAME_MAX_LEN + 2 + 4;
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
    // TODO: don't actually need to pass in length right now
    public TorMessage(byte[] packedMessageBytes, int length) throws Exception {
        ByteBuffer packedMessage = ByteBuffer.wrap(packedMessageBytes);

        this.length = length;
        int typeInt = packedMessage.getInt();
        type = Type.toEnum(typeInt);

        byte[] publicKeyBytes;
        switch (type) {
            case CREATE:
            case CREATED:
            case EXTENDED:
                publicKeyBytes = new byte[length - 4];
                packedMessage.get(publicKeyBytes, 0, length - 4);

                publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
                break;

            case BEGIN:
            case DATA:
            case AES_REQUEST:
            case AES_RESPONSE:
                payload = new byte[length - 4];
                packedMessage.get(payload, 0, length - 4);
                break;

            case EXTEND:
                byte[] serverNameBytes = new byte[SERVER_NAME_MAX_LEN + 2];
                packedMessage.get(serverNameBytes, 0, SERVER_NAME_MAX_LEN + 2);
                
                String extendHostJunk = new String(serverNameBytes);
                extendHost = extendHostJunk.split(" ")[0];

                extendPort = packedMessage.getInt();

                publicKeyBytes = new byte[packedMessage.remaining()];
                packedMessage.get(publicKeyBytes, 0, packedMessage.remaining());
                publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
                break;
        }
    }

    private void pack() {
        bytes = ByteBuffer.allocate(length);

        bytes.putInt(length);
        bytes.putInt(type.toInt());

        switch (type) {
            case CREATE:
            case CREATED:
            case EXTENDED:
                bytes.put(publicKey.getEncoded());
                break;
            case EXTEND:
                int start = bytes.position();

                bytes.put(extendHost.getBytes());
                bytes.put(" ".getBytes());

                bytes.position(start + SERVER_NAME_MAX_LEN + 2);

                bytes.putInt(extendPort); // padding host
                bytes.put(publicKey.getEncoded());
                break;
            case BEGIN:
            case DATA:
            case AES_REQUEST:
            case AES_RESPONSE:
                bytes.put(payload);
                break;
        }
    }

    public byte[] getBytes() {
        byte[] byteRepr = new byte[bytes.capacity()];
        
        // read
        bytes.flip();

        bytes.get(byteRepr, 0, byteRepr.length);

        // go back to writing
        // bytes.flip();
        return byteRepr; 
    }

    // for debugging purposes
    public void printString() {
        System.out.println("length: " + Integer.toString(length));
        System.out.println("type: " + type);

        if (publicKey != null) {
            System.out.println("publicKey: " + publicKey.toString());
        }

        if (extendHost != null) {
            System.out.println("host: " + extendHost);   
        }

        if (extendPort != 0) {
            System.out.println("port: " + Integer.toString(extendPort));
        }

        if (payload != null) {
            System.out.println("payload: " + new String(payload));
        }

        if (url != null) {
            System.out.println("url: " + url);
        }
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
        if(payload==null){
            return new byte[0];
        }
        else {
            return payload;
        }
    }

    public String getURL() {
        return url;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
