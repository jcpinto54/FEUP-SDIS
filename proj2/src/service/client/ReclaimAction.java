package service.client;

import application.Server;
import service.client.message.ReclaimMessage;
import service.peer.message.RemovedFileMessage;
import ssl.SSLConnection;
import ssl.SSLOutput;
import ssl.data.SSLMessage;
import storage.File;
import storage.Storage;

import java.io.IOException;

public class ReclaimAction extends ClientRequestAction<ReclaimMessage> {
    protected ReclaimAction(SSLConnection connection, ReclaimMessage message) {
        super(connection, message);
    }

    @Override
    public void run() {
        Server server = Server.getInstance();
        long maxDiskSpace = message.getMaximumSpace();
        Storage storage = Server.getStorage();

        while (storage.getStoredSize() > maxDiskSpace) {
            File largestFile = null;
            int maxSize = 0;
            for (File file : storage.getFiles().values()) {
                if (file.getSize() > maxSize) {
                    maxSize = file.getSize();
                    largestFile = file;
                }
            }
            if (largestFile != null) {
                largestFile.delete();
                storage.getFiles().remove(largestFile.getFileId());

                RemovedFileMessage removedFileMessage = new RemovedFileMessage(server.getAddress(), largestFile.getFileId());
                try {
                    server.send(largestFile.getInitiatorPeer(), removedFileMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Server.getStorage().setMaxDiskSpace(maxDiskSpace);

        stop();

        SSLMessage messageToClient = new SSLMessage("Success");

        try {
            Server.getInstance().write(connection, messageToClient);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
