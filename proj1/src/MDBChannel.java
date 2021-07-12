import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class MDBChannel extends Channel implements Runnable {
    public MDBChannel(String group, int port) {
        super(group, port);
    }

    @Override
    public void run() {
        try {
            this.socket = new MulticastSocket(this.port);
            this.socket.joinGroup(this.group);

            while (true)
            {
                byte[] mCastBuf = new byte[65000];
                DatagramPacket mCastPacket = new DatagramPacket(mCastBuf, mCastBuf.length);
                this.socket.receive(mCastPacket);

                byte[] buf = Arrays.copyOf(mCastPacket.getData(), mCastPacket.getLength());

                Message message;
                try {
                    message = new Message(buf);
                }   catch (MessageException e) {
                    continue;
                }

                if (message.getSenderID() == Peer.getInstance().getId()) {
                    continue;
                }

                if (!message.getMessageType().equals("PUTCHUNK"))
                    continue;

                Random r = new Random();
                int low = 0;
                int high = 401;
                int delay = r.nextInt(high-low) + low;

                Peer.getInstance().getThreadExecutor().schedule(new ReceivedPutChunkThread(message), delay, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}