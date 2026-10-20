package screen;

import entity.Boss;

/**
 * Standalone checks that both boss types are reachable in play. Boss B was
 * implemented but never spawned, because every boss stage asked for Boss A.
 */
public final class BossTypeByLevelTest {

	private BossTypeByLevelTest() {
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	public static void main(final String[] args) {
		checkTheFirstBossStageOpensWithTheSlowBoss();
		checkLaterBossStagesUseTheFastBoss();
		checkBothTypesDifferInMoreThanHealth();

		System.out.println("Boss type by level checks passed.");
	}

	private static void checkTheFirstBossStageOpensWithTheSlowBoss() {
		check(GameScreen.bossTypeForLevel(3) == Boss.BossType.BOSS_A,
				"Stage 3 opens the run with the slow boss");
	}

	private static void checkLaterBossStagesUseTheFastBoss() {
		check(GameScreen.bossTypeForLevel(5) == Boss.BossType.BOSS_B,
				"Stage 5 uses the fast boss");
		check(GameScreen.bossTypeForLevel(7) == Boss.BossType.BOSS_B,
				"Stage 7 uses the fast boss");
	}

	/**
	 * R4 asks for bosses with difficulty levels, so the two must differ in
	 * how they play and not only in the size of their health bar.
	 */
	private static void checkBothTypesDifferInMoreThanHealth() {
		Boss.BossType slow = Boss.BossType.BOSS_A;
		Boss.BossType fast = Boss.BossType.BOSS_B;

		check(slow.getMaxHealth() != fast.getMaxHealth(),
				"The two bosses differ in health");
		check(slow.getSpeed() != fast.getSpeed(),
				"The two bosses differ in speed");
	}
}
