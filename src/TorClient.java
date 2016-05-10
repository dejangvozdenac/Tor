/**
 * TorClient
 * Created by dejan on 5/6/16.
 * Usage: java TorClient [server addr] [server port]
 */

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.Provider;
import java.security.PublicKey;
import java.util.*;

public class TorClient {
    static boolean DEBUG = false; // if false, then automatically sets up and tears down circuit
    private static final int ONION_SERVER_COUNT = 2;
    private static RSA encryption;
    private static PublicKey[] remotePublicKeys = new PublicKey[ONION_SERVER_COUNT];
    private static SecretKey[] secretKeys = new SecretKey[ONION_SERVER_COUNT];

    public static void main(String args[]) throws Exception {
        if (args.length != 3) {
            Debug("Usage: java TCPClient [server addr] [server port] [OR file]");
            System.exit(1);
        }

        String serverName = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String orFilename = args[2];

        List<String> onionRouters = readRouters(orFilename);
        List<String> path = pickPath(ONION_SERVER_COUNT, onionRouters); // TODO: randomize number of ORs

        encryption = new RSA();

        // create socket to first onion router
        String orServerName = path.get(0).split(" ")[0];
        InetAddress orServerIPAddress = InetAddress.getByName(orServerName);
        int orPort = Integer.parseInt(path.get(0).split(" ")[1]);
        Socket clientSocket = new Socket(orServerIPAddress, orPort);


        // get input from keyboard
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        //create buffered readers
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        InputStream inFromServer = clientSocket.getInputStream();

        //do the handshake
        establishSecureConnection(outToServer, inFromServer);

        Debug("AUTOMATIC MODE");

        setupCircuit(outToServer, inFromServer, path);

        // get input
        System.out.printf("file to retrieve: ");
        String filename = inFromUser.readLine();

        // fetch data
        while (filename.length() != 0) {
            // write to target server
            String url = filenametoURL(serverName, serverPort, filename);
            retrieveURL(outToServer, inFromServer, url);

            // get more input
            System.out.printf("file to retrieve: ");
            filename = inFromUser.readLine();
        }

        teardownCircuit(outToServer, inFromServer);

        // close client socket
        clientSocket.close();
    }

    private static void retrieveURL(DataOutputStream outToServer, InputStream inFromServer, String url) throws Exception {
          TorMessage dataMsg = new TorMessage(TorMessage.Type.BEGIN, url);
        TorMessage encrypted = AESMultipleEncrypt(dataMsg,secretKeys,ONION_SERVER_COUNT);
        outToServer.write(encrypted.getBytes());

        TorMessage msgFromServer = readMessage(inFromServer);
        Debug("Received: " + msgFromServer.getPayload());
    }

    private static void teardownCircuit(DataOutputStream outToServer, InputStream inFromServer) throws Exception {
        TorMessage teardownMsg = new TorMessage(TorMessage.Type.TEARDOWN);
        outToServer.write(teardownMsg.getBytes());
    }

    private static void establishSecureConnection(DataOutputStream outBuffer, InputStream inBuffer) throws Exception {
        //find out what the public key is
        TorMessage createMsg = new TorMessage(TorMessage.Type.CREATE, encryption.getPublicKey());
        outBuffer.write(createMsg.getBytes());

        TorMessage msgFromServer = readMessage(inBuffer);
        remotePublicKeys[0] = msgFromServer.getPublicKey();

        //arrange the aes key
        AES symmetricEncryption = new AES();

        TorMessage aesMsg = new TorMessage(TorMessage.Type.AES_REQUEST,
                encryption.encrypt(symmetricEncryption.createHalf(),remotePublicKeys[0]));
        outBuffer.write(aesMsg.getBytes());

        TorMessage aesFromServer = readMessage(inBuffer);
        Debug("Received: " + aesFromServer.toString());
        secretKeys[0] = new SecretKeySpec(
                encryption.decrypt(aesFromServer.getPayload(), encryption.getPrivateKey()),"AES");
    }

    private static void setupCircuit(DataOutputStream outToServer, InputStream inFromServer, List<String> orPath)
            throws Exception {
        for (int i = 1; i < orPath.size(); i++) {
            //get the next hop
            String orServerName = orPath.get(i).split(" ")[0];
            int orPort = Integer.parseInt(orPath.get(i).split(" ")[1]);
            //send extend messsage
            TorMessage extendMsg = new TorMessage(TorMessage.Type.EXTEND,encryption.getPublicKey(),orServerName,orPort);
            //encrypt and relay it
            TorMessage encryptedExtend = AESMultipleEncrypt(extendMsg, secretKeys, i);
            outToServer.write(encryptedExtend.getBytes());

            //get response back
            TorMessage extendResponse= readMessage(inFromServer);
            TorMessage extendDecrypted = AESMultipleDecrypt(extendResponse,secretKeys,i);
            remotePublicKeys[i] = extendDecrypted.getPublicKey();

            //arrange the aes key
            AES symmetricEncryption = new AES();

            TorMessage aesMsg = new TorMessage(TorMessage.Type.AES_REQUEST,
                    encryption.encrypt(symmetricEncryption.createHalf(),remotePublicKeys[i]));
            TorMessage encryptedAES = AESMultipleEncrypt(aesMsg,secretKeys,i);
            outToServer.write(encryptedAES.getBytes());

            TorMessage aesFromServer = readMessage(inFromServer);
            TorMessage aesDecrypted = AESMultipleEncrypt(aesFromServer,secretKeys,i);
            secretKeys[i] = new SecretKeySpec(
                    encryption.decrypt(aesDecrypted.getPayload(), encryption.getPrivateKey()),"AES");

        }
    }

    public static TorMessage readMessage(InputStream in) throws Exception{
        byte[] intByte = new byte[4];
        in.read(intByte,0,4);
        ByteBuffer wrapped = ByteBuffer.wrap(intByte);
        int length = wrapped.getInt()-4;

        byte[] msg = new byte[length];
        in.read(msg, 0, length);
        return (new TorMessage(msg,length));
    }

    private static TorMessage AESMultipleEncrypt(TorMessage msg, SecretKey[] keys, int times) throws  Exception{
        TorMessage previous = msg;
        previous.printString();
        TorMessage newMsg;
        AES aesEncrypt = new AES();
        for (int i =0; i<times; i++){
            newMsg=new TorMessage(TorMessage.Type.RELAY,aesEncrypt.encrypt(previous.getBytes(),keys[i]));
            previous=newMsg;
            previous.printString();
        }
        return previous;
    }

    private static TorMessage AESMultipleDecrypt(TorMessage msg, SecretKey[] keys, int times) throws Exception{
        TorMessage previous = msg;
        TorMessage newMsg;
        AES encrypt = new AES();
        for(int i = 0; i < times; i++){
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(previous.getPayload()));
            int length = in.readInt();

            byte[] bytes = new byte[length];
            in.read(bytes, 0, length);

            newMsg = new TorMessage(encrypt.decrypt(bytes, keys[times - 1 - i]), length);
            previous = newMsg;
        }
        return previous;
    }
    private static String filenametoURL(String serverName, int port, String filename) {
        return "http://" + serverName + ":" + Integer.toString(port) + "/" + filename;
    }

    // takes in list of hostnames and path length
    private static List<String> pickPath(int length, List<String> onionRouters) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i=1; i<=length; i++) {
            list.add(new Integer(i));
        }
        Collections.shuffle(list);
        List<String> routers = new ArrayList<String>();
        for(int i=0; i<length; i++){
            routers.add(onionRouters.get(list.get(i)-1));
        }
        return routers;
    }

    private static List<String> readRouters(String filename) {
        List<String> routers = new ArrayList<String>();
        
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(filename));
            
            String line = br.readLine();
            while (line != null) {
                routers.add(line);
                line = br.readLine();
            }

            br.close();
        } catch (Exception e) {
            Debug("ded");
        }

        return routers;
    }

    private static void Debug(String s){
        System.out.println(s);
    }
}
