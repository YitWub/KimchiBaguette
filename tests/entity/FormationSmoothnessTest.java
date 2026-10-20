package entity;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.logging.LogManager;

import engine.GameSettings;
import engine.GameState;
import screen.GameScreen;

/**
 * Standalone checks that every movement pattern moves in small, frequent
 * steps rather than in rare jumps, and that the stage setting and the number
 * of enemies left still change its pace.
 *
 * <p>A pattern that waits movementSpeed frames (about seventy on the opening
 * stage) between steps of several pixels looks like it stutters, so the
 * checks look at the longest pause and the longest jump, not only at the
 * average pace.</p>
 */
public final class FormationSmoothnessTest {

	/** Frames to observe each configuration for. */
	private static final int SAMPLE_FRAMES = 3000;
	/**
	 * Longest pause allowed between two steps, in frames. The diagonal
	 * pattern, which already moves smoothly, pauses for up to three.
	 */
	private static final int MAX_IDLE_FRAMES = 3;
	/**
	 * Longest distance covered in one frame, in pixels: the largest step on
	 * each axis at once. Anything beyond it reads as a teleport.
	 */
	private static final double MAX_JUMP = 16.0 * Math.sqrt(2);
	/** Width of the screen the formation is built on. */
	private static final int SCREEN_WIDTH = 448;
	/** Margin the formation keeps on each side. */
	private static final int SIDE_MARGIN = 20;
	/** Distance the classic pattern goes down each pass. */
	private static final int DESCENT_DISTANCE = 20;

	private static final String[] PATTERNS = { "CLASSIC", "WAVE_BOUNCE",
			"DIAGONAL_BOUNCE" };

	private FormationSmoothnessTest() {
	}

	/** Result of observing one configuration. */
	private static final class Measure {
		int maxIdle;
		double maxJump;
		double pace;
		int minX = Integer.MAX_VALUE;
		int maxRight = Integer.MIN_VALUE;
		boolean offGrid;
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	public static void main(final String[] args) throws Exception {
		LogManager.getLogManager().reset();
		boolean report = args.length > 0 && args[0].equals("report");

		for (String pattern : PATTERNS) {
			double previousPace = -1;
			for (int[] setting : new int[][] { { 140, 0 }, { 60, 0 },
					{ 60, 50 }, { 60, 90 } }) {
				Measure m = measure(pattern, setting[0], setting[1]);
				if (report)
					System.out.printf("%-16s base=%3d killed=%2d%%  maxIdle=%3d"
							+ "  maxJump=%5.1f  pace=%5.2f%n", pattern,
							setting[0], setting[1], m.maxIdle, m.maxJump,
							m.pace);
				else
					assertSmooth(pattern, setting, m, previousPace);
				previousPace = m.pace;
			}
		}
		if (!report)
			System.out.println("Formation smoothness checks passed.");
	}

	private static void assertSmooth(final String pattern, final int[] setting,
			final Measure m, final double previousPace) {
		String where = pattern + " (base " + setting[0] + ", "
				+ setting[1] + "% killed)";
		check(m.maxIdle <= MAX_IDLE_FRAMES, where + " never pauses for more"
				+ " than " + MAX_IDLE_FRAMES + " frame (paused " + m.maxIdle
				+ ")");
		check(m.maxJump <= MAX_JUMP, where + " never jumps more than "
				+ MAX_JUMP + " px in a frame (jumped " + m.maxJump + ")");
		check(m.pace > previousPace, where + " is faster than the previous"
				+ " setting, which is slower or has more enemies (" + m.pace
				+ " vs " + previousPace + ")");
		check(m.minX >= SIDE_MARGIN - 1 && m.maxRight <= SCREEN_WIDTH
				- SIDE_MARGIN + 1, where + " stays inside the side margins"
				+ " (left " + m.minX + ", right " + m.maxRight + ")");
		check(!m.offGrid, where + " only moves sideways on a descent"
				+ " altitude, so it never gets stuck going down");
	}

	private static Measure measure(final String pattern, final int baseSpeed,
			final int killedPercent) throws Exception {
		EnemyShipFormation formation = formation(baseSpeed, pattern);
		destroyShare(formation, killedPercent);

		Measure m = new Measure();
		int lastX = read(formation, "positionX");
		int lastY = read(formation, "positionY");
		int lastWidth = read(formation, "width");
		int lastHeight = read(formation, "height");
		int idle = 0;
		double travelled = 0;

		for (int frame = 0; frame < SAMPLE_FRAMES; frame++) {
			formation.update();
			int x = read(formation, "positionX");
			int y = read(formation, "positionY");
			int width = read(formation, "width");
			int height = read(formation, "height");

			// When defeated enemies leave, the formation re-anchors itself
			// on the ones that remain. That is not movement, so it is left
			// out of the measure.
			if (width != lastWidth || height != lastHeight) {
				lastWidth = width;
				lastHeight = height;
				lastX = x;
				lastY = y;
				continue;
			}
			double step = Math.hypot(x - lastX, y - lastY);

			if (step == 0)
				idle++;
			else
				idle = 0;
			m.maxIdle = Math.max(m.maxIdle, idle);
			m.maxJump = Math.max(m.maxJump, step);
			travelled += step;
			m.minX = Math.min(m.minX, x);
			m.maxRight = Math.max(m.maxRight, x + width);
			if (pattern.equals("CLASSIC") && x != lastX
					&& y % DESCENT_DISTANCE != 0)
				m.offGrid = true;
			lastX = x;
			lastY = y;
		}
		m.pace = travelled / SAMPLE_FRAMES;
		return m;
	}

	private static void destroyShare(final EnemyShipFormation formation,
			final int killedPercent) {
		if (killedPercent <= 0)
			return;

		int total = 0;
		for (Iterator<EnemyShip> it = formation.iterator(); it.hasNext();) {
			it.next();
			total++;
		}

		int toKill = total * killedPercent / 100;
		int killed = 0;
		for (Iterator<EnemyShip> it = formation.iterator();
				it.hasNext() && killed < toKill; killed++)
			formation.destroy(it.next());
	}

	/** Builds a formation on a real screen and forces its movement pattern. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static EnemyShipFormation formation(final int baseSpeed,
			final String pattern) throws Exception {
		GameScreen game = new GameScreen(new GameState(1, 0, 3, 0, 0),
				new GameSettings(5, 4, baseSpeed, 2000), false, SCREEN_WIDTH,
				520, 60);
		game.initialize();

		Field screenField = GameScreen.class
				.getDeclaredField("enemyShipFormation");
		screenField.setAccessible(true);
		EnemyShipFormation formation = (EnemyShipFormation) screenField
				.get(game);

		Field patternField = EnemyShipFormation.class
				.getDeclaredField("movementPattern");
		patternField.setAccessible(true);
		patternField.set(formation,
				Enum.valueOf((Class<Enum>) patternField.getType(), pattern));
		return formation;
	}

	private static int read(final Object target, final String name)
			throws Exception {
		Field field = EnemyShipFormation.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}
}
