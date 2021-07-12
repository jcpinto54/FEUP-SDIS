package ssl.socket;

import ssl.data.SSLMessage;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;

public class SSLSocketServer implements Runnable {
    private InetAddress ip;
    private int port;
    private final SSLServerSocket sslServerSocket;
    private SSLSocket connection;

    public SSLSocketServer(int port) throws IOException {
        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.port = port;
        sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);
    }

    public SSLSocket accept() throws IOException {
        connection = (SSLSocket) sslServerSocket.accept();
        return connection;
    }

    public SSLMessage receive() throws IOException {
        if (connection.isInputShutdown() || connection.isOutputShutdown())
            return null;

        byte[] buffer = new byte[256];
        connection.getInputStream().read(buffer);
        return new SSLMessage(buffer);
    }

    protected boolean write(SSLMessage message) throws IOException {
        connection.getOutputStream().write(message.getBytes());
        return true;
    }

    public void run() {
        try {
            while (true) {
                SSLSocket socket = accept();
                System.out.println("New connection on port " + socket.getPort());
                while (!socket.isClosed()) {
                    SSLMessage received = receive();
                    if (received == null)
                        break;
                    if (!received.isEmpty()) {
                        System.out.println("Received " + received);
                        write(new SSLMessage("Hello o caralho"));
                    }
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    protected void start() throws Exception {

    }

    public void stop() throws Exception {

    }
}