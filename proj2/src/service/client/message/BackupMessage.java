package service.client.message;

public class BackupMessage extends ClientFileServiceMessage {
    protected final int replicationDegree;

    public BackupMessage(String fileName, int replicationDegree) {
        super(fileName);
        append("BACKUP ").append(fileName).append(" ").append(replicationDegree).append(";");
        this.replicationDegree = replicationDegree;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }
}
