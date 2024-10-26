import java.awt.Color;

public enum ParticleType {
    DEPLETED(0, 20, 0, new Color(147, 161, 173)),
    URANIUM(0, 20, 0, new Color(105, 219, 124)),
    NEUTRON(5, 5, 5, new Color(100, 181, 246)),
    GRAPHITE(0, 20, 0, new Color(72, 75, 81));

    private final double defaultSpeed;
    private final double radius;
    private final double temperatureIncrease;
    private final Color color;

    ParticleType(double defaultSpeed, double radius, double temperatureIncrease, Color color) {
        this.defaultSpeed = defaultSpeed;
        this.radius = radius;
        this.temperatureIncrease = temperatureIncrease;
        this.color = color;
    }

    public double getDefaultSpeed() { return defaultSpeed; }
    public double getRadius() { return radius; }
    public double getTemperatureIncrease() { return temperatureIncrease; }
    public Color getColor() { return color; }
}