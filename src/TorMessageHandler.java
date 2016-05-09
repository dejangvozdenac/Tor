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
                            byte[] decryptedBytes = aesEncryption.decrypt(receivedMsg.getPayload(),secretKey);
                            TorMessage decrypted = new TorMessage(decryptedBytes, decryptedBytes.length+4);
                            String nextTorHost = decrypted.getExtendHost();
                            int nextTorPort = decrypted.getExtendPort();
                            InetAddress nextTorIP = InetAddress.getByName(nextTorHost);
                            Socket nextTorSocket = new Socket(nextTorIP, nextTorPort);
                            inFromNext = nextTorSocket.getInputStream();
                            outToNext = new DataOutputStream(nextTorSocket.getOutputStream());
                            break;
                        case DATA:
                            TorMessage dataRelay = new TorMessage(TorMessage.Type.DATA, receivedMsg.getPayload());
                            outToPrevious.write(dataRelay.getBytes());
                            outToPrevious.write("\n".getBytes());
                            break;
                        case BEGIN:
                            if (exitServer) {
                                String targetURL = receivedMsg.getURL();
                                String response = getHTML(targetURL);
                                TorMessage dataResponse = new TorMessage(TorMessage.Type.DATA, response );
                                outToPrevious.write(dataResponse.getBytes());
                            } else {
                                TorMessage beginRelay = new TorMessage(
                                        TorMessage.Type.BEGIN, receivedMsg.getURL() );
                                outToNext.write(beginRelay.getBytes());
                            }
                            break;
                        case TEARDOWN:
                            if (!exitServer) {
                                TorMessage tearDown = new TorMessage(TorMessage.Type.TEARDOWN, "");
                                outToNext.write(tearDown.getBytes());
                            }
                            connOpen = false;
                            break;
                        case AES_REQUEST:
                            AES aes = new AES(
                                    encryption.decrypt(receivedMsg.getPayload(), encryption.getPrivateKey()),
                                    aesEncryption.createHalf());
                            TorMessage aesResponse = new TorMessage(TorMessage.Type.AES_RESPONSE,
                                    encryption.encrypt(aes.getSecretKey().getEncoded(),remotePublicKey));
                            outToPrevious.write(aesResponse.getBytes());
                            break;
                        default:
                            TorServer.Debug("Bad Message!");
                    }
                    TorServer.Debug("Processed Request");
                }
                if(!exitServer&&inFromNext.available()>0){
//                    &&(nextResponseLen=inFromNext.readLine())!=""
                    TorMessage receivedMsg = TorClient.readMessage(inFromNext);
                    TorServer.Debug("RELAYING BACK");
                    AES enc = new AES();
                    TorMessage dataRelay = new TorMessage(TorMessage.Type.RELAY,
                            enc.encrypt(receivedMsg.getPayload(),secretKey));
                    outToPrevious.write(dataRelay.getBytes());
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