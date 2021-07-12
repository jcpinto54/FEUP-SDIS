import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SendMessageThread implements Runnable{
    public static int MCChannel = 0;
    public static int MDBChannel = 1;
    public static int MDRChannel = 2;

    private final int type;
    private final Message message;

    public SendMessageThread(int type, Message message)
    {
        this.type = type;
        this.message = message;
    }

    @Override
    public void run() {
        if ("CHUNK".equals(message.getMessageType())) {
            ScheduledFuture<?> cleanupThread = Peer.getInstance().getCleanupChunkThreads().getOrDefault(this.message.getChunkID(), null);
            if (cleanupThread == null) {
                ScheduledFuture<?> scheduledCleanup = Peer.getInstance().getThreadExecutor().schedule(new PeerCleanupThread(this.message.getChunkID(), PeerCleanupThread.cleanUpChunksReceived), 1000, TimeUnit.MILLISECONDS);
                Peer.getInstance().getCleanupChunkThreads().put(this.message.getChunkID(), scheduledCleanup);
            }
            if (Peer.getInstance().getChunksReceived().getOrDefault(this.message.getChunkID(), false)) {
                return;
            }
        }
        else if ("PUTCHUNK".equals(message.getMessageType())) {
            ScheduledFuture<?> cleanupThread = Peer.getInstance().getCleanupPutChunkThreads().getOrDefault(this.message.getChunkID(), null);
            if (cleanupThread == null) {
                ScheduledFuture<?> scheduledCleanup = Peer.getInstance().getThreadExecutor().schedule(new PeerCleanupThread(this.message.getChunkID(), PeerCleanupThread.cleanUpPutChunksReceived), 1000, TimeUnit.MILLISECONDS);
                Peer.getInstance().getCleanupPutChunkThreads().put(this.message.getChunkID(), scheduledCleanup);
            }
            if (Peer.getInstance().getPutChunksReceived().getOrDefault(this.message.getChunkID(), false)) {
                return;
            }
        }
        else if (Peer.getInstance().getVersion().equals("2.0")) {
            if ("DEGREES".equals(message.getMessageType())) {
                ScheduledFuture<?> cleanupThread = Peer.getInstance().getCleanupDegreesThreads().getOrDefault(this.message.getBootedID(), null);
                if (cleanupThread == null) {
                    ScheduledFuture<?> scheduledCleanup = Peer.getInstance().getThreadExecutor().schedule(new PeerCleanupThread(this.message.getBootedID()), 1000, TimeUnit.MILLISECONDS);
                    Peer.getInstance().getCleanupDegreesThreads().put(this.message.getBootedID(), scheduledCleanup);
                }
                if (Peer.getInstance().getDegreesReceived().getOrDefault(message.getBootedID(), false)) {
                    return;
                }
            }
        }

        switch (this.type)
        {
            case 0:
                Peer.getInstance().getMcChannel().sendMessage(this.message);
                break;

            case 1:
                Peer.getInstance().getMdbChannel().sendMessage(this.message);
                break;

            case 2:
                Peer.getInstance().getMdrChannel().sendMessage(this.message);
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SendMessageThread that = (SendMessageThread) o;
        return Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }
}
