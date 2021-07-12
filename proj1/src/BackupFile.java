import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class BackupFile implements Serializable {
    private String filename;
    private File file;
    private int replicationDegree;

    private String id;
    private ArrayList<Chunk> chunks = new ArrayList<>();
    
    public BackupFile(String filename, int replicationDegree) throws IOException {
        this.filename = "../../peerFiles/" + Peer.getInstance().getId() + "/originals/" + filename;
        this.file = new File(this.filename);

        this.replicationDegree = replicationDegree;

        this.id = createID(this.file);

        this.chunks = this.divideChunks();
    }
    
    public static String createID(File file) {
        String fileID = file.getName() + file.lastModified();

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] encodedFileID = digest.digest(fileID.getBytes(StandardCharsets.UTF_8));

        return Utils.bytesToHex(encodedFileID);
    }

    private ArrayList<Chunk> divideChunks() throws IOException {
        byte[] buf = Files.readAllBytes(Paths.get(this.filename));

        InputStream reader = new ByteArrayInputStream(buf);

        int chunkNum = 0;
        int readReturn;
        do {
            byte[] cbuf = new byte[64000];
            readReturn = reader.read(cbuf, 0, 64000);

            ChunkID chunkID = new ChunkID(this.id, chunkNum);

            Chunk chunk;

            if (readReturn > -1)
                 chunk = new Chunk(this.replicationDegree, chunkID, Arrays.copyOf(cbuf, readReturn));
            else
                chunk = new Chunk(this.replicationDegree, chunkID, new byte[0]);

            chunks.add(chunk);
            chunkNum++;
        } while (readReturn == 64000);

        return chunks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackupFile that = (BackupFile) o;
        return Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename);
    }

    public String getID() {
        return id;
    }

    public String getFilename() {
        return this.filename;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public ArrayList<Chunk> getChunks()
    {
        return this.chunks;
    }
}
