import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class LogManager {

    File logFile;

    public LogManager(File logFile) {
        this.logFile = logFile;
    }

    public void clear() {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(this.logFile, false))) {
        } catch (FileNotFoundException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public void println(String s) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(this.logFile, true))) {
            writer.println(s);
        } catch (FileNotFoundException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public void print(String s) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(this.logFile, true))) {
            writer.print(s);
        } catch (FileNotFoundException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
}
