import java.util.Objects;

public class Chunk {
    private int desiredReplicationDegree;
    private ChunkID id;
    private byte[] data;

    public Chunk(int desiredReplicationDegree, ChunkID id, byte[] data) {
        this.desiredReplicationDegree = desiredReplicationDegree;
        this.id = id;
        this.data = data;
    }

    public int getDesiredReplicationDegree() {
        return desiredReplicationDegree;
    }

    public ChunkID getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chunk)) return false;
        Chunk chunk = (Chunk) o;
        return Objects.equals(id, chunk.id);
    }
}

