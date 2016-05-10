/**
 * Created by dejan on 5/6/16.
 */

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

class TorMessageHandler implements Runnable{
    private Socket connSocket;
    private InputStream inFromPrevious;
    private DataOutputStream outToPrevious;
    private InputStream inFromNext;
    private DataOutputStream outToNext;
    private boolean entryServer;
    private boolean exitServer;
    private RSA encryption;
    private AES aesEncryption;
    private PublicKey remotePublicKey;
    private SecretKey secretKey;

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
        inFromPrevious = connSocket.getInputStream();

        outToPrevious =
                new DataOutputStream(connSocket.getOutputStream());

        encryption = new RSA();
        aesEncryption = new AES();

    }

    public void processRequest()
    {
        try {
            String clientReqLen="";
            String nextResponseLen="";
            boolean connOpen = true;
            while(connOpen) {
                if(inFromPrevious.available()>0) {
//                    &&(clientReqLen=inFromPrevious.readLine())!=""
                    TorMessage receivedMsg = TorClient.readMessage(inFromPrevious);
                    connOpen = parseIncomingMessage(connOpen, receivedMsg);
                    TorServer.Debug("Processed Request");
                }
                if(!exitServer&&inFromNext.available()>0){
                    TorMessage receivedMsg = TorClient.readMessage(inFromNext);
                    TorServer.Debug("RELAYING BACK");
                    TorMessage dataRelay = new TorMessage(TorMessage.Type.RELAY,
                            aesEncryption.encrypt(receivedMsg.getBytes(),secretKey));
                    outToPrevious.write(dataRelay.getBytes());
                }
            }
            connSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean parseIncomingMessage(boolean connOpen, TorMessage receivedMsg) throws Exception {
        switch (receivedMsg.getType()) {
            case CREATE:
                //alocate resources, agree on keytoString
                TorMessage createdMsg = new TorMessage(TorMessage.Type.CREATED,
                        encryption.getPublicKey());
                remotePublicKey = receivedMsg.getPublicKey();
                outToPrevious.write(createdMsg.getBytes());
                break;
            case EXTEND:
                exitServer = false;
                String nextTorHost = receivedMsg.getExtendHost();
                int nextTorPort = receivedMsg.getExtendPort();
                InetAddress nextTorIP = InetAddress.getByName(nextTorHost);
                Socket nextTorSocket = new Socket(nextTorIP, nextTorPort);
                inFromNext = nextTorSocket.getInputStream();
                outToNext = new DataOutputStream(nextTorSocket.getOutputStream());
                break;
            case RELAY:
                 byte[] decryptedRelay = aesEncryption.decrypt(receivedMsg.getPayload(), secretKey);
                 outToNext.write(decryptedRelay);
                break;
            case DATA:
                byte[] decryptedData = aesEncryption.decrypt(receivedMsg.getPayload(), secretKey);
                receivedMsg = new TorMessage(decryptedData);
                receivedMsg.printString();
                connOpen=parseIncomingMessage(connOpen,receivedMsg);
                break;
            case BEGIN:
                assert(exitServer);
                byte[] decryptedBegin = aesEncryption.decrypt(receivedMsg.getPayload(), secretKey);

                String targetURL = new String (decryptedBegin);
                String response = getHTML(targetURL);
                TorMessage dataResponse = new TorMessage(TorMessage.Type.DATA, response);
                TorMessage beginResponse = new TorMessage(TorMessage.Type.DATA,
                        aesEncryption.encrypt(dataResponse.getBytes(),secretKey));
                outToPrevious.write(beginResponse.getBytes());
                break;
            case TEARDOWN:
                if (!exitServer) {
                    TorMessage tearDown = new TorMessage(TorMessage.Type.TEARDOWN, "");
                    outToNext.write(tearDown.getBytes());
                }
                connOpen = false;
                break;
            case AES_REQUEST:
                receivedMsg.printString();
              System.out.println(encryption.decrypt(receivedMsg.getPayload(), encryption.getPrivateKey()).length);
                AES aes = new AES(
                        aesEncryption.createHalf(),
                        aesEncryption.createHalf());
                secretKey = aes.getSecretKey();
                TorMessage aesResponse = new TorMessage(TorMessage.Type.AES_RESPONSE,
                        encryption.encrypt(aes.getSecretKey().getEncoded(),remotePublicKey));
                outToPrevious.write(aesResponse.getBytes());
                break;
            default:
                TorServer.Debug(receivedMsg.getType()+"- not handled");
        }
        return connOpen;
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