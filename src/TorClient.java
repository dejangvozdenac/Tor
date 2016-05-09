/**
 * TorClient
 * Created by dejan on 5/6/16.
 * Usage: java TorClient [server addr] [server port]
 */

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.Provider;
import java.security.PublicKey;
import java.util.*;
// import java.math.BigInteger;
// import java.security.*;
// import java.security.spec.*;
// import java.security.interfaces.*;
// import javax.crypto.*;
// import javax.crypto.spec.*;
// import javax.crypto.interfaces.*;
// import com.sun.crypto.provider.SunJCE;

public class TorClient {
    static boolean DEBUG = false; // if false, then automatically sets up and tears down circuit
    private static final int ONION_SERVER_COUNT = 1;
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
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        //do the handshake
        establishSecureConnection(outToServer, inFromServer);

        // manually EXTEND, DATA, TEARDOWN
        if (DEBUG) {
            Debug("DEBUG MODE");

            // get input
            System.out.printf("Send: ");
            String sentence = inFromUser.readLine();

            while (sentence.length() != 0) {
                // write to onion router
                outToServer.writeBytes(sentence + '\n');

                // create read stream and receive from server
                String sentenceFromServer = inFromServer.readLine();
                Debug("Received: " + sentenceFromServer);

                // get more input
                System.out.printf("Send: ");
                sentence = inFromUser.readLine();
            }
        }
        // automatically EXTEND, DATA, TEARDOWN
        else {
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
        }

        // close client socket
        clientSocket.close();
    }

    private static void retrieveURL(DataOutputStream outToServer, BufferedReader inFromServer, String url) throws Exception {
          TorMessage dataMsg = new TorMessage(TorMessage.Type.BEGIN, url);
        outToServer.write(dataMsg.getBytes());

        String msgFromServer = inFromServer.readLine();
        Debug("Received: " + msgFromServer);
    }

    private static void teardownCircuit(DataOutputStream outToServer, BufferedReader inFromServer) throws Exception {
        TorMessage teardownMsg = new TorMessage(TorMessage.Type.TEARDOWN);
        outToServer.write(teardownMsg.getBytes());
    }

    private static void establishSecureConnection(DataOutputStream outBuffer, BufferedReader inBuffer) throws Exception {
        //find out what the public key is
        TorMessage createMsg = new TorMessage(TorMessage.Type.CREATE, encryption.getPublicKey());
        outBuffer.write(createMsg.getBytes());
        Debug("Sent: " + createMsg.getString());

        TorMessage msgFromServer = readMessage(inBuffer);
        Debug("Received: " + msgFromServer.toString());
        remotePublicKeys[0] = msgFromServer.getPublicKey();

        //arrange the aes key
        AES symmetricEncryption = new AES();

        TorMessage aesMsg = new TorMessage(TorMessage.Type.AES_REQUEST,
                encryption.encrypt(new String(symmetricEncryption.createHalf(),"UTF-8"),remotePublicKeys[0]));
        outBuffer.write(aesMsg.getBytes());
        Debug("Sent: " + aesMsg.getString());

        TorMessage aesFromServer = new TorMessage(readMessage(inBuffer));
        Debug("Received: " + aesFromServer.toString());
        secretKeys[0] = new SecretKeySpec(
                encryption.decrypt(aesFromServer.getPayload(), encryption.getPrivateKey()),"SHA-1");
    }

    private static void setupCircuit(DataOutputStream outToServer, BufferedReader inFromServer, List<String> orPath)
            throws Exception {
        for (int i = 1; i < orPath.size(); i++) {
            String orServerName = orPath.get(i).split(" ")[0];
            int orPort = Integer.parseInt(orPath.get(i).split(" ")[1]);

            TorMessage extendMsg = new TorMessage(TorMessage.Type.EXTEND,encryption.getPublicKey(),orServerName,orPort);
            System.out.printf("Sent: " + extendMsg.getString());
            outToServer.write(extendMsg.getBytes());

            String msgFromServer = inFromServer.readLine();
            Debug("Received: " + msgFromServer);
        }
    }

    private static TorMessage readMessage(BufferedReader in) throws Exception{
        int length = Integer.parseInt(in.readLine());
        char[] msg = new char[length];
        in.read(msg, 0, length);
        return (new TorMessage(new String(msg).getBytes("UTF-8"),length));
    }

    private static String filenametoURL(String serverName, int port, String filename) {
        return "http://" + serverName + ":" + Integer.toString(port) + "/" + filename;
    }

    // TODO: pick randomly, currently picks first "length" number of onion routers    
    // takes in list of hostnames and path length
    private static List<String> pickPath(int length, List<String> onionRouters) {
        return onionRouters.subList(0, length);
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
