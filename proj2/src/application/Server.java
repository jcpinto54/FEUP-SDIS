package application;

import chord.CHORDPeer;
import chord.message.CHORDMessage;
import service.Service;
import service.client.ClientRequestAction;
import service.client.message.ClientServiceMessage;
import service.peer.PeerRequestAction;
import service.peer.message.PeerServiceMessage;
import ssl.SSLConnection;
import ssl.SSLOutput;
import ssl.application.SSLClient;
import ssl.data.SSLMessage;
import storage.Storage;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends CHORDPeer<SSLClient> implements Application {
    private static final String DATA_FOLDER = "../data";
    private Storage storage = new Storage();
    private static Server instance = null;
    private final Set<Service> servicesRunning = ConcurrentHashMap.newKeySet();

    private Server(InetSocketAddress hostAddress, Selector selector, int knownPeerKey, InetSocketAddress knownPeerAddress) throws Exception {
        super(hostAddress, selector, knownPeerKey, knownPeerAddress);
    }

    private Server(InetSocketAddress hostAddress, Selector selector) throws Exception {
        super(hostAddress, selector);
    }

    private SSLMessage processMessage(SSLConnection connection, SSLMessage message) throws IllegalAccessException {
        Service c = null;

        if (message instanceof ClientServiceMessage) {
            c = ClientRequestAction.fromMessage(connection, (ClientServiceMessage) message);
        } else if (message instanceof PeerServiceMessage) {
            PeerServiceMessage peerMessage = (PeerServiceMessage) message;
            c = PeerRequestAction.fromMessage(peerMessage);

            for (Service process : servicesRunning) {
                process.processMessage(peerMessage);
            }
        }

        if (c != null)
            execute(c);

        return null;
    }

    public static void removeFromServicesRunning(Service service) {
        instance.servicesRunning.remove(service);
    }

    public static void execute(Service service) {
        instance.servicesRunning.add(service);
        instance.executor.execute(service);
    }

    @Override
    public void respond(SSLMessage sslMessage, SSLConnection connection) throws IOException, IllegalAccessException {
        if (sslMessage == null)
            return;
        List<SSLMessage> messages = SSLMessage.parseMessages(sslMessage.getContent());
        for (SSLMessage message : messages) {
            SSLMessage response;
            if (message instanceof CHORDMessage)
                response = processCHORDMessage((CHORDMessage) message);
            else
                response = processMessage(connection, message);

            if (response == null)
                continue;
            SSLOutput.print("Sending response");
            if (write(connection, response))
                SSLOutput.print("- Sent \"" + response + "\"");
            else
                SSLOutput.print("- Fail");
        }
    }

    public static void main(String[] args) throws Exception {
        int key = 2;
        /*args = key == 0?
                new String[]{
                        "localhost:5000"
                }:
                new String[]{
                "localhost:"+(5000+key),
                "0",
                "localhost:5000"
        };*/

        try {
            createInstance(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // getInstance().stop();
                getInstance().closeAllConnections();
                getInstance().save();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }));

        getInstance().run();
    }

    private void loadStorage() throws IOException, ClassNotFoundException {
        File file = new File(DATA_FOLDER + "/" + address.getPort() + "/storage.save");
        if (!file.exists())
            return;

        ObjectInputStream stream = new ObjectInputStream(new FileInputStream(file));
        this.storage = (Storage) stream.readObject();
    }

    public void save() throws IOException {
        File file = new File(getFolder() + "/storage.save");
        ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
        stream.writeObject(Server.getStorage());
    }

    public static void createInstance(String[] args) throws Exception {
        //application myaddress=localhost:5000 knownAddress=localhost:5000
        InetSocketAddress knownAddress = new InetSocketAddress("localhost", 5000);
        if (args.length == 1)
            instance = new Server(knownAddress, SelectorProvider.provider().openSelector());
        else if (args.length == 3)
            instance = new Server(Client.stringToAddress(args[0]), SelectorProvider.provider().openSelector(), Integer.parseInt(args[1]), Client.stringToAddress(args[2]));
        else
            throw new Exception("Usage: Application host:port [knownKey knowHost:knownPort]");
        instance.loadStorage();
    }

    public static Server getInstance() {
        return instance;
    }

    public static Storage getStorage() {
        return instance.storage;
    }

    public String getFolder() {
        return DATA_FOLDER + "/" + address.getPort();
    }
}
