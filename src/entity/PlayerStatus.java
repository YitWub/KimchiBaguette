package entity;

/**
 * Manages the player's discrete lives and damage processing. The current game
 * does not model a separate health pool; one accepted hit consumes one life.
 */
public class PlayerStatus {

	/** Current number of lives. */
	private int lives;
	/** Maximum number of lives for the selected game setup. */
	private final int maxLives;

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
	 * Processes incoming damage and reduces lives.
	 *
	 * @param damage
	 *            Amount of damage to apply.
	 * @return Whether damage was applied.
	 */
	public final boolean takeDamage(final int damage) {
		if (damage <= 0 || !isAlive())
			return false;

		this.lives = Math.max(0, this.lives - damage);
		return true;
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
