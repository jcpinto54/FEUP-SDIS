package service.client.message;

public class RestoreMessage extends ClientFileServiceMessage {
    public RestoreMessage(String fileName) {
        super(fileName);
        append("RESTORE ").append(fileName).append(";");
    }
}
