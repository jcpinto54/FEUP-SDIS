import java.io.*;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class FileManager {
    private long remainingStorage;
    private long maxStorage;
    private ConcurrentHashMap<ChunkID, Integer> actualReplicationDegrees;
    private ConcurrentHashMap<ChunkID, Integer> desiredReplicationDegrees;
    private ConcurrentHashMap<ChunkID, Boolean> chunksInLocalStorage;
    private ConcurrentHashMap<String, Boolean> filesInLocalStorage;
    private ConcurrentHashMap<BackupFile, Boolean> backedUpFiles;

    private boolean repDegreesRead = false;

    public FileManager(int peerID) throws IOException {
        this.remainingStorage = 1000000; // 1MB
        this.maxStorage = 1000000; // 1MB
        this.actualReplicationDegrees = new ConcurrentHashMap<>();
        this.desiredReplicationDegrees = new ConcurrentHashMap<>();
        this.filesInLocalStorage = new ConcurrentHashMap<>();
        this.chunksInLocalStorage = new ConcurrentHashMap<>();
        this.backedUpFiles = new ConcurrentHashMap<>();

        File peerDir = new File("../../peerFiles/" + peerID + "/");
        if (!peerDir.exists()) {
            peerDir.mkdirs();
        }

        File backupDir = new File("../../peerFiles/" + peerID + "/backup/");
        if (backupDir.exists()) {
            File[] filesStored = backupDir.listFiles();
            for (File fileFile : filesStored) {
                this.filesInLocalStorage.put(fileFile.getName(), true);

                File[] chunksStored = fileFile.listFiles();
                for (File chunkFile : chunksStored) {
                    int chunkNum = Integer.parseInt(chunkFile.getName());
                    ChunkID chunkID = new ChunkID(fileFile.getName(), chunkNum);
                    this.chunksInLocalStorage.put(chunkID, true);
                    this.decrementRemainingStorage(chunkFile.length());
                    this.actualReplicationDegrees.put(chunkID, 1);
                }
            }
        }
        File replicationDegreesFile =  new File("../../peerFiles/" + peerID + "/repDegrees.txt");

        if (!replicationDegreesFile.exists()) {

            replicationDegreesFile.createNewFile();
            this.repDegreesRead = true;
            return;
        }

        BufferedReader input = new BufferedReader(new FileReader(replicationDegreesFile));
        String line = input.readLine();
        if (line == null) return;
        String[] firstLineSplitted = line.split(" ");
        this.remainingStorage = Integer.parseInt(firstLineSplitted[0]);
        this.maxStorage = Integer.parseInt(firstLineSplitted[1]);

        while ((line = input.readLine()) != null) {
            String[] lineSplit = line.split(" ");

            String fileID = null;
            int chunkNum = -1, actualReplicationDegree = -1, desiredReplicationDegree = -1;
            try {
                fileID = lineSplit[0];
                chunkNum = Integer.parseInt(lineSplit[1]);
                actualReplicationDegree = Integer.parseInt(lineSplit[3]);
                desiredReplicationDegree = Integer.parseInt(lineSplit[5]);
            } catch (NumberFormatException e) {
                System.err.println("FILES: Replication degrees file badly formatted!");
            }
            this.actualReplicationDegrees.put(new ChunkID(fileID, chunkNum), actualReplicationDegree);
            this.desiredReplicationDegrees.put(new ChunkID(fileID, chunkNum), desiredReplicationDegree);
        }
        this.repDegreesRead = true;
    }

    public void shutdown() {
        if (!this.repDegreesRead) return;
        File replicationDegreesFile =  new File("../../peerFiles/" + Peer.getInstance().getId() + "/repDegrees.txt");

        if (!replicationDegreesFile.exists()) {
            try {
                replicationDegreesFile.createNewFile();
            } catch (IOException e) {
                System.err.println("FILES: Problem creating repDegrees.txt file!");
            }
        }

        try {
            BufferedWriter output;
            output = new BufferedWriter(new FileWriter(replicationDegreesFile));

            output.write(this.remainingStorage + " " + this.maxStorage + "\n");
            Iterator it = this.actualReplicationDegrees.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                ChunkID key = (ChunkID) pair.getKey();
                int value = (int) pair.getValue();
                int desiredRepDegree = this.getDesiredReplicationDegree(key);
                String toWrite = key.toString() + " - " + value + " / " + desiredRepDegree + "\n";
                output.write(toWrite);
            }
            output.flush();
            output.close();
        } catch (IOException e) {
            System.err.println("FILES: Error opening repDegrees.txt output buffer!");
        }
    }

    public synchronized void incrementRemainingStorage(long add) {
        this.remainingStorage = min(this.maxStorage, this.remainingStorage + add);
    }

    public synchronized void decrementRemainingStorage(long sub) {
        this.remainingStorage = max(this.remainingStorage - sub, 0);
    }

    public synchronized void incrementReplicationDegree(ChunkID chunkID) {
        Integer degree = this.actualReplicationDegrees.get(chunkID);
        if (degree == null) {
            this.actualReplicationDegrees.put(chunkID, 1);
            return;
        }
        this.actualReplicationDegrees.put(chunkID, degree + 1);
    }

    public synchronized void decrementReplicationDegree(ChunkID chunkID) {
        Integer degree = this.actualReplicationDegrees.get(chunkID);
        if (degree == null) {
            System.err.println("SOMETHING IS WRONG WITH THE CHUNK'S REPLICATION DEGREES!");
            return;
        }
        this.actualReplicationDegrees.put(chunkID, degree - 1);
    }

    public synchronized boolean isFileStored(String fileID) {
        return this.filesInLocalStorage.getOrDefault(fileID, false);
    }

    public synchronized boolean isChunkStored(ChunkID chunkID) {
        return this.chunksInLocalStorage.getOrDefault(chunkID, false);
    }

    public synchronized int getActualReplicationDegree(ChunkID chunkID) {
        return this.actualReplicationDegrees.getOrDefault(chunkID, 0);
    }

    public synchronized ConcurrentHashMap<ChunkID, Integer> getActualReplicationDegrees() {
        return actualReplicationDegrees;
    }

    public synchronized ConcurrentHashMap<ChunkID, Integer> getDesiredReplicationDegrees() {
        return desiredReplicationDegrees;
    }

    public synchronized int getDesiredReplicationDegree(ChunkID chunkID) {
        return this.desiredReplicationDegrees.getOrDefault(chunkID, 0);
    }

    public synchronized void setDesiredReplicationDegree(ChunkID chunkID, int replicationDegree) {
        this.desiredReplicationDegrees.put(chunkID, replicationDegree);
    }

    public void setActualReplicationDegree(ChunkID chunkID, int actualReplicationDegree) {
        this.actualReplicationDegrees.put(chunkID, actualReplicationDegree);
    }

    public synchronized ArrayList<ChunkID> storeChunk(Chunk chunk, String version) {

        ArrayList<ChunkID> removedChunks = new ArrayList<>();

        if (this.getDesiredReplicationDegree(chunk.getId()) != chunk.getDesiredReplicationDegree()) {
            this.setDesiredReplicationDegree(chunk.getId(), chunk.getDesiredReplicationDegree());
        }

        if (this.chunksInLocalStorage.getOrDefault(chunk.getId(), false)) {
            return null;
        }

        if (version.equals("2.0") && this.getActualReplicationDegree(chunk.getId()) >= chunk.getDesiredReplicationDegree()) {
            return null;
        }

        if (this.remainingStorage < chunk.getData().length) {

            Iterator it = this.chunksInLocalStorage.entrySet().iterator();
            while (it.hasNext() && this.remainingStorage < chunk.getData().length) {
                Map.Entry pair = (Map.Entry)it.next();
                ChunkID chunkID = (ChunkID) pair.getKey();
                if (!((boolean) pair.getValue())) continue;

                int actualRepDegree = this.getActualReplicationDegree(chunkID);
                int desiredRepDegree = this.getDesiredReplicationDegree(chunkID);

                if (actualRepDegree > desiredRepDegree) {
                    try {
                        this.removeChunk(chunkID);
                        removedChunks.add(chunkID);
                    } catch (FileSystemException e) {
                        System.err.println("Couldn't remove this chunk!");
                    }
                }
            }

            if (this.remainingStorage < chunk.getData().length) return null;
        }


        try{
            String dirName = "../../peerFiles/" + Peer.getInstance().getId() + "/backup/" + chunk.getId().getFileID() + "/";
            File directory = new File(dirName);

            if (!directory.exists()) {
                //noinspection ResultOfMethodCallIgnored
                directory.mkdirs();
            }

            File chunkFile = new File(dirName + chunk.getId().getChunkNum());
            FileOutputStream fos = new FileOutputStream(chunkFile);
            fos.write(chunk.getData());

            this.chunksInLocalStorage.put(chunk.getId(), true);
        }catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        this.decrementRemainingStorage(chunk.getData().length);
        this.incrementReplicationDegree(chunk.getId());
        this.desiredReplicationDegrees.put(chunk.getId(), chunk.getDesiredReplicationDegree());
        this.filesInLocalStorage.put(chunk.getId().getFileID(), true);

        return removedChunks;
    }

    public synchronized void removeReplicationDegrees(String fileID) {
        for (Map.Entry<ChunkID, Integer> chunkIDIntegerEntry : this.actualReplicationDegrees.entrySet()) {
            Map.Entry pair = (Map.Entry) chunkIDIntegerEntry;
            ChunkID key = (ChunkID) pair.getKey();
            if (key.getFileID().equals(fileID)) {
                this.actualReplicationDegrees.put(key, 0);
                this.desiredReplicationDegrees.put(key, 0);
            }
        }

    }

    public synchronized void removeBackupFile(String fileID) throws FileSystemException {

        Iterator it = Peer.getInstance().getFileManager().getActualReplicationDegrees().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ChunkID key = (ChunkID) pair.getKey();
            if (key.getFileID().equals(fileID)) {
                this.actualReplicationDegrees.put(key, 0);
                this.desiredReplicationDegrees.put(key, 0);
            }
        }

        if (!isFileStored(fileID)) return;

        File fileDir = new File("../../peerFiles/" + Peer.getInstance().getId() + "/backup/" + fileID + "/");
        File[] chunks = fileDir.listFiles();
        if (chunks == null) {
            throw new FileSystemException("This is supposed to be a directory!");
        }
        for (File chunk : chunks) {
            this.chunksInLocalStorage.put(new ChunkID(fileID, Integer.parseInt(chunk.getName())), false);
            this.incrementRemainingStorage(chunk.length());
            if (!chunk.delete()) {
                throw new FileSystemException("Couldn't delete chunk file!");
            }
        }
        if (!fileDir.delete()) {
            throw new FileSystemException("Couldn't delete file directory");
        }
        this.filesInLocalStorage.put(fileID, false);
    }

    public synchronized void removeChunk(ChunkID chunkID) throws FileSystemException {
        File chunkFile = new File("../../peerFiles/" + Peer.getInstance().getId() + "/backup/" + chunkID.getFileID() + "/" + chunkID.getChunkNum());
        if (!this.isChunkStored(chunkID)) {
//            System.err.println("FILES: Trying to remove chunk that doesn't exist!");
            return;
        }
        if (!chunkFile.exists()) {
//            throw new FileSystemException("Trying to remove chunk that doesn't exist!");
            return;
        }

        this.decrementReplicationDegree(chunkID);
        this.chunksInLocalStorage.put(chunkID, false);

        this.incrementRemainingStorage(chunkFile.length());
        if (!chunkFile.delete()) {
            throw new FileSystemException("Couldn't delete chunk file!");
        }

        File fileDir = new File("../../peerFiles/" + Peer.getInstance().getId() + "/backup/" + chunkID.getFileID() + "/");
        if (fileDir.listFiles().length == 0) {
            fileDir.delete();
            this.filesInLocalStorage.put(chunkID.getFileID(), false);
        }
    }

    public synchronized Chunk retrieveChunk(ChunkID chunkID) throws FileNotFoundException {
        File chunkFile = new File("../../peerFiles/" + Peer.getInstance().getId() + "/backup/" + chunkID.getFileID() + "/" + chunkID.getChunkNum());

        if (!chunkFile.exists()) {
            File originalFiles = new File("../../peerFiles/" + Peer.getInstance().getId() + "/originals/");

            if (!originalFiles.exists())
                throw new FileNotFoundException("Chunk isn't here!");

            for (File file : originalFiles.listFiles()) {
                String fileID = BackupFile.createID(file);
                if (!fileID.equals(chunkID.getFileID())) {
                     continue;
                }
                BackupFile backupFile = null;
                try {
                    backupFile = new BackupFile(file.getName(), this.getDesiredReplicationDegree(chunkID));
                } catch (IOException e) {
                    System.err.println("FILES: Problems with creating BackupFile! - " + e.getMessage());
                }
                List<Chunk> chunks = backupFile.getChunks();

                return chunks.get((int) chunkID.getChunkNum());
            }

            throw new FileNotFoundException("Chunk isn't here!");
        }

        try {
            Chunk retChunk;
            byte[] buf = Files.readAllBytes(Paths.get(chunkFile.getPath()));

            retChunk = new Chunk(this.getDesiredReplicationDegree(chunkID), chunkID, buf);
            return retChunk;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public synchronized void restoreChunkTemp(Message message) throws IOException {
        String dirName = "../../peerFiles/" + Peer.getInstance().getId() + "/restoreTemp/" + message.getFileID() + "/";
        File restoreTempDir = new File(dirName);

        if (!restoreTempDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            restoreTempDir.mkdirs();
        }

        File chunkFile = new File(dirName + message.getChunkNum());
        FileOutputStream fos = new FileOutputStream(chunkFile);
        fos.write(message.getData());
    }

    public synchronized void restoreFile(String fileID) throws FileNotFoundException, FileSystemException {
        String restoreTempDirStr = "../../peerFiles/" + Peer.getInstance().getId() + "/restoreTemp/";

        File restoreTempDir = new File(restoreTempDirStr);
        restoreTempDir.deleteOnExit();

        File restoreTempFileDir = new File(restoreTempDirStr + fileID + "/");

        if (!restoreTempFileDir.exists()) {
            throw new FileNotFoundException("File chunks to restore aren't here!");
        }

        File[] chunks = restoreTempFileDir.listFiles();
        if (chunks == null) {
            throw new FileSystemException("This is supposed to be a directory!");
        }

        HashMap<Integer, File> chunksRestored = new HashMap<>();
        ArrayList<Integer> chunkNums = new ArrayList<>();

        for (File chunk : chunks) {
            int chunkNum = Integer.parseInt(chunk.getName());
            chunkNums.add(chunkNum);
            chunksRestored.put(chunkNum, chunk);
        }

        chunkNums.sort(Integer::compareTo);

        for (int i = 0; i < chunkNums.size(); i++) {
            if (chunkNums.get(i) != i) {
                System.err.println("CHUNK: File badly restored!");
                return;
            }
        }


        String restoreDirName = "../../peerFiles/" + Peer.getInstance().getId() + "/restore/";
        File restoreDir = new File(restoreDirName);

        if (!restoreDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            restoreDir.mkdirs();
        }

        File restoredFile = new File(restoreDirName + fileID);

        FileOutputStream fos = new FileOutputStream(restoredFile, true);

        Iterator it = chunksRestored.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Integer key = (Integer) pair.getKey();
            File value = (File) pair.getValue();
            try {
                byte[] buf = Files.readAllBytes(Paths.get(value.getPath()));

                fos.write(buf);
            } catch (IOException e) {
                System.err.println("FILES: Problems restoring file!");
                e.printStackTrace();
            }
        }

        for (File chunk : chunks) {
            if (!chunk.delete()) {
                throw new FileSystemException("Couldn't delete temporary chunk file!");
            }
        }

        restoreTempFileDir.delete();
    }

    public synchronized boolean isFileRestored(String fileID) {
        String dirName = "../../peerFiles/" + Peer.getInstance().getId() + "/restore/";
        File directory = new File(dirName);

        if (!directory.exists()) {
            directory.mkdirs();
            return false;
        }

        String file = dirName + fileID;
        File fileFile = new File(file);

        return fileFile.exists();
    }

    public synchronized boolean changeRestoredFileName(String oldName, String newName) {
        String file = "../../peerFiles/" + Peer.getInstance().getId() + "/restore/" + oldName;
        File fileFile = new File(file);
        File newNameFile = new File("../../peerFiles/" + Peer.getInstance().getId() + "/restore/" + newName);
        if (newNameFile.exists()) newNameFile.delete();
        return fileFile.renameTo(newNameFile);
    }

    public synchronized ArrayList<ChunkID> setMaxStorage(long newStorage) throws FileSystemException {
        if (this.maxStorage <= newStorage) {
            this.remainingStorage += (newStorage - this.maxStorage);
            this.maxStorage = newStorage;
            return null;
        }
        this.remainingStorage -= (this.maxStorage - newStorage);
        this.maxStorage = newStorage;
        if (this.remainingStorage >= 0) {
            return null;
        }

        ArrayList<ChunkID> removedChunks = new ArrayList<>();
        Iterator it = this.chunksInLocalStorage.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ChunkID chunkID = (ChunkID) pair.getKey();
            if (!((boolean) pair.getValue())) continue;

            int actualRepDegree = this.getActualReplicationDegree(chunkID);
            int desiredRepDegree = this.getDesiredReplicationDegree(chunkID);

            if (desiredRepDegree < actualRepDegree) {
                this.removeChunk(chunkID);
                removedChunks.add(chunkID);
                if (this.remainingStorage >= 0) {
                    return removedChunks;
                }
            }
        }

        it = this.chunksInLocalStorage.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ChunkID chunkID = (ChunkID) pair.getKey();
            if (!((boolean) pair.getValue())) continue;

            this.removeChunk(chunkID);
            removedChunks.add(chunkID);
            if (this.remainingStorage >= 0) {
                return removedChunks;
            }
        }

        return null; // hopefully this return won't be needed
    }

    public ConcurrentHashMap<BackupFile, Boolean> getBackedUpFiles() {
        return backedUpFiles;
    }

    public ConcurrentHashMap<ChunkID, Boolean> getChunksInLocalStorage() {
        return chunksInLocalStorage;
    }

    public long getRemainingStorage() {
        return remainingStorage;
    }

    public long getMaxStorage() {
        return maxStorage;
    }

    public void deleteRestoredFile(String fileID) {
        String fileName = "../../peerFiles/" + Peer.getInstance().getId() + "/restore/" + fileID;
        File file = new File(fileName);
        file.delete();
    }
}
