public class ReceivedStoredThread implements Runnable {
    private Message message;

    public ReceivedStoredThread(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        FileManager manager = Peer.getInstance().getFileManager();
        ChunkID chunkID = this.message.getChunkID();

        manager.incrementReplicationDegree(chunkID);
    }
}
