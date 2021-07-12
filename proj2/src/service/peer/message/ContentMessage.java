package service.peer.message;

import java.net.InetSocketAddress;

public class ContentMessage extends PeerContentMessage {
    public ContentMessage(InetSocketAddress senderAddress, String fileId, String fileBody) {
        super(senderAddress, fileId, fileBody);
        appendMessage();
    }

    public String getFileBody() {
        return fileBody;
    }

    @Override
    protected void appendMessage() {
        super.appendMessage();
        append(" ").append(normalize(fileBody)).append(";");
    }

    @Override
    public String getName() {
        return "CONTENT";
    }
}
