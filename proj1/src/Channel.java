import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

public abstract class Channel {
    protected MulticastSocket socket;
    protected InetAddress group;
    protected int port;

    public Channel(String group, int port) {
        try {
            this.group = InetAddress.getByName(group);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.port = port;
    }

    public void sendMessage(Message message) {
        byte[] messageBytes = message.getBytes();
        DatagramPacket messagePacket = new DatagramPacket(messageBytes, messageBytes.length, this.group, this.port);

        try {
            this.socket.send(messagePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
