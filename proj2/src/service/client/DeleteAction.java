package service.client;

import application.Server;
import service.client.message.DeleteMessage;
import service.peer.message.DeleteFileMessage;
import ssl.SSLConnection;
import ssl.data.SSLMessage;
import storage.FileInitiated;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class DeleteAction extends ClientRequestAction<DeleteMessage> {
    protected DeleteAction(SSLConnection connection, DeleteMessage message) {
        super(connection, message);
    }

    @Override
    public void run() {
        Server server = Server.getInstance();

        SSLMessage messageToClient = null;
        FileInitiated file = Server.getStorage().getFileByPath(message.getFileName());
        if (file != null) {
            DeleteFileMessage messageToSend = new DeleteFileMessage(Server.getInstance().getAddress(), file.getFileId());
            for (InetSocketAddress key : file.getPeersWithCopy()) {
                try {
                    server.send(key, messageToSend);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            messageToClient = new SSLMessage("Could not find file, did you make a backup of this file using this address before?");
        }

        stop();

        if (messageToClient == null)
            messageToClient = new SSLMessage("Success");

        try {
            Server.getInstance().write(connection, messageToClient);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
