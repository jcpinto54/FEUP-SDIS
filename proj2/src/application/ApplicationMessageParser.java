package application;

import chord.message.CHORDMessage;
import ssl.data.SSLMessage;

import java.util.ArrayList;
import java.util.List;

public class ApplicationMessageParser {
    public static List<SSLMessage> parse(SSLMessage originalMessage) {
        return new ArrayList<>(CHORDMessage.fromSSLMessage(originalMessage));
    }
}
