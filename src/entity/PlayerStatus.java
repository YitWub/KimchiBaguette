package entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the player's discrete lives and damage processing. The current game
 * does not model a separate health pool; one accepted hit consumes one life.
 *
 * A timed shield can absorb incoming damage. It is held here rather than on
 * {@link Ship} so that every damage source goes through one check, and so the
 * Gameplay HUD can read its remaining time without touching the ship.
 *
 * Every change is announced to the registered
 * {@link PlayerStatusListener}s, so the Gameplay HUD and Visual Effect
 * System can react without polling.
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
	 * Whether a shield was up the last time the clock was looked at. Keeps
	 * {@link PlayerStatusEvent.Type#SHIELD_ENDED} to one event per shield,
	 * since nothing counts the shield down frame by frame.
	 */
	private boolean shieldAnnounced;
	/** Listeners from the other systems. */
	private final List<PlayerStatusListener> listeners =
			new ArrayList<PlayerStatusListener>();

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
	 * Registers a listener. Registering the same listener twice has no
	 * effect, so it never receives an event twice.
	 *
	 * @param listener
	 *            Listener to add.
	 */
	public final void addListener(final PlayerStatusListener listener) {
		if (listener != null && !this.listeners.contains(listener))
			this.listeners.add(listener);
	}

	/**
	 * Removes a listener.
	 *
	 * @param listener
	 *            Listener to remove.
	 */
	public final void removeListener(final PlayerStatusListener listener) {
		this.listeners.remove(listener);
	}

	/**
	 * Sends an event to all listeners.
	 *
	 * @param event
	 *            Event to send.
	 */
	private void publish(final PlayerStatusEvent event) {
		// Copy so that a listener may add or remove one during the call.
		for (PlayerStatusListener listener
				: new ArrayList<PlayerStatusListener>(this.listeners))
			listener.onPlayerStatusEvent(event);
	}

	/**
	 * Reports a shield that has run out. The shield is held as an expiry time
	 * rather than counted down frame by frame, so something has to look at
	 * the clock for its end to be announced; {@link screen.GameScreen} calls
	 * this once per frame.
	 */
	public final void update() {
		announceExpiredShield();
	}

	/**
	 * Sends {@link PlayerStatusEvent.Type#SHIELD_ENDED} once, the first time
	 * the clock is read after the shield has run out. Called before anything
	 * that depends on the shield being up, so the end of a shield is never
	 * announced after the hit that got through it.
	 */
	private void announceExpiredShield() {
		if (!this.shieldAnnounced || isShieldActive())
			return;

		// Cleared before publishing, so a listener that calls back in does
		// not see the same expiry a second time.
		this.shieldAnnounced = false;
		publish(PlayerStatusEvent.shieldEnded(this));
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

		announceExpiredShield();
		if (isShieldActive()) {
			publish(PlayerStatusEvent.damageBlocked(this, damage));
			return false;
		}

		this.lives = Math.max(0, this.lives - damage);
		publish(PlayerStatusEvent.damageTaken(this, damage));
		return true;
	}

	/**
	 * Grants one life when the player is alive and below the configured maximum.
	 * A player with no lives cannot be revived; the game-over flow has already
	 * ended level progression at that point.
	 *
	 * @return Whether a life was added.
	 */
	public final boolean gainLife() {
		if (!isAlive() || this.lives >= this.maxLives)
			return false;

		this.lives++;
		publish(PlayerStatusEvent.lifeGained(this));
		return true;
	}

	/**
	 * Grants a shield, or refreshes the one already active. The player takes
	 * no damage until it expires. A refresh is announced as its own kind of
	 * event, so the HUD can restart a gauge rather than start a second one.
	 */
	public final void activateShield() {
		announceExpiredShield();

		boolean refreshed = isShieldActive();
		this.shieldExpiryTime = currentTimeMillis() + SHIELD_DURATION;
		this.shieldAnnounced = true;
		publish(refreshed ? PlayerStatusEvent.shieldRefreshed(this)
				: PlayerStatusEvent.shieldStarted(this));
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
		boolean wasActive = isShieldActive();

		this.shieldExpiryTime = 0;
		this.shieldAnnounced = false;
		if (wasActive)
			publish(PlayerStatusEvent.shieldEnded(this));
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
