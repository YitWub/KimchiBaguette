package screen;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import boss.BossManager;
import boss.BossSpawnRequest;
import boss.BossStatus;
import boss.LoggingBossEventListener;
import engine.Cooldown;
import engine.Core;
import engine.GameSettings;
import engine.GameState;
import entity.BonusEnemyManager;
import entity.Boss;
import entity.BossBullet;
import entity.Bullet;
import entity.BulletPool;
import entity.BulletType;
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.Entity;
import entity.PlayerStatus;
import entity.Ship;
import entity.ShipType;

/**
 * Implements the game screen, where the action happens.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class GameScreen extends Screen {

	/** Milliseconds until the screen accepts user input. */
	private static final int INPUT_DELAY = 6000;
	/** Bonus score for each life remaining at the end of the level. */
	private static final int LIFE_SCORE = 100;
	/** Time from finishing the level to screen change. */
	private static final int SCREEN_CHANGE_INTERVAL = 1500;
	/** Height of the interface separation line. */
	private static final int SEPARATION_LINE_HEIGHT = 40;
	/** Damage dealt to the boss by one player shot (to confirm with the
	 * weapon developer). */
	private static final int BOSS_DAMAGE_PER_SHOT = 50;
	/** Time the boss explosion stays on screen. */
	private static final int BOSS_EXPLOSION_TIME = 500;
	/** Stages where a boss appears (agreed with the team, may change). */
	private static final int[] BOSS_LEVELS = { 3, 5, 7 };

	/** Current game difficulty settings. */
	private GameSettings gameSettings;
	/** Current difficulty level number. */
	private int level;
	/** Formation of enemy ships. */
	private EnemyShipFormation enemyShipFormation;
	/** Player's ship. */
	private Ship ship;
	/** Time the defeated boss stays on screen as an explosion. */
	private Cooldown bossExplosionCooldown;
	/** Boss of the level, its attacks, events and links to other systems.
	 * The only source of truth for the boss. */
	private BossManager bossManager;
	/** Set of all bullets fired by the boss. */
	private Set<BossBullet> bossBullets;
	/** Type of player ship selected for the current game. */
	private ShipType shipType;
	/** Bonus enemy ship that appears sometimes. */
	private BonusEnemyManager bonusEnemyManager;
	/** Time from finishing the level to screen change. */
	private Cooldown screenFinishedCooldown;
	/** Set of all bullets fired by on screen ships. */
	private Set<Bullet> bullets;
	/** Current score. */
	private int score;
	/** Player lives left. */
	private PlayerStatus playerStatus;
	/** Total bullets shot by the player. */
	private int bulletsShot;
	/** Total ships destroyed by the player. */
	private int shipsDestroyed;
	/** Moment the game starts. */
	private long gameStartTime;
	/** Checks if the level is finished. */
	private boolean levelFinished;
	/** Checks if a bonus life is received. */
	private boolean bonusLife;

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param gameState
	 *            Current game state.
	 * @param gameSettings
	 *            Current game settings.
	 * @param bonusLife
	 *            Checks if a bonus life is awarded this level.
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public GameScreen(final GameState gameState,
			final GameSettings gameSettings, final boolean bonusLife,
			final int width, final int height, final int fps) {
		this(gameState, gameSettings, bonusLife, width, height, fps,
				ShipType.STANDARD);
	}

	/**
	 * Constructor, establishes the properties of the screen using a selected
	 * player ship type.
	 *
	 * @param gameState
	 *            Current game state.
	 * @param gameSettings
	 *            Current game settings.
	 * @param bonusLife
	 *            Checks if a bonus life is awarded this level.
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 * @param shipType
	 *            Selected player ship type.
	 */
	public GameScreen(final GameState gameState,
			final GameSettings gameSettings, final boolean bonusLife,
			final int width, final int height, final int fps,
			final ShipType shipType) {
		super(width, height, fps);

		if (shipType == null)
			throw new IllegalArgumentException("Ship type cannot be null.");

		this.gameSettings = gameSettings;
		this.bonusLife = bonusLife;
		this.shipType = shipType;
		this.level = gameState.getLevel();
		this.score = gameState.getScore();

		int initialLives = gameState.getLivesRemaining()
				+ (this.bonusLife ? 1 : 0);

		initialLives = Math.min(initialLives, this.shipType.getMaxLives());

		this.playerStatus = new PlayerStatus(initialLives,
				this.shipType.getMaxLives());
		this.bulletsShot = gameState.getBulletsShot();
		this.shipsDestroyed = gameState.getShipsDestroyed();
	}

	/**
	 * Initializes basic screen properties, and adds necessary elements.
	 */
	public final void initialize() {
		super.initialize();

		enemyShipFormation = new EnemyShipFormation(this.gameSettings);
		enemyShipFormation.attach(this);
		this.ship = new Ship(this.width / 2, this.height - 30,
				this.shipType);
		this.bossBullets = new HashSet<BossBullet>();
		this.bossManager = new BossManager();
		// Logs boss events until HUD and Visual Effect listeners are added.
		this.bossManager.getEventManager().addListener(
				new LoggingBossEventListener());
		// The boss appears only on boss stages.
		if (isBossLevel(this.level))
			this.bossManager.spawn(new BossSpawnRequest(
					Boss.BossType.BOSS_A, this.width / 2 - Boss.WIDTH / 2, 78,
					this.level, 0));
		this.bossExplosionCooldown = Core.getCooldown(BOSS_EXPLOSION_TIME);
		this.bonusEnemyManager = new BonusEnemyManager();
		this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);
		this.bullets = new HashSet<Bullet>();

		// Special input delay / countdown.
		this.gameStartTime = System.currentTimeMillis();
		this.inputDelay = Core.getCooldown(INPUT_DELAY);
		this.inputDelay.reset();
	}

	/**
	 * Starts the action.
	 * 
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();

		this.score += LIFE_SCORE * (this.playerStatus.getLives() - 1);
		this.logger.info("Screen cleared with a score of " + this.score);

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();

		if (this.inputDelay.checkFinished() && !this.levelFinished) {

			if (!this.ship.isDestroyed()) {
				boolean moveRight = inputManager.isKeyDown(KeyEvent.VK_RIGHT)
						|| inputManager.isKeyDown(KeyEvent.VK_D);
				boolean moveLeft = inputManager.isKeyDown(KeyEvent.VK_LEFT)
						|| inputManager.isKeyDown(KeyEvent.VK_A);

				if (moveRight) {
					this.ship.moveRight();
				}
				if (moveLeft) {
					this.ship.moveLeft();
				}
				this.ship.setPositionX(clampShipPositionX(
						this.ship.getPositionX(), this.ship.getWidth(),
						this.width));
				if (inputManager.isKeyDown(KeyEvent.VK_SPACE))
					this.bulletsShot += this.ship.shoot(this.bullets);
			}

			this.bonusEnemyManager.update(this.width);
			Boss boss = getBossOnScreen();
			if (boss != null) {
				boss.update(1, this.width - 1);
				this.bossManager.update(
						this.ship.getPositionX() + this.ship.getWidth() / 2,
						this.ship.getPositionY(), this.bossBullets);
			}

			this.ship.update();

			if (!this.ship.isDestroyed()) {
				this.bulletsShot += this.ship.updateBurst(this.bullets);
			}

			// Once every ship and explosion is gone, the formation stops
			// updating, so it keeps valid position and size values.
			if (!this.enemyShipFormation.isEmpty()
					|| this.enemyShipFormation.iterator().hasNext())
				this.enemyShipFormation.update();
			// An empty formation has no shooters left, so it must not shoot.
			if (!this.enemyShipFormation.isEmpty())
				this.enemyShipFormation.shoot(this.bullets);
		}

		manageCollisions();
		cleanBullets();
		draw();

		// The level is cleared only when the enemies and the boss are gone.
		// The boss battle state comes from the BossManager, and the boss
		// leaves the screen after its defeat explosion has been shown.
		boolean bossCleared = this.bossManager.getStatus()
				.getBattleState() != BossStatus.BattleState.IN_BATTLE
				&& getBossOnScreen() == null;
		boolean levelCleared = this.enemyShipFormation.isEmpty()
				&& bossCleared;
		if ((levelCleared || !this.playerStatus.isAlive())
				&& !this.levelFinished) {
			this.levelFinished = true;
			this.screenFinishedCooldown.reset();
		}

		if (this.levelFinished && this.screenFinishedCooldown.checkFinished())
			this.isRunning = false;

	}

	/**
	 * Clamps the player's ship to the visible horizontal screen area.
	 *
	 * @param positionX
	 *            Proposed left coordinate of the ship.
	 * @param shipWidth
	 *            Width of the ship.
	 * @param screenWidth
	 *            Width of the screen.
	 * @return A left coordinate within the screen boundaries.
	 */
	static int clampShipPositionX(final int positionX, final int shipWidth,
			final int screenWidth) {
		final int leftBoundary = 1;
		final int rightBoundary = Math.max(leftBoundary,
				screenWidth - shipWidth - 1);
		return Math.max(leftBoundary, Math.min(positionX, rightBoundary));
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		drawManager.drawEntity(this.ship, this.ship.getPositionX(),
				this.ship.getPositionY());
		EnemyShip bonusShip = this.bonusEnemyManager.getShip();
		if (bonusShip != null)
			drawManager.drawEntity(bonusShip, bonusShip.getPositionX(),
					bonusShip.getPositionY());

		Boss boss = getBossOnScreen();
		if (boss != null)
			drawManager.drawEntity(boss, boss.getPositionX(),
					boss.getPositionY());

		enemyShipFormation.draw();

		for (Bullet bullet : this.bullets)
			drawManager.drawEntity(bullet, bullet.getPositionX(),
					bullet.getPositionY());
		for (BossBullet bossBullet : this.bossBullets)
			drawManager.drawEntity(bossBullet, bossBullet.getPositionX(),
					bossBullet.getPositionY());

		// Interface.
		drawManager.drawScore(this, this.score);
		drawManager.drawLives(this, this.playerStatus.getLives());
		drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);

		// Countdown to game start.
		if (!this.inputDelay.checkFinished()) {
			int countdown = (int) ((INPUT_DELAY
					- (System.currentTimeMillis()
							- this.gameStartTime)) / 1000);
			drawManager.drawCountDown(this, this.level, countdown,
					this.bonusLife);
			drawManager.drawHorizontalLine(this, this.height / 2 - this.height
					/ 12);
			drawManager.drawHorizontalLine(this, this.height / 2 + this.height
					/ 12);
		}

		drawManager.completeDrawing(this);
	}

	/**
	 * Cleans bullets that go off screen.
	 */
	private void cleanBullets() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets) {
			bullet.update();
			if (bullet.getPositionY() < SEPARATION_LINE_HEIGHT
					|| bullet.getPositionY() > this.height)
				recyclable.add(bullet);
		}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);

		Set<BossBullet> outside = new HashSet<BossBullet>();
		for (BossBullet bossBullet : this.bossBullets) {
			bossBullet.update();
			if (bossBullet.isOutside(SEPARATION_LINE_HEIGHT, this.width,
					this.height))
				outside.add(bossBullet);
		}
		this.bossBullets.removeAll(outside);
	}

	/**
	 * Manages collisions between bullets and ships.
	 */
	private void manageCollisions() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets)
			if (bullet.getSpeed() > 0) {
				if (checkCollision(bullet, this.ship) && !this.levelFinished) {
					recyclable.add(bullet);
					if (!this.ship.isDestroyed()
							&& this.playerStatus.takeDamage(1)) {
						this.ship.destroy();
						this.logger.info("Hit on player ship, "
								+ this.playerStatus.getLives()
								+ " lives remaining.");
					}
				}
			} else {
				for (EnemyShip enemyShip : this.enemyShipFormation)
					if (!enemyShip.isDestroyed()
							&& checkCollision(bullet, enemyShip)) {
						this.score += enemyShip.getPointValue();
						this.shipsDestroyed++;
						this.enemyShipFormation.destroy(enemyShip);
						recyclable.add(bullet);
					}
				if (this.bonusEnemyManager.isActive() && checkCollision(bullet,
						this.bonusEnemyManager.getShip())) {
					this.score += this.bonusEnemyManager.destroy();
					this.shipsDestroyed++;
					recyclable.add(bullet);
				}
				// A bullet that already hit a ship cannot also hit the boss.
				Boss boss = getBossOnScreen();
				if (boss != null && !boss.isDefeated()
						&& !recyclable.contains(bullet)
						&& checkCollision(bullet, boss)) {
					damageBoss(getBossDamagePerBullet());
					recyclable.add(bullet);
				}
			}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);

		Set<BossBullet> hitBossBullets = new HashSet<BossBullet>();
		for (BossBullet bossBullet : this.bossBullets)
			if (checkCollision(bossBullet, this.ship) && !this.levelFinished) {
				hitBossBullets.add(bossBullet);
				if (!this.ship.isDestroyed() && this.playerStatus
						.takeDamage(bossBullet.getDamage())) {
					this.ship.destroy();
					this.logger.info("Hit on player ship by the boss, "
							+ this.playerStatus.getLives()
							+ " lives remaining.");
				}
			}
		this.bossBullets.removeAll(hitBossBullets);
	}

	/**
	 * Checks if two entities are colliding.
	 * 
	 * @param a
	 *            First entity, the bullet.
	 * @param b
	 *            Second entity, the ship.
	 * @return Result of the collision test.
	 */
	private boolean checkCollision(final Entity a, final Entity b) {
		// Calculate center point of the entities in both axis.
		int centerAX = a.getPositionX() + a.getWidth() / 2;
		int centerAY = a.getPositionY() + a.getHeight() / 2;
		int centerBX = b.getPositionX() + b.getWidth() / 2;
		int centerBY = b.getPositionY() + b.getHeight() / 2;
		// Calculate maximum distance without collision.
		int maxDistanceX = a.getWidth() / 2 + b.getWidth() / 2;
		int maxDistanceY = a.getHeight() / 2 + b.getHeight() / 2;
		// Calculates distance.
		int distanceX = Math.abs(centerAX - centerBX);
		int distanceY = Math.abs(centerAY - centerBY);

		return distanceX < maxDistanceX && distanceY < maxDistanceY;
	}

	/**
	 * Applies damage to the boss. Every attack that hurts the boss must go
	 * through this method, so that the boss events are sent and the defeat
	 * explosion starts.
	 *
	 * @param damage
	 *            Amount of health to remove.
	 */
	private void damageBoss(final int damage) {
		Boss boss = this.bossManager.getBoss();
		if (boss == null || boss.isDefeated())
			return;
		boss.takeDamage(damage);
		this.bossManager.sync();
		if (boss.isDefeated())
			this.bossExplosionCooldown.reset();
	}

	/**
	 * Returns the damage one player bullet deals to the boss. A RAPID shot
	 * fires its bullets one after another on the same line, so they share
	 * the damage of one shot. MULTI bullets spread apart and at most one of
	 * them can reach the boss, so each keeps the full damage.
	 *
	 * @return Damage per bullet.
	 */
	private int getBossDamagePerBullet() {
		// The current type, which can change during the game.
		if (this.ship.getCurrentBulletType() == BulletType.RAPID)
			return BOSS_DAMAGE_PER_SHOT
					/ BulletType.RAPID.getShotCount();
		return BOSS_DAMAGE_PER_SHOT;
	}

	/**
	 * Returns the boss to update and draw. A defeated boss stays on screen
	 * until its explosion has been shown.
	 *
	 * @return Boss on screen, or null.
	 */
	private Boss getBossOnScreen() {
		Boss boss = this.bossManager.getBoss();
		if (boss == null)
			return null;
		if (boss.isDefeated() && this.bossExplosionCooldown.checkFinished())
			return null;
		return boss;
	}

	/**
	 * Checks if a boss appears in the given stage.
	 *
	 * @param level
	 *            Stage number, starting at 1.
	 * @return True if the stage is a boss stage.
	 */
	private static boolean isBossLevel(final int level) {
		for (int bossLevel : BOSS_LEVELS)
			if (bossLevel == level)
				return true;
		return false;
	}

	/**
	 * Returns a GameState object representing the status of the game.
	 * 
	 * @return Current game state.
	 */
	public final GameState getGameState() {
		return new GameState(this.level, this.score, this.playerStatus.getLives(),
				this.bulletsShot, this.shipsDestroyed);
	}
}
