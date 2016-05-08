/**
 * Created by dejan on 5/6/16.
 */

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
    private boolean entryServer;
    private boolean exitServer;

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
                    TorMessage receivedMsg = new TorMessage(clientReq);
                    TorServer.Debug(receivedMsg.getType()+"");
                    switch (receivedMsg.getType()) {
                        case CREATE:
                            //alocate resources, agree on key
                            break;
                        case EXTEND:
                            exitServer = false;
                            String nextTorHost = receivedMsg.getExtendHost();
                            int nextTorPort = receivedMsg.getExtendPort();
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
                        case DATA:
                            TorMessage dataRelay = new TorMessage(TorMessage.Type.DATA, receivedMsg.getDataPayload());
                            outToPrevious.write(dataRelay.getBytes());
                            outToPrevious.write("\n".getBytes());
                            break;
                        case BEGIN:
                            if (exitServer) {
                                String targetURL = receivedMsg.getBeginURL();
                                String response = getHTML(targetURL);
                                TorMessage dataResponse = new TorMessage(TorMessage.Type.DATA, response + "\n");
                                outToPrevious.write(dataResponse.getBytes());
                            } else {
                                TorMessage beginRelay = new TorMessage(
                                        TorMessage.Type.BEGIN, receivedMsg.getBeginURL() + "\n");
                                outToNext.write(beginRelay.getBytes());
                            }
                            break;
                        case TEARDOWN:
                            if (!exitServer) {
                                TorMessage tearDown = new TorMessage(TorMessage.Type.TEARDOWN, "\n");
                                outToNext.write(tearDown.getBytes());
                            }
                            connOpen = false;
                            break;
                        default:
                            outToPrevious.write("Bad Message!\n".getBytes());
                            TorServer.Debug("Bad Message!");
                    }
                }
                if(!exitServer&&inFromNext.ready()&&(nextResponse=inFromNext.readLine())!=""){
                    TorMessage receivedMsg = new TorMessage(nextResponse);
                    TorServer.Debug("RELAYING "+receivedMsg.getType());
                    switch (receivedMsg.getType()) {
                        case DATA:
                            TorMessage dataRelay = new TorMessage(TorMessage.Type.DATA, receivedMsg.getDataPayload());
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