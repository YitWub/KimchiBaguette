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
 */
public class Ship extends Entity {

	/** Speed of the bullets shot by the ship. */
	private static final int BULLET_SPEED = -6;

	/** Type of ship selected by the player. */
	private final ShipType shipType;

	/** Movement speed of the ship. */
	private final int speed;

	/** Minimum time between shots. */
	private Cooldown shootingCooldown;

	/** Time spent inactive between hits. */
	private Cooldown destructionCooldown;

	/**
	 * Default constructor.
	 * Uses the STANDARD ship configuration.
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
	 * Constructor with a selectable ship type.
	 *
	 * @param positionX
	 *            Initial position of the ship in the X axis.
	 * @param positionY
	 *            Initial position of the ship in the Y axis.
	 * @param shipType
	 *            Selected player ship type.
	 */
	public Ship(final int positionX, final int positionY,
			final ShipType shipType) {

		super(positionX, positionY, 13 * 2, 8 * 2, Color.GREEN);

		if (shipType == null)
			throw new IllegalArgumentException(
					"Ship type cannot be null.");

		this.shipType = shipType;
		this.speed = shipType.getSpeed();

		this.spriteType = SpriteType.Ship;
		this.shootingCooldown =
				Core.getCooldown(shipType.getShootingInterval());
		this.destructionCooldown = Core.getCooldown(1000);
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
	 * @param bullets
	 *            List of bullets on screen, to add the new bullet.
	 * @return Checks if the bullet was shot correctly.
	 */
	public final boolean shoot(final Set<Bullet> bullets) {
		if (this.shootingCooldown.checkFinished()) {
			this.shootingCooldown.reset();

			bullets.add(BulletPool.getBullet(
					positionX + this.width / 2,
					positionY,
					BULLET_SPEED));

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

	/**
	 * Getter for the selected ship type.
	 *
	 * @return Current ship type.
	 */
	public final ShipType getShipType() {
		return this.shipType;
	}

	/**
	 * Checks whether this ship type supports a shield.
	 *
	 * @return True if the selected ship supports a shield.
	 */
	public final boolean isShieldCapable() {
		return this.shipType.isShieldCapable();
	}
}
