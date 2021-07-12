package ssl.data;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class SSLStream {
    private final SSLBuffer applicationBuffer = new SSLBuffer();
    private final SSLBuffer networkBuffer = new SSLBuffer();

    public SSLBuffer getApplicationBuffer() {
        return applicationBuffer;
    }

    public SSLBuffer getNetworkBuffer() {
        return networkBuffer;
    }

    public void setContent(byte[] content) {
        applicationBuffer.setContent(content);
    }

    public byte[] getContent() {
        return applicationBuffer.getContent();
    }

    // -------------------------------------------------------------------------
    // Channel operations
    // -------------------------------------------------------------------------

    public int read(ReadableByteChannel channel) throws IOException {
        return channel.read(networkBuffer.getBuffer());
    }

    public int write(WritableByteChannel channel) throws IOException {
        networkBuffer.flip();
        int bytesWritten = 0;
        while (networkBuffer.hasRemaining())
            bytesWritten += channel.write(networkBuffer.getBuffer());
        return bytesWritten;
    }

    // -------------------------------------------------------------------------
    // SSL Operations
    // -------------------------------------------------------------------------

    public SSLEngineResult wrap(SSLEngine engine) throws SSLException {
        networkBuffer.clearContent();
        return engine.wrap(applicationBuffer.getBuffer(), networkBuffer.getBuffer());
    }

    public SSLEngineResult unwrap(SSLEngine engine) throws SSLException {
        applicationBuffer.clearContent();
        return engine.unwrap(networkBuffer.getBuffer(), applicationBuffer.getBuffer());
    }

    // -------------------------------------------------------------------------
    // Allocation
    // -------------------------------------------------------------------------

    public void allocateApplicationBuffer(int bufferSize) {
        applicationBuffer.allocate(bufferSize);
    }

    public void allocateNetworkBuffer(int bufferSize) {
        networkBuffer.allocate(bufferSize);
    }

    public void enlargeApplicationBuffer(SSLEngine engine) {
        applicationBuffer.enlarge(engine.getSession().getApplicationBufferSize());
    }

    public void enlargeNetworkBuffer(SSLEngine engine) {
        networkBuffer.enlarge(engine.getSession().getPacketBufferSize());
    }

    public void resizeNetworkBuffer(SSLEngine engine) {
        if (engine.getSession().getPacketBufferSize() < networkBuffer.getLimit())
            return;
        networkBuffer.resize(engine.getSession().getPacketBufferSize());
    }
}
