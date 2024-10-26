public class Particle {
    
    public static final int FRAME_COOLDOWN = 15;
    
    public double x;
    public double y;
    public double dx;
    public double dy;
    public ParticleType type;
    public int framesSinceChange;

    public Particle(double x, double y, double theta, ParticleType type) {
        this.x = x;
        this.y = y;
        this.dx = Math.cos(theta) * type.getDefaultSpeed();
        this.dy = Math.sin(theta) * type.getDefaultSpeed();
        this.type = type;
        this.framesSinceChange = FRAME_COOLDOWN;
    }

    public Particle(double x, double y, ParticleType type) {
        this(x, y, 0, type);
    }

    public void setType(ParticleType type) {
        this.type = type;
        this.framesSinceChange = 0;
    }

    public double getRadius() {
        return type.getRadius();
    }

    public void tick() {
        this.framesSinceChange++;
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