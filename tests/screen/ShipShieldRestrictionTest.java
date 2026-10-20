package screen;

import java.lang.reflect.Field;

import engine.GameSettings;
import engine.GameState;
import entity.PlayerStatus;
import entity.ShipType;

/**
 * Standalone checks for the part of R1 that says only a shield-capable ship
 * may hold a shield. ShipType.isShieldCapable() existed but was never read,
 * so every ship could raise one.
 */
public final class ShipShieldRestrictionTest {

	private ShipShieldRestrictionTest() {
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	public static void main(final String[] args) throws Exception {
		checkOnlyTheTankIsShieldCapable();
		checkSelectedShipDecidesWhetherAShieldCanBeRaised();
		checkStatusWithoutAStatedCapabilityStillAllowsShields();

		System.out.println("Ship shield restriction checks passed.");
	}

	/** The capability comes from the ship table, so pin what it says. */
	private static void checkOnlyTheTankIsShieldCapable() {
		check(ShipType.TANK.isShieldCapable(),
				"The Heavy Tank is the shield-capable ship");
		check(!ShipType.STANDARD.isShieldCapable(),
				"The Standard Ship is not shield-capable");
		check(!ShipType.SPEEDSTER.isShieldCapable(),
				"The Speedster is not shield-capable");
	}

	/** The R1 condition, read through the screen that owns the choice. */
	private static void checkSelectedShipDecidesWhetherAShieldCanBeRaised()
			throws Exception {
		PlayerStatus tank = statusFor(ShipType.TANK);
		check(tank.activateShield(), "The Heavy Tank can raise a shield");
		check(tank.isShieldActive(), "The Heavy Tank's shield comes up");

		PlayerStatus standard = statusFor(ShipType.STANDARD);
		check(!standard.activateShield(),
				"The Standard Ship is refused a shield");
		check(!standard.isShieldActive(),
				"No shield comes up for the Standard Ship");

		PlayerStatus speedster = statusFor(ShipType.SPEEDSTER);
		check(!speedster.activateShield(),
				"The Speedster is refused a shield");
	}

	/**
	 * The two-argument constructor is used by the other harnesses and by any
	 * caller that does not care about ship types, so it must keep allowing
	 * shields.
	 */
	private static void checkStatusWithoutAStatedCapabilityStillAllowsShields() {
		PlayerStatus status = new PlayerStatus(3, 3);
		check(status.isShieldCapable(),
				"A status built without a stated capability can hold a shield");
		check(status.activateShield(), "and raises one when asked");
	}

	private static PlayerStatus statusFor(final ShipType shipType)
			throws Exception {
		GameScreen game = new GameScreen(new GameState(1, 0, 3, 0, 0),
				new GameSettings(5, 4, 60, 2000), false, 448, 520, 60,
				shipType);
		game.initialize();

		Field field = GameScreen.class.getDeclaredField("playerStatus");
		field.setAccessible(true);
		return (PlayerStatus) field.get(game);
	}
}
