package service.client;

import application.Server;
import service.client.message.RestoreMessage;
import service.peer.message.ContentMessage;
import service.peer.message.GetContentMessage;
import service.peer.message.PeerServiceMessage;
import ssl.SSLConnection;
import ssl.data.SSLMessage;
import storage.FileInitiated;

import java.io.IOException;

public class RestoreAction extends ClientRequestAction<RestoreMessage> {
    private String content;
    private FileInitiated file;

    protected RestoreAction(SSLConnection connection, RestoreMessage message) {
        super(connection, message);
    }

    @Override
    public void run() {
        System.out.println("Starting Restore");
        Server server = Server.getInstance();

        file = Server.getStorage().getFileByPath(message.getFileName());
        GetContentMessage messageToSend = new GetContentMessage(server.getAddress(), file.getFileId());

        while (!stop) {
            try {
                server.sendToAll(messageToSend);
                Thread.sleep(100);
            } catch (Exception exception) {
                stop();
                return;
            }
        }

        try {
            file.restore(content);
            Server.getInstance().write(connection, new SSLMessage("Success"));
        } catch (IOException exception) {
            exception.printStackTrace();
            try {
                Server.getInstance().write(connection, new SSLMessage("Could not restore file"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Restore finished");
    }

    @Override
    public void processMessage(PeerServiceMessage message) {
        if (!(message instanceof ContentMessage))
            return;

        ContentMessage contentMessage = (ContentMessage) message;
        if (!contentMessage.getFileId().equals(file.getFileId()))
            return;

        content = contentMessage.getFileBody();
        stop();
    }
}
