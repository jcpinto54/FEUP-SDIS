package ssl.data;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SSLBuffer {
    private ByteBuffer buffer;

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    ByteBuffer getBuffer() {
        return buffer;
    }

    public int getLimit() {
        return buffer.limit();
    }

    public void flip() {
        buffer.flip();
    }

    // -------------------------------------------------------------------------
    // Content functions
    // -------------------------------------------------------------------------

    public byte[] getContent() {
        return Arrays.copyOfRange(buffer.array(), 0, buffer.position());
    }

    public void clearContent() {
        buffer.clear();
    }

    public void setContent(byte[] content) {
        clearContent();
        buffer.put(content);
        buffer.flip();
    }

    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    public void compact() {
        buffer.compact();
    }

    // -------------------------------------------------------------------------
    // Allocation functions
    // -------------------------------------------------------------------------

    public void allocate(int size) {
        buffer = createBuffer(size);
    }

    public void enlarge(int size) {
        buffer = createLargerBuffer(size);
    }

    public void resize(int size) {
        ByteBuffer newBuffer = createLargerBuffer(size);
        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;
    }

    // -------------------------------------------------------------------------
    // Buffer creation
    // -------------------------------------------------------------------------

    private static ByteBuffer createBuffer(int size) {
        return ByteBuffer.allocate(size);
    }

    private ByteBuffer createLargerBuffer(int size) {
        if (size > buffer.capacity())
            return createBuffer(size);
        else
            return createBuffer(buffer.capacity() * 2);
    }
}
