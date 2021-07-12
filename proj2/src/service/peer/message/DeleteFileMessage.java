package service.peer.message;

import java.net.InetSocketAddress;

public class DeleteFileMessage extends PeerServiceMessage {
    public DeleteFileMessage(InetSocketAddress senderAddress, String fileId) {
        super(senderAddress, fileId);
        appendMessage();
    }

    @Override
    protected void appendMessage() {
        super.appendMessage();
        append(";");
    }

    @Override
    public String getName() {
        return "DELETE";
    }
}
