package ssl.data;

import chord.message.CHORDMessage;
import service.client.message.ClientServiceMessage;
import service.peer.message.PeerServiceMessage;

import java.util.ArrayList;
import java.util.List;

public class SSLMessage {
    private final StringBuilder content = new StringBuilder();

    public SSLMessage() {

    }

    public SSLMessage(String content) {
        this.content.append(content);
    }

    public SSLMessage(byte[] bytes) {
        this.content.append(getString(bytes));
    }

    public boolean isEmpty() {
        return content.length() == 0;
    }

    public String getContent() {
        return content.toString();
    }

    public SSLMessage append(byte[] bytes) {
        content.append(getString(bytes));
        return this;
    }

    public SSLMessage append(String string) {
        content.append(string);
        return this;
    }

    public SSLMessage append(int integer) {
        content.append(integer);
        return this;
    }

    public SSLMessage append(long integer) {
        content.append(integer);
        return this;
    }

    public static List<SSLMessage> parseMessages(String sslMessage) {
        List<SSLMessage> result = new ArrayList<>();
        String[] messages = sslMessage.split(";");

        for (String message : messages) {
            try {
                result.add(CHORDMessage.parseMessage(message));
                continue;
            } catch (Exception e) {
                //ok
            }

            try {
                result.add(PeerServiceMessage.parseMessage(message));
                continue;
            } catch (Exception e) {
                //ok
            }

            try {
                result.add(ClientServiceMessage.parseMessage(message));
            } catch (Exception e) {
                //ok
            }
        }

        return result;
    }

    public byte[] getBytes() {
        return getContent().getBytes();
    }

    @Override
    public String toString() {
        return getContent();
    }

    private static String getString(byte[] bytes) {
        String str = new String(bytes);
        str = str.replaceFirst("\u0000+$", "");
        return str;
    }
}
