import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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

public class Simulation extends JPanel implements MouseListener, MouseMotionListener, KeyListener {

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

    public static boolean closeColliding(Particle a, Particle b) {
        if (a.framesSinceChange < Particle.FRAME_COOLDOWN || b.framesSinceChange < Particle.FRAME_COOLDOWN) 
            return false;
        double distance = Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
        return distance < Math.max(a.getRadius(), b.getRadius());
    }

    private ArrayList<Particle> neutrons;
    private ArrayList<Particle> fuels;
    private ArrayList<Rectangle> graphiteBlocks;
    
    private double temperature = 0;
    private int screenWidth;
    private int screenHeight;

    // FLAGS
    private static boolean ENABLE_CHAIN_REACTION_TRACE = false; // enable the lines showing chain reactions
    private static boolean ENABLE_DEBUG_STATS = true; // enable temperature, and particle count stats
    private static boolean ENABLE_GEIGER_CLICK = true; // enable geiger click sound playing

    // CONSTANTS
    private static final int INITIAL_STRAY_NEUTRON_COUNT = 10;
    private static final long CHAIN_REACTION_LIFETIME = 3000; // Lifespan in ms of chain reaction lines
    private static final int MAX_LINE_ALPHA = 100; // Maximum opacity for reaction lines
    private static final double PI2 = Math.PI * 2; // 2π
    private static final double NEUTRON_DECAY_PROB = 0.01; // Probability for a neutron to decay and increase ambient temperature
    private static final double DEPLETED_RECAY_PROB = 0.001; // Probability for a depleted particle to turn back to uranium (simulate refueling)
    private static final double DEPLETED_DECAY_PROB = 0.001; // Probability for a depleted particle to turn to graphite
    private static final double IDLE_TEMP_MUL = 0.98; // The temperature gets multiplied by this every frame

    private void initializeParticleGrid(int numParticles, double spacing, double uraniumChance) {
        // Calculate grid dimensions to make it as square as possible
        int cols = (int) Math.ceil(Math.sqrt(numParticles));
        int rows = (int) Math.ceil(numParticles / (double) cols);

        // Calculate starting position to center the grid
        double startX = (screenWidth - (cols - 1) * spacing) / 2;
        double startY = (screenHeight - (rows - 1) * spacing) / 2;

        int particlesCreated = 0;

        for (int row = 0; row < rows && particlesCreated < numParticles; row++) {
            for (int col = 0; col < cols && particlesCreated < numParticles; col++) {
                double x = startX + col * spacing;
                double y = startY + row * spacing;

                ParticleType type = Math.random() < uraniumChance ? ParticleType.URANIUM : ParticleType.DEPLETED;
                fuels.add(new Particle(x, y, type));
                particlesCreated++;
            }
        }
    }

    public Simulation(int w, int h) {
        this.setFocusable(true);
        this.setBackground(Color.WHITE);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);

        this.screenWidth = w;
        this.screenHeight = h;

        // Create the particle lists
        neutrons = new ArrayList<Particle>();
        fuels = new ArrayList<Particle>();

        // Create initial stray neutrons
        for (int i = 0; i < INITIAL_STRAY_NEUTRON_COUNT; i++) {
            double x = Math.random() * screenWidth;
            double y = Math.random() * screenHeight;
            double theta = Math.random() * PI2;
            neutrons.add(new Particle(x, y, theta, ParticleType.NEUTRON));
        }

        // Create a grid of uranium particles
        initializeParticleGrid(100, 50, 0.3);

        t.start();
    }

    private void simulateDegradation() {

        // Handle fuel transitions
        for (int i = 0; i < fuels.size(); i++) {
            Particle fuel = fuels.get(i);
            fuel.tick();

            // Chance to turn Depleted to Uranium
            if (fuel.type == ParticleType.DEPLETED && Math.random() < DEPLETED_RECAY_PROB) {
                fuel.setType(ParticleType.URANIUM);
            }

            // Chance to turn Depleted to Graphite
            else if (fuel.type == ParticleType.DEPLETED && Math.random() < DEPLETED_DECAY_PROB) {
                fuel.setType(ParticleType.GRAPHITE);
                neutrons.add(new Particle(fuel.x, fuel.y, Math.random() * PI2, ParticleType.NEUTRON));
            }
        }
    }

    private void simulateChemicalReactions() {
        boolean click = false;

        for (int i = 0; i < neutrons.size(); i++) {

            // Simulate the neutron
            Particle neutron = neutrons.get(i);
            neutron.tick();

            // Handle neutron decay
            if (Math.random() < NEUTRON_DECAY_PROB) {
                neutrons.remove(i);
                i--;
                temperature += ParticleType.NEUTRON.getTemperatureIncrease();
                continue;
            }

            // Move the neutron
            neutron.move();
            neutron.bounceBounds(0, screenWidth, 0, screenHeight);

            // Check for collisions with fuel particles
            searchForNeutronLoop: for (int j = 0; j < fuels.size(); j++) {
                Particle fuel = fuels.get(j);

                // Collide with uranium
                if (fuel.type == ParticleType.URANIUM && closeColliding(neutron, fuel)) {
                    click = true;
                    neutrons.remove(i);
                    i--;

                    // Create three new neutrons with parent tracking
                    for (int k = 0; k < 3; k++) {
                        Particle newNeutron = new Particle(neutron.x, neutron.y,
                                Math.random() * PI2, ParticleType.NEUTRON);
                        newNeutron.setParent(neutron);
                        neutrons.add(newNeutron);
                    }

                    fuel.setType(ParticleType.DEPLETED);
                    break searchForNeutronLoop;
                }

                // Collide with graphite
                else if (fuel.type == ParticleType.GRAPHITE && closeColliding(neutron, fuel)) {
                    click = true;
                    neutrons.remove(i);
                    i--;
                    fuel.setType(ParticleType.DEPLETED);
                    break searchForNeutronLoop;
                }
            }
        }
        
        // Play the click sound
        if (ENABLE_GEIGER_CLICK && click) {
            playAudioAsync("src/geiger.wav");
        }
    }

    private Timer t = new Timer(1000 / 10, (e) -> {
        
        // Decrease the temperature
        temperature *= IDLE_TEMP_MUL;

        // Simulate
        simulateDegradation();
        simulateChemicalReactions();

        // Repaint everything
        repaint();
    });

    public Dimension getPreferredSize() {
        return new Dimension(screenWidth, screenHeight);
    }

    private void drawChainReactions(Graphics2D g2) {
        long currentTime = System.currentTimeMillis();

        for (Particle neutron : neutrons) {
            if (neutron.parent != null) {
                // Calculate line age and alpha
                long age = currentTime - neutron.creationTime;
                if (age > CHAIN_REACTION_LIFETIME)
                    continue;

                // Calculate alpha based on age
                int alpha = (int) (MAX_LINE_ALPHA * (1 - (double) age / CHAIN_REACTION_LIFETIME));

                // Draw line with fade effect
                g2.setColor(new Color(255, 165, 0, alpha)); // Orange with fade
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(
                        (int) neutron.x, (int) neutron.y,
                        (int) neutron.parent.x, (int) neutron.parent.y);
            }
        }
    }

    public void paintComponent(Graphics g) {
        
        // Set up painting
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        this.setBackground(new Color(240, 244, 248));

        // Draw chain reactions
        if (ENABLE_CHAIN_REACTION_TRACE) {
            drawChainReactions(g2);
        }

        // Set up counters for stats
        int dCount = 0;
        int uCount = 0;
        int gCount = 0;

        for (Particle p : fuels) {
            g2.setColor(p.type.getColor());

            // Update counters based on particle type
            if (ENABLE_DEBUG_STATS) {
                if (p.type == ParticleType.DEPLETED) {
                    dCount++;
                } else if (p.type == ParticleType.URANIUM) {
                    uCount++;
                } else if (p.type == ParticleType.GRAPHITE) {
                    gCount++;
                }
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

        if (ENABLE_GEIGER_CLICK) {
            g2.setColor(ParticleType.DEPLETED.getColor());
            g2.drawString(
                    "N:" + neutrons.size() + ",F:" + fuels.size() + "[D:" + dCount + ",U:" + uCount + ",G:" + gCount + "]",
                    10, 20);
            g2.drawString("T:" + String.format("%.3f", temperature), 10, 35);
        }
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

    

}
