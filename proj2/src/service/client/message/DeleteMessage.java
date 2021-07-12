package service.client.message;

public class DeleteMessage extends ClientFileServiceMessage {
    public DeleteMessage(String fileName) {
        super(fileName);
        append("DELETE ").append(fileName).append(";");
    }
}
