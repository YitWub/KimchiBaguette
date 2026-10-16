package entity;

/**
 * Manages the player's discrete lives and damage processing. The current game
 * does not model a separate health pool; one accepted hit consumes one life.
 *
 * A timed shield can absorb incoming damage. It is held here rather than on
 * {@link Ship} so that every damage source goes through one check, and so the
 * Gameplay HUD can read its remaining time without touching the ship.
 */
public class PlayerStatus {

	/** Duration of a shield, in milliseconds. */
	private static final int SHIELD_DURATION = 3000;

	/** Current number of lives. */
	private int lives;
	/** Maximum number of lives for the selected game setup. */
	private final int maxLives;
	/**
	 * Point in time the shield lasts until. The shield is active only while
	 * the current time is before it, so a stale value from an expired shield
	 * is harmless. Never compare this field against zero to decide whether a
	 * shield is up; call {@link #isShieldActive()}.
	 */
	private long shieldExpiryTime;

	/**
	 * Creates a valid player-life state.
	 *
	 * @param initialLives
	 *            Initial number of lives.
	 * @param maxLives
	 *            Maximum number of lives.
	 */
	public PlayerStatus(final int initialLives, final int maxLives) {
		if (maxLives <= 0)
			throw new IllegalArgumentException(
					"Maximum lives must be positive.");
		if (initialLives < 0 || initialLives > maxLives)
			throw new IllegalArgumentException(
					"Initial lives must be between zero and the maximum.");

		this.lives = initialLives;
		this.maxLives = maxLives;
	}

	/**
	 * Processes incoming damage and reduces lives. An active shield absorbs
	 * the hit, so no life is lost and the caller is told that no damage was
	 * applied. Every damage source reaches this method, so the shield covers
	 * enemy bullets and boss bullets alike.
	 *
	 * @param damage
	 *            Amount of damage to apply.
	 * @return Whether damage was applied.
	 */
	public final boolean takeDamage(final int damage) {
		if (damage <= 0 || !isAlive())
			return false;
		if (isShieldActive())
			return false;

		this.lives = Math.max(0, this.lives - damage);
		return true;
	}

	/**
	 * Grants a shield, or refreshes the one already active. The player takes
	 * no damage until it expires.
	 */
	public final void activateShield() {
		this.shieldExpiryTime = currentTimeMillis() + SHIELD_DURATION;
	}

	/**
	 * Reads the clock the shield is measured against. Tests override this to
	 * reach the expiry boundary without waiting. A pause feature would also
	 * hook in here, since the wall clock keeps running while the game is
	 * paused.
	 *
	 * @return Current time in milliseconds.
	 */
	long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	/** Ends an active shield immediately, if there is one. */
	public final void clearShield() {
		this.shieldExpiryTime = 0;
	}

	/** @return Whether a shield is currently absorbing damage. */
	public final boolean isShieldActive() {
		return getShieldRemainingMillis() > 0;
	}

	/**
	 * Reports how much of the shield is left, for HUD gauges.
	 *
	 * @return Milliseconds left on the active shield, or zero when no shield
	 *         is active.
	 */
	public final int getShieldRemainingMillis() {
		long remaining = this.shieldExpiryTime - currentTimeMillis();
		return remaining > 0 ? (int) remaining : 0;
	}

	/** @return Full duration of a shield, in milliseconds, for HUD gauges. */
	public static int getShieldDuration() {
		return SHIELD_DURATION;
	}

	/** @return Current number of lives. */
	public final int getLives() {
		return this.lives;
	}

	/** @return Maximum number of lives. */
	public final int getMaxLives() {
		return this.maxLives;
	}

	/** @return Whether at least one life remains. */
	public final boolean isAlive() {
		return this.lives > 0;
	}
}
