package service.peer;

import application.Server;
import service.peer.message.StoreFileMessage;
import service.peer.message.StoredFileMessage;
import storage.File;

import java.io.IOException;

public class StoreFileAction extends PeerRequestAction<StoreFileMessage> {
    public StoreFileAction(StoreFileMessage message) {
        super(message);
    }

    @Override
    public void run() {
        File file = new File(message.getFileId(), message.getSenderAddress());

        if (message.getFileBody().length() > Server.getStorage().getRemainingSpace()) {
            stop();
            return;
        }

        try {
            Server.getStorage().storeFile(file, message.getFileBody());
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        Server server = Server.getInstance();
        try {
            server.send(message.getSenderAddress(), new StoredFileMessage(server.getAddress(), message.getFileId()));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        stop();
    }
}
