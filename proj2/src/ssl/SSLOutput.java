package ssl;

import application.ApplicationOutput;
import application.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SSLOutput extends ApplicationOutput {
    private static final boolean ACTIVE = true;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static FileOutputStream logFile = null;

    private static OutputStream getLog() throws IOException {
        if (Server.getInstance() == null)
            return null;
        if (logFile != null)
            return logFile;
        String folder = Server.getInstance().getFolder();
        File log = new File(folder + "/ssl.log");
        if (log.exists())
            log.delete();
        log.getParentFile().mkdirs();
        log.createNewFile();
        logFile = new FileOutputStream(log, true);
        return logFile;
    }

    private static void writeToLog(String toWrite) {
        try {
            OutputStream file = getLog();
            if (file == null)
                return;
            file.write(dtf.format(LocalDateTime.now()).getBytes());
            file.write((" " + toWrite + "\n").getBytes());
        } catch (IOException exception) {
            // Ignore
        }

    }

    public static void print(String message) {
        if (ACTIVE)
            writeToLog(message);
    }

    public static void warn(String message) {
        if (ACTIVE)
            writeToLog("[WARNING] " + message);
    }
}
