import java.nio.file.FileSystemException;

public class ReceivedDeleteThread implements Runnable {
    private Message message;

    public ReceivedDeleteThread(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        try {
            Peer.getInstance().getFileManager().removeBackupFile(message.getFileID());
        } catch (FileSystemException e) {
            System.err.println("DELETE: Problems while removing file! - " + e.getMessage());
        }
    }
}
