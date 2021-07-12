package chord.message;

import chord.CHORDPeer;

import java.net.InetSocketAddress;

public class SetPredecessor extends CHORDMessage {
    private final int key;
    private final InetSocketAddress address;

    public SetPredecessor(int key, InetSocketAddress address) {
        super("SET_PREDECESSOR " + key + " " + addressToString(address));
        this.key = key;
        this.address = address;
    }

    public SetPredecessor(PeerAddress peer) {
        super("SET_PREDECESSOR " + peer.getKey() + " " + addressToString(peer.getAddress()));
        this.key = peer.getKey();
        this.address = peer.getAddress();
    }

    @Override
    public void process(CHORDPeer peer) {
        peer.setPredecessor(new PeerAddress(key, address));
    }
}
