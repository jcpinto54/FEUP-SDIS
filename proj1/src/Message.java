import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class Message {
    private static final String CRLF = "\r\n";

    private String version;
    private String messageType;
    private int senderID;
    private String fileID;
    private long chunkNum;
    private int replicationDegree;
    private byte[] data;

    // Version 2.0
    private int bootedID;

    public Message(String version, String messageType, int senderID, ChunkID chunkID, int replicationDegree, byte[] data) {
        this.version = version;
        this.messageType = messageType;
        this.senderID = senderID;
        this.fileID = chunkID.getFileID();
        this.chunkNum = chunkID.getChunkNum();
        this.replicationDegree = replicationDegree;
        this.data = data;
        this.bootedID = -1;
    }

    // Version 2.0
    public Message(String version, String messageType, int senderID, byte[] data, int bootedID) {
        this.version = version;
        this.messageType = messageType;
        this.senderID = senderID;
        this.fileID = null;
        this.chunkNum = -1;
        this.replicationDegree = -1;
        this.data = data;
        this.bootedID = bootedID;
    }


    public Message(byte[] messageBytes) throws MessageException {
        String messageStr = new String(messageBytes);
        String[] messageStrSplit = messageStr.split(" ");
        int j = 0;
        try {
            // Handle Multiple Spaces between header info
            for (int i = 0; i < messageStrSplit.length && j < 7; i++) {
                if (messageStrSplit[i].length() == 0) {
                    continue;
                }
                switch (j) {
                    // version
                    case 0 : this.version = messageStrSplit[i]; break;
                    // message type
                    case 1 :
                        this.messageType = messageStrSplit[i];
                        if (!this.checkMessageType())
                            throw new MessageException("Wrong message type");
                        break;
                    // sender ID
                    case 2 : this.senderID = Integer.parseInt(messageStrSplit[i]); break;
                    // file ID
                    case 3 :
                        if (peerAndMessageAreV2()) j++;
                        else {
                            if (messageStrSplit[i].length() != 64) throw new MessageException("FileID is not a SHA256");
                            this.fileID = messageStrSplit[i];
                            break;
                        }
                    // chunk Num
                    case 4 :
                        if (this.getMessageType().equals("DELETE") || peerAndMessageAreV2()) { j++; }
                        else {
                            this.chunkNum = Integer.parseInt(messageStrSplit[i]);
                            break;
                        }
                    // replication degree
                    case 5 :
                        if (!this.getMessageType().equals("PUTCHUNK")) { j++; }
                        else {
                            this.replicationDegree = Integer.parseInt(messageStrSplit[i]);
                            break;
                        }
                    // CRLF and next header line
                    case 6 :
                        if (this.getVersion().equals("1.0")) {
                            if (!(messageStrSplit[i].charAt(0) == '\r' && messageStrSplit[i].charAt(1) == '\n' && messageStrSplit[i].charAt(2) == '\r' && messageStrSplit[i].charAt(3) == '\n'))
                                throw new MessageException("No double CRLF at the end of header found");
                        } else if (this.getVersion().equals("2.0")) {
                            if (messageStrSplit[i].charAt(0) == '\r' && messageStrSplit[i].charAt(1) == '\n' && messageStrSplit[i].charAt(2) == '\r' && messageStrSplit[i].charAt(3) == '\n') {
                                break;
                            }

                            if (getMessageType().equals("BOOT")) throw new MessageException("MESSAGE: BOOT isn't supposed to have a new line!");

                            if (!(messageStrSplit[i].charAt(0) == '\r' && messageStrSplit[i].charAt(1) == '\n'))
                                throw new MessageException("No double CRLF at the end of header found");

                            messageStrSplit[i] = messageStrSplit[i].replace("\r\n", "");

                            // Parsing the second line
                            int l = 0;
                            for (int k = i; k < messageStrSplit.length && l < 2; k++) {
                                if (messageStrSplit[i].length() == 0) {
                                    continue;
                                }
                                switch (l) {
                                    case 0 :
                                        this.bootedID = Integer.parseInt(messageStrSplit[i]);
                                        break;
                                    case 1 :
                                        if (!(messageStrSplit[i].charAt(0) == '\r' && messageStrSplit[i].charAt(1) == '\n' && messageStrSplit[i].charAt(2) == '\r' && messageStrSplit[i].charAt(3) == '\n'))
                                            throw new MessageException("No double CRLF at the end of header found");
                                        break;
                                }
                                l++; i++;
                            }
                        }
                        break;
                }
                j++;
            }
        } catch (NumberFormatException e) {
            throw new MessageException("Something was not an int");
        }

        if (j != 7) {
            throw new MessageException("Message isn't complete");
        }

        if (getMessageType().equals("PUTCHUNK") || getMessageType().equals("CHUNK") || this.version.equals("2.0") && getMessageType().equals("DEGREES")) {
            int i;
            for (i = 3; i < messageBytes.length; i++)
            {
                if (messageBytes[i-1] == 0xD && messageBytes[i] == 0xA)
                    if (messageBytes[i-3] == 0xD && messageBytes[i-2] == 0xA)
                        break;
            }

            this.data = Arrays.copyOfRange(messageBytes, i+1, messageBytes.length);
        }
        else this.data = null;
    }

    // Create PUTCHUNK/CHUNK messages
    public static Message createMessage(String type, Chunk chunk) throws MessageException {
        if (!type.equals("PUTCHUNK") && !type.equals("CHUNK")) throw new MessageException("Wrong message type");
        return new Message(Peer.getInstance().getVersion(), type, Peer.getInstance().getId(), chunk.getId(), chunk.getDesiredReplicationDegree(), chunk.getData());
    }

    // Create STORED/GETCHUNK/REMOVED messages
    public static Message createMessage(String type, ChunkID chunkID) throws MessageException {
        if (!type.equals("STORED") && !type.equals("GETCHUNK") && !type.equals("REMOVED")) throw new MessageException("Wrong message type");
        return new Message(Peer.getInstance().getVersion(), type, Peer.getInstance().getId(), chunkID, -1, null);
    }

    // Create DELETE message
    public static Message createMessage(String fileID) {
        return new Message(Peer.getInstance().getVersion(), "DELETE", Peer.getInstance().getId(), new ChunkID(fileID, -1), -1, null);
    }

    // Version 2.0  -  Create BOOT message
    public static Message createMessagev2() throws MessageException {
        if (!Peer.getInstance().getVersion().equals("2.0")) throw new MessageException("This peer needs to be version 2.0 to use this message!");

        return new Message(Peer.getInstance().getVersion(), "BOOT", Peer.getInstance().getId(), null, -1);
    }

    // Version 2.0  -  Create DEGREES message
    public static Message createMessagev2(int bootedID) throws MessageException {
        if (!Peer.getInstance().getVersion().equals("2.0")) throw new MessageException("This peer needs to be version 2.0 to use this message!");

        StringWriter writer = new StringWriter();

        Iterator it = Peer.getInstance().getFileManager().getActualReplicationDegrees().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ChunkID key = (ChunkID) pair.getKey();
            int value = (int) pair.getValue();
            int desiredRepDegree = Peer.getInstance().getFileManager().getDesiredReplicationDegree(key);
            writer.write(key.toString() + " - " + value + " / " + desiredRepDegree + "\n");
        }
        writer.flush();

        byte[] data = writer.toString().getBytes();

        return new Message(Peer.getInstance().getVersion(), "DEGREES", Peer.getInstance().getId(), data, bootedID);
    }

    public static String getCRLF() {
        return CRLF;
    }

    public int getBootedID() {
        return bootedID;
    }

    public boolean checkMessageType() {
        if (peerAndMessageAreV2()) return true;
        return this.getMessageType().equals("PUTCHUNK") || this.getMessageType().equals("STORED") || this.getMessageType().equals("GETCHUNK") || this.getMessageType().equals("CHUNK") || this.getMessageType().equals("DELETE") || this.getMessageType().equals("REMOVED");
    }

    // Version 2.0
    private boolean peerAndMessageAreV2() {
        return Peer.getInstance().getVersion().equals("2.0") && this.getVersion().equals("2.0") && (this.getMessageType().equals("BOOT") || this.getMessageType().equals("DEGREES"));
    }

    public static boolean checkVersion(String version) {
        int dotsAppeared = 0, numsAppeared = 0;
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (!('0' <= c && c <= '9' || c == '.')) {
                return false;
            }
            if (c != '.')
                numsAppeared++;
            else
                dotsAppeared++;

            if (numsAppeared == 0 && c == '.') {
                return false;
            }
            if (dotsAppeared == 2) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        switch (this.getMessageType()) {
            case "DELETE":
                return this.getVersion() + " " + this.getMessageType() + " " + this.getSenderID() + " " + this.getFileID() + " " + Message.getCRLF() + Message.getCRLF();
            case "CHUNK":
            case "STORED":
            case "GETCHUNK":
            case "REMOVED":
                return this.getVersion() + " " + this.getMessageType() + " " + this.getSenderID() + " " + this.getFileID() + " " + this.getChunkNum()
                        + " " + Message.getCRLF() + Message.getCRLF();
            case "PUTCHUNK":
                 return this.getVersion() + " " + this.getMessageType() + " " + this.getSenderID() + " " + this.getFileID() + " " + this.getChunkNum() + " " +
                        this.getReplicationDegree() + " " + Message.getCRLF() + Message.getCRLF();
        }
        if (Peer.getInstance().getVersion().equals("2.0") && this.getVersion().equals("2.0")) {
            switch (this.getMessageType()) {
                case "BOOT":
                    return this.getVersion() + " " + this.getMessageType() + " " + this.getSenderID() + " " + Message.getCRLF() + Message.getCRLF();
                case "DEGREES":
                    return this.getVersion() + " " + this.getMessageType() + " " + this.getSenderID() +  " " + Message.getCRLF() + this.bootedID + " " + Message.getCRLF() + Message.getCRLF();
            }
        }
        return "";
    }

    public byte[] getBytes() {
        String str = this.toString();
        if (getMessageType().equals("PUTCHUNK") || getMessageType().equals("CHUNK") ||
                Peer.getInstance().getVersion().equals("2.0") && this.getVersion().equals("2.0") && getMessageType().equals("DEGREES")) {
            int fal = str.length();
            int sal = this.getData().length;
            byte[] result = new byte[fal + sal];
            System.arraycopy(str.getBytes(), 0, result, 0, fal);
            System.arraycopy(this.getData(), 0, result, fal, sal);
            return result;
        }

        return this.toString().getBytes();
    }

    public String getVersion() {
        return version;
    }

    public String getMessageType() {
        return messageType;
    }

    public int getSenderID() {
        return senderID;
    }

    public String getFileID() {
        return fileID;
    }

    public long getChunkNum() {
        return chunkNum;
    }

    public ChunkID getChunkID() {
        return new ChunkID(fileID, chunkNum);
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public byte[] getData() {
        return data;
    }
}
