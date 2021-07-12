package chord.message;

import chord.CHORDPeer;
import ssl.data.SSLMessage;

import java.net.InetSocketAddress;
import java.util.List;

public abstract class CHORDMessage extends SSLMessage {
    public CHORDMessage(String message) {
        super(message + ";");
    }

    public void process(CHORDPeer peer) {

    }

    public CHORDMessage getResponse(CHORDPeer peer) {
        return null;
    }

    public static CHORDMessage parseMessage(String message) {
        String[] components = message.split(" ");

        InetSocketAddress address = null;
        if (components.length > 2) {
            String[] addressComponents = components[2].split(":");
            String hostName = addressComponents[0];
            address = new InetSocketAddress(hostName, Integer.parseInt(addressComponents[1]));
        }

        return switch (components[0]) {
            case "PEER_ADDRESS" -> new PeerAddress(Integer.parseInt(components[1]), address);
            case "GET_POSITION" -> new GetPosition(Integer.parseInt(components[1]));
            case "GET_PREDECESSOR" -> new GetPredecessor();
            case "GET_SUCCESSOR" -> new GetSuccessor();
            case "SET_PREDECESSOR" -> new SetPredecessor(Integer.parseInt(components[1]), address);
            case "SET_SUCCESSOR" -> new SetSuccessor(Integer.parseInt(components[1]), address);
            default -> throw new IllegalStateException("Unexpected message type: " + components[0]);
        };
    }

    public static List<SSLMessage> fromSSLMessage(SSLMessage message) {
        if (message == null)
            return null;
        return SSLMessage.parseMessages(message.getContent());
    }

    public static String addressToString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }
}
