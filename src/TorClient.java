/**
 * TorClient
 * Created by dejan on 5/6/16.
 * Usage: java TorClient [server addr] [server port]
 */

import java.io.*;
import java.net.*;
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
    static boolean DEBUG = true; // if false, then automatically sets up and tears down circuit

    public static void main(String args[]) throws Exception {
        if (args.length != 3) {
            Debug("Usage: java TCPClient [server addr] [server port] [OR file]");
            System.exit(1);
        }

        String serverName = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String orFilename = args[2];

        List<String> onionRouters = readRouters(orFilename);
        List<String> path = pickPath(2, onionRouters); // TODO: randomize number of ORs

        // create socket to first onion router
        String orServerName = path.get(0).split(" ")[0];
        InetAddress orServerIPAddress = InetAddress.getByName(orServerName);
        int orPort = Integer.parseInt(path.get(0).split(" ")[1]);
        Socket clientSocket = new Socket(orServerName, orPort);

        // get input from keyboard
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

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
                retreiveURL(outToServer, inFromServer, url);

                // get more input
                System.out.printf("file to retrieve: ");
                filename = inFromUser.readLine();
            }

            teardownCircuit(outToServer, inFromServer);
        }

        // close client socket
        clientSocket.close();
    }

    public static void retreiveURL(DataOutputStream outToServer, BufferedReader inFromServer, String url) throws Exception {
        // String msg = "DATA`" + url + "\n";
        // System.out.printf("Sent: " + msg);
        // outToServer.writeBytes(msg);

        TorMessage dataMsg = new TorMessage(TorMessage.Type.BEGIN, url + "\n");
        outToServer.write(dataMsg.getBytes());

        String msgFromServer = inFromServer.readLine();
        Debug("Received: " + msgFromServer);
    }

    public static void teardownCircuit(DataOutputStream outToServer, BufferedReader inFromServer) throws Exception {
        TorMessage teardownMsg = new TorMessage(TorMessage.Type.TEARDOWN, "\n");
        outToServer.write(teardownMsg.getBytes());
    }

    public static void setupCircuit(DataOutputStream outToServer, BufferedReader inFromServer, List<String> orPath) throws Exception {
        for (int i = 1; i < orPath.size(); i++) {
            String orServerName = orPath.get(i).split(" ")[0];
            int orPort = Integer.parseInt(orPath.get(i).split(" ")[1]);

            // String extendMsg = "EXTEND`" + orServerName + "`" + orPort + "\n";
            // System.out.printf("Sent: " + extendMsg);
            TorMessage extendMsg = new TorMessage(TorMessage.Type.EXTEND, orServerName + "`" + orPort + "\n");
            System.out.printf("Sent: " + extendMsg.getString());
            outToServer.write(extendMsg.getBytes());

            String msgFromServer = inFromServer.readLine();
            Debug("Received: " + msgFromServer);
        }
    }

    public static String filenametoURL(String serverName, int port, String filename) {
        return "http://" + serverName + ":" + Integer.toString(port) + "/" + filename;
    }

    // TODO: pick randomly, currently picks first "length" number of onion routers    
    // takes in list of hostnames and path length
    public static List<String> pickPath(int length, List<String> onionRouters) {
        return onionRouters.subList(0, length);
    }

    public static List<String> readRouters(String filename) {
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

    public static void Debug(String s){
        System.out.println(s);
    }
}
