public class Particle {
    public double x;
    public double y;
    public double dx;
    public double dy;
    public double r;

    public static final int PARTICLE_TYPE_DEPLETED = 0;
    public static final int PARTICLE_TYPE_URANIUM = 1;
    public static final int PARTICLE_TYPE_NEUTRON = 2;
    public static final int PARTICLE_TYPE_GRAPHITE = 3;
    public int type;

    /**
     * Creates a particle.
     * 
     * @param x     The initial x position
     * @param y     The initial y position
     * @param theta The initial angle (rads) of the particle
     * @param speed The speed of the particle
     * @param r     The radius of the particle
     * @param type  The type of the particle
     */
    public Particle(double x, double y, double theta, double speed, double r, int type) {
        this.x = x;
        this.y = y;
        this.dx = Math.cos(theta) * speed;
        this.dy = Math.sin(theta) * speed;
        this.r = r;
        this.type = type;
    }

    /**
     * Creates a particle that has zero velocity.
     * 
     * @param x    The initial x position
     * @param y    The initial y position
     * @param r    The radius of the particle
     * @param type The type of the particle
     */
    public Particle(double x, double y, double r, int type) {
        this.x = x;
        this.y = y;
        this.dx = 0;
        this.dy = 0;
        this.r = r;
        this.type = type;
    }

    public void move() {
        this.x += dx;
        this.y += dy;
    }

    public void bounceBounds(double minX, double maxX, double minY, double maxY) {
        if (this.x < minX) {
            this.dx = -this.dx;
            this.x = minX;
        } else if (this.x > maxX) {
            this.dx = -this.dx;
            this.x = maxX;
        }
        if (this.y < minY) {
            this.dy = -this.dy;
            this.y = minY;
        } else if (this.y > maxY) {
            this.dy = -this.dy;
            this.y = maxY;
        }
    }

}