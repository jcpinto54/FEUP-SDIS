package service.peer.message;

import java.net.InetSocketAddress;

public class GetContentMessage extends PeerServiceMessage {
    public GetContentMessage(InetSocketAddress senderAddress, String fileId) {
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
        return "GET_FILE";
    }
}
