# CPSC 433 Networks Final Project: Tor
Dejan Gvozdenac and Jay Hou

### Introduction
We built a simplication of a Tor network based off the [design](https://svn.torproject.org/svn/projects/design-paper/tor-design.pdf) and [specification](https://gitweb.torproject.org/torspec.git/tree/tor-spec.txt) of the [real Tor project](https://www.torproject.org/).

### Design and Program Flow
1. _Picking the path_:
  The client provides a file of hostnames and ports of running Tor servers, and selects a static number of random servers to construct an anonymized virtual path to the target server.
  
2. _Connection to the onion router network_:
  To establish a secure connection to the first onion router, the client creates a TCP connection to the first onion router, and sends ```CREATE``` message containing a generated public key for its RSA encryption cipher. The server responds its own public key, with which the client can use to encrypt its half of the Diffie-Hellman handshake for the AES symmetric key in an ```AES_REQUEST``` message. The server now creates the symmetric key for the AES encryption and sends this to the client in an ```AES_RESPONSE``` message.

3. _Extending the path_:
  Then, to finish creating the path to the target server, the client sends an ```EXTEND``` message containing the next hop's hostname and port number, along with the client's RSA public key. This message is encrypted multiple times with all the symmetric keys of the established Tor servers, and wrapped in ```RELAY``` messages so that the intermediate servers know to forward the message. At each hop, the Tor server can decrypt its layer of the onion. Once the last hop receives the message, it can create a TCP connection with the next hop. Repeating the previous step, the client and new server can handshake to establish a symmetric AES key.
  
4. _Data exchange_:
  Once the path is established, the client and target server can now communicate anonymously through the network. The client sends URL requests to the server in a ```BEGIN``` message, encrypted multiple times with all the AES keys arranged with the intermediate Tor servers and wrapped in ```RELAY``` messages. At each hop, the Tor server decrypts one layer of the onion. The last Tor server should receive the ```BEGIN``` message, and send the HTTP request on behalf of the client. Upon receival, it sends the HTTP response back in a ```DATA``` message. At each hop, the data is encrypted by the Tor server. When the client receives the message, it can decrypt each layer using the agreed upon AES keys.
  
5. _Teardown_:
  Once the client has finished communicating with the target server, it sends a ```TEARDOWN``` message, which is propagated to the last  Tor server, who can then close the socket to the target server.

### Compilation
To compile, run `make`.

### How to Run
1. Create a list of hostnames and ports for an available onion router. Start the servers with ```java TorServer [port number]```. Onion router list file format example:

  ``` 
  aphid.zoo.cs.yale.edu 8000
  lion.zoo.cs.yale.edu 6789
  tick.zoo.cs.yale.edu 3000
  ```
  
2. Start a web server on a machine that is not listed in the onion router. Example, on Python 2.x:
  ``` python
  python -m SimpleHTTPServer 8000
  ```

3. Start a client on another machine. Example:
  ```
  java TorClient [server addr] [server port] [path to OR list file]
  ```

Project hosted at: https://github.com/dejangvozdenac/Tor
