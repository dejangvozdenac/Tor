/**
 * Created by dejan on 5/6/16.
 */
/**
 ** Yale CS433/533 Demo Basic Web Server
 **/
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;

class TorMessageHandler implements Runnable{
    private Socket connSocket;
    private BufferedReader inFromPrevious;
    private DataOutputStream outToPrevious;
    private BufferedReader inFromNext;
    private DataOutputStream outToNext;
    boolean entryServer;
    boolean exitServer;

    public void run()
    {
        processRequest();
    }

    public TorMessageHandler(Socket connectionSocket
                             ) throws Exception
    {
        this.connSocket = connectionSocket;
        this.entryServer = true;
        this.exitServer = false;
        inFromPrevious = new BufferedReader(
                new InputStreamReader(connSocket.getInputStream(),"US-ASCII"));

        outToPrevious =
                new DataOutputStream(connSocket.getOutputStream());

    }

    public void processRequest()
    {
        try {
            String clientReq;
            while((clientReq=inFromPrevious.readLine())!="") {
                String[] split = clientReq.split(":");
                TorServer.Debug(split[0]);
                switch(split[0]){
                    case "CREATE":
                        //alocate resources, agree on key
                        break;
                    case "EXTEND":
                        exitServer = false;
                        String nextTorHost = split[1];
                        int nextTorPort = Integer.parseInt(split[2]);
                        InetAddress nextTorIP = InetAddress.getByName(nextTorHost);
                        Socket nextTorSocket = new Socket(nextTorIP, nextTorPort);
                        TorMessage extMessage = new TorMessage(TorMessage.Type.CREATE, "\n");
                        TorMessage extended  = new TorMessage(TorMessage.Type.EXTENDED, "\n");
                        inFromNext = new BufferedReader(
                                 new InputStreamReader(nextTorSocket.getInputStream(),"US-ASCII"));
                        outToNext =
                                new DataOutputStream(nextTorSocket.getOutputStream());
                        outToNext.write(extMessage.getBytes());
                        outToPrevious.write(extended.getBytes());
                        break;
                    case "DATA":
                        break;
                    case "TEARDOWN":
                        if(exitServer){

                        }
                        break;
                    default:
                        TorServer.Debug("Bad Message!");
                }
                TorMessage message = new TorMessage(TorMessage.Type.BEGIN, "\n");
                byte[] toSend = message.getBytes();
                outToPrevious.write(toSend);
            }
            connSocket.close();
            TorServer.Debug("Closed");
        } catch (Exception e) {
        }
    }
}
