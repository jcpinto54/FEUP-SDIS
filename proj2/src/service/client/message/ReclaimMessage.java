package service.client.message;

public class ReclaimMessage extends ClientServiceMessage {
    protected final long maximumSpace;

    public long getMaximumSpace() {
        return maximumSpace;
    }

    public ReclaimMessage(long maximumSpace) {
        this.maximumSpace = maximumSpace;
        append("RECLAIM ").append(maximumSpace).append(";");
    }
}
