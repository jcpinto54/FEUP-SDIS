package ssl.thread;

import ssl.SSLConnection;
import ssl.application.SSLPeer;
import ssl.data.SSLMessage;

public class SSLReceiver implements Runnable {
    private boolean received = false;
    private SSLMessage message;
    private final SSLPeer peer;
    private final SSLConnection connection;

    public SSLReceiver(SSLPeer peer, SSLConnection connection) {
        this.peer = peer;
        this.connection = connection;
    }

    public boolean hasReceived() {
        return received;
    }

    public SSLMessage getMessage() {
        return message;
    }

    @Override
    public void run() {
        try {
            message = peer.read(connection);
            if (!message.isEmpty())
                received = true;
        } catch (Exception e) {
            received = true;
        }
    }
}
