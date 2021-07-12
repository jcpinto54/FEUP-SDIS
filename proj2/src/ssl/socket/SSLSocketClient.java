package ssl.socket;

import ssl.application.SSLClient;
import ssl.data.SSLMessage;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SSLSocketClient extends SSLClient {
    private final SSLSocket sslSocket;
    private final OutputStream out;
    private final InputStream in;

    public SSLSocketClient(InetAddress ip, int port) throws Exception {
        super(new InetSocketAddress(ip, port));
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        this.sslSocket = (SSLSocket) sslSocketFactory.createSocket(ip, port);

        this.out = sslSocket.getOutputStream();
        this.in = sslSocket.getInputStream();
    }

    @Override
    public boolean connect() {
        return true;
    }

    @Override
    public boolean send(SSLMessage message) throws IOException {
        out.write(message.getBytes());
        return true;
    }

    @Override
    public SSLMessage receive() throws IOException {
        byte[] bytes = new byte[256];
        in.read(bytes);
        return new SSLMessage(bytes);
    }

    @Override
    public void shutdown() throws IOException {
        this.sslSocket.close();
    }

    @Override
    public void run() {
        for (int i = 0; i < 3; i++) {
            try {
                send(new SSLMessage("Hello " + i));
                SSLMessage received = receive();
                System.out.println("Received " + received);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        try {
            shutdown();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static SSLClient newInstance(InetSocketAddress hostAddress) throws Exception {
        return new SSLSocketClient(hostAddress.getAddress(), hostAddress.getPort());
    }
}