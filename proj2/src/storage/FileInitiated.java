package storage;

import application.Server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

public class FileInitiated extends File {
    private final String originalFilePath;
    private final int replicationDegree;
    private final Set<InetSocketAddress> peersWithCopy = new HashSet<>();

    public FileInitiated(String originalFilePath, int replicationDegree) {
        super(pathToFileId(originalFilePath), Server.getInstance().getAddress());
        this.originalFilePath = originalFilePath;
        this.replicationDegree = replicationDegree;
    }

    public int getPerceivedReplicationDegree() {
        return peersWithCopy.size();
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public int neededCopies() {
        return Integer.max(getReplicationDegree() - getPerceivedReplicationDegree(), 0);
    }

    public void addCopy(InetSocketAddress key) {
        peersWithCopy.add(key);
    }

    public void removeCopy(InetSocketAddress key) {
        peersWithCopy.remove(key);
    }

    public boolean hasEnoughCopies() {
        return neededCopies() == 0;
    }

    private FileInputStream getOriginalStream() {
        try {
            assert originalFilePath != null;
            return new FileInputStream(originalFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String readOriginalToString() {
        String fileContent;
        try {
            FileInputStream file = getOriginalStream();
            assert file != null;
            fileContent = new String(file.readAllBytes());
        } catch (IOException ex) {
            return null;
        }
        return fileContent;
    }

    public boolean restore(String content) throws IOException {
        FileOutputStream stream = getOriginalOutputStream();
        if (stream == null)
            return false;
        if (content != null) {
            stream.write(content.getBytes());
            size = content.length();
        }
        return true;
    }

    private FileOutputStream getOriginalOutputStream() {
        java.io.File file = new java.io.File(getOriginalFilePath());
        return getFileOutputStream(file);
    }

    public Set<InetSocketAddress> getPeersWithCopy() {
        return peersWithCopy;
    }

    private static String pathToFileId(String filePath) {
        // Add last modification date to id
        try {
            BasicFileAttributes attributes = Files.readAttributes(Path.of(filePath), BasicFileAttributes.class);
            filePath += attributes.lastAccessTime();
        } catch (IOException exception) {
            return null;
        }

        // Create digest
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            return null;
        }
        byte[] fileIdBytes = digest.digest(filePath.getBytes(StandardCharsets.UTF_8));

        // Make string
        StringBuilder fileId = new StringBuilder();
        for (byte b : fileIdBytes) {
            fileId.append(String.format("%02X", b));
        }
        return fileId.toString();
    }
}
