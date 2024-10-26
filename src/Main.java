import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Main {

    private static final int PREF_W = 800; // Initial window width
    private static final int PREF_H = 600; // Initial window height

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Nuclear Reactor Simulation");
        Simulation gamePanel = new Simulation(PREF_W, PREF_H);

        frame.getContentPane().add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
