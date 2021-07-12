import java.util.Objects;

public class ChunkID {
    private String fileID;
    private long chunkNum;

    public ChunkID(String fileID, long chunkNum) {
        this.fileID = fileID;
        this.chunkNum = chunkNum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkID)) return false;
        ChunkID chunkID = (ChunkID) o;
        return getChunkNum() == chunkID.getChunkNum() && getFileID().equals(chunkID.getFileID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileID(), getChunkNum());
    }

    @Override
    public String toString() {
        return getFileID() + ' ' + getChunkNum();
    }

    public String getFileID() {
        return fileID;
    }

    public long getChunkNum() {
        return chunkNum;
    }
}
