import java.io.*;
import java.nio.file.FileSystemException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;


// Singleton class
public class Peer implements RemotePeer
{
    // private instance, so that it can be
    // accessed by only by getInstance() method
    private static Peer peer;

    // Program arguments
    private final String version;
    private final int id;
    private final String rmiName;

    private FileManager fileManager;
    private ScheduledThreadPoolExecutor threadExecutor;
    private MCChannel mcChannel;
    private MDBChannel mdbChannel;
    private MDRChannel mdrChannel;
    private ConcurrentHashMap<ChunkID, Boolean> chunksAskedFor;
    private ConcurrentHashMap<ChunkID, Boolean> chunksReceived;
    private ConcurrentHashMap<ChunkID, Boolean> putchunksReceived;

    private ConcurrentHashMap<ChunkID, ScheduledFuture<?>> cleanupPutChunkThreads;
    private ConcurrentHashMap<ChunkID, ScheduledFuture<?>> cleanupChunkThreads;

    // Version 2.0
    private ConcurrentHashMap<Integer, Boolean> degreesReceived;
    private ConcurrentHashMap<Integer, ScheduledFuture<?>> cleanupDegreesThreads;
    private boolean readyForDegrees;


    public Peer(String[] args) throws IOException {
        this.threadExecutor = new ScheduledThreadPoolExecutor(20);
        this.mcChannel = new MCChannel(args[3], Integer.parseInt(args[4]));
        this.mdbChannel = new MDBChannel(args[5], Integer.parseInt(args[6]));
        this.mdrChannel = new MDRChannel(args[7], Integer.parseInt(args[8]));
        this.chunksAskedFor = new ConcurrentHashMap<>();
        this.chunksReceived = new ConcurrentHashMap<>();
        this.putchunksReceived = new ConcurrentHashMap<>();
        this.cleanupPutChunkThreads = new ConcurrentHashMap<>();
        this.cleanupChunkThreads = new ConcurrentHashMap<>();
        this.degreesReceived = new ConcurrentHashMap<>();
        this.cleanupDegreesThreads = new ConcurrentHashMap<>();
        this.version = args[0];
        this.id = Integer.parseInt(args[1]);
        this.rmiName = args[2];
        this.readyForDegrees = false;
        this.fileManager = new FileManager(id);
    }

    public synchronized FileManager getFileManager() {
        return fileManager;
    }

    public ScheduledThreadPoolExecutor getThreadExecutor() {
        return threadExecutor;
    }

    public MCChannel getMcChannel() {
        return mcChannel;
    }

    public MDBChannel getMdbChannel() {
        return mdbChannel;
    }

    public MDRChannel getMdrChannel() {
        return mdrChannel;
    }

    public String getVersion() {
        return version;
    }

    public int getId() {
        return id;
    }

    public ConcurrentHashMap<ChunkID, Boolean> getChunksAskedFor() {
        return chunksAskedFor;
    }

    public ConcurrentHashMap<ChunkID, Boolean> getChunksReceived() {
        return chunksReceived;
    }

    public ConcurrentHashMap<ChunkID, Boolean> getPutChunksReceived() {
        return putchunksReceived;
    }

    // Version 2.0
    public ConcurrentHashMap<Integer, Boolean> getDegreesReceived() {
        return degreesReceived;
    }

    public ConcurrentHashMap<ChunkID, ScheduledFuture<?>> getCleanupPutChunkThreads() {
        return cleanupPutChunkThreads;
    }

    public ConcurrentHashMap<ChunkID, ScheduledFuture<?>> getCleanupChunkThreads() {
        return cleanupChunkThreads;
    }

    // Version 2.0
    public ConcurrentHashMap<Integer, ScheduledFuture<?>> getCleanupDegreesThreads() {
        return cleanupDegreesThreads;
    }

    // Version 2.0
    public boolean ableToRespondDegrees() {
        return readyForDegrees;
    }

    // Version 2.0
    public void setReadyForDegreesTrue() {
        readyForDegrees = true;
    }


    //synchronized method to control simultaneous access
    synchronized public static Peer getInstance()
    {
        return peer;
    }

    synchronized public static Peer createInstance(String[] args) {
        try {
            peer = new Peer(args);
        } catch (IOException e) {
            System.err.println("PEER: Error while creating Peer!");
        }

        if (peer.version.equals("2.0")) {
            Message bootMessage = null;
            try {
                bootMessage = Message.createMessagev2();
            } catch (MessageException e) {
                System.err.println("PEER: Problmes creating BOOT message! - " + e.getMessage());
            }

            peer.getThreadExecutor().execute(new SendMessageThread(SendMessageThread.MCChannel, bootMessage));

            long startTime = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();

            while (currentTime - startTime < 1000 && !peer.readyForDegrees) {
                currentTime = System.currentTimeMillis();
                Thread.yield();
            }

            peer.setReadyForDegreesTrue();

        }
        return peer;
    }

    public static void main(String[] args){
        if (args.length != 9) {
            System.err.println("Usage: Peer <version> <peerId> <rmiName> <MC_multicast_address> <MC_multicast_port> <MDB_multicast_address> <MDB_multicast_port> <MDR_multicast_address> <MDR_multicast_port>");
            System.exit(1);
        }

        if (!Message.checkVersion(args[0])) {
            System.err.println("Usage: Peer <version> <peerId> <rmiName> <MC_multicast_address> <MC_multicast_port> <MDB_multicast_address> <MDB_multicast_port> <MDR_multicast_address> <MDR_multicast_port>");
            System.exit(1);
        }

        if (!Utils.isNumber(args[1])) {
            System.err.println("Usage: Peer <version> <peerId> <rmiName> <MC_multicast_address> <MC_multicast_port> <MDB_multicast_address> <MDB_multicast_port> <MDR_multicast_address> <MDR_multicast_port>");
            System.exit(1);
        }

        if (!Utils.isIPAddress(args[3]) || !Utils.isIPAddress(args[5]) || !Utils.isIPAddress(args[7])) {
            System.err.println("Usage: Peer <version> <peerId> <rmiName> <MC_multicast_address> <MC_multicast_port> <MDB_multicast_address> <MDB_multicast_port> <MDR_multicast_address> <MDR_multicast_port>");
            System.exit(1);
        }

        if (!Utils.isNumber(args[4]) || !Utils.isNumber(args[6]) || !Utils.isNumber(args[8])) {
            System.err.println("Usage: Peer <version> <peerId> <rmiName> <MC_multicast_address> <MC_multicast_port> <MDB_multicast_address> <MDB_multicast_port> <MDR_multicast_address> <MDR_multicast_port>");
            System.exit(1);
        }

        Peer peer = Peer.createInstance(args);
        peer.getThreadExecutor().execute(peer.mcChannel);
        peer.getThreadExecutor().execute(peer.mdbChannel);
        peer.getThreadExecutor().execute(peer.mdrChannel);

        try {
            RemotePeer stub = (RemotePeer) UnicastRemoteObject.exportObject(peer, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(Peer.getInstance().rmiName, stub);

            ShutdownThread thread = new ShutdownThread(registry, Peer.getInstance().rmiName);
            Runtime.getRuntime().addShutdownHook(thread);

            System.err.println("RMI: Ready to accept requests!");
        } catch (Exception e) {
            System.err.println("RMI exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void backupRequest(String filename, int replicationDegree) throws IOException {
        BackupFile file = new BackupFile(filename, replicationDegree);

        this.fileManager.getBackedUpFiles().put(file, true);

        for(Chunk chunk : file.getChunks())
        {
            Message message = null;
            try {
                message = Message.createMessage("PUTCHUNK", chunk);
            } catch (MessageException e) {
                e.printStackTrace();
            }
            this.fileManager.setDesiredReplicationDegree(chunk.getId(), replicationDegree);

            ArrayList<ScheduledFuture<?>> messagesToSend = new ArrayList<>();

            this.threadExecutor.execute(new SendMessageThread(SendMessageThread.MDBChannel, message));
            long miliSecondsToWait = 1000L;
            for (int i = 2; i <= 5; i++) {
                ScheduledFuture<?> sendingMessage = this.threadExecutor.schedule(new SendMessageThread(SendMessageThread.MDBChannel, message), miliSecondsToWait, TimeUnit.MILLISECONDS);
                messagesToSend.add(sendingMessage);
                miliSecondsToWait = (long) Math.pow(2, i) * 1000;
            }

            while (!messagesToSend.get(3).isDone()) {
                if (this.getFileManager().getActualReplicationDegree(chunk.getId()) >= replicationDegree) {
                    for (int i = 0; i < 4; i++) {
                        messagesToSend.get(i).cancel(false);        // Didn't use true, so that there are no unexpected errors
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void deleteRequest(String filename) throws IOException {
        File file = new File("../../peerFiles/" + this.id + "/originals/" + filename);

        if (!file.exists()) {
            System.err.println("RMI: Cannot ask to delete a file that I do not possess!");
        }

        String fileID = BackupFile.createID(file);

        Message message = Message.createMessage(fileID);

        this.fileManager.removeReplicationDegrees(fileID);

        for (int i = 0; i < 3; i++) {
            this.threadExecutor.schedule(new SendMessageThread(SendMessageThread.MCChannel, message), 2000 * i, TimeUnit.MILLISECONDS);
        }

        //Assume the file is deleted
        this.fileManager.getBackedUpFiles().put(new BackupFile(filename, -1), false);
    }

    @Override
    public void restoreRequest(String filename) throws IOException {
        File file = new File("../../peerFiles/" + this.id + "/originals/" + filename);

        if (!file.exists()) {
            System.err.println("RMI: Cannot ask to delete a file that I do not possess!");
        }

        String fileID = BackupFile.createID(file);

        int chunkNum = 0;

        ChunkID chunkID = new ChunkID(fileID, chunkNum);

        if (this.getFileManager().getDesiredReplicationDegree(chunkID) == 0) {
            return;
        }

        Message message = null;
        try {
            message = Message.createMessage("GETCHUNK", chunkID);
        } catch (MessageException e) {
            e.printStackTrace();
        }

        this.chunksAskedFor.put(chunkID, true);

        ArrayList<ScheduledFuture<?>> messagesToSend = new ArrayList<>();

        int i = 0;
        do {
            ScheduledFuture<?> scheduledMessage =  this.threadExecutor.schedule(new SendMessageThread(SendMessageThread.MCChannel, message), 5000L * i, TimeUnit.MILLISECONDS);
            messagesToSend.add(scheduledMessage);
            i++;
        } while (i <= 3);

        do {
            if (this.getFileManager().isFileRestored(fileID)) {
                for (int j = 0; j < 3; j++) {
                    messagesToSend.get(j).cancel(false);        // Didn't use true, so that there are no unexpected errors
                }
                break;
            }
        } while (!messagesToSend.get(2).isDone());
        if (!this.getFileManager().changeRestoredFileName(fileID, filename)) {
            this.getFileManager().deleteRestoredFile(fileID);
        }

    }

    public void sendRemovedChunks(ArrayList<ChunkID> removedChunks) {
        int i = 0;
        for (ChunkID removedChunk : removedChunks) {
            Message removedMessage = null;
            try {
                removedMessage = Message.createMessage("REMOVED", removedChunk);
            } catch (MessageException e) {
                System.err.println("RMI: Problems while creating message! - " + e.getMessage());
            }

            Random r = new Random();
            int low = 0;
            int high = 401;
            int delay = r.nextInt(high-low) + low;

            this.threadExecutor.schedule(new SendMessageThread(SendMessageThread.MCChannel, removedMessage), delay + i * 500L, TimeUnit.MILLISECONDS);
            i++;
        }
    }

    @Override
    public void reclaimRequest(long maxStorage) {
        ArrayList<ChunkID> removedChunks = null;
        try {
            removedChunks = this.fileManager.setMaxStorage(maxStorage);
        } catch (FileSystemException e) {
            System.err.println("RMI: Problems while setting max storage! - " + e.getMessage());
        }
        if (removedChunks == null) {
            return;
        }

        this.sendRemovedChunks(removedChunks);
    }

    @Override
    public String stateRequest() throws FileNotFoundException {
        String state = "\nSTATE OF PEER " + this.id;

        ConcurrentHashMap<BackupFile, Boolean> files = this.fileManager.getBackedUpFiles();

        state += "\n\nINITIATED BACKUPS";

        int i = 1;

        for (Map.Entry<BackupFile, Boolean> entry : files.entrySet())
        {
            if (entry.getValue())
            {
                state += "\n\nFile " + i;
                BackupFile file = entry.getKey();

                state += "\nPathname: " + file.getFilename();
                state +="\nFile ID: " + file.getID();
                state +="\nDesired Replication Degree: " + file.getReplicationDegree();

                state +="\n\nFile Chunks";

                for (Chunk chunk : file.getChunks())
                {
                    state += "\nChunk " + chunk.getId();
                    state += "\nPerceived Replication Degree: " + this.fileManager.getActualReplicationDegree(chunk.getId());
                }

                i++;
            }
        }

        if (i == 1)
            state += "\nThis Peer has no Initiated Backups";

        ConcurrentHashMap<ChunkID, Boolean> chunks = this.fileManager.getChunksInLocalStorage();

        state += "\n\nCHUNKS STORED";

        Boolean hasChunks = false;

        for (Map.Entry<ChunkID, Boolean> entry : chunks.entrySet())
        {
            if (entry.getValue())
            {
                hasChunks = true;

                ChunkID chunkID = entry.getKey();
                state += "\nChunk " + chunkID.toString();

                Chunk chunk = this.fileManager.retrieveChunk(chunkID);
                state += "\nSize: " + chunk.getData().length;
                state += "\nDesired Replication Degree: " + chunk.getDesiredReplicationDegree();
                state += "\nActual Replication Degree: " +  this.fileManager.getActualReplicationDegree(chunkID);
            }
        }

        if (!hasChunks)
            state += "\nThis peer currently has no chunks stored";

        state += "\n\nSTORAGE";

        state += "\nStorage Capacity: " + this.fileManager.getMaxStorage();

        long usedStorage = this.fileManager.getMaxStorage() - this.fileManager.getRemainingStorage();

        state += "\nStorage used by Backed Up Files: " + usedStorage;

        return state;
    }
}


class ShutdownThread extends Thread{
    private Registry registry;
    private String name;

    public ShutdownThread(Registry registry, String name) {
        this.registry = registry;
        this.name = name;
    }

    @Override
    public void run() {
        Peer.getInstance().getFileManager().shutdown();

        try {
            registry.unbind(name);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
}
