package chord;

import chord.message.*;
import service.Delay;
import ssl.SSLConnection;
import ssl.SSLOutput;
import ssl.application.SSLClient;
import ssl.application.SSLServer;
import ssl.data.SSLMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class CHORDPeer<C extends SSLClient> extends SSLServer {
    public final static int M_CONSTANT = 4;
    public final static int MAX_NUMBER_OF_KEYS = (int) Math.pow(2, M_CONSTANT);
    private final static int STABILIZATION_INTERVAL = 90;
    private final static int CONNECTION_TRIES = 3;

    private int key;
    protected final InetSocketAddress address;
    private final SortedMap<Integer, InetSocketAddress> addressMap = new ConcurrentSkipListMap<>();
    private final SortedMap<Integer, Integer> proximityMap = new ConcurrentSkipListMap<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------


    public CHORDPeer(InetSocketAddress hostAddress, Selector selector, int knownPeerKey, InetSocketAddress knownPeerAddress) throws Exception {
        super(hostAddress, selector);
        this.setSuccessor(new PeerAddress(knownPeerKey, knownPeerAddress));
        this.address = hostAddress;
    }

    public CHORDPeer(InetSocketAddress hostAddress, Selector selector) throws Exception {
        super(hostAddress, selector);
        this.address = hostAddress;
    }

    public int getKey() {
        return key;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void resetConnection(PeerAddress address) throws Exception {
        closeConnection(address.getKey());
        connect(address);
    }

    public void resetConnection(int key) throws Exception {
        SSLOutput.print("<<<RESETING CONNECTION!!!>>>");
        closeConnection(key);
        connect(key);
    }

    // -------------------------------------------------------------------------
    // Successor and Predecessor
    // -------------------------------------------------------------------------

    public String getInfo() {
        StringBuilder out = new StringBuilder();
        out.append(":::::::::::::::::::::::::::\n\t\tCHORD PEER\n:::::::::::::::::::::::::::\n");
        out.append("KEY:\t").append(this.key).append("\n");
        out.append("ADDRESS:\t").append(this.address.getHostString()).append(":").append(this.address.getPort()).append("\n");
        out.append("PREDECESSOR:\t").append(getPredecessor() == null ? "null" : getPredecessor().getKey()).append("\n");
        out.append("SUCCESSORS:\n");
        for (int key : addressMap.keySet())
            out.append("\t").append(key).append(":\t").append(addressMap.get(key).getHostString()).append(":").append(addressMap.get(key).getPort()).append("\n");
        return out.toString();
    }

    private static int maxKeyMod(int code) {
        return (code % MAX_NUMBER_OF_KEYS + MAX_NUMBER_OF_KEYS) % MAX_NUMBER_OF_KEYS;
    }

    private boolean between(int key, int start, int end) {
        if (start < end)
            return key > start && key < end;
        return key > start || key < end;
    }

    public PeerAddress getSuccessor(int n) {
        Integer mapPosition = proximityMap.getOrDefault(maxKeyMod(n), null);
        if (mapPosition == null)
            return null;
        return getPosition(mapPosition);
    }

    public PeerAddress getSuccessor() {
        return getSuccessor(1);
    }

    public PeerAddress getPredecessor() {
        return getSuccessor(-1);
    }

    public void setSuccessor(int n, PeerAddress address) {
        int realN = maxKeyMod(n);
        if (proximityMap.containsKey(realN)) {
            int oldAddress = proximityMap.getOrDefault(realN, null);

            boolean remove = true;
            for (Integer key : proximityMap.keySet()) {
                if (key != realN && proximityMap.getOrDefault(key, null) == oldAddress) {
                    remove = false;
                    break;
                }
            }

            if (remove)
                addressMap.remove(oldAddress);
        }

        addressMap.put(address.getKey(), address.getAddress());
        proximityMap.put(realN, address.getKey());
        CHORDOutput.printInfo();
    }

    public void setSuccessor(PeerAddress successor) {
        if (getSuccessor() != null && !between(successor.getKey(), key, getSuccessor().getKey()))
            return;
        setSuccessor(1, successor);
        CHORDOutput.print("New successor: " + getSuccessor().getKey());
    }

    public void setPredecessor(PeerAddress predecessor) {
        if (getPredecessor() != null && !between(predecessor.getKey(), getPredecessor().getKey(), key))
            return;
        setSuccessor(-1, predecessor);
        CHORDOutput.print("New predecessor: " + getPredecessor().getKey());
        CHORDOutput.printInfo();
    }

    private void setAsDead(int key) {
        addressMap.remove(key);

        for (int item : proximityMap.keySet())
            if (proximityMap.get(item) == key)
                proximityMap.remove(item);

        CHORDOutput.printInfo();
    }

    // -------------------------------------------------------------------------
    // Messages
    // -------------------------------------------------------------------------

    public PeerAddress getPosition(int key) {
        key = key % MAX_NUMBER_OF_KEYS;

        // Finds the closest preceding peer
        Integer responsible = null;

        if (addressMap.containsKey(key)) {
            responsible = key;
        } else {
            for (int mapKey : addressMap.keySet()) {
                if (mapKey > key) {
                    responsible = mapKey;
                    break;
                }
            }

            if (responsible == null)
                responsible = addressMap.firstKey();
        }

        return new PeerAddress(responsible, addressMap.get(responsible));
    }

    protected CHORDMessage processCHORDMessage(CHORDMessage message) {
        message.process(this);
        return message.getResponse(this);
    }

    // -------------------------------------------------------------------------
    // SSL
    // -------------------------------------------------------------------------

    public void respond(SSLMessage sslMessage, SSLConnection connection) throws IOException, IllegalAccessException {
        if (sslMessage == null)
            return;
        List<SSLMessage> messages = CHORDMessage.parseMessages(sslMessage.getContent());
        for (SSLMessage message : messages) {
            if (!(message instanceof CHORDMessage))
                continue;

            CHORDMessage response = processCHORDMessage((CHORDMessage) message);
            if (response == null)
                continue;
            SSLOutput.print("Sending response");
            if (write(connection, response))
                SSLOutput.print("- Sent \"" + response + "\"");
            else
                SSLOutput.print("- Fail");
        }
    }

    // -------------------------------------------------------------------------
    // Clients
    // -------------------------------------------------------------------------

    private final Map<Integer, C> connections = new HashMap<>();

    private boolean canConnect(PeerAddress address) throws Exception {
        if (address == null)
            return false;
        C client = (C) C.newInstance(address.getAddress());

        boolean connected = false;

        for (int i = 0; i < CONNECTION_TRIES; i++) {
            try {
                connected = client.connect();
            } catch (Exception e) {
                connected = false;
                continue;
            }
            if (connected)
                break;
        }

        return connected;
    }

    private C connect(int key, InetSocketAddress address) throws Exception {
        if (connections.containsKey(key))
            return connections.get(key);

        C client = (C) C.newInstance(address);

        boolean connected = false;

        for (int i = 0; i < CONNECTION_TRIES; i++) {
            try {
                connected = client.connect();
            } catch (Exception e) {
                connected = false;
                continue;
            }
            if (connected)
                break;
        }

        if (!connected) {
            setAsDead(key);
            return null;
        }

        connections.put(key, client);
        SSLOutput.print("Created connection " + key);
        return client;
    }

    private C connect(PeerAddress address) throws Exception {
        if (address == null)
            return null;
        return connect(address.getKey(), address.getAddress());
    }

    private C connect(int key) throws Exception {
        return connect(key, this.addressMap.get(key));
    }

    public void sendToN(int n, SSLMessage message) throws Exception {
        Set<Integer> sentIds = new HashSet<>();

        var randomStream = (new Random()).ints(0, MAX_NUMBER_OF_KEYS).distinct().limit(MAX_NUMBER_OF_KEYS).iterator();
        while (sentIds.size() < n) {
            if (!randomStream.hasNext())
                throw new RuntimeException("Not enough peers exist to keep replication degree");

            int key = findResponsible(randomStream.next()).getKey();
            if (sentIds.contains(key) || key == this.key)
                continue;

            C client = connect(key);
            client.send(message);
            sentIds.add(key);
        }
    }

    public void sendToAll(SSLMessage message) throws Exception {
        Set<Integer> sentIds = new HashSet<>();

        var randomStream = (new Random()).ints(0, MAX_NUMBER_OF_KEYS).distinct().limit(MAX_NUMBER_OF_KEYS).iterator();
        while (randomStream.hasNext()) {
            int key = findResponsible(randomStream.next()).getKey();
            if (sentIds.contains(key) || key == this.key)
                continue;

            C client = connect(key);
            client.send(message);
            sentIds.add(key);
        }
    }

    public boolean send(int key, SSLMessage message) {
        try {
            C client = connect(key);
            client.send(message);
            CHORDOutput.print("Send to " + key + " \"" + message.getContent() + "\"");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean send(InetSocketAddress address, SSLMessage message) {
        try {
            C client = (C) C.newInstance(address);
            boolean connected = client.connect();
            if (!connected)
                throw new Exception("Error connecting to server!");
            client.send(message);
            CHORDOutput.print("Send to " + address + " \"" + message.getContent() + "\"");
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    protected SSLMessage receive(SSLConnection connection) throws IOException {
        SSLMessage sslMessage = super.receive(connection);
        if (sslMessage == null)
            connection.close();
        return sslMessage;
    }

    private List<SSLMessage> receive(int key) throws IOException, InterruptedException, ClassNotFoundException {
        for (int i = 0; i < CONNECTION_TRIES; i++) {
            try {
                SSLMessage message = connections.get(key).receive();
                if (message == null)
                    continue;
                List<SSLMessage> messages = CHORDMessage.fromSSLMessage(message);
                CHORDOutput.print("Received \"" + messages + "\"");
                return messages;
            } catch (Exception e) {
                //Is it dead?
            }
        }
        setAsDead(key);
        return null;
    }

    private SSLMessage receiveFirst(int key) throws IOException, InterruptedException, ClassNotFoundException {
        List<SSLMessage> received = receive(key);
        if (received == null || received.isEmpty())
            return null;
        return received.get(0);
    }

    private SSLMessage sendAndReceiveFirst(int key, CHORDMessage message) throws Exception {
        SSLMessage received = null;

        for (int i = 0; i < 10; i++) {
            if (!send(key, message))
                continue;
            received = receiveFirst(key);
            if (received != null)
                break;
        }

        return received;
    }

    private void closeConnection(int key) throws IOException {
        if (!connections.containsKey(key))
            return;
        connections.get(key).shutdown();
        connections.remove(key);
        SSLOutput.print("Closed connection with " + key);
    }

    public void closeAllConnections() throws IOException {
        while (!connections.isEmpty())
            closeConnection(connections.keySet().iterator().next());
    }

    // -------------------------------------------------------------------------
    // CHORD commands
    // -------------------------------------------------------------------------

    @Override
    protected void start() throws Exception {
        super.start();

        // First peer
        if (addressMap.isEmpty()) {
            this.key = 0;
            CHORDOutput.print("Peer registered with key: " + key);
            setPredecessor(new PeerAddress(key, address));
            setSuccessor(new PeerAddress(key, address));
        } else {
            connect(getSuccessor());

            // Finds a key that is not used
            var randomStream = (new Random()).ints(0, MAX_NUMBER_OF_KEYS).distinct().limit(MAX_NUMBER_OF_KEYS).iterator();
            PeerAddress responsiblePeer;
            do {
                if (!randomStream.hasNext())
                    throw new CHORDException("Maximum number of peers in network reached");
                this.key = randomStream.next();
                responsiblePeer = findResponsible(this.key);
            } while (responsiblePeer.getKey() == this.key);
            CHORDOutput.print("Peer registered with key: " + key);

            // Gets its predecessor
            this.setSuccessor(responsiblePeer);
            int successorKey = getSuccessor().getKey();
            this.setPredecessor((PeerAddress) sendAndReceiveFirst(successorKey, new GetPredecessor()));

            notifySuccessors();
            fixFingers();

            // Close ssl connections
            closeAllConnections();
        }

        addressMap.put(key, address);
        proximityMap.put(0, key);

        Delay.executePeriodically(new Thread(() -> {
            try {
                System.out.println("Stabilizing CHORD");
                SSLOutput.print("Stabilizing CHORD");
                stabilize();
                SSLOutput.print("---------> DONE");
                System.out.println("-> Done");
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }), STABILIZATION_INTERVAL);

        SSLOutput.print("---------- ENTERED CHORD ----------");
    }

    private PeerAddress getFirstSuccessorAlive() throws Exception {
        PeerAddress firstSuccessorAlive = null;

        for (int exp = 0; exp < M_CONSTANT; ++exp) {
            int key = maxKeyMod((int) Math.pow(2, exp));
            PeerAddress aux = getSuccessor(key);
            if (aux == null)
                continue;
            if (canConnect(aux)) {
                if (firstSuccessorAlive == null)
                    firstSuccessorAlive = aux;
                continue;
            }
            setAsDead(maxKeyMod(this.key + key));
        }

        if (firstSuccessorAlive == null) {
            firstSuccessorAlive = getPredecessor();
            if (!canConnect(firstSuccessorAlive))
                throw new Exception("Why is it so dark in here? Am I alone? Does life have a meaning?");
        }
        return firstSuccessorAlive;
    }

    public void stabilize() throws Exception {
        PeerAddress firstSuccessorAlive = getFirstSuccessorAlive();

        setSuccessor(firstSuccessorAlive);
        send(firstSuccessorAlive.getKey(), new GetPredecessor());
        PeerAddress predecessorOfSuccessor = (PeerAddress) receiveFirst(firstSuccessorAlive.getKey());

        if (predecessorOfSuccessor == null) {
            PeerAddress self = new PeerAddress(this.key, this.address);
            send(firstSuccessorAlive.getKey(), new SetPredecessor(self));
            predecessorOfSuccessor = self;
        }
        if (between(predecessorOfSuccessor.getKey(), key, firstSuccessorAlive.getKey())) {
            setSuccessor(predecessorOfSuccessor);
        }

        notifySuccessors();
        fixFingers();
        //notifySuccessors();
        CHORDOutput.printInfo();
    }

    public void notifySuccessors() {
        // notifies n‘s successor of its existence, so it can change its predecessor to n

        if (getPredecessor() != null) {
            int predecessorKey = getPredecessor().getKey();
            send(predecessorKey, new SetSuccessor(this.key, this.address));
        }

        if (getSuccessor() != null) {
            int successorKey = getSuccessor().getKey();
            send(successorKey, new SetPredecessor(this.key, this.address));
        }
    }

    public PeerAddress findResponsible(int key) throws Exception {
        PeerAddress toSend = getFirstSuccessorAlive();
        PeerAddress successor;
        int tries = 0;

        do {
            successor = (PeerAddress) sendAndReceiveFirst(toSend.getKey(), new GetPosition(key));

            if (successor == null) {
                resetConnection(toSend);
                if (++tries > 5) break;
                continue;
            }

            // Received what I wanted
            if (successor.getKey() == key || successor.getKey() == this.key)
                break;

            // If predecessor is smaller than what I wanted, then this is the successor
            connect(successor);
            PeerAddress predecessor = (PeerAddress) sendAndReceiveFirst(successor.getKey(), new GetPredecessor());

            if (predecessor != null && (between(key, predecessor.getKey(), successor.getKey()) || predecessor.equals(successor)))
                break;

            // We will continue asking for the next peer
            toSend = successor;
        } while (true);

        return successor != null ? successor : new PeerAddress(this.key, this.address);
    }

    public void fixFingers() throws Exception {
        // updates finger tables
        for (int i = 2; i < MAX_NUMBER_OF_KEYS; i *= 2) {
            int k = (key + i) % MAX_NUMBER_OF_KEYS;
            PeerAddress successor = findResponsible(k);
            setSuccessor(i, successor);
        }

        PeerAddress p = getPredecessor();
        if (!canConnect(p)) {
            if (p != null)
                setAsDead(p.getKey());
            for (int i = MAX_NUMBER_OF_KEYS - 1; i > 0; i--) {
                int k = maxKeyMod(this.key + i);
                PeerAddress predecessor = findResponsible(k);
                if (predecessor.getKey() != this.key && connect(predecessor) != null) {
                    setPredecessor(predecessor);
                    break;
                }
            }
        }

        // Verify if all the members of addressMap are in proximityMap
        for (int addressKey : addressMap.keySet())
            if (!proximityMap.containsValue(addressKey))
                addressMap.remove(addressKey);
    }
}
