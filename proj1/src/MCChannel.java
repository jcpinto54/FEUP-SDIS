import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class MCChannel extends Channel implements Runnable {

    public MCChannel(String group, int port) {
        super(group, port);
    }

    @Override
    public void run() {
        try {
            socket = new MulticastSocket(this.port);
            socket.joinGroup(this.group);

            while (true) {
                byte[] mCastBuf = new byte[1024];

                DatagramPacket mCastPacket = new DatagramPacket(mCastBuf, mCastBuf.length);
                socket.receive(mCastPacket);


                byte[] buf = Arrays.copyOf(mCastPacket.getData(), mCastPacket.getLength());

                Message message;
                try {
                    message = new Message(buf);
                }   catch (MessageException e) {
                    System.err.println("MC: Got an unknown message! - " + e.getMessage());
                    continue;
                }

                if (message.getSenderID() == Peer.getInstance().getId()) {
                    continue;
                }
                switch (message.getMessageType()) {
                    case "STORED":
                        Peer.getInstance().getThreadExecutor().execute(new ReceivedStoredThread(message));
                        break;
                    case "GETCHUNK":
                        Peer.getInstance().getThreadExecutor().execute(new ReceivedGetChunkThread(message));
                        break;
                    case "DELETE":
                        Peer.getInstance().getThreadExecutor().execute(new ReceivedDeleteThread(message));
                        break;
                    case "REMOVED":
                        Peer.getInstance().getThreadExecutor().execute(new ReceivedRemovedThread(message));
                        break;
                }

                if (Peer.getInstance().getVersion().equals("2.0") && message.getVersion().equals("2.0")) {
                    switch (message.getMessageType()) {
                        case "BOOT":
                            Peer.getInstance().getThreadExecutor().execute(new ReceivedBootThread(message));
                            break;
                        case "DEGREES":
                            Peer.getInstance().getThreadExecutor().execute(new ReceivedDegreesThread(message));
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
