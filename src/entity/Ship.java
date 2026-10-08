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

	/** The type of bullet the ship is currently using	*/
	private BulletType currentBulletType = BulletType.NORMAL;
	/** Remaining number of Bullets */
	private int remainingShots = 0;
	/** Delay between shots */
	private long shotDelay = 0;

	/** Time between shots. */
	private int SHOOTING_INTERVAL = currentBulletType.getShootingInterval();
	/** Speed of the bullets shot by the ship. */
	private int BULLET_SPEED = currentBulletType.getSpeed();
	/** Movement of the ship for each unit of time. */
	private static final int SPEED = 2;
	
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
		this.shootingCooldown = Core.getCooldown(SHOOTING_INTERVAL);
		this.destructionCooldown = Core.getCooldown(1000);
	}

	/**
	 * Moves the ship speed uni ts right, or until the right screen border is
	 * reached.
	 */
	public final void moveRight() {
		this.positionX += SPEED;
	}

	/**
	 * Moves the ship speed units left, or until the left screen border is
	 * reached.
	 */
	public final void moveLeft() {
		this.positionX -= SPEED;
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

			// Set up burst shooting
			remainingShots = currentBulletType.getShotCount() - 1;

			shotDelay = System.currentTimeMillis() + 
								currentBulletType.getShotDelay();
			
			// Set multi shot bullets
			if(currentBulletType == BulletType.MULTI){
				int x = positionX+ this.width / 2;
				int y = positionY;
				int speedY = BULLET_SPEED;
				
				// left
				bullets.add(BulletPool.getBullet(x,y,speedY,-2));

				// center
				bullets.add(BulletPool.getBullet(x,y,speedY,0));

				// right
				bullets.add(BulletPool.getBullet(x,y,speedY,2));

			}
			
			return true;
		}
		return false;
	}

	public int updateBurst(final Set<Bullet> bullets){
		if(remainingShots <=0){
			return 0;
		}

		long currentTime = System.currentTimeMillis();

		if(currentTime >= shotDelay){
			bullets.add(BulletPool.getBullet(
					positionX + this.width / 2,
					positionY, BULLET_SPEED));
			remainingShots--;
			shotDelay = currentTime + currentBulletType.getShotDelay();

			return 1;
		}
		return 0;
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
		return SPEED;
	}

	public void setBulletType(BulletType bulletType) {
		this.currentBulletType = bulletType;
	}
}
