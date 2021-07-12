import java.util.Random;

// Version 2.0
public class ReceivedBootThread implements Runnable {
    private Message message;

    public ReceivedBootThread(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        while (!Peer.getInstance().ableToRespondDegrees());

        int bootedID = this.message.getSenderID();

        Message degreesMessage = null;

        try {
            degreesMessage = Message.createMessagev2(bootedID);
        } catch (MessageException e) {  // Won't enter this
            System.err.println("BOOT: Problems creating degrees message! - " + e.getMessage());
        }

        Random r = new Random();
        int low = 0;
        int high = 401;
        int delay = r.nextInt(high-low) + low;


        Peer.getInstance().getThreadExecutor().schedule(new SendMessageThread(SendMessageThread.MCChannel, degreesMessage), delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
