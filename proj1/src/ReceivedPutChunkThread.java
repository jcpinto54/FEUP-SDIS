import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ReceivedPutChunkThread implements Runnable {
    private Chunk chunk;

    public ReceivedPutChunkThread(Message message) {
        this.chunk = new Chunk(message.getReplicationDegree(), message.getChunkID(), message.getData());
    }

    @Override
    public void run() {
        Peer.getInstance().getPutChunksReceived().put(chunk.getId(), true);
        ArrayList<ChunkID> removedChunks = Peer.getInstance().getFileManager().storeChunk(chunk, Peer.getInstance().getVersion());

        if (removedChunks == null) {
            return;
        }

        Peer.getInstance().sendRemovedChunks(removedChunks);

        Message confirmation = null;

        try {
            confirmation = Message.createMessage("STORED", chunk.getId());
        } catch (MessageException e) {  // Won't enter this
            e.printStackTrace();
        }

        Peer.getInstance().getThreadExecutor().execute(new SendMessageThread(SendMessageThread.MCChannel, confirmation));
    }
}
