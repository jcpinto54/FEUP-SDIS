package service.peer.message;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class RemovedFileMessage extends PeerServiceMessage {
    public RemovedFileMessage(InetSocketAddress senderAddress, String fileId) {
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
        return "REMOVED";
    }
}
