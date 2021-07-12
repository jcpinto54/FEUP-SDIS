package service.client.message;

public class StateMessage extends ClientServiceMessage {
    public StateMessage() {
        append("STATE").append(";");
    }
}
