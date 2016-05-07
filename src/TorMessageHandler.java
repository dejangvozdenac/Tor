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
        this.exitServer = true;
        inFromPrevious = new BufferedReader(
                new InputStreamReader(connSocket.getInputStream(),"US-ASCII"));

        outToPrevious =
                new DataOutputStream(connSocket.getOutputStream());

    }

    public void processRequest()
    {
        try {
            String clientReq="";
            String nextResponse="";
            boolean connOpen = true;
            while(connOpen) {
                if(inFromPrevious.ready()&&(clientReq=inFromPrevious.readLine())!="") {
                    String[] previousSplit = clientReq.split("`");
                    TorServer.Debug(previousSplit[0]);
                    switch (previousSplit[0]) {
                        case "CREATE":
                            //alocate resources, agree on key
                            break;
                        case "EXTEND":
                            exitServer = false;
                            String nextTorHost = previousSplit[1];
                            int nextTorPort = Integer.parseInt(previousSplit[2]);
                            InetAddress nextTorIP = InetAddress.getByName(nextTorHost);
                            Socket nextTorSocket = new Socket(nextTorIP, nextTorPort);
                            TorMessage extMessage = new TorMessage(TorMessage.Type.CREATE, "\n");
                            TorMessage extended = new TorMessage(TorMessage.Type.EXTENDED, "\n");
                            inFromNext = new BufferedReader(
                                    new InputStreamReader(nextTorSocket.getInputStream(), "US-ASCII"));
                            outToNext =
                                    new DataOutputStream(nextTorSocket.getOutputStream());
                            outToNext.write(extMessage.getBytes());
                            outToPrevious.write(extended.getBytes());
                            break;
                        case "DATA":
                            TorMessage dataRelay = new TorMessage(TorMessage.Type.DATA, previousSplit[1]);
                            outToPrevious.write(dataRelay.getBytes());
                            break;
                        case "BEGIN":
                            if (exitServer) {
                                String targetURL = previousSplit[1];
                                String response = getHTML(targetURL);
                                TorMessage dataResponse = new TorMessage(TorMessage.Type.DATA, response + "\n");
                                outToPrevious.write(dataResponse.getBytes());
                            } else {
                                TorMessage beginRelay = new TorMessage(TorMessage.Type.BEGIN, previousSplit[1] + "\n");
                                outToNext.write(beginRelay.getBytes());
                            }
                            break;
                        case "TEARDOWN":
                            if (!exitServer) {
                                TorMessage tearDown = new TorMessage(TorMessage.Type.TEARDOWN, "\n");
                                outToNext.write(tearDown.getBytes());
                            }
                            connOpen = false;
                            break;
                        default:
                            TorServer.Debug("Bad Message!");
                    }
                }
                if(!exitServer&&inFromNext.ready()&&(nextResponse=inFromNext.readLine())!=""){
                    String[] nextSplit = nextResponse.split("`");
                    TorServer.Debug("NEXT"+nextSplit[0]);
                    switch (nextSplit[0]) {
                        case "DATA":
                            TorMessage dataRelay = new TorMessage(TorMessage.Type.DATA, nextSplit[1]);
                            outToPrevious.write(dataRelay.getBytes());
                            break;
                        default:
                            TorServer.Debug("BAD");
                    }
                }
            }
            connSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }
}