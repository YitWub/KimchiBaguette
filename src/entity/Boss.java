package entity;

import java.awt.Color;

import engine.DrawManager.SpriteType;

/**
 * Implements the boss, a strong enemy that moves left and right
 * and changes phase as its health goes down.
 *
 * Boss Developer 1: structure, health, damage, state, movement, phase, defeat.
 * Boss Developer 2: uses the getters below for attacks and events.
 */
public class Boss extends Entity {

    /** States the boss can be in. */
    public enum State {
        IDLE, MOVING, ATTACKING, HIT, DEFEATED
    }

    /** Kinds of bosses and their base values. */
    public enum BossType {
        /** Slow boss. */
        BOSS_A(1000, 1),
        /** Fast boss. */
        BOSS_B(800, 3);

        /** Maximum health of this kind of boss. */
        private final int maxHealth;
        /** Base horizontal speed in pixels per frame. */
        private final int speed;

        BossType(final int maxHealth, final int speed) {
            this.maxHealth = maxHealth;
            this.speed = speed;
        }

        public int getMaxHealth() {
            return this.maxHealth;
        }

        public int getSpeed() {
            return this.speed;
        }
    }

    /** Phase 2 starts below this share of max health. */
    private static final double PHASE_2_THRESHOLD = 0.6;
    /** Phase 3 starts below this share of max health. */
    private static final double PHASE_3_THRESHOLD = 0.3;

    /** Kind of this boss. */
    private final BossType type;
    /** Maximum health of the boss. */
    private final int maxHealth;
    /** Current health of the boss. */
    private int currentHealth;
    /** Horizontal speed in pixels per frame. */
    private int speed;
    /** Moving direction: 1 is right, -1 is left. */
    private int direction;
    /** Current state of the boss. */
    private State state;
    /** Current phase: 1, 2 or 3. */
    private int phase;

    /**
     * Constructor, creates the boss and resets its health and state.
     *
     * @param positionX
     *            Initial position of the boss in the X axis.
     * @param positionY
     *            Initial position of the boss in the Y axis.
     * @param type
     *            Kind of boss to create.
     */
    public Boss(final int positionX, final int positionY, final BossType type) {
        // Temporarily reuses the bonus ship sprite (16 x 7 cells, 2px each).
        super(positionX, positionY, 16 * 2, 7 * 2, Color.MAGENTA);
        this.spriteType = SpriteType.EnemyShipSpecial;

        this.type = type;
        this.maxHealth = type.getMaxHealth();
        this.currentHealth = this.maxHealth;
        this.speed = type.getSpeed();
        this.direction = 1;
        this.state = State.MOVING;
        this.phase = 1;
    }

    /**
     * Moves the boss and turns it around at the borders of the play area.
     * Call once per frame.
     *
     * @param leftLimit
     *            Smallest X position the boss may reach.
     * @param rightLimit
     *            Largest X position the right edge of the boss may reach.
     */
    public final void update(final int leftLimit, final int rightLimit) {
        if (this.state == State.DEFEATED)
            return;

        this.positionX += this.speed * this.direction;

        if (this.positionX < leftLimit) {
            this.positionX = leftLimit;
            this.direction = 1;
        } else if (this.positionX + this.width > rightLimit) {
            this.positionX = rightLimit - this.width;
            this.direction = -1;
        }
    }

    /**
     * Applies damage from a player attack.
     *
     * To detect events, compare getPhase() before and after this call
     * (phase changed) and check isDefeated() after it (defeated).
     *
     * @param damage
     *            Amount of health to remove.
     */
    public final void takeDamage(final int damage) {
        // TODO (Dev 1): ignore if already defeated.
        // TODO (Dev 1): lower currentHealth, keep it between 0 and maxHealth.
        // TODO (Dev 1): call updatePhase().
        // TODO (Dev 1): if health is 0, call defeat().
    }

    /**
     * Updates the phase from the current health ratio.
     * Each threshold is applied only once and the phase never goes back.
     */
    private void updatePhase() {
        // TODO (Dev 1): below PHASE_2_THRESHOLD -> phase 2,
        // below PHASE_3_THRESHOLD -> phase 3.
        // TODO (Dev 1): raise this.speed when the phase goes up.
    }

    /**
     * Marks the boss as defeated and stops its movement.
     */
    private void defeat() {
        // TODO (Dev 1): set state to DEFEATED, show explosion sprite.
    }

    public final BossType getType() {
        return this.type;
    }

    public final int getMaxHealth() {
        return this.maxHealth;
    }

    public final int getCurrentHealth() {
        return this.currentHealth;
    }

    public final int getSpeed() {
        return this.speed;
    }

    public final int getDirection() {
        return this.direction;
    }

    public final State getState() {
        return this.state;
    }

    /** Lets the attack system mark the boss as attacking or moving. */
    public final void setState(final State state) {
        if (this.state != State.DEFEATED)
            this.state = state;
    }

    /** @return Current phase: 1, 2 or 3. */
    public final int getPhase() {
        return this.phase;
    }

    /** @return True when the boss has been defeated. */
    public final boolean isDefeated() {
        return this.state == State.DEFEATED;
    }
}