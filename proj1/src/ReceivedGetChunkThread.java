import java.io.FileNotFoundException;
import java.util.Random;

public class ReceivedGetChunkThread implements Runnable {
    private Message message;

    public ReceivedGetChunkThread(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        FileManager manager = Peer.getInstance().getFileManager();
        ChunkID chunkID = this.message.getChunkID();

        if (!manager.isChunkStored(chunkID))
        {
            return;
        }

        Chunk chunk = null;
        try {
            chunk = manager.retrieveChunk(chunkID);
        } catch (FileNotFoundException e) {
            System.err.println("GETCHUNK: Problems retrieving chunk! - " + e.getMessage());
        }

        Message chunkMessage = null;

        try {
            chunkMessage = Message.createMessage("CHUNK", chunk);
        } catch (MessageException e) {  // Won't enter this
            System.err.println("GETCHUNK: Problems creating chunk message! - " + e.getMessage());
        }

        Random r = new Random();
        int low = 0;
        int high = 401;
        int delay = r.nextInt(high-low) + low;


        Peer.getInstance().getThreadExecutor().schedule(new SendMessageThread(SendMessageThread.MDRChannel, chunkMessage), delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
