import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.sound.sampled.*;

import java.io.File;
import java.io.IOException;

public class Game extends JPanel implements MouseListener, MouseMotionListener, KeyListener {

    public static void playAudioAsync(String filePath) {
        new Thread(() -> {
            try (AudioInputStream audioInput = AudioSystem.getAudioInputStream(new File(filePath))) {
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
                clip.drain();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // public static boolean colliding(Particle a, Particle b) {
    //     double distance = Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    //     return distance < a.getRadius() + b.getRadius();
    // }

    public static boolean closeColliding(Particle a, Particle b) {
        if (a.framesSinceChange < Particle.FRAME_COOLDOWN || b.framesSinceChange < Particle.FRAME_COOLDOWN) 
            return false;
        double distance = Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
        return distance < Math.max(a.getRadius(), b.getRadius());
    }

    private ArrayList<Particle> neutrons;
    private ArrayList<Particle> fuels;
    private double temperature = 0;

    public static final int PREF_W = 800;
    public static final int PREF_H = 600;
    public static final double PI2 = Math.PI * 2;
    public static final double NEUTRON_DECAY_PROB = 0.01;
    public static final double URANIUM_RECAY_PROB = 0.001;
    public static final double DEPLETED_DECAY_PROB = 0.001;
    public static final double IDLE_TEMP_MUL = 0.98;

    public Game() {
        this.setFocusable(true);
        this.setBackground(Color.WHITE);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);

        neutrons = new ArrayList<Particle>();
        fuels = new ArrayList<Particle>();

        // Create initial stray neutrons
        for (int i = 0; i < 3; i++) {
            double x = Math.random() * PREF_W;
            double y = Math.random() * PREF_H;
            double theta = Math.random() * PI2;
            neutrons.add(new Particle(x, y, theta, ParticleType.NEUTRON));
        }

        // Create a grid of uranium particles
        double rows = PREF_W / 50;
        double cols = PREF_H / 50;
        double cellWidth = PREF_W / rows;
        double cellHeight = PREF_H / cols;
        for (int xIdx = 0; xIdx < rows; xIdx++) {
            for (int yIdx = 1; yIdx < cols; yIdx++) {
                double x = xIdx * cellWidth + 0.5 * cellWidth;
                double y = yIdx * cellHeight + 0.5 * cellHeight;

                ParticleType type = Math.random() < 0.3 ? ParticleType.URANIUM : ParticleType.DEPLETED;
                fuels.add(new Particle(x, y, type));
            }
        }

        t.start();
    }

    private Timer t = new Timer(1000 / 60, (e) -> {
        temperature *= IDLE_TEMP_MUL;

        // Handle fuel transitions
        for (int i = 0; i < fuels.size(); i++) {
            Particle fuel = fuels.get(i);
            fuel.tick();

            // Chance to turn Depleted to Uranium
            if (fuel.type == ParticleType.DEPLETED && Math.random() < URANIUM_RECAY_PROB) {
                fuel.setType(ParticleType.URANIUM);
            }

            // Chance to turn Depleted to Graphite
            else if (fuel.type == ParticleType.DEPLETED && Math.random() < DEPLETED_DECAY_PROB) {
                fuel.setType(ParticleType.GRAPHITE);
                neutrons.add(new Particle(fuel.x, fuel.y, Math.random() * PI2, ParticleType.NEUTRON));
            }
        }

        boolean click = false;
        for (int i = 0; i < neutrons.size(); i++) {
            Particle neutron = neutrons.get(i);
            neutron.tick();

            // Handle neutron decay
            if (Math.random() < NEUTRON_DECAY_PROB) {
                neutrons.remove(i);
                i--;
                temperature += ParticleType.NEUTRON.getTemperatureIncrease();
                continue;
            }

            neutron.move();
            neutron.bounceBounds(0, PREF_W, 0, PREF_H);

            // Check for collisions with fuel particles
            searchForNeutronLoop: for (int j = 0; j < fuels.size(); j++) {
                Particle fuel = fuels.get(j);

                // Handle uranium collision
                if (fuel.type == ParticleType.URANIUM && closeColliding(neutron, fuel)) {
                    click = true;
                    neutrons.remove(i);
                    i--;

                    // Create three new neutrons
                    for (int k = 0; k < 3; k++) {
                        neutrons.add(new Particle(neutron.x, neutron.y, Math.random() * PI2, ParticleType.NEUTRON));
                    }

                    fuel.setType(ParticleType.DEPLETED);
                    break searchForNeutronLoop;
                }
                // Handle graphite collision
                else if (fuel.type == ParticleType.GRAPHITE && closeColliding(neutron, fuel)) {
                    click = true;
                    neutrons.remove(i);
                    i--;
                    fuel.setType(ParticleType.DEPLETED);
                    break searchForNeutronLoop;
                }
            }
        }

        if (click) {
            playAudioAsync("src/geiger.wav");
        }

        repaint();
    });

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        this.setBackground(new Color(240, 244, 248));

        int dCount = 0;
        int uCount = 0;
        int gCount = 0;

        for (Particle p : fuels) {
            g2.setColor(p.type.getColor());

            // Update counters based on particle type
            if (p.type == ParticleType.DEPLETED) {
                dCount++;
            } else if (p.type == ParticleType.URANIUM) {
                uCount++;
            } else if (p.type == ParticleType.GRAPHITE) {
                gCount++;
            }

            double x = p.x - p.getRadius() / 2;
            double y = p.y - p.getRadius() / 2;
            g2.fillOval((int) x, (int) y, (int) p.getRadius(), (int) p.getRadius());
        }

        g2.setColor(ParticleType.NEUTRON.getColor());
        for (Particle p : neutrons) {
            double x = p.x - p.getRadius() / 2;
            double y = p.y - p.getRadius() / 2;
            g2.fillOval((int) x, (int) y, (int) p.getRadius(), (int) p.getRadius());
        }

        g2.setColor(ParticleType.DEPLETED.getColor());
        g2.drawString(
                "N:" + neutrons.size() + ",F:" + fuels.size() + "[D:" + dCount + ",U:" + uCount + ",G:" + gCount + "]",
                10, 20);
        g2.drawString("T:" + String.format("%.3f", temperature), 10, 35);
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    /* METHODS FOR CREATING JFRAME AND JPANEL */

    public Dimension getPreferredSize() {
        return new Dimension(PREF_W, PREF_H);
    }

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("You're Mother");
        JPanel gamePanel = new Game();

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
