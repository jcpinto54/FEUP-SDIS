package service.client.message;

public abstract class ClientFileServiceMessage extends ClientServiceMessage {
    protected final String fileName;

    public ClientFileServiceMessage(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
