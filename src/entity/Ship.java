package entity;

import java.awt.Color;
import java.util.Set;

import engine.Cooldown;
import engine.Core;
import engine.DrawManager.SpriteType;

/**
 * Implements a ship, to be controlled by the player.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class Ship extends Entity {

	/** Baseline time between shots in milliseconds. */
	private int baseShootingInterval = 750;
	/** Current time between shots in milliseconds. */
	private int shootingInterval = 750;
	/** Speed of the bullets shot by the ship. */
	private static final int BULLET_SPEED = -6;
	/** Baseline movement speed of the ship. */
	private int baseSpeed = 2;
	/** Current movement speed of the ship. */
	private int speed = 2;
	
	/** Minimum time between shots. */
	private Cooldown shootingCooldown;
	/** Time spent inactive between hits. */
	private Cooldown destructionCooldown;

	/**
	 * Constructor, establishes the ship's properties.
	 * 
	 * @param positionX
	 *            Initial position of the ship in the X axis.
	 * @param positionY
	 *            Initial position of the ship in the Y axis.
	 */
	public Ship(final int positionX, final int positionY) {
		super(positionX, positionY, 13 * 2, 8 * 2, Color.GREEN);

		this.spriteType = SpriteType.Ship;
		this.shootingCooldown = Core.getCooldown(this.shootingInterval);
		this.destructionCooldown = Core.getCooldown(1000);
	}

	/**
	 * Moves the ship speed uni ts right, or until the right screen border is
	 * reached.
	 */
	public final void moveRight() {
		this.positionX += this.speed;
	}

	/**
	 * Moves the ship speed units left, or until the left screen border is
	 * reached.
	 */
	public final void moveLeft() {
		this.positionX -= this.speed;
	}

	/**
	 * Shoots a bullet upwards.
	 * 
	 * @param bullets
	 *            List of bullets on screen, to add the new bullet.
	 * @return Checks if the bullet was shot correctly.
	 */
	public final boolean shoot(final Set<Bullet> bullets) {
		if (this.shootingCooldown.checkFinished()) {
			this.shootingCooldown.reset();
			bullets.add(BulletPool.getBullet(positionX + this.width / 2,
					positionY, BULLET_SPEED));
			return true;
		}
		return false;
	}

	/**
	 * Updates status of the ship.
	 */
	public final void update() {
		if (!this.destructionCooldown.checkFinished())
			this.spriteType = SpriteType.ShipDestroyed;
		else
			this.spriteType = SpriteType.Ship;
	}

	/**
	 * Switches the ship to its destroyed state.
	 */
	public final void destroy() {
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
	 */
	public final void setSpeed(final int speed) {
		if (speed < 0)
			throw new IllegalArgumentException(
					"Ship speed must be non-negative.");
		this.speed = speed;
	}

	/**
	 * Sets the baseline movement speed and makes it the current speed.
	 *
	 * @param speed
	 *            Positive movement distance per update.
	 */
	public final void setBaseSpeed(final int speed) {
		if (speed <= 0)
			throw new IllegalArgumentException(
					"Base ship speed must be positive.");
		this.baseSpeed = speed;
		this.speed = speed;
	}

	/**
	 * Changes the firing interval without restarting the current cooldown.
	 *
	 * @param shootingInterval
	 *            Positive interval in milliseconds.
	 */
	public final void setShootingInterval(final int shootingInterval) {
		if (shootingInterval <= 0)
			throw new IllegalArgumentException(
					"Shooting interval must be positive (milliseconds).");
		this.shootingCooldown.setDuration(shootingInterval);
		this.shootingInterval = shootingInterval;
	}

	/**
	 * Sets the baseline shooting interval and makes it the current interval.
	 *
	 * @param shootingInterval
	 *            Positive interval in milliseconds.
	 */
	public final void setBaseShootingInterval(final int shootingInterval) {
		if (shootingInterval <= 0)
			throw new IllegalArgumentException(
					"Base shooting interval must be positive (milliseconds).");
		setShootingInterval(shootingInterval);
		this.baseShootingInterval = shootingInterval;
	}

	/** Restores the current speed and shooting interval to their baseline values. */
	public final void restoreBaseStats() {
		this.speed = this.baseSpeed;
		setShootingInterval(this.baseShootingInterval);
	}
}
