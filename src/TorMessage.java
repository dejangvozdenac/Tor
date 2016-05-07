/**
 * Created by dejan on 5/6/16.
 */
public class TorMessage {
    public enum Type {
        CREATE, CREATED, EXTEND, EXTENDED, BEGIN, DATA, TEARDOWN;
    }

    private Type type;
    private String body;

    public TorMessage(Type type, String body){
        this.type = type;
        this.body = body;
    }

    public TorMessage(String packedMessage){
        this.type = Type.BEGIN;
        this.body = "\n";
    }

    public byte[] getBytes(){
        byte[] toSend=(type+"`"+body).getBytes();
        return toSend;
    }

    public String getString(){
        return type+body;
    }

    public Type getType() {
        return type;
    }
}
