import java.security.PublicKey;

/**
 * Created by dejan on 5/6/16.
 */
public class TorMessage {
    private static final String SEPARATOR = "`";

    public enum Type {
        CREATE, CREATED, EXTEND, EXTENDED, RELAY, RELAYED, BEGIN, DATA, TEARDOWN;
    }

    private Type type;
    private String body;
    private String extendHost;
    private int extendPort;
    private String dataPayload;
    private String beginURL;
    private String remotePublicKey;


    //used to construct when sending
    // type CREATE, CREATED, EXTENDED
    public TorMessage(Type type, PublicKey publicKey) {
        
    }

    // type EXTEND
    public TorMessage(Type type, PublicKey publicKey, String nextServerName, int nextServerPort) {
        
    }

    // type DATA
    public TorMessage(Type type, byte[] payload) {
        
    }

    // type BEGIN
    public TorMessage(Type type, String url) {
        
    }

    // type TEARDOWN
    public TorMessage(Type type) {
        
    }

    //used to construct when receiving
    public TorMessage(String packedMessage) {
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

    public static Type parseType(String s){
        switch(s){
            case "CREATE":
                return Type.CREATE;
                // type publickey
            case "CREATED":
                return Type.CREATED;
                // type publickey
            case "EXTEND":
                return Type.EXTEND;
                // type nextservername nextserverport publickey
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

    public byte[] getBytes(){
        byte[] toSend=(type+SEPARATOR+body).getBytes();
        return toSend;
    }

    public String getString(){
        return type+SEPARATOR+body+"\n";
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

    public String getDataPayload() {
        return dataPayload;
    }

    public String getBeginURL() {
        return beginURL;
    }

    public String getRemotePublicKey() {
        return remotePublicKey;
    }
}
