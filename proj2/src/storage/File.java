package storage;

import application.Server;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class File implements Serializable {
    private final InetSocketAddress initiatorPeer;
    private final String fileId;
    protected int size;

    public File(String fileId, InetSocketAddress initiatorPeer) {
        this.initiatorPeer = initiatorPeer;
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }

    public int getSize() {
        return size;
    }

    protected String getFilePath() {
        return Server.getInstance().getFolder() + "/" + fileId + ".file";
    }

    static FileOutputStream getFileOutputStream(java.io.File file) {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
        try {
            return new FileOutputStream(file, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected FileOutputStream getStream() {
        java.io.File file = new java.io.File(getFilePath());
        return getFileOutputStream(file);
    }

    public boolean writeContent(String content) throws IOException {
        FileOutputStream stream = getStream();
        if (stream == null)
            return false;
        if (content != null) {
            stream.write(content.getBytes());
            size = content.length();
        }
        return true;
    }

    public String readContent() throws IOException {
        java.io.File file = new java.io.File(getFilePath());
        FileInputStream stream = new FileInputStream(file);
        byte[] contentBytes = new byte[100000000]; // 100MB
        int sizeRead = stream.read(contentBytes, 0, 100000000);
        return new String(contentBytes, 0, sizeRead);
    }

    public boolean delete() {
        java.io.File file = new java.io.File(getFilePath());
        return file.delete();
    }

    public InetSocketAddress getInitiatorPeer() {
        return initiatorPeer;
    }
}
