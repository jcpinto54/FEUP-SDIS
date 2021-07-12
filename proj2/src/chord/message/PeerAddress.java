package chord.message;

import java.net.InetSocketAddress;
import java.util.Objects;

public class PeerAddress extends CHORDMessage {
    private final int key;
    private final InetSocketAddress address;

    public PeerAddress(int key, InetSocketAddress address) {
        super("PEER_ADDRESS " + key + " " + addressToString(address));
        this.key = key;
        this.address = address;
    }

    public int getKey() {
        return key;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerAddress that = (PeerAddress) o;
        return key == that.key || Objects.equals(address, that.address);
    }
}
