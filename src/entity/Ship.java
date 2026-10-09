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

	/** Time between shots in milliseconds. */
	private int shootingInterval;
	/** Speed of the bullets shot by the ship. */
	private static final int BULLET_SPEED = -6;
	/** Movement of the ship for each unit of time. */
	private int speed;
	/** Maximum lives supplied by the ship type. */
	private final int maxLives;

	/** Minimum time between shots. */
	private Cooldown shootingCooldown;
	/** Time spent inactive between hits. */
	private Cooldown destructionCooldown;

	/** Type of ship selected by the player. */
	private ShipType shipType;
	/** Shield status indicator. */
	private boolean shieldActive;

	/**
	 * Default constructor, uses the STANDARD ship configuration.
	 *
	 * @param positionX Initial position of the ship in the X axis.
	 * @param positionY Initial position of the ship in the Y axis.
	 */
	public Ship(final int positionX, final int positionY) {
		this(positionX, positionY, ShipType.STANDARD);
	}

	/**
	 * Creates a ship using the selected type's base stats.
	 *
	 * @param positionX Initial X position.
	 * @param positionY Initial Y position.
	 * @param shipType Source of speed, shooting interval and maximum lives.
	 */
	public Ship(final int positionX, final int positionY,
			final ShipType shipType) {
		super(positionX, positionY, 13 * 2, 8 * 2, Color.GREEN);
		if (shipType == null)
			throw new IllegalArgumentException("Ship type cannot be null.");

		this.shipType = shipType;
		this.spriteType = SpriteType.Ship;
		this.maxLives = shipType.getMaxLives();
		this.shootingCooldown = Core.getCooldown(shipType.getShootCooldown());
		setSpeed(shipType.getSpeed());
		setShootingInterval(shipType.getShootCooldown());
		this.destructionCooldown = Core.getCooldown(1000);
		this.shieldActive = shipType.hasShield();
	}

	/**
	 * Moves the ship speed units right.
	 */
	public final void moveRight() {
		this.positionX += this.speed;
	}

	/**
	 * Moves the ship speed units left.
	 */
	public final void moveLeft() {
		this.positionX -= this.speed;
	}

	/**
	 * Shoots a bullet upwards.
	 *
	 * @param bullets List of bullets on screen, to add the new bullet.
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

	/** @return Base shooting interval in milliseconds. */
	public final int getShootingInterval() {
		return this.shootingInterval;
	}

	/** @return Maximum lives supplied by the ship type. */
	public final int getMaxLives() {
		return this.maxLives;
	}

	/**
	 * Changes base speed.
	 * @param speed Positive movement distance per update.
	 */
	public final void setSpeed(final int speed) {
		if (speed <= 0)
			throw new IllegalArgumentException("Speed must be positive.");
		this.speed = speed;
	}

	/**
	 * Changes the firing interval while preserving the last shot's timestamp.
	 * @param shootingInterval Positive interval in milliseconds.
	 */
	public final void setShootingInterval(final int shootingInterval) {
		this.shootingCooldown.setDuration(shootingInterval);
		this.shootingInterval = shootingInterval;
	}

	/**
	 * Getter for the ship type.
	 *
	 * @return Current ShipType.
	 */
	public final ShipType getShipType() {
		return this.shipType;
	}

	/**
	 * Checks if shield is active.
	 *
	 * @return True if shield is active.
	 */
	public final boolean isShieldActive() {
		return this.shieldActive;
	}

	/**
	 * Sets the shield status.
	 *
	 * @param active Shield state.
	 */
	public final void setShieldActive(final boolean active) {
		this.shieldActive = active;
	}
}
