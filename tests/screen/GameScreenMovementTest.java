package screen;

import entity.Ship;

/** Standalone checks for clamped player-ship movement. */
public final class GameScreenMovementTest {

	private GameScreenMovementTest() {
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	public static void main(final String[] args) {
		final int screenWidth = 800;
		Ship ship = new Ship(400, 100);
		ship.setSpeed(20);

		for (int i = 0; i < 100; i++) {
			ship.moveRight();
			ship.setPositionX(GameScreen.clampShipPositionX(
					ship.getPositionX(), ship.getWidth(), screenWidth));
		}
		int rightBoundary = screenWidth - ship.getWidth() - 1;
		check(ship.getPositionX() == rightBoundary,
				"High speed can reach the right boundary");

		for (int i = 0; i < 100; i++) {
			ship.moveLeft();
			ship.setPositionX(GameScreen.clampShipPositionX(
					ship.getPositionX(), ship.getWidth(), screenWidth));
		}
		check(ship.getPositionX() == 1,
				"High speed can reach the left boundary");

		check(GameScreen.clampShipPositionX(-100, ship.getWidth(), screenWidth)
				== 1, "Coordinates clamp at the left boundary");
		check(GameScreen.clampShipPositionX(2000, ship.getWidth(), screenWidth)
				== rightBoundary, "Coordinates clamp at the right boundary");

		System.out.println("Game screen movement checks passed.");
	}
}
