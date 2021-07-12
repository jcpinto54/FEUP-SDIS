package service.client;

import application.Server;
import service.client.message.StateMessage;
import ssl.SSLConnection;
import ssl.data.SSLMessage;
import storage.File;
import storage.FileInitiated;

import java.io.IOException;

public class StateAction extends ClientRequestAction<StateMessage> {
    private static final String LINE_BREAK = "---------------------------------------------------";

    protected StateAction(SSLConnection connection, StateMessage message) {
        super(connection, message);
    }

    private String getStateString() {
        StringBuilder initiatedString = new StringBuilder();
        StringBuilder notInitiatedString = new StringBuilder();

        for (File file : Server.getStorage().getFiles().values()) {
            StringBuilder toAdd;
            FileInitiated initiatedFile = null;
            if (file instanceof FileInitiated) {
                toAdd = initiatedString;
                initiatedFile = (FileInitiated) file;
            } else {
                toAdd = notInitiatedString;
            }

            toAdd.append("file: ").append(file.getFileId());
            if (initiatedFile != null) {
                toAdd.append(" (").append(initiatedFile.getOriginalFilePath()).append(")\n");
                toAdd.append("\tcopies: ").append(initiatedFile.getPerceivedReplicationDegree()).append("/").append(initiatedFile.getReplicationDegree());
            }
            toAdd.append("\n");
            toAdd.append("\tsize: ").append(file.getSize()).append("\n");
        }

        StringBuilder result = new StringBuilder();
        result.append(LINE_BREAK).append("\n");
        result.append("Initiated Files").append("\n");
        result.append(LINE_BREAK).append("\n");
        if (initiatedString.length() == 0)
            result.append("None\n");
        result.append(initiatedString).append("\n");

        result.append(LINE_BREAK).append("\n");
        result.append("Stored Files").append("\n");
        result.append(LINE_BREAK).append("\n");
        if (notInitiatedString.length() == 0)
            result.append("None\n");
        result.append(notInitiatedString).append("\n");

        result.append(LINE_BREAK).append("\n");
        result.append("Disk Usage").append("\n");
        result.append(LINE_BREAK).append("\n");
        long maximumStorage = Server.getStorage().getMaxDiskSpace();
        result.append("Capacity: ");
        result.append(maximumStorage == -1 ? "unlimited" : maximumStorage).append("\n");
        result.append("Used: ").append(Server.getStorage().getStoredSize()).append("\n");

        stop();
        return result.toString();
    }

    @Override
    public void run() {
        SSLMessage messageToSend = new SSLMessage(getStateString());
        try {
            Server.getInstance().write(connection, messageToSend);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
