/**
 * Created by dejan on 5/6/16.
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class TorServer {
    public static void main(String args[]) throws Exception  {
        int torPort=Integer.parseInt(args[0]);
        // create server socket
        ServerSocket listenSocket = new ServerSocket(torPort);
        while (true) {
            try {
                // take a ready connection from the accepted queue
                Socket connectionSocket = listenSocket.accept();
                Debug("\nRequest from " + connectionSocket);
                TorMessageHandler tmh = new TorMessageHandler(
                        connectionSocket);
                tmh.processRequest();
                Debug("Done");
            } catch (Exception e){}
        }
    }



    public static void Debug(String s){
        System.out.println(s);
    }
}
