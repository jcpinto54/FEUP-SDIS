package service.peer.message;

import java.net.InetSocketAddress;

public class StoreFileMessage extends PeerContentMessage {
    protected final int replicationDegree;

    public StoreFileMessage(InetSocketAddress senderAddress, String fileId, int replicationDegree, String fileBody) {
        super(senderAddress, fileId, fileBody);
        this.replicationDegree = replicationDegree;
        appendMessage();
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public String getFileBody() {
        return fileBody;
    }

    @Override
    protected void appendMessage() {
        super.appendMessage();
        append(" ").append(replicationDegree).append(" ").append(normalize(fileBody)).append(";");
    }

    @Override
    public String getName() {
        return "STORE";
    }
}
