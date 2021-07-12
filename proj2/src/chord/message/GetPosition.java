package chord.message;

import chord.CHORDPeer;

public class GetPosition extends CHORDMessage {
    private final int key;

    public GetPosition(int key) {
        super("GET_POSITION " + key);
        this.key = key;
    }

    @Override
    public CHORDMessage getResponse(CHORDPeer peer) {
        return peer.getPosition(key);
    }
}
