import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import engine.Cooldown;
import engine.Core;
import entity.Bullet;
import entity.Ship;

/** Small standalone regression checks for the B2 ship-stat changes. */
public final class PlayerStatSourceTest {

	private PlayerStatSourceTest() {
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void checkInvalidSpeed(final Ship ship, final int speed) {
		try {
			ship.setSpeed(speed);
			throw new AssertionError("Expected invalid speed to be rejected.");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}

	private static void checkInvalidInterval(final Ship ship,
			final int interval) {
		try {
			ship.setShootingInterval(interval);
			throw new AssertionError(
					"Expected invalid shooting interval to be rejected.");
		} catch (IllegalArgumentException expected) {
			check(expected.getMessage().contains("Shooting interval"),
					"Ship-specific interval error message");
		}
	}

	private static long getCooldownStart(final Cooldown cooldown)
			throws Exception {
		Field time = Cooldown.class.getDeclaredField("time");
		time.setAccessible(true);
		return time.getLong(cooldown);
	}

	private static void setCooldownStart(final Cooldown cooldown,
			final long timeValue) throws Exception {
		Field time = Cooldown.class.getDeclaredField("time");
		time.setAccessible(true);
		time.setLong(cooldown, timeValue);
	}

	private static void checkCooldownValidation() {
		try {
			Core.getCooldown(0);
			throw new AssertionError("Zero cooldown should be rejected.");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
		try {
			Core.getCooldown(-1);
			throw new AssertionError("Negative cooldown should be rejected.");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
		try {
			Core.getVariableCooldown(1000, -1);
			throw new AssertionError("Negative variance should be rejected.");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
		try {
			Core.getVariableCooldown(1000, 1000);
			throw new AssertionError(
					"Variance equal to duration should be rejected.");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}

	private static void checkCooldownDurationChanges() throws Exception {
		Cooldown shortened = Core.getCooldown(5000);
		shortened.reset();
		setCooldownStart(shortened, System.currentTimeMillis() - 3000);
		long shortenedStart = getCooldownStart(shortened);
		shortened.setDuration(1000);
		check(getCooldownStart(shortened) == shortenedStart,
				"Shortening preserves the original start time");
		check(shortened.checkFinished(),
				"Shortened duration applies to elapsed cooldown time");

		Cooldown extended = Core.getCooldown(1000);
		extended.reset();
		setCooldownStart(extended, System.currentTimeMillis() - 3000);
		long extendedStart = getCooldownStart(extended);
		extended.setDuration(5000);
		check(getCooldownStart(extended) == extendedStart,
				"Extending preserves the original start time");
		check(!extended.checkFinished(),
				"Extended duration applies to elapsed cooldown time");
	}

	public static void main(final String[] args) throws Exception {
		Ship ship = new Ship(100, 100);
		check(ship.getSpeed() == 2, "Default ship speed");
		check(ship.getBaseSpeed() == 2, "Default baseline ship speed");
		check(ship.getShootingInterval() == 750,
				"Default shooting interval");
		check(ship.getBaseShootingInterval() == 750,
				"Default baseline shooting interval");

		ship.setBaseSpeed(4);
		check(ship.getBaseSpeed() == 4 && ship.getSpeed() == 4,
				"Changing baseline speed updates current speed");
		ship.moveRight();
		check(ship.getPositionX() == 104, "Updated movement speed");
		ship.moveLeft();
		check(ship.getPositionX() == 100, "Updated reverse movement speed");
		ship.setSpeed(0);
		ship.moveRight();
		check(ship.getPositionX() == 100, "Zero speed disables movement");
		checkInvalidSpeed(ship, -1);
		check(ship.getSpeed() == 0,
				"Invalid speed leaves current speed intact");
		ship.setSpeed(12);
		ship.setBaseShootingInterval(1000);
		ship.setShootingInterval(300);
		ship.restoreBaseStats();
		check(ship.getSpeed() == 4 && ship.getShootingInterval() == 1000,
				"Restoring stats returns to baseline values");

		Ship firingShip = new Ship(100, 100);
		Set<Bullet> bullets = new HashSet<Bullet>();
		check(firingShip.shoot(bullets), "A new ship can fire immediately");
		firingShip.setShootingInterval(60000);
		check(firingShip.getShootingInterval() == 60000,
				"Updated shooting interval");
		check(!firingShip.shoot(bullets),
				"Changing interval does not grant an immediate extra shot");

		checkInvalidInterval(firingShip, 0);
		checkInvalidInterval(firingShip, -1);
		check(firingShip.getShootingInterval() == 60000,
				"Invalid interval leaves current interval intact");

		try {
			Core.getVariableCooldown(1000, 100).setDuration(500);
			throw new AssertionError(
					"Variable cooldown duration should not be changed.");
		} catch (IllegalStateException expected) {
			// Expected.
		}
		checkCooldownValidation();
		checkCooldownDurationChanges();

		System.out.println("Player stat source checks passed.");
	}
}
