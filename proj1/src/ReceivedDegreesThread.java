import java.io.*;

// Version 2.0
public class ReceivedDegreesThread implements Runnable {
    private final Message message;

    public ReceivedDegreesThread(Message message) {
        this.message = message;
    }
    @Override
    public void run() {
        int bootedID = message.getBootedID();
        Peer.getInstance().getDegreesReceived().put(bootedID, true);

        if (bootedID == Peer.getInstance().getId()) {
            ByteArrayInputStream stream = new ByteArrayInputStream(this.message.getData());
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader input = new BufferedReader(reader);

            try {
                String line;

                while ((line = input.readLine()) != null) {
                    String[] splitLine = line.split(" ");

                    ChunkID chunkID = new ChunkID(splitLine[0], Integer.parseInt(splitLine[1]));
                    int actualReplicationDegree = Integer.parseInt(splitLine[3]);
                    int desiredReplicationDegree = Integer.parseInt(splitLine[5]);

                    if (desiredReplicationDegree == 0) {
                        Peer.getInstance().getFileManager().removeChunk(chunkID);
                    }

                    Peer.getInstance().getFileManager().setActualReplicationDegree(chunkID, actualReplicationDegree);
                    Peer.getInstance().getFileManager().setDesiredReplicationDegree(chunkID, desiredReplicationDegree);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            Peer.getInstance().setReadyForDegreesTrue();
        }
    }
}
