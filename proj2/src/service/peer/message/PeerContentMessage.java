package service.peer.message;

import java.net.InetSocketAddress;

public abstract class PeerContentMessage extends PeerServiceMessage {
    protected String fileBody;

    protected PeerContentMessage(InetSocketAddress senderAddress, String fileId, String fileBody) {
        super(senderAddress, fileId);
        this.fileBody = fileBody;
    }

    public static String normalize(String string) {
        return string.replace(";", "\2\3PTVG\2\3");
    }

    public static String denormalize(String string) {
        return string.replace("\2\3PTVG\2\3", ";");
    }
}
