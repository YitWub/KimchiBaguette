package entity;

/**
 * Manages player status including lives and damage processing.
 */
public class PlayerStatus {

    private int lives;
    private int maxLives;

    public PlayerStatus(final int initialLives, final int maxLives) {
        this.lives = initialLives;
        this.maxLives = maxLives;
    }

    /**
     * Processes incoming damage and reduces lives.
     *
     * @param damage Amount of damage to apply.
     */
    public void takeDamage(final int damage) {
        if (damage <= 0) {
            return;
        }
        this.lives -= damage;
        if (this.lives < 0) {
            this.lives = 0;
        }
    }

    public int getLives() {
        return this.lives;
    }

    public int getMaxLives() {
        return this.maxLives;
    }

    public boolean isAlive() {
        return this.lives > 0;
    }
}