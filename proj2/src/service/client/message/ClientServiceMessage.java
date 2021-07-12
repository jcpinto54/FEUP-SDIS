package service.client.message;

import ssl.data.SSLMessage;

import java.util.Arrays;
import java.util.List;

public abstract class ClientServiceMessage extends SSLMessage {
    public static ClientServiceMessage fromArguments(List<String> arguments) {
        String action = arguments.get(0).toUpperCase();
        String secondArgument = arguments.size() > 1 ? arguments.get(1) : null;
        String thirdArgument = arguments.size() > 2 ? arguments.get(2) : null;
        return switch (action) {
            case "BACKUP" -> {
                assert arguments.size() == 3;
                yield new BackupMessage(secondArgument, Integer.parseInt(thirdArgument));
            }
            case "RESTORE" -> {
                assert arguments.size() == 2;
                yield new RestoreMessage(secondArgument);
            }
            case "DELETE" -> {
                assert arguments.size() == 2;
                yield new DeleteMessage(secondArgument);
            }
            case "RECLAIM" -> {
                assert arguments.size() == 2;
                yield new ReclaimMessage(Integer.parseInt(secondArgument));
            }
            case "STATE" -> {
                assert arguments.size() == 1;
                yield new StateMessage();
            }
            default -> throw new IllegalStateException("Unexpected value: " + action);
        };
    }

    public static ClientServiceMessage parseMessage(String sslMessage) {
        return fromArguments(Arrays.asList(sslMessage.split(" ")));
    }
}
