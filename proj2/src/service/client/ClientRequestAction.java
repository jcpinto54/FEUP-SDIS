package service.client;

import service.Service;
import service.client.message.*;
import ssl.SSLConnection;

import java.io.IOException;
import java.net.InetAddress;

public abstract class ClientRequestAction<M extends ClientServiceMessage> extends Service {
    protected final M message;
    protected final SSLConnection connection;

    protected ClientRequestAction(SSLConnection connection, M message) {
        this.message = message;
        this.connection = connection;
    }

    public static ClientRequestAction<?> fromMessage(SSLConnection connection, ClientServiceMessage message) throws IllegalAccessException {
        if (message instanceof BackupMessage)
            return new BackupAction(connection, (BackupMessage) message);
        if (message instanceof DeleteMessage)
            return new DeleteAction(connection, (DeleteMessage) message);
        if (message instanceof ReclaimMessage)
            return new ReclaimAction(connection, (ReclaimMessage) message);
        if (message instanceof RestoreMessage)
            return new RestoreAction(connection, (RestoreMessage) message);
        if (message instanceof StateMessage)
            return new StateAction(connection, (StateMessage) message);
        throw new IllegalAccessException("I'm not supposed to happen unknown class " + message.getClass().getName());
    }
}
