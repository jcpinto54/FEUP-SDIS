package chord.message;

import chord.CHORDPeer;

import java.net.InetSocketAddress;

public class SetSuccessor extends CHORDMessage {
    private final int key;
    private final InetSocketAddress address;

    public SetSuccessor(int key, InetSocketAddress address) {
        super("SET_SUCCESSOR " + key + " " + addressToString(address));
        this.key = key;
        this.address = address;
    }

    public SetSuccessor(PeerAddress peer) {
        super("SET_SUCCESSOR " + peer.getKey() + " " + addressToString(peer.getAddress()));
        this.key = peer.getKey();
        this.address = peer.getAddress();
    }

    @Override
    public void process(CHORDPeer peer) {
        peer.setSuccessor(new PeerAddress(key, address));
    }
}
