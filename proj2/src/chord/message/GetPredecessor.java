package chord.message;

import chord.CHORDPeer;

public class GetPredecessor extends CHORDMessage {
    public GetPredecessor() {
        super("GET_PREDECESSOR");
    }

    @Override
    public CHORDMessage getResponse(CHORDPeer peer) {
        return peer.getPredecessor();
    }
}
