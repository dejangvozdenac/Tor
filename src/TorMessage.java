import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.nio.ByteBuffer;

/**
 * Created by dejan on 5/6/16.
 */
public class TorMessage {
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

    //used to construct when sending
    // type CREATE, CREATED, EXTENDED
    public TorMessage(Type type, PublicKey publicKey) {
        this.length = 4 + 4 + publicKey.getEncoded().length;
        this.type = type;
        this.publicKey = publicKey;
    }

    // type EXTEND
    public TorMessage(Type type, PublicKey publicKey, String extendHost, int extendPort) {
        this.length = 4 + 4 + publicKey.getEncoded().length + extendHost.getBytes().length + 4;
        this.type = type;
        this.publicKey = publicKey;
        this.extendHost = extendHost;
        this.extendPort = extendPort;
    }

    // type DATA, AES_REQUEST, AES_RESPONSE
    public TorMessage(Type type, byte[] payload) {
        this.length = 4 + 4 + payload.length;
        this.type = type;
        this.payload = payload;
    }

    // type BEGIN
    public TorMessage(Type type, String url) {
        this.length = 4 + 4 + url.length();
        this.type = type;
        this.url = url;
    }

    // type TEARDOWN
    public TorMessage(Type type) {
        this.length = 4 + 4;
        this.type = type;
    }

    // used to construct when receiving
    public TorMessage(byte[] packedMessage, int length) {
        String[] split = packedMessage.split(SEPARATOR);
        this.type = parseType(split[0]);
        if(type==Type.CREATE){
            remotePublicKey = split[1];
        }
        else if(type==Type.EXTEND){
            extendHost = split[1];
            extendPort = Integer.parseInt(split[2]);
        }
        else if(type==Type.DATA){
            dataPayload=split[1];
        }
        else if(type==Type.BEGIN){
            beginURL=split[1];
        }
        this.body = "\n";
    }

    public static Type parseType(String s) {
        switch (s) {
            case "CREATE":
                return Type.CREATE;
                // type publickey
            case "CREATED":
                return Type.CREATED;
                // type publickey
            case "EXTEND":
                return Type.EXTEND;
                // type nextserverport nextservername publickey
            case "EXTENDED":
                return Type.EXTENDED;
                // type publickey
            case "BEGIN":
                return Type.BEGIN;
                // type payload
            case "DATA":
                return Type.DATA;
                // type payload
            case "TEARDOWN":
                return Type.TEARDOWN;
                // type
            default:
                return Type.BEGIN; //TODO handle this error better
        }
    }

    public ByteBuffer[] getBytes() {
        // TODO: put this in initialization
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
                bytes.putInt(extendPort, start + 20); // padding host
                bytes.put(publicKey.getEncoded());
                break;
            case Type.BEGIN:
            case Type.DATA:
                bytes.put(payload);
                break;
        }

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
