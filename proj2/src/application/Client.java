package application;

import service.client.message.ClientServiceMessage;
import ssl.application.SSLClient;
import ssl.data.SSLMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client extends SSLClient implements Application {
    private final ClientServiceMessage message;

    public Client(String addressString, List<String> arguments) throws Exception {
        super(stringToAddress(addressString));
        message = ClientServiceMessage.fromArguments(arguments);
    }

    public static InetSocketAddress stringToAddress(String addressString) {
        String[] addressComponents = addressString.split(":");
        return new InetSocketAddress(addressComponents[0], Integer.parseInt(addressComponents[1]));
    }

    private static List<String> arrayToList(String[] array) {
        List<String> result = new ArrayList<>();
        for (String arg : array)
            if (!arg.equals(""))
                result.add(arg);
        return result;
    }

    @Override
    public SSLMessage receive() throws InterruptedException, IOException, ClassNotFoundException {
        SSLMessage message = null;
        for (int i = 0; i < 10; i++) {
            message = super.receive();
            if (message != null)
                break;
            Thread.sleep(50);
        }
        return message;
    }

    @Override
    public void run() {
        try {
            connect();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        try {
            SSLMessage received = null;
            for (int i = 0; i < 10; i++) {
                send(message);
                received = receive();
                if (received != null)
                    break;
                Thread.sleep(50);
            }
            System.out.println("Server response:\n" + received);
        } catch (IOException | InterruptedException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }

        try {
            shutdown();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        List<String> arguments = arrayToList(args);

        if (arguments.size() < 2)
            throw new ArgumentException("Invalid number of arguments, at least two are necessary");

        Application.setKeys("client");
        Client c = new Client(arguments.get(0), arguments.subList(1, arguments.size()));
        c.run();
    }
}
