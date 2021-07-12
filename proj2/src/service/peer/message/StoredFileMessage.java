package service.peer.message;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class StoredFileMessage extends PeerServiceMessage {
    public StoredFileMessage(InetSocketAddress senderAddress, String fileId) {
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
        return "STORED";
    }
}
