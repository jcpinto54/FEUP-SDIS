import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Arrays;

public class MDRChannel extends Channel implements Runnable {

    public MDRChannel(String group, int port) {
        super(group, port);
    }

    @Override
    public void run() {
        try {
            socket = new MulticastSocket(this.port);
            socket.joinGroup(this.group);

            while (true) {
                byte[] mCastBuf = new byte[65000];

                DatagramPacket mCastPacket = new DatagramPacket(mCastBuf, mCastBuf.length);
                socket.receive(mCastPacket);

                byte[] buf = Arrays.copyOf(mCastPacket.getData(), mCastPacket.getLength());

                Message message;
                try {
                    message = new Message(buf);
                }   catch (MessageException e) {
                    System.err.println("MDB: Got an unknown message! - " + e.getMessage());
                    continue;
                }

                if (message.getSenderID() == Peer.getInstance().getId()) {
                    continue;
                }

                if ("CHUNK".equals(message.getMessageType())) {
                    Peer.getInstance().getThreadExecutor().execute(new ReceivedChunkThread(message));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
