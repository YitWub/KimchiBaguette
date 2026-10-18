package entity;

import engine.Cooldown;
import engine.Core;
import engine.DrawManager.SpriteType;
import java.awt.Color;
import java.util.Set;

/**
 * Implements a ship, to be controlled by the player.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class Ship extends Entity {

    /** Type of this ship, which supplies its speed, fire rate and lives. */
    private final ShipType shipType;

    /** Time between shots, taken from the ship type. */
    private int shootingInterval;

    /** Baseline time between shots, before any buff changes it. */
    private int baseShootingInterval;

    /** Bullet type the ship is currently firing. */
    private BulletType currentBulletType = BulletType.NORMAL;

    /** Bullets still owed by an in-flight RAPID burst. */
    private int remainingShots = 0;

    /** Time the next bullet of a burst may be fired. */
    private long nextShotTime = 0;

    /** Speed of the bullets shot by the ship, from the bullet type. */
    private int bulletSpeed = BulletType.NORMAL.getSpeed();

    /** Movement of the ship for each unit of time, from the ship type. */
    private int speed;

    /** Baseline movement speed, before any buff changes it. */
    private int baseSpeed;

    /** Number of times the current stats have explicitly been restored. */
    private long baseStatsRestoreVersion;

    /** Whether the Hyperfire buff is currently enabled. */
    private boolean hyperfireEnabled = false;

    /** Time of the last shot. */
    private long lastShotTime = 0;

    /** Time spent inactive between hits. */
    private Cooldown destructionCooldown;

    private boolean isShieldActive = false;

    private static final Color[] CHAMELEON_COLORS = {
            Color.CYAN,
            Color.MAGENTA,
            Color.YELLOW,
            Color.ORANGE,
            Color.BLUE
    };

    private boolean chameleonHullActive;
    private long chameleonActivationTime;
    private Color colorBeforeChameleon;
    private double chameleonMovementRemainder;

    /** Whether the player-status slow debuff is affecting ship movement. */
    private boolean slowDebuffActive;
    /** Fractional slow movement retained independently for each direction. */
    private final double[] slowMovementRemainders = new double[4];

    private int widthBeforeChameleon;
    private int heightBeforeChameleon;
    private int normalPositionY;

    /** Duration of the damage flashing feedback in milliseconds (0.5s). */
    private static final int DAMAGE_FEEDBACK_DURATION = 500;
    /** Rate in ms at which the ship color alternates during damage feedback. */
    private static final int FLASH_INTERVAL = 100;
    /** Default normal color of the ship. */
    private static final Color DEFAULT_COLOR = Color.GREEN;

    /** Cooldown tracking the 0.5s damage feedback duration. */
    private Cooldown damageFeedbackCooldown;
    /** Timestamp tracking when the damage effect started. */
    private long damageFeedbackStartTime;

    /**
     * Constructor, establishes the ship's properties.
     *
     * @param positionX
     *            Initial position of the ship in the X axis.
     * @param positionY
     *            Initial position of the ship in the Y axis.
     */
    public Ship(final int positionX, final int positionY) {
        this(positionX, positionY, ShipType.STANDARD);
    }

    /**
     * Constructor, establishes the ship's properties from a selected type.
     *
     * @param positionX
     *            Initial position of the ship in the X axis.
     * @param positionY
     *            Initial position of the ship in the Y axis.
     * @param shipType
     *            Selected ship type, source of speed, fire rate and lives.
     */
    public Ship(final int positionX, final int positionY,
            final ShipType shipType) {
        super(positionX, positionY, 13 * 2, 8 * 2, Color.GREEN);

        if (shipType == null)
            throw new IllegalArgumentException("Ship type cannot be null.");

        this.shipType = shipType;
        this.baseSpeed = shipType.getSpeed();
        this.speed = this.baseSpeed;
        this.baseShootingInterval = shipType.getShootingInterval();
        this.shootingInterval = this.baseShootingInterval;
        this.currentBulletType = shipType.getBulletType();
        this.bulletSpeed = this.currentBulletType.getSpeed();
        this.spriteType = SpriteType.Ship;
        this.destructionCooldown = Core.getCooldown(1000);
        this.damageFeedbackCooldown = Core.getCooldown(DAMAGE_FEEDBACK_DURATION);
    }

    /**
     * Moves the ship speed uni ts right, or until the right screen border is
     * reached.
     */
    public final void moveRight() {
        if (this.slowDebuffActive) {
            this.positionX += getSlowedMovementPixels(0);
            return;
        }
        this.positionX += this.speed;
        addChameleonMovement(1);
    }

    /**
     * Moves the ship speed units left, or until the left screen border is
     * reached.
     */
    public final void moveLeft() {
        if (this.slowDebuffActive) {
            this.positionX -= getSlowedMovementPixels(1);
            return;
        }
        this.positionX -= this.speed;
        addChameleonMovement(-1);
    }

    /** Enables or disables the PlayerStatus-owned slow movement modifier. */
    final void setSlowDebuffActive(final boolean active) {
        if (this.slowDebuffActive != active)
            resetSlowMovementRemainders();
        this.slowDebuffActive = active;
    }

    /** Clears fractional movement after the effective speed changes. */
    final void resetSlowMovementRemainders() {
        for (int i = 0; i < this.slowMovementRemainders.length; i++)
            this.slowMovementRemainders[i] = 0;
    }

    /**
     * Applies half of the current movement distance while retaining fractional
     * pixels per direction. Separate remainders prevent a direction change
     * from converting a fraction in one direction into movement in the other.
     *
     * @param direction Direction slot, kept separate to avoid cross-direction carry.
     * @return Whole pixels to move in this update.
     */
    private int getSlowedMovementPixels(final int direction) {
        double movement = getMovementSpeed() * 0.5
                + this.slowMovementRemainders[direction];
        int wholePixels = (int) Math.floor(movement + 1e-9);
        this.slowMovementRemainders[direction] = movement - wholePixels;
        return wholePixels;
    }

    /**
     * Shoots a bullet upwards.
     *
     * @param bullets
     *            List of bullets on screen, to add the new bullet.
     * @return Number of bullets added to the screen.
     */
    public final int shoot(final Set<Bullet> bullets) {
        if (this.chameleonHullActive) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();

        int interval = this.shootingInterval;

        if (this.hyperfireEnabled) {
            interval = (int) (this.shootingInterval * 0.40);
        }

        if (lastShotTime != 0 && currentTime - lastShotTime < interval) {
            return 0;
        }

        lastShotTime = currentTime;

        int x = this.positionX + this.width / 2;
        int y = this.positionY;

        // A MULTI shot spreads sideways and owes no follow-up bullets.
        if (this.currentBulletType == BulletType.MULTI) {
            int count = 0;
            if (bullets.add(BulletPool.getBullet(x, y, this.bulletSpeed, -2)))
                count++;
            if (bullets.add(BulletPool.getBullet(x, y, this.bulletSpeed, 0)))
                count++;
            if (bullets.add(BulletPool.getBullet(x, y, this.bulletSpeed, 2)))
                count++;
            return count;
        }

        boolean added = bullets.add(
                BulletPool.getBullet(x, y, this.bulletSpeed));

        this.remainingShots = this.currentBulletType.getShotCount() - 1;
        this.nextShotTime = currentTime
                + this.currentBulletType.getShotDelay();

        return added ? 1 : 0;
    }

    /**
     * Fires the bullets still owed by a RAPID burst, one per shot delay.
     *
     * @param bullets
     *            List of bullets on screen, to add the new bullet.
     * @return Number of bullets added to the screen.
     */
    public final int updateBurst(final Set<Bullet> bullets) {
        if (this.remainingShots <= 0 || this.isDestroyed()) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();

        if (currentTime < this.nextShotTime) {
            return 0;
        }

        boolean added = bullets.add(BulletPool.getBullet(
                this.positionX + this.width / 2, this.positionY,
                this.bulletSpeed));

        this.remainingShots--;
        this.nextShotTime = currentTime
                + this.currentBulletType.getShotDelay();

        return added ? 1 : 0;
    }

    /**
     * Enables or disables the Hyperfire shooting buff.
     *
     * @param enabled True to enable Hyperfire, false to disable it.
     */
    public final void setHyperfireEnabled(final boolean enabled) {
        this.hyperfireEnabled = enabled;
    }

    /**
     * Checks whether Hyperfire is currently enabled.
     */
    public final boolean isHyperfireEnabled() {
        return this.hyperfireEnabled;
    }

    /**
     * Updates status and visual feedback of the ship.
     */
    public final void update() {
        if (this.chameleonHullActive) {
            long elapsed = System.nanoTime() - this.chameleonActivationTime;

            int index = (int) (
                    (elapsed / entity.item.ChameleonHull.COLOR_INTERVAL_NANOS)
                            % CHAMELEON_COLORS.length
            );

            setColor(CHAMELEON_COLORS[index]);
        } else if (this.damageFeedbackCooldown != null
                && !this.damageFeedbackCooldown.checkFinished()) {
            // Alternate red and normal for 0.5s immediately after damage
            long elapsed = System.currentTimeMillis() - this.damageFeedbackStartTime;
            if ((elapsed / FLASH_INTERVAL) % 2 == 0) {
                setColor(Color.RED);
            } else {
                setColor(DEFAULT_COLOR);
            }
        } else {
            // Restore default color once the feedback is over
            setColor(DEFAULT_COLOR);
        }

        if (!this.destructionCooldown.checkFinished())
            this.spriteType = SpriteType.ShipDestroyed;
        else
            this.spriteType = SpriteType.Ship;
    }

    /**
     * Switches the ship to its destroyed state.
     */
    public final void destroy() {
        if (this.chameleonHullActive) {
            return;
        }

        deactivateChameleonHull();
        this.destructionCooldown.reset();
    }

    /**
     * Checks if the ship is destroyed.
     *
     * @return True if the ship is currently destroyed.
     */
    public final boolean isDestroyed() {
        return !this.destructionCooldown.checkFinished();
    }

    /**
     * Getter for the ship's speed.
     *
     * @return Speed of the ship.
     */
    public final int getSpeed() {
        return this.speed;
    }

    /**
     * Getter for the type this ship was created from.
     *
     * @return Selected ship type.
     */
    public final ShipType getShipType() {
        return this.shipType;
    }

    /**
     * Changes the bullet type and cancels any burst still in flight.
     *
     * @param bulletType
     *            New bullet type.
     */
    public final void setBulletType(final BulletType bulletType) {
        if (bulletType == null)
            throw new IllegalArgumentException("Bullet type cannot be null.");
        this.currentBulletType = bulletType;
        this.bulletSpeed = bulletType.getSpeed();
        this.remainingShots = 0;
        this.nextShotTime = 0;
    }

    /**
     * Returns the bullet type the ship is firing now. It can differ from the
     * ship type's default after setBulletType().
     *
     * @return Current bullet type.
     */
    public final BulletType getCurrentBulletType() {
        return this.currentBulletType;
    }

    /** @return Time between shots in milliseconds. */
    public final int getShootingInterval() {
        return this.shootingInterval;
    }

    /** @return Baseline movement speed. */
    public final int getBaseSpeed() {
        return this.baseSpeed;
    }

    /** @return Baseline time between shots in milliseconds. */
    public final int getBaseShootingInterval() {
        return this.baseShootingInterval;
    }

    /**
     * Changes the ship's current movement speed. Zero stops movement.
     *
     * @param speed
     *            Non-negative movement distance per update.
     * @throws IllegalArgumentException
     *             If the speed is negative.
     */
    public final void setSpeed(final int speed) {
        if (speed < 0)
            throw new IllegalArgumentException(
                    "Ship speed must be non-negative.");
        this.speed = speed;
    }

    /**
     * Sets the baseline movement speed without changing the current speed.
     *
     * @param speed
     *            Positive movement distance per update.
     * @throws IllegalArgumentException
     *             If the baseline speed is not positive.
     */
    public final void setBaseSpeed(final int speed) {
        if (speed <= 0)
            throw new IllegalArgumentException(
                    "Base ship speed must be positive.");
        this.baseSpeed = speed;
    }

    /**
     * Changes the firing interval. The upstream ship measures the interval
     * from the time of the last shot, so there is no cooldown to restart.
     *
     * @param shootingInterval
     *            Positive interval in milliseconds.
     * @throws IllegalArgumentException
     *             If the interval is not positive.
     */
    public final void setShootingInterval(final int shootingInterval) {
        if (shootingInterval <= 0)
            throw new IllegalArgumentException(
                    "Shooting interval must be positive (milliseconds).");
        this.shootingInterval = shootingInterval;
    }

    /**
     * Sets the baseline shooting interval without changing the current one.
     *
     * @param shootingInterval
     *            Positive interval in milliseconds.
     * @throws IllegalArgumentException
     *             If the baseline interval is not positive.
     */
    public final void setBaseShootingInterval(final int shootingInterval) {
        if (shootingInterval <= 0)
            throw new IllegalArgumentException(
                    "Base shooting interval must be positive (milliseconds).");
        this.baseShootingInterval = shootingInterval;
    }

    /** Restores the current speed and shooting interval to their baselines. */
    public final void restoreBaseStats() {
        this.speed = this.baseSpeed;
        setShootingInterval(this.baseShootingInterval);
        this.baseStatsRestoreVersion++;
    }

    /** @return Version used internally to detect an explicit base-stat restore. */
    final long getBaseStatsRestoreVersion() {
        return this.baseStatsRestoreVersion;
    }

    public boolean isShieldActive() {
        return this.isShieldActive;
    }

    public void setShieldActive(final boolean active) {
        this.isShieldActive = active;
    }

    public final void activateChameleonHull() {
        if (!this.chameleonHullActive) {
            this.colorBeforeChameleon = getColor();
            this.widthBeforeChameleon = this.width;
            this.heightBeforeChameleon = this.height;
            this.normalPositionY = this.positionY;

            this.width = entity.item.ChameleonHull.BALL_SIZE;
            this.height = entity.item.ChameleonHull.BALL_SIZE;
        }

        this.chameleonHullActive = true;
        this.chameleonActivationTime = System.nanoTime();

        setColor(CHAMELEON_COLORS[0]);
    }

    public final void deactivateChameleonHull() {
        if (this.chameleonHullActive) {
            setColor(this.colorBeforeChameleon);

            this.width = this.widthBeforeChameleon;
            this.height = this.heightBeforeChameleon;
            this.positionY = this.normalPositionY;
        }

        this.chameleonHullActive = false;
        this.chameleonMovementRemainder = 0;
    }

    public final boolean isChameleonHullActive() {
        return this.chameleonHullActive;
    }

    public final double getMovementSpeed() {
        return this.speed * (
                this.chameleonHullActive
                        ? entity.item.ChameleonHull.SPEED_MULTIPLIER
                        : 1.0
        );
    }

    /** Adds the power-up movement; the original movement code is preserved. */
    private void addChameleonMovement(final int direction) {
        if (!this.chameleonHullActive) {
            return;
        }

        this.chameleonMovementRemainder += direction * this.speed
                * (entity.item.ChameleonHull.SPEED_MULTIPLIER - 1.0);

        int extraPixels = (int) (
                this.chameleonMovementRemainder >= 0
                        ? Math.floor(this.chameleonMovementRemainder + 1e-9)
                        : Math.ceil(this.chameleonMovementRemainder - 1e-9)
        );

        this.positionX += extraPixels;
        this.chameleonMovementRemainder -= extraPixels;
    }

    public final void moveUp() {
        if (this.chameleonHullActive && !isDestroyed()) {
            if (this.slowDebuffActive)
                this.positionY -= getSlowedMovementPixels(2);
            else
                this.positionY -= (int) getMovementSpeed();
        }
    }

    public final void moveDown() {
        if (this.chameleonHullActive && !isDestroyed()) {
            if (this.slowDebuffActive)
                this.positionY += getSlowedMovementPixels(3);
            else
                this.positionY += (int) getMovementSpeed();
        }
    }

    /**
     * Triggers the 0.5-second red flashing damage feedback.
     */
    public final void triggerDamageFeedback() {
        if (this.chameleonHullActive || this.isShieldActive) {
            return;
        }
        this.damageFeedbackStartTime = System.currentTimeMillis();
        this.damageFeedbackCooldown.reset();
    }
}
