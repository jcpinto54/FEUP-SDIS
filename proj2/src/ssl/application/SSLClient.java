package ssl.application;

import ssl.SSLConnection;
import ssl.SSLOutput;
import ssl.data.SSLMessage;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SSLClient extends SSLPeer {
    private final SSLEngine engine;
    private SSLConnection connection;

    public SSLClient(InetSocketAddress hostAddress) throws Exception {
        super(hostAddress, "client");

        // Engine and buffers
        engine = context.createSSLEngine(hostAddress.getHostName(), hostAddress.getPort());
        engine.setUseClientMode(true);
        allocateBuffers(engine.getSession());
    }

    public boolean connect() throws IOException {
        SSLOutput.print("Establishing connection with server "+address.getHostName()+":"+address.getPort());
        connection = new SSLConnection(mutex, engine, address, executor);
        boolean connected = connection.establish();
        if (connected)
            SSLOutput.print("- Established");
        else
            SSLOutput.print("- Fail");
        return connected;
    }

    public boolean send(SSLMessage message) throws IOException {
        SSLOutput.print("Sending message to server");
        boolean sent = write(connection, message);
        if (sent)
            SSLOutput.print("- Sent \"" + message + "\"");
        else
            SSLOutput.print("- Fail");
        return sent;
    }

    public SSLMessage receive() throws InterruptedException, IOException, ClassNotFoundException {
        SSLOutput.print("Receiving server response");

        SSLMessage message = null;
        for (int i = 0; i < 10; i++) {
            try {
                message = read(connection);
            } catch (IOException exception) {
                SSLOutput.print("- Could not receive message");
                return null;
            }
            if (message == null) {
                SSLOutput.print("- Received null message");
                return null;
            }
            if (!message.isEmpty())
                break;

            // TODO: no busy waiting
            Thread.sleep(100);
        }

        if (message.isEmpty()){
            SSLOutput.print("- Received empty message");
            return null;
        }

        SSLOutput.print("- Received \"" + message + "\"");
        return message;
    }

    public void shutdown() throws IOException {
        SSLOutput.print("Closing connection with server");

        connection.close();
        executor.shutdown();

        SSLOutput.print("- Closed");
    }

    @Override
    public void run() {
        try {
            if (connect())
                if (send(new SSLMessage("Hello, World!")))
                    receive();
            shutdown();
        } catch (IOException | InterruptedException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }

    public static SSLClient newInstance(InetSocketAddress hostAddress) throws Exception {
        return new SSLClient(hostAddress);
    }
}