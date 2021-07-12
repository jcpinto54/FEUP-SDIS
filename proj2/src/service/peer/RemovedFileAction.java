package service.peer;

import application.Server;
import service.client.BackupAction;
import service.peer.message.RemovedFileMessage;
import service.peer.message.StoreFileMessage;
import storage.File;
import storage.FileInitiated;

public class RemovedFileAction extends PeerRequestAction<RemovedFileMessage> {
    public RemovedFileAction(RemovedFileMessage message) {
        super(message);
    }

    @Override
    public void run() {
        File file = Server.getStorage().getFile(message.getFileId());
        if (!(file instanceof FileInitiated))
            return;

        FileInitiated fileInitiated = (FileInitiated) file;
        fileInitiated.removeCopy(message.getSenderAddress());

        int neededCopies = fileInitiated.neededCopies();
        if (neededCopies == 0)
            return;

        Server server = Server.getInstance();

        StoreFileMessage messageToSend = new StoreFileMessage(server.getAddress(), fileInitiated.getFileId(), fileInitiated.getReplicationDegree(), fileInitiated.readOriginalToString());
        Server.execute(new BackupAction(null, messageToSend));
        stop();
    }
}
