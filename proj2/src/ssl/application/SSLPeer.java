package ssl.application;

import ssl.SSLConnection;
import ssl.SSLKeyManager;
import ssl.SSLOutput;
import ssl.data.SSLMessage;
import ssl.data.SSLStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.*;

public abstract class SSLPeer implements Runnable {
    protected final InetSocketAddress address;
    protected final SSLContext context;
    protected final Object mutex = new Object();
    private final SSLStream incomingStream = new SSLStream();
    private final SSLStream outgoingStream = new SSLStream();
    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(10);
    protected ExecutorService executor = new ThreadPoolExecutor(10, 10, 1, TimeUnit.MINUTES, queue);

    protected SSLPeer(InetSocketAddress address, String keyFile) throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        this.address = address;
        this.context = SSLKeyManager.createSecureContext(keyFile, "trust");
    }

    protected void allocateBuffers(SSLSession session) {
        outgoingStream.allocateApplicationBuffer(session.getApplicationBufferSize());
        incomingStream.allocateApplicationBuffer(session.getApplicationBufferSize());
        outgoingStream.allocateNetworkBuffer(session.getPacketBufferSize());
        incomingStream.allocateNetworkBuffer(session.getPacketBufferSize());
    }

    public final SSLMessage read(SSLConnection connection) throws IOException {
        synchronized (mutex) {
        SSLMessage content = new SSLMessage();
        incomingStream.getNetworkBuffer().clearContent();
            int bytesRead = incomingStream.read(connection.getChannel());
            if (bytesRead < 0) {
                handleEndOfStream(connection);
                return null;
            } else if (bytesRead == 0) {
                return content;
            }

            incomingStream.getNetworkBuffer().flip();
            while (incomingStream.getNetworkBuffer().hasRemaining()) {
                SSLEngineResult result = incomingStream.unwrap(connection.getEngine());
                switch (result.getStatus()) {
                    case OK -> content.append(incomingStream.getContent());
                    case BUFFER_OVERFLOW -> incomingStream.enlargeApplicationBuffer(connection.getEngine());
                    case BUFFER_UNDERFLOW -> incomingStream.resizeNetworkBuffer(connection.getEngine());
                    case CLOSED -> connection.close();
                }
                if (result.getStatus() == SSLEngineResult.Status.CLOSED)
                    return null;
            }
            return content;
        }
    }

    public final boolean write(SSLConnection connection, SSLMessage message) throws IOException {
        boolean success = false;
        synchronized (mutex) {
            outgoingStream.setContent(message.getBytes());
            while (outgoingStream.getApplicationBuffer().hasRemaining()) {
                // The loop has a meaning for (outgoing) messages larger than 16KB.
                // Every wrap call will remove 16KB from the original message and send it to the remote peer.

                SSLEngineResult result = outgoingStream.wrap(connection.getEngine());
                switch (result.getStatus()) {
                    case OK -> {
                        outgoingStream.write(connection.getChannel());
                        success = true;
                    }
                    case BUFFER_OVERFLOW -> outgoingStream.enlargeNetworkBuffer(connection.getEngine());
                    case BUFFER_UNDERFLOW -> throw new SSLException("Buffer underflow occurred after a wrap. I don't think we should ever get here.");
                    case CLOSED -> {
                        connection.close();
                        return success;
                    }
                }
            }
        }
        return success;
    }

    private void handleEndOfStream(SSLConnection connection) throws IOException {
        try {
            connection.getEngine().closeInbound();
        } catch (Exception e) {
            SSLOutput.warn("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
        }
        connection.close();
    }
}