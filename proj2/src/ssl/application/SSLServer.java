package ssl.application;

import ssl.SSLConnection;
import ssl.SSLOutput;
import ssl.data.SSLMessage;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class SSLServer extends SSLPeer {
    protected boolean active;
    protected final Selector selector;


    public SSLServer(InetSocketAddress hostAddress, Selector selector) throws Exception {
        super(hostAddress, "server");

        // Buffers
        SSLSession dummySession = context.createSSLEngine().getSession();
        allocateBuffers(dummySession);
        dummySession.invalidate();

        // Selector
        this.selector = selector;
    }

    protected void start() throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        active = true;
    }

    @Override
    public void run() {
        SSLOutput.print("Starting server");
        try {
            start();
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }
        SSLOutput.print("- Started");
        while (active) {
            try {
                selector.select();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    try {
                        accept(key);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                } else if (key.isReadable()) {
                    SSLConnection connection = new SSLConnection(mutex, key, executor);
                    try {
                        SSLMessage message = receive(connection);
                        if (message != null)
                            respond(message, connection);
                    } catch (IOException | IllegalAccessException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }

        SSLOutput.print("- Stopped");
    }

    public void stop() throws Exception {
        SSLOutput.print("Stopping server");
        active = false;
        executor.shutdown();
        selector.wakeup();
    }

    protected void accept(SelectionKey key) throws IOException {
        SSLOutput.print("Accepting client request");

        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);
        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);

        SSLOutput.print("- Created socket channel");

        SSLConnection connection = new SSLConnection(mutex, engine, socketChannel, executor);
        if (connection.establish()) {
            SSLOutput.print("- Handshake complete");
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
            SSLOutput.print("- Accepted");
        } else {
            socketChannel.close();
            SSLOutput.print("- Handshake failure");
        }
    }

    protected SSLMessage receive(SSLConnection connection) throws IOException {
        SSLOutput.print("Receiving client message");
        SSLMessage message;
        try {
            message = read(connection);
        } catch (IOException exception) {
            SSLOutput.print("- Received an incomprehensible message");
            SSLOutput.print(exception.toString());
            return null;
        }
        if (message == null)
            SSLOutput.print("- Received nothing");
        else
            SSLOutput.print("- Received \"" + message + "\"");
        return message;
    }

    public void respond(SSLMessage message, SSLConnection connection) throws IOException, IllegalAccessException {
        SSLMessage responseMessage = new SSLMessage("I'm not the world, I'm a server!");
        SSLOutput.print("Sending response to client ");
        if (write(connection, responseMessage))
            SSLOutput.print("- Sent \"" + responseMessage + "\"");
        else
            SSLOutput.print("- Fail");
    }

    public boolean isActive() {
        return active;
    }
}