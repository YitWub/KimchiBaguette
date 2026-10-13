package entity;

/**
 * Represents the different types of player ships with unique gameplay
 * attributes.
 */
public enum ShipType {

	// Name, Speed, FireRateCooldown (ms), Lives, ShieldAbility
	STANDARD("Standard Ship", 2, 750, 3, false),
	SPEEDSTER("Speedster", 4, 400, 2, false),
	TANK("Heavy Tank", 1, 1000, 5, true);

	/** Name displayed to the player. */
	private final String name;

	/** Horizontal movement speed. */
	private final int speed;

	/** Minimum time between shots in milliseconds. */
	private final int shootCooldown;

	/** Maximum number of lives. */
	private final int maxLives;

	/** Whether this ship supports a shield. */
	private final boolean hasShield;

	/**
	 * Constructor.
	 *
	 * @param name
	 *            Display name.
	 * @param speed
	 *            Horizontal movement speed.
	 * @param shootCooldown
	 *            Time between shots in milliseconds.
	 * @param maxLives
	 *            Maximum number of lives.
	 * @param hasShield
	 *            Whether the ship supports a shield.
	 */
	ShipType(final String name, final int speed,
			final int shootCooldown, final int maxLives,
			final boolean hasShield) {

		this.name = name;
		this.speed = speed;
		this.shootCooldown = shootCooldown;
		this.maxLives = maxLives;
		this.hasShield = hasShield;
	}

	public final String getName() {
		return this.name;
	}

	public final int getSpeed() {
		return this.speed;
	}

	/**
	 * Getter used by the ship selection implementation.
	 *
	 * @return Shooting cooldown in milliseconds.
	 */
	public final int getShootCooldown() {
		return this.shootCooldown;
	}

	/**
	 * Alias used by the player ship implementation.
	 *
	 * @return Shooting interval in milliseconds.
	 */
	public final int getShootingInterval() {
		return this.shootCooldown;
	}

	public final int getMaxLives() {
		return this.maxLives;
	}

	/**
	 * Getter used by the existing ship selection implementation.
	 *
	 * @return Whether the ship supports a shield.
	 */
	public final boolean hasShield() {
		return this.hasShield;
	}

	/**
	 * Alias describing the shield as a capability rather than an active state.
	 *
	 * @return Whether the ship supports a shield.
	 */
	public final boolean isShieldCapable() {
		return this.hasShield;
	}
}