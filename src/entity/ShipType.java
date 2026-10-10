package entity;

public enum ShipType {
    STANDARD("Standard", 2, 750, 3, false),
    SPEEDSTER("Speedster", 4, 400, 2, false),
    TANK("Tank", 1, 1000, 5, true);

    private final String name;
    private final int speed;
    private final int shootCooldown;
    private final int maxLives;
    private final boolean hasShield;

    ShipType(String name, int speed, int shootCooldown, int maxLives, boolean hasShield) {
        this.name = name;
        this.speed = speed;
        this.shootCooldown = shootCooldown;
        this.maxLives = maxLives;
        this.hasShield = hasShield;
    }

    public String getName() { return name; }
    public int getSpeed() { return speed; }
    public int getShootCooldown() { return shootCooldown; }
    public int getMaxLives() { return maxLives; }
    public boolean hasShield() { return hasShield; }
}
