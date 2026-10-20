package screen;

import java.lang.reflect.Method;

import engine.GameSettings;
import engine.GameState;
import entity.Boss;

/**
 * Standalone checks that defeating a boss gives its score exactly once.
 * The score used to stay the same when a boss was defeated.
 */
public final class BossKillScoreTest {

	private BossKillScoreTest() {
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	public static void main(final String[] args) throws Exception {
		checkEachBossTypeHasAScore();
		checkDefeatGivesTheScoreOnce(GameScreen.firstBossLevel(),
				Boss.BossType.BOSS_A);
		for (int level : GameScreen.BOSS_LEVELS)
			if (level != GameScreen.firstBossLevel())
				checkDefeatGivesTheScoreOnce(level, Boss.BossType.BOSS_B);

		System.out.println("Boss kill score checks passed.");
	}

	private static void checkEachBossTypeHasAScore() {
		for (Boss.BossType type : Boss.BossType.values())
			check(type.getPointValue() > 0,
					type + " gives a score when defeated");
		check(Boss.BossType.BOSS_B.getPointValue()
				> Boss.BossType.BOSS_A.getPointValue(),
				"The harder boss is worth more");
	}

	private static void checkDefeatGivesTheScoreOnce(final int level,
			final Boss.BossType expected) throws Exception {
		GameScreen game = new GameScreen(new GameState(level, 0, 3, 0, 0),
				new GameSettings(5, 4, 60, 2000), false, 448, 520, 60);
		game.initialize();

		Method damageBoss = GameScreen.class.getDeclaredMethod("damageBoss",
				int.class);
		damageBoss.setAccessible(true);

		int before = game.getGameState().getScore();
		// One hit short of the boss's health: no score yet.
		damageBoss.invoke(game, expected.getMaxHealth() - 1);
		check(game.getGameState().getScore() == before,
				"Stage " + level + ": no score while the boss is alive");

		damageBoss.invoke(game, 1);
		check(game.getGameState().getScore()
				== before + expected.getPointValue(),
				"Stage " + level + ": defeating " + expected + " gives "
						+ expected.getPointValue() + " points");

		damageBoss.invoke(game, 50);
		check(game.getGameState().getScore()
				== before + expected.getPointValue(),
				"Stage " + level + ": hitting a defeated boss gives nothing");
	}
}
