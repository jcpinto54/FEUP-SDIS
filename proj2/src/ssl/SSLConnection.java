package ssl;

import ssl.data.SSLStream;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.FINISHED;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

public class SSLConnection {
    private final Object mutex;
    private final SSLStream outgoingStream = new SSLStream();
    private final SSLStream incomingStream = new SSLStream();
    private final SSLEngine engine;
    private final SocketChannel channel;
    private final Executor executor;
    private SSLEngineResult.HandshakeStatus handshakeStatus;

    public SSLConnection(Object mutex, SSLEngine engine, SocketChannel channel, Executor executor) {
        allocateBuffers(engine.getSession());
        this.engine = engine;
        this.channel = channel;
        this.executor = executor;
        this.mutex = mutex;
    }

    public SSLConnection(Object mutex, SelectionKey key, Executor executor) {
        this(mutex, (SSLEngine) key.attachment(), (SocketChannel) key.channel(), executor);
    }

    public SSLConnection(Object mutex, SSLEngine engine, InetSocketAddress address, Executor executor) throws IOException {
        this(mutex, engine, createChannel(address), executor);
    }

    public SSLEngine getEngine() {
        return engine;
    }

    public boolean establish() throws IOException {
        engine.beginHandshake();
        return handshake();
    }

    public void close() throws IOException {
        engine.closeOutbound();
        // allocateBuffers();
        // handshake();
        channel.close();
    }

    private boolean handshake() throws IOException {
        allocateBuffers();
        do {
            updateStatus();
            boolean success = switch (handshakeStatus) {
                case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> receiveHandshakeData();
                case NEED_WRAP -> sendHandshakeData();
                case NEED_TASK -> delegateHandshakeTasks();
                default -> true;
            };
            if (!success)
                return false;
        } while (!hasFinished());

        return true;
    }

    // -------------------------------------------------------------------------
    // Buffers functions
    // -------------------------------------------------------------------------

    private void allocateBuffers(SSLSession session) {
        outgoingStream.allocateApplicationBuffer(session.getApplicationBufferSize());
        incomingStream.allocateApplicationBuffer(session.getApplicationBufferSize());
        outgoingStream.allocateNetworkBuffer(session.getPacketBufferSize());
        incomingStream.allocateNetworkBuffer(session.getPacketBufferSize());
    }

    // -------------------------------------------------------------------------
    // Status functions
    // -------------------------------------------------------------------------

    private void updateStatus() {
        handshakeStatus = engine.getHandshakeStatus();
    }

    private boolean hasFinished() {
        return handshakeStatus == FINISHED || handshakeStatus == NOT_HANDSHAKING;
    }

    // -------------------------------------------------------------------------
    // Handshake steps
    // -------------------------------------------------------------------------

    private void allocateBuffers() {
        int appBufferSize = engine.getSession().getApplicationBufferSize();
        outgoingStream.allocateApplicationBuffer(appBufferSize);
        incomingStream.allocateApplicationBuffer(appBufferSize);
        outgoingStream.getNetworkBuffer().clearContent();
        incomingStream.getNetworkBuffer().clearContent();
    }

    // -------------------------------------------------------------------------
    // Handshake handlers
    // -------------------------------------------------------------------------

    private boolean receiveHandshakeData() throws IOException {
        SSLEngineResult result;
        synchronized (mutex) {
            if (incomingStream.read(channel) < 0) {
                if (engine.isInboundDone() && engine.isOutboundDone()) {
                    return false;
                }
                try {
                    engine.closeInbound();
                } catch (SSLException e) {
                    SSLOutput.warn("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
                }
                engine.closeOutbound();
                // After closeOutbound the engine will be set to WRAP state, in order to try to send a close message to the client.
                return true;
            }
            incomingStream.getNetworkBuffer().flip();
            try {
                result = incomingStream.unwrap(engine);
                incomingStream.getNetworkBuffer().compact();
            } catch (SSLException sslException) {
                SSLOutput.warn("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
                engine.closeOutbound();
                return true;
            }
            switch (result.getStatus()) {
                case OK:
                    break;
                case BUFFER_OVERFLOW:
                    // Will occur when peerAppData's capacity is smaller than the data derived from peerNetData's unwrap.
                    incomingStream.enlargeApplicationBuffer(engine);
                    break;
                case BUFFER_UNDERFLOW:
                    // Will occur either when no data was read from the peer or when the peerNetData buffer was too small to hold all peer's data.
                    incomingStream.resizeNetworkBuffer(engine);
                    break;
                case CLOSED:
                    if (engine.isOutboundDone()) {
                        return false;
                    } else {
                        engine.closeOutbound();
                        break;
                    }
            }
        }
        return true;
    }

    private boolean sendHandshakeData() throws IOException {
        SSLEngineResult result;
        outgoingStream.getNetworkBuffer().clearContent();
        synchronized (mutex) {
            try {
                result = outgoingStream.wrap(engine);
            } catch (SSLException sslException) {
                SSLOutput.warn("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
                engine.closeOutbound();
                return true;
            }
            switch (result.getStatus()) {
                case OK:
                    outgoingStream.write(channel);
                    break;
                case BUFFER_OVERFLOW:
                    // Will occur if there is not enough space in myNetData buffer to write all the data that would be generated by the method wrap.
                    // Since myNetData is set to session's packet size we should not get to this point because SSLEngine is supposed
                    // to produce messages smaller or equal to that, but a general handling would be the following:
                    outgoingStream.enlargeNetworkBuffer(engine);
                    break;
                case BUFFER_UNDERFLOW:
                    throw new SSLException("Buffer underflow occurred after a wrap. I don't think we should ever get here.");
                case CLOSED:
                    try {
                        outgoingStream.write(channel);
                        // At this point the handshake status will probably be NEED_UNWRAP so we make sure that peerNetData is clear to read.
                        incomingStream.getNetworkBuffer().clearContent();
                    } catch (Exception e) {
                        SSLOutput.warn("Failed to send server's CLOSE message due to socket channel's failure.");
                    }
                    break;
            }
        }
        return true;
    }

    private boolean delegateHandshakeTasks() {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null)
            executor.execute(task);
        return true;
    }

    // -------------------------------------------------------------------------
    // Channel functions
    // -------------------------------------------------------------------------

    public SocketChannel getChannel() {
        return channel;
    }

    private static SocketChannel createChannel(InetSocketAddress address) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(address);
        while (!socketChannel.finishConnect()) {
            // TODO: no busy waiting
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return socketChannel;
    }
}
