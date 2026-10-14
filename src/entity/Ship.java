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

	/** The type of bullet the ship is currently using	*/
	private BulletType currentBulletType = BulletType.NORMAL;
	/** Remaining number of Bullets */
	private int remainingShots = 0;
	/** Delay between shots */
	private long nextShotTime = 0;
	/** shooting interval */
	private int shootingInterval;

	/** Speed of the bullets shot by the ship. */
	private int bulletSpeed = currentBulletType.getSpeed();
	/** Movement of the ship for each unit of time. */
	private int speed;
	
	/** Type of ship selected by the player. */
	private final ShipType shipType;

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
	public Ship(final int positionX, final int positionY){
		this(positionX, positionY, ShipType.STANDARD);
	}

	public Ship(final int positionX, final int positionY, final ShipType shipType) {
		super(positionX, positionY, 13 * 2, 8 * 2, Color.GREEN);

		if (shipType == null)
            throw new IllegalArgumentException("Ship type cannot be null.");

        this.shipType = shipType;
        this.speed = shipType.getSpeed();
		this.shootingInterval = shipType.getShootingInterval();

		this.currentBulletType = shipType.getBulletType();
		this.bulletSpeed = currentBulletType.getSpeed();

		this.spriteType = SpriteType.Ship;

		this.shootingCooldown = Core.getCooldown(this.getShootingInterval());
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
	public final int shoot(final Set<Bullet> bullets) {
		if (!this.shootingCooldown.checkFinished()) {
			return 0;
		}

		this.shootingCooldown.reset();

		int x = positionX + this.width / 2;
		int y = positionY;

		if (currentBulletType == BulletType.MULTI) {
			int count = 0;

			if (bullets.add(BulletPool.getBullet(x, y, bulletSpeed, -2)))
				count++;

			if (bullets.add(BulletPool.getBullet(x, y, bulletSpeed, 0)))
				count++;

			if (bullets.add(BulletPool.getBullet(x, y, bulletSpeed, 2)))
				count++;

			return count;
		}

		boolean added = bullets.add(
				BulletPool.getBullet(x, y, bulletSpeed));

		remainingShots = currentBulletType.getShotCount() - 1;
		nextShotTime = System.currentTimeMillis()
				+ currentBulletType.getShotDelay();

		return added ? 1 : 0;
	}

	/**
	 * Fires remaining bullets in a RAPID burst.
	 *
	 * @param bullets Bullets currently on screen.
	 * @return Number of bullets successfully added.
	 */
	public final int updateBurst(final Set<Bullet> bullets) {
		if (this.remainingShots <= 0 || this.isDestroyed()) {
			return 0;
		}

		long currentTime = System.currentTimeMillis();

		if (currentTime < this.nextShotTime) {
			return 0;
		}

		int x = this.positionX + this.width / 2;
		int y = this.positionY;

		boolean added = bullets.add(
				BulletPool.getBullet(x, y, this.bulletSpeed));

		this.remainingShots--;
		this.nextShotTime = currentTime
				+ this.currentBulletType.getShotDelay();

		return added ? 1 : 0;
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

		// Cancel pending brust shots
		this.remainingShots = 0;
		this.nextShotTime = 0;
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

	public void setBulletType(BulletType bulletType) {
		this.currentBulletType = bulletType;
		this.bulletSpeed = bulletType.getSpeed();
		this.remainingShots =0;
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

	public final void setSpeed(final int speed) {
		if (speed <= 0) {
			throw new IllegalArgumentException(
					"Speed must be positive.");
		}

		this.speed = speed;
	}
	public final int getShootingInterval() {
		return this.shootingInterval;
	}

	public final void setShootingInterval(final int shootingInterval) {

		if (shootingInterval <= 0) {
			throw new IllegalArgumentException(
					"Shooting interval must be positive.");
		}

		this.shootingCooldown.setDuration(shootingInterval);
		this.shootingInterval = shootingInterval;
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
