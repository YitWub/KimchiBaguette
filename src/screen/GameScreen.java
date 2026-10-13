package screen;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import boss.BossManager;
import boss.BossSpawnRequest;
import boss.LoggingBossEventListener;
import engine.Cooldown;
import engine.Core;
import engine.GameSettings;
import engine.GameState;
import entity.Boss;
import entity.BossBullet;
import entity.Bullet;
import entity.BulletPool;
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.Entity;
import entity.PlayerStatus;
import entity.Ship;

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
	/** Minimum time between bonus ship's appearances. */
	private static final int BONUS_SHIP_INTERVAL = 20000;
	/** Maximum variance in the time between bonus ship's appearances. */
	private static final int BONUS_SHIP_VARIANCE = 10000;
	/** Time until bonus ship explosion disappears. */
	private static final int BONUS_SHIP_EXPLOSION = 500;
	/** Time from finishing the level to screen change. */
	private static final int SCREEN_CHANGE_INTERVAL = 1500;
	/** Height of the interface separation line. */
	private static final int SEPARATION_LINE_HEIGHT = 40;
	/** Damage dealt to the boss by one player bullet. */
	private static final int BOSS_DAMAGE_PER_HIT = 50;
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
	/** Boss of the level. */
	private Boss boss;
	/** Time the defeated boss stays on screen as an explosion. */
	private Cooldown bossExplosionCooldown;
	/** Boss attacks, events and links to other systems. */
	private BossManager bossManager;
	/** Set of all bullets fired by the boss. */
	private Set<BossBullet> bossBullets;
	/** Bonus enemy ship that appears sometimes. */
	private EnemyShip enemyShipSpecial;
	/** Minimum time between bonus ship appearances. */
	private Cooldown enemyShipSpecialCooldown;
	/** Time until bonus ship explosion disappears. */
	private Cooldown enemyShipSpecialExplosionCooldown;
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
	 * @param bonnusLife
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
		super(width, height, fps);

		this.gameSettings = gameSettings;
		this.bonusLife = bonusLife;
		this.level = gameState.getLevel();
		this.score = gameState.getScore();

		int initialLives = gameState.getLivesRemaining()
				+ (this.bonusLife ? 1 : 0);

		this.playerStatus = new PlayerStatus(initialLives, Core.getMaxLives());
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
		this.ship = new Ship(this.width / 2, this.height - 30);
		this.bossBullets = new HashSet<BossBullet>();
		this.bossManager = new BossManager();
		// Logs boss events until HUD and Visual Effect listeners are added.
		this.bossManager.getEventManager().addListener(
				new LoggingBossEventListener());
		// The boss appears only on boss stages.
		if (isBossLevel(this.level))
			this.boss = this.bossManager.spawn(new BossSpawnRequest(
					Boss.BossType.BOSS_A, this.width / 2 - Boss.WIDTH / 2, 78,
					this.level, 0));
		this.bossExplosionCooldown = Core.getCooldown(BOSS_EXPLOSION_TIME);
		// Appears each 10-30 seconds.
		this.enemyShipSpecialCooldown = Core.getVariableCooldown(
				BONUS_SHIP_INTERVAL, BONUS_SHIP_VARIANCE);
		this.enemyShipSpecialCooldown.reset();
		this.enemyShipSpecialExplosionCooldown = Core
				.getCooldown(BONUS_SHIP_EXPLOSION);
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

				boolean isRightBorder = this.ship.getPositionX()
						+ this.ship.getWidth() + this.ship.getSpeed() > this.width - 1;
				boolean isLeftBorder = this.ship.getPositionX()
						- this.ship.getSpeed() < 1;

				if (moveRight && !isRightBorder) {
					this.ship.moveRight();
				}
				if (moveLeft && !isLeftBorder) {
					this.ship.moveLeft();
				}
				if (inputManager.isKeyDown(KeyEvent.VK_SPACE))
					if (this.ship.shoot(this.bullets))
						this.bulletsShot++;
			}

			if (this.enemyShipSpecial != null) {
				if (!this.enemyShipSpecial.isDestroyed())
					this.enemyShipSpecial.move(2, 0);
				else if (this.enemyShipSpecialExplosionCooldown.checkFinished())
					this.enemyShipSpecial = null;

			}
			if (this.enemyShipSpecial == null
					&& this.enemyShipSpecialCooldown.checkFinished()) {
				this.enemyShipSpecial = new EnemyShip();
				this.enemyShipSpecialCooldown.reset();
				this.logger.info("A special ship appears");
			}
			if (this.enemyShipSpecial != null
					&& this.enemyShipSpecial.getPositionX() > this.width) {
				this.enemyShipSpecial = null;
				this.logger.info("The special ship has escaped");
			}
			if (this.boss != null) {
				this.boss.update(1, this.width - 1);
				this.bossManager.update(
						this.ship.getPositionX() + this.ship.getWidth() / 2,
						this.ship.getPositionY(), this.bossBullets);
				// Removes the boss once its explosion has been shown.
				if (this.boss.isDefeated()
						&& this.bossExplosionCooldown.checkFinished())
					this.boss = null;
			}

			this.ship.update();
			// Once every ship and explosion is gone, the formation stops
			// updating, so it keeps valid position and size values.
			if (this.enemyShipFormation.iterator().hasNext())
				this.enemyShipFormation.update();
			// An empty formation has no shooters left, so it must not shoot.
			if (!this.enemyShipFormation.isEmpty())
				this.enemyShipFormation.shoot(this.bullets);
		}

		manageCollisions();
		cleanBullets();
		draw();

		// The level is cleared only when the enemies and the boss are gone.
		// The boss becomes null after its defeat explosion has been shown.
		boolean levelCleared = this.enemyShipFormation.isEmpty()
				&& this.boss == null;
		if ((levelCleared || !this.playerStatus.isAlive())
				&& !this.levelFinished) {
			this.levelFinished = true;
			this.screenFinishedCooldown.reset();
		}

		if (this.levelFinished && this.screenFinishedCooldown.checkFinished())
			this.isRunning = false;

	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		drawManager.drawEntity(this.ship, this.ship.getPositionX(),
				this.ship.getPositionY());
		if (this.enemyShipSpecial != null)
			drawManager.drawEntity(this.enemyShipSpecial,
					this.enemyShipSpecial.getPositionX(),
					this.enemyShipSpecial.getPositionY());

		if (this.boss != null)
			drawManager.drawEntity(this.boss, this.boss.getPositionX(),
					this.boss.getPositionY());

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
				if (this.enemyShipSpecial != null
						&& !this.enemyShipSpecial.isDestroyed()
						&& checkCollision(bullet, this.enemyShipSpecial)) {
					this.score += this.enemyShipSpecial.getPointValue();
					this.shipsDestroyed++;
					this.enemyShipSpecial.destroy();
					this.enemyShipSpecialExplosionCooldown.reset();
					recyclable.add(bullet);
				}
				// A bullet that already hit a ship cannot also hit the boss.
				if (this.boss != null && !this.boss.isDefeated()
						&& !recyclable.contains(bullet)
						&& checkCollision(bullet, this.boss)) {
					this.boss.takeDamage(BOSS_DAMAGE_PER_HIT);
					this.bossManager.sync();
					recyclable.add(bullet);
					this.logger.info("Boss hit: "
							+ this.boss.getCurrentHealth() + "/"
							+ this.boss.getMaxHealth() + ", phase "
							+ this.boss.getPhase());
					if (this.boss.isDefeated()) {
						this.bossExplosionCooldown.reset();
						this.logger.info("Boss defeated");
					}
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