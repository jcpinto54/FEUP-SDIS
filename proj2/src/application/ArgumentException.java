package application;

public class ArgumentException extends Exception {
    public ArgumentException(String message) {
        super(message + "\n\tusage: java Client <server_hostname:server_port> <sub_protocol> <opnd_1> <opnd_2>");
    }

    public ArgumentException(String message, String action) {
        super(message + "\n\tusage: java Client <server_hostname:server_port> <sub_protocol> <opnd_1> <opnd_2>");
    }
}
