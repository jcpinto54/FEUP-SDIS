import java.io.FileNotFoundException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ReceivedRemovedThread implements Runnable {
    private Message message;

    public ReceivedRemovedThread(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        FileManager manager = Peer.getInstance().getFileManager();
        ChunkID chunkID = this.message.getChunkID();

        manager.decrementReplicationDegree(chunkID);
        if (manager.getActualReplicationDegree(chunkID) < manager.getDesiredReplicationDegree(chunkID)) {
            Chunk toSend = null;
            Message putChunkMessage = null;
            try {
                toSend = manager.retrieveChunk(chunkID);
                putChunkMessage = Message.createMessage("PUTCHUNK", toSend);
            } catch (MessageException e) {
                System.err.println("REMOVED: Problems creating PUTCHUNK message! - " + e.getMessage());
            } catch (FileNotFoundException e) {
                System.err.println("REMOVED: Problems retrieving chunk from file system! - " + e.getMessage());
            }

            Random r = new Random();
            int low = 0;
            int high = 401;
            int delay = r.nextInt(high-low) + low;

            Peer.getInstance().getThreadExecutor().schedule(new SendMessageThread(SendMessageThread.MDBChannel, putChunkMessage), delay, TimeUnit.MILLISECONDS);
        }
    }
}
