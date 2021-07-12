import java.io.Serializable;

public class PeerCleanupThread implements Runnable {
    public static int cleanUpPutChunksReceived = 0;
    public static int cleanUpChunksReceived = 1;

    private ChunkID chunkID;
    private int type;

    // Version 2.0
    private int senderID;

    public PeerCleanupThread(ChunkID chunkID, int type) {
        this.type = type;
        this.chunkID = chunkID;
        this.senderID = -1;
    }

    // Version 2.0
    public PeerCleanupThread(int senderID) {
        this.senderID = senderID;
        this.type = 2;
        this.chunkID = null;
    }

    @Override
    public void run() {
        switch (this.type) {
            case 0:
                Peer.getInstance().getPutChunksReceived().remove(this.chunkID);
                Peer.getInstance().getCleanupPutChunkThreads().remove(this.chunkID);
                break;
            case 1:
                Peer.getInstance().getChunksReceived().remove(this.chunkID);
                Peer.getInstance().getCleanupChunkThreads().remove(this.chunkID);
                break;
            case 2:
                Peer.getInstance().getDegreesReceived().remove(this.senderID);
                Peer.getInstance().getCleanupDegreesThreads().remove(this.senderID);
                break;
        }
    }
}
