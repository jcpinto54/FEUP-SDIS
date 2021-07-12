package chord;

import application.ApplicationOutput;
import application.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CHORDOutput extends ApplicationOutput {

    private static OutputStream getInfoFile() throws IOException {
        if (Server.getInstance() == null)
            return System.out;
        String folder = Server.getInstance().getFolder();
        File file = new File(folder + "/chord.info");
        file.getParentFile().mkdirs();
        file.createNewFile();
        return new FileOutputStream(file, false);
    }

    public static void printInfo() {
        Server server = Server.getInstance();
        if (server == null)
            return;
        try {
            getInfoFile().write(server.getInfo().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
