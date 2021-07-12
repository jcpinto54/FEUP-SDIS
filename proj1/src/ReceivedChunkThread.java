import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ReceivedChunkThread implements Runnable {
    private Message message;

    public ReceivedChunkThread(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        Peer.getInstance().getChunksReceived().put(message.getChunkID(), true);
        Boolean removed = Peer.getInstance().getChunksAskedFor().remove(message.getChunkID());
        if (removed == null) return;
        if (removed) {
            try {
                Peer.getInstance().getFileManager().restoreChunkTemp(this.message);

                if (this.message.getData().length == 64000) {
                    ChunkID nextChunkID = new ChunkID(this.message.getFileID(), this.message.getChunkNum() + 1);
                    Message message = null;
                    try {
                        message = Message.createMessage("GETCHUNK", nextChunkID);
                    } catch (MessageException e) {
                        System.err.println("CHUNK: Error while creating GETCHUNK message! - " + e.getMessage());
                    }

                    Peer.getInstance().getChunksAskedFor().put(nextChunkID, true);

                    Random r = new Random();
                    int low = 0;
                    int high = 401;
                    int delay = r.nextInt(high-low) + low;

                    Peer.getInstance().getThreadExecutor().schedule(new SendMessageThread(SendMessageThread.MCChannel, message), delay, TimeUnit.MILLISECONDS);
                } else {
                    Peer.getInstance().getFileManager().restoreFile(this.message.getFileID());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
