import java.util.HashSet;
import java.util.Set;

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
			// Expected.
		}
	}

	public static void main(final String[] args) {
		Ship ship = new Ship(100, 100);
		check(ship.getSpeed() == 2, "Default ship speed");
		check(ship.getShootingInterval() == 750,
				"Default shooting interval");

		ship.setSpeed(4);
		ship.moveRight();
		check(ship.getPositionX() == 104, "Updated movement speed");
		ship.moveLeft();
		check(ship.getPositionX() == 100, "Updated reverse movement speed");
		checkInvalidSpeed(ship, 0);
		checkInvalidSpeed(ship, -1);
		check(ship.getSpeed() == 4, "Invalid speed leaves current speed intact");

		Set<Bullet> bullets = new HashSet<Bullet>();
		check(ship.shoot(bullets), "A new ship can fire immediately");
		ship.setShootingInterval(60000);
		check(ship.getShootingInterval() == 60000,
				"Updated shooting interval");
		check(!ship.shoot(bullets),
				"Changing interval does not grant an immediate extra shot");

		checkInvalidInterval(ship, 0);
		checkInvalidInterval(ship, -1);
		check(ship.getShootingInterval() == 60000,
				"Invalid interval leaves current interval intact");

		try {
			Core.getVariableCooldown(1000, 100).setDuration(500);
			throw new AssertionError(
					"Variable cooldown duration should not be changed.");
		} catch (IllegalStateException expected) {
			// Expected.
		}

		System.out.println("Player stat source checks passed.");
	}
}
