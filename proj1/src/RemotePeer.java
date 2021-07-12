import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemotePeer extends Remote {
    void backupRequest(String filename, int replicationDegree) throws IOException;
    void deleteRequest(String filename) throws IOException;
    void restoreRequest(String filename) throws IOException;
    void reclaimRequest(long maxStorage) throws IOException;
    String stateRequest() throws IOException;
}
