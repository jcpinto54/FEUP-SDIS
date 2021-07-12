package service.client;

import application.Server;
import service.client.message.BackupMessage;
import service.peer.message.PeerServiceMessage;
import service.peer.message.StoreFileMessage;
import service.peer.message.StoredFileMessage;
import ssl.SSLConnection;
import ssl.data.SSLMessage;
import storage.FileInitiated;

import java.io.IOException;

public class BackupAction extends ClientRequestAction<BackupMessage> {
    private FileInitiated file;
    private StoreFileMessage messageToSend = null;

    public BackupAction(SSLConnection connection, BackupMessage message) {
        super(connection, message);
    }

    public BackupAction(SSLConnection connection, StoreFileMessage message) {
        super(connection, null);
        messageToSend = message;
    }

    @Override
    public void run() {
        System.out.println("Starting Backup");
        Server server = Server.getInstance();

        if (messageToSend == null) {
            file = new FileInitiated(message.getFileName(), message.getReplicationDegree());
            messageToSend = new StoreFileMessage(server.getAddress(), file.getFileId(), file.neededCopies(), file.readOriginalToString());
        } else {
            file = (FileInitiated) Server.getStorage().getFile(messageToSend.getFileId());
        }

        SSLMessage messageToClient = null;

        for (int i = 0; !stop && i < 10; i++) {
            try {
                server.sendToN(messageToSend.getReplicationDegree(), messageToSend);
                Thread.sleep(100);
            } catch (Exception exception) {
                messageToClient = new SSLMessage(exception.getMessage());
                stop();
            }
        }

        if (stop) {
            Server.getStorage().addFile(file);
        } else {
            messageToClient = new SSLMessage("Could not backup file with desired replication degree");
        }

        if (messageToClient == null)
            messageToClient = new SSLMessage("Success");

        try {
            if (connection != null) {
                Server.getInstance().write(connection, messageToClient);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        System.out.println("Backup finished");
    }

    @Override
    public void processMessage(PeerServiceMessage message) {
        if (!(message instanceof StoredFileMessage))
            return;

        StoredFileMessage storedFileMessage = (StoredFileMessage) message;
        if (!storedFileMessage.getFileId().equals(file.getFileId()))
            return;

        file.addCopy(storedFileMessage.getSenderAddress());
        if (file.hasEnoughCopies())
            stop();
    }
}
