package engine;

import java.lang.reflect.Field;

/** Regression checks for the per-level enemy firing intervals. */
public final class CoreDifficultySettingsTest {

	private CoreDifficultySettingsTest() {
	}

	public static void main(final String[] args) throws Exception {
		int[] expectedIntervals = { 2800, 3500, 2100, 2100, 1400, 1400, 700 };
		for (int i = 0; i < expectedIntervals.length; i++) {
			Field settingField = Core.class.getDeclaredField(
					"SETTINGS_LEVEL_" + (i + 1));
			settingField.setAccessible(true);
			GameSettings settings = (GameSettings) settingField.get(null);
			if (settings.getShootingFrecuency() != expectedIntervals[i])
				throw new AssertionError("Unexpected firing interval at level "
						+ (i + 1));
		}
		System.out.println("Core difficulty setting checks passed.");
	}
}
