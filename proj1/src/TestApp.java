import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) {
            System.err.println("Usage: TestApp <peer_access_point> <operation> (<operand1> <operand2>)");
            System.exit(1);
        } else if (args[1].equals("BACKUP") && args.length != 4) {
            System.err.println("Usage: TestApp <peer_access_point> <operation> (<operand1> <operand2>)");
            System.exit(1);
        } else if ((args[1].equals("DELETE") || args[1].equals("RESTORE") || args[1].equals("RECLAIM")) && args.length != 3) {
            System.err.println("Usage: TestApp <peer_access_point> <operation> (<operand1> <operand2>)");
            System.exit(1);
        } else if (args[1].equals("STATE") && args.length != 2) {
            System.err.println("Usage: TestApp <peer_access_point> <operation> (<operand1> <operand2>)");
            System.exit(1);
        }

        if (args[1].equals("RECLAIM")) {
            try {
                Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: TestApp <peer_access_point> <operation> (<operand1> <operand2>)");
                System.exit(1);
            }
        }

        String rmiName = args[0], operation = args[1];

        try {
            Registry registry = LocateRegistry.getRegistry();
            RemotePeer stub = (RemotePeer) registry.lookup(rmiName);

            switch(operation)
            {
                case "BACKUP":
                    int replicationDegree = Integer.parseInt(args[3]);
                    stub.backupRequest(args[2], replicationDegree);
                    break;

                case "DELETE":
                    stub.deleteRequest(args[2]);
                    break;

                case "RESTORE":
                    stub.restoreRequest(args[2]);
                    break;

                case "RECLAIM":
                    long maxStorage = Long.parseLong(args[2]);
                    stub.reclaimRequest(maxStorage);
                    break;

                case "STATE":
                    String state = stub.stateRequest();
                    System.out.println(state);
                    break;
            }

        } catch (Exception e) {
            System.err.println("TestApp exception: " + e.toString());
            e.printStackTrace();
        }
    }
}