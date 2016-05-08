/**
 * Created by dejan on 5/6/16.
 */
public class TorMessage {
    private static final String SEPARATOR = "`";

    public enum Type {
        CREATE, CREATED, EXTEND, EXTENDED, BEGIN, DATA, TEARDOWN;
    }

    private Type type;
    private String body;
    private String extendHost;
    private int extendPort;
    private String dataPayload;
    private String beginURL;


    //used to construct when sending
    public TorMessage(Type type, String body){
        this.type = type;
        this.body = body;
    }

    //used to construct when receiving
    public TorMessage(String packedMessage){
        String[] split = packedMessage.split(SEPARATOR);
        this.type = parseType(split[0]);
        if(type==Type.EXTEND){
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
            case "CREATED":
                return Type.CREATED;
            case "EXTEND":
                return Type.EXTEND;
            case "EXTENDED":
                return Type.EXTENDED;
            case "BEGIN":
                return Type.BEGIN;
            case "DATA":
                return Type.DATA;
            case "TEARDOWN":
                return Type.TEARDOWN;
            default:
                return Type.BEGIN; //TODO handle this error better
        }
    }

    public byte[] getBytes(){
        byte[] toSend=(type+SEPARATOR+body).getBytes();
        return toSend;
    }

    public String getString(){
        return type+SEPARATOR+body;
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
}
