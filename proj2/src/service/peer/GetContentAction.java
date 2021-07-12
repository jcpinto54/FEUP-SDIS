package service.peer;

import application.Server;
import service.peer.message.ContentMessage;
import service.peer.message.GetContentMessage;
import storage.File;

public class GetContentAction extends PeerRequestAction<GetContentMessage> {
    public GetContentAction(GetContentMessage message) {
        super(message);
    }

    @Override
    public void run() {
        File file = Server.getStorage().getFile(message.getFileId());
        Server server = Server.getInstance();
        try {
            String content = file.readContent();
            server.send(message.getSenderAddress(), new ContentMessage(server.getAddress(), message.getFileId(), content));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        stop();
    }
}
