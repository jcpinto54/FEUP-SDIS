package storage;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Storage implements Serializable {
    private long maxDiskSpace = -1;
    private final Map<String, File> fileList = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Files
    // -------------------------------------------------------------------------

    public void addFile(FileInitiated file) {
        fileList.put(file.getFileId(), file);
    }

    public void storeFile(File file, String content) throws IOException {
        fileList.put(file.getFileId(), file);
        file.writeContent(content);
    }

    public String readFile(String fileId) throws IOException {
        File file = fileList.getOrDefault(fileId, null);
        if (file == null) return null;
        return file.readContent();
    }

    public void deleteFile(String fileId) {
        File file = fileList.getOrDefault(fileId, null);
        if (file == null) return;
        fileList.remove(fileId);
        file.delete();
    }

    public Map<String, File> getFiles() {
        return fileList;
    }

    public File getFile(String fileId) {
        return fileList.get(fileId);
    }

    public FileInitiated getFileByPath(String filePath) {
        for (File file : fileList.values()) {
            if (!(file instanceof FileInitiated))
                continue;
            FileInitiated fileInitiated = (FileInitiated) file;
            if (fileInitiated.getOriginalFilePath().equals(filePath))
                return fileInitiated;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Disk Space
    // -------------------------------------------------------------------------

    public long getMaxDiskSpace() {
        return maxDiskSpace;
    }

    public void setMaxDiskSpace(long maxDiskSpace) {
        this.maxDiskSpace = maxDiskSpace;
    }

    public long getStoredSize() {
        long size = 0;
        for (File file : fileList.values())
            size += file.getSize();
        return size;
    }

    public long getRemainingSpace() {
        return this.maxDiskSpace == -1 ? Long.MAX_VALUE : this.maxDiskSpace - getStoredSize();
    }


}
