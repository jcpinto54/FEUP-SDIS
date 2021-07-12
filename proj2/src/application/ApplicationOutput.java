package application;

public class ApplicationOutput {
    public static void print(String message) {
        System.out.println(message);
    }

    public static void warn(String message) {
        System.err.println(message);
    }
}
