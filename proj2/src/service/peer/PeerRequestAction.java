package service.peer;

import service.Service;
import service.peer.message.*;

public abstract class PeerRequestAction<M extends PeerServiceMessage> extends Service {
    protected final M message;

    protected PeerRequestAction(M message) {
        this.message = message;
    }

    public static PeerRequestAction<?> fromMessage(PeerServiceMessage message) {
        if (message instanceof DeleteFileMessage)
            return new DeleteFileAction((DeleteFileMessage) message);
        if (message instanceof GetContentMessage)
            return new GetContentAction((GetContentMessage) message);
        if (message instanceof RemovedFileMessage)
            return new RemovedFileAction((RemovedFileMessage) message);
        if (message instanceof StoreFileMessage)
            return new StoreFileAction((StoreFileMessage) message);
        return null;
    }
}
