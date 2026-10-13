package engine;

/**
 * Imposes a cooldown period between two actions.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class Cooldown {

	/** Cooldown duration. */
	private int milliseconds;
	/** Maximum difference between durations. */
	private int variance;
	/** Duration of this run, varies between runs if variance > 0. */
	private int duration;
	/** Beginning time. */
	private long time;

	/**
	 * Constructor, established the time until the action can be performed
	 * again.
	 * 
	 * @param milliseconds
	 *            Time until cooldown period is finished.
	 */
	protected Cooldown(final int milliseconds) {
		if (milliseconds <= 0)
			throw new IllegalArgumentException(
					"Cooldown duration must be positive.");
		this.milliseconds = milliseconds;
		this.variance = 0;
		this.duration = milliseconds;
		this.time = 0;
	}

	/**
	 * Constructor, established the time until the action can be performed
	 * again, with a variation of +/- variance.
	 * 
	 * @param milliseconds
	 *            Time until cooldown period is finished.
	 * @param variance
	 *            Variance in the cooldown period.
	 */
	protected Cooldown(final int milliseconds, final int variance) {
		if (milliseconds <= 0)
			throw new IllegalArgumentException(
					"Cooldown duration must be positive.");
		if (variance < 0 || variance >= milliseconds)
			throw new IllegalArgumentException(
					"Cooldown variance must be non-negative and less than its duration.");
		this.milliseconds = milliseconds;
		this.variance = variance;
		this.duration = milliseconds;
		this.time = 0;
	}

	/**
	 * Checks if the cooldown is finished.
	 * 
	 * @return Cooldown state.
	 */
	public final boolean checkFinished() {
		if ((this.time == 0)
				|| this.time + this.duration < System.currentTimeMillis())
			return true;
		return false;
	}

	/**
	 * Changes a fixed cooldown's duration without restarting its timer. The new
	 * duration applies from the original start time, so shortening can make an
	 * active cooldown finish immediately and extending can keep it active longer.
	 * The change is not measured from the time this method is called.
	 *
	 * @param milliseconds
	 *            New positive cooldown duration in milliseconds.
	 */
	public final void setDuration(final int milliseconds) {
		if (milliseconds <= 0)
			throw new IllegalArgumentException(
					"Cooldown duration must be positive.");
		if (this.variance != 0)
			throw new IllegalStateException(
					"Cannot change a variable cooldown duration.");

		this.milliseconds = milliseconds;
		this.duration = milliseconds;
	}

	/**
	 * Restarts the cooldown.
	 */
	public final void reset() {
		this.time = System.currentTimeMillis();
		if (this.variance != 0)
			this.duration = (this.milliseconds - this.variance)
					+ (int) (Math.random()
							* (this.milliseconds + this.variance));
	}
}
