package service.peer.message;

import application.Client;
import ssl.data.SSLMessage;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class PeerServiceMessage extends SSLMessage {
    protected final String fileId;
    protected final InetSocketAddress senderAddress;

    public abstract String getName();

    protected PeerServiceMessage(InetSocketAddress senderAddress, String fileId) {
        this.senderAddress = senderAddress;
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }

    public InetSocketAddress getSenderAddress() {
        return senderAddress;
    }

    protected void appendMessage() {
        append(getName()).append(" ").append(senderAddress.getAddress().getHostAddress()).append(":").append(senderAddress.getPort()).append(" ").append(fileId);
    }

    public static PeerServiceMessage parseMessage(String sslMessage) throws UnknownHostException {
        List<String> components = new ArrayList<>(Arrays.asList(sslMessage.split(" ")));
        String typeOfMessage, fileId;
        InetSocketAddress senderId;
        typeOfMessage = components.get(0);
        senderId = Client.stringToAddress(components.get(1));
        fileId = components.get(2);

        return switch (typeOfMessage) {
            case "CONTENT" -> new ContentMessage(senderId, fileId, PeerContentMessage.denormalize(String.join(" ", components.subList(3, components.size()))));
            case "DELETE" -> new DeleteFileMessage(senderId, fileId);
            case "GET_FILE" -> new GetContentMessage(senderId, fileId);
            case "REMOVED" -> new RemovedFileMessage(senderId, fileId);
            case "STORED" -> new StoredFileMessage(senderId, fileId);
            case "STORE" -> new StoreFileMessage(senderId, fileId, Integer.parseInt(components.get(3)), PeerContentMessage.denormalize(String.join(" ", components.subList(4, components.size()))));
            default -> throw new IllegalStateException("Unexpected message type: " + typeOfMessage);
        };
    }
}
