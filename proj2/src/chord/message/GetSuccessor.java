package chord.message;

import chord.CHORDPeer;

public class GetSuccessor extends CHORDMessage {
    public GetSuccessor() {
        super("GET_SUCCESSOR");
    }

    @Override
    public CHORDMessage getResponse(CHORDPeer peer) {
        return peer.getSuccessor();
    }
}
