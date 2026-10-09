import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import engine.Core;
import engine.Frame;
import engine.GameSettings;
import engine.GameState;
import entity.Bullet;
import entity.PlayerStatus;
import entity.Ship;
import entity.ShipType;
import screen.GameScreen;

/** Standalone B1/B2 regression checks; run with compiled src and res on classpath. */
public final class PlayerStatSourceTest {
	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static Object field(final Object target, final String name)
			throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void checkShipTypes() throws Exception {
		for (ShipType type : ShipType.values()) {
			Ship ship = new Ship(100, 100, type);
			check(ship.getSpeed() == type.getSpeed(), "Type speed: " + type);
			check(ship.getShootingInterval() == type.getShootCooldown(),
					"Type firing interval: " + type);
			PlayerStatus status = new PlayerStatus(type.getMaxLives(), ship);
			check(status.getMaxLives() == type.getMaxLives(), "Type lives: " + type);
			ship.moveRight();
			check(ship.getPositionX() == 100 + type.getSpeed(), "Movement: " + type);
			ship.moveLeft();
			check(ship.getPositionX() == 100, "Reverse movement: " + type);

			GameState state = new GameState(2, 250, type.getMaxLives() - 1,
					8, 4, type);
			GameScreen screen = new GameScreen(state,
					new GameSettings(5, 4, 60, 2000), false, 448, 520, 60);
			Ship screenShip = (Ship) field(screen, "ship");
			PlayerStatus screenStatus = (PlayerStatus) field(screen, "playerStatus");
			check(screenShip.getSpeed() == type.getSpeed(), "Screen speed: " + type);
			check(screenShip.getShootingInterval() == type.getShootCooldown(),
					"Screen firing interval: " + type);
			check(screenStatus.getMaxLives() == type.getMaxLives(),
					"Screen life source: " + type);
			check(screenStatus.takeDamage(1), "B1 damage contract");
			GameState saved = screen.getGameState();
			GameState next = new GameState(saved.getLevel() + 1, saved.getScore(),
					saved.getLivesRemaining(), saved.getBulletsShot(),
					saved.getShipsDestroyed(), saved.getShipType());
			GameScreen nextScreen = new GameScreen(next,
					new GameSettings(5, 4, 60, 2000), true, 448, 520, 60);
			check(nextScreen.getGameState().getShipType() == type,
					"Ship type survives next level: " + type);
			check(nextScreen.getGameState().getLivesRemaining() == type.getMaxLives() - 1,
					"Damage carried over and existing bonus life preserved: " + type);
			GameScreen full = new GameScreen(new GameState(3, 0, type.getMaxLives(),
					0, 0, type), new GameSettings(5, 4, 60, 2000), true, 448, 520, 60);
			check(full.getGameState().getLivesRemaining() == type.getMaxLives(),
					"Existing level bonus respects ship maximum: " + type);
		}
		check(new Ship(0, 0).getSpeed() == ShipType.STANDARD.getSpeed(),
				"Legacy ship constructor uses STANDARD");
		check(new GameState(1, 0, Core.getMaxLives(), 0, 0).getShipType()
				== ShipType.STANDARD, "Legacy game state uses STANDARD");
	}

	private static void checkCooldown() throws Exception {
		Ship ship = new Ship(0, 0);
		Set<Bullet> bullets = new HashSet<Bullet>();
		ship.setShootingInterval(60000);
		check(ship.shoot(bullets), "Initial shot should be ready");
		Object cooldown = field(ship, "shootingCooldown");
		long started = (Long) field(cooldown, "time");
		for (int i = 0; i < 100; i++) {
			ship.setShootingInterval(60000 + i);
			check(!ship.shoot(bullets), "Setter must not grant a free shot");
		}
		check(field(ship, "shootingCooldown") == cooldown, "Keep cooldown instance");
		check((Long) field(cooldown, "time") == started, "Keep last-shot timestamp");
		for (int invalid : new int[] {0, -1}) {
			boolean rejected = false;
			try {
				ship.setShootingInterval(invalid);
			} catch (IllegalArgumentException expected) {
				rejected = true;
			}
			check(rejected, "Reject non-positive interval");
			check(ship.getShootingInterval() == 60099, "Invalid setter is atomic");
		}
		// Simulate elapsed time deterministically, without sleeping.
		Field time = cooldown.getClass().getDeclaredField("time");
		time.setAccessible(true);
		time.setLong(cooldown, System.currentTimeMillis() - 5000);
		ship.setShootingInterval(1000);
		check(ship.shoot(bullets), "An elapsed shortened cooldown permits a shot");
		check(!ship.shoot(bullets), "Successful shot starts the next cooldown");
	}

	/** Runs the actual game update and drawing path in a temporary game window. */
	private static void checkGameWindow() throws Exception {
		Frame frame = new Frame(448, 520);
		Core.getDrawManager().setFrame(frame);
		try {
			for (ShipType type : ShipType.values()) {
				GameScreen screen = new GameScreen(new GameState(1, 0,
						type.getMaxLives(), 0, 0, type),
						new GameSettings(5, 4, 60, 2000), false,
						frame.getWidth(), frame.getHeight(), 60);
				screen.initialize();
				// The test bypasses only the countdown, then runs a real update.
				Field inputDelay = screen.Screen.class.getDeclaredField("inputDelay");
				inputDelay.setAccessible(true);
				inputDelay.set(screen, Core.getCooldown(1));
				Ship ship = (Ship) field(screen, "ship");
				int startX = ship.getPositionX();
				KeyEvent right = new KeyEvent(frame, KeyEvent.KEY_PRESSED,
						System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT,
						KeyEvent.CHAR_UNDEFINED);
				KeyEvent fire = new KeyEvent(frame, KeyEvent.KEY_PRESSED,
						System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, ' ');
				Core.getInputManager().keyPressed(right);
				Core.getInputManager().keyPressed(fire);
				try {
					Method update = GameScreen.class.getDeclaredMethod("update");
					update.setAccessible(true);
					update.invoke(screen);
				} finally {
					Core.getInputManager().keyReleased(right);
					Core.getInputManager().keyReleased(fire);
				}
				check(ship.getPositionX() == startX + type.getSpeed(),
						"Game update movement: " + type);
				check(screen.getGameState().getBulletsShot() == 1,
						"Game update shooting: " + type);
			}
		} finally {
			frame.dispose();
		}
	}

	public static void main(final String[] args) throws Exception {
		checkShipTypes();
		checkCooldown();
		if (args.length > 0 && "--gui".equals(args[0]))
			checkGameWindow();
		System.out.println("Player stat source checks passed.");
	}
}
