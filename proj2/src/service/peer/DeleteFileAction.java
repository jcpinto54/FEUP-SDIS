package service.peer;

import application.Server;
import service.peer.message.DeleteFileMessage;

public class DeleteFileAction extends PeerRequestAction<DeleteFileMessage> {
    public DeleteFileAction(DeleteFileMessage message) {
        super(message);
    }

    @Override
    public void run() {
        Server.getStorage().deleteFile(message.getFileId());
        stop();
    }
}
