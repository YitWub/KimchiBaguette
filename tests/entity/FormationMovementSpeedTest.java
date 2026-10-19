package entity;

import java.lang.reflect.Field;
import java.util.Iterator;

import engine.GameSettings;
import engine.GameState;
import screen.GameScreen;

/**
 * Standalone checks that every movement pattern still obeys the stage's
 * speed setting and still speeds up as enemies are defeated. The diagonal
 * pattern used a delay of its own, which threw both away.
 */
public final class FormationMovementSpeedTest {

	private FormationMovementSpeedTest() {
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	public static void main(final String[] args) throws Exception {
		checkStageSettingChangesThePace();
		checkFormationSpeedsUpAsEnemiesAreDefeated();

		System.out.println("Formation movement speed checks passed.");
	}

	/**
	 * A stage configured to move slowly must move slowly. The base speed is
	 * the frame delay between steps, so a larger one means fewer steps.
	 */
	private static void checkStageSettingChangesThePace() throws Exception {
		int slow = framesPerStep(90);
		int fast = framesPerStep(20);

		check(slow > fast,
				"A higher base speed setting means a longer delay between "
						+ "steps, so the stage setting still reaches the "
						+ "movement (slow=" + slow + ", fast=" + fast + ")");
	}

	/**
	 * The formation accelerates as it is thinned out. With a fixed delay it
	 * ran at one pace from the first enemy to the last.
	 */
	private static void checkFormationSpeedsUpAsEnemiesAreDefeated()
			throws Exception {
		EnemyShipFormation full = formation(60);
		int atFullStrength = framesPerStep(full);

		EnemyShipFormation thinned = formation(60);
		destroyAllButOne(thinned);
		int nearlyCleared = framesPerStep(thinned);

		check(nearlyCleared < atFullStrength,
				"The formation moves more often once enemies are defeated "
						+ "(full=" + atFullStrength + ", thinned="
						+ nearlyCleared + ")");
	}

	/** Counts the frames a formation takes to move once. */
	private static int framesPerStep(final int baseSpeed) throws Exception {
		return framesPerStep(formation(baseSpeed));
	}

	private static int framesPerStep(final EnemyShipFormation formation)
			throws Exception {
		int startX = readInt(formation, "positionX");
		int startY = readInt(formation, "positionY");

		for (int frame = 1; frame <= 2000; frame++) {
			formation.update();
			if (readInt(formation, "positionX") != startX
					|| readInt(formation, "positionY") != startY)
				return frame;
		}
		throw new AssertionError("The formation never moved");
	}

	/** Leaves one enemy alive, so the acceleration is at its strongest. */
	private static void destroyAllButOne(final EnemyShipFormation formation) {
		Iterator<EnemyShip> iterator = formation.iterator();
		EnemyShip spared = iterator.next();
		while (iterator.hasNext()) {
			EnemyShip enemy = iterator.next();
			if (enemy != spared)
				formation.destroy(enemy);
		}
	}

	/**
	 * Builds a formation attached to a real screen. GameScreen owns that
	 * wiring, so it is borrowed rather than rebuilt here.
	 */
	private static EnemyShipFormation formation(final int baseSpeed)
			throws Exception {
		GameScreen game = new GameScreen(new GameState(1, 0, 3, 0, 0),
				new GameSettings(5, 4, baseSpeed, 2000), false, 448, 520, 60);
		game.initialize();

		Field field = GameScreen.class.getDeclaredField("enemyShipFormation");
		field.setAccessible(true);
		return (EnemyShipFormation) field.get(game);
	}

	private static int readInt(final Object target, final String name)
			throws Exception {
		Field field = EnemyShipFormation.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}
}
