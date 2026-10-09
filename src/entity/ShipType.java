package entity;

/**
 * Represents the different types of player ships with unique gameplay attributes.
 * Part of the Player Ship System requirements.
 */
public enum ShipType {
    // Name, Speed, FireRateCooldown (ms), Lives, ShieldAbility
    STANDARD("Standard Ship", 2, 750, 3, false),
    SPEEDSTER("Speedster", 4, 400, 2, false),
    TANK("Heavy Tank", 1, 1000, 5, true);

    private final String name;
    private final int speed;
    private final int shootCooldown; // Intervalle de tir en ms
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
