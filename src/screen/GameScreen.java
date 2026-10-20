package screen;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Random;

import entity.item.ChameleonHull;
import entity.item.ItemManager;
import entity.item.ShieldItem;
import engine.Cooldown;
import engine.Core;
import engine.GameSettings;
import engine.GameState;
import engine.ControlsConfig;
import entity.Bullet;
import boss.BossManager;
import boss.BossSpawnRequest;
import boss.BossStatus;
import boss.LoggingBossEventListener;
import entity.BonusEnemyManager;
import entity.Boss;
import entity.BossBullet;
import entity.BulletPool;
import entity.BulletType;
import vfx.ParticleSystem;
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.Entity;
import entity.FallingObstacle;
import entity.LoggingPlayerStatusListener;
import entity.PlayerStatusListener;
import entity.PlayerStatusEvent;
import entity.PlayerStatus;
import entity.Ship;
import entity.ShipType;
import audio.AudioManager;
import records.RunSession;
import records.RecordsService;
import records.Achievement;
import event.EnemyDefeatedEvent;
import event.GameplayEventListener;
import event.GameplayEventPublisher;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.UUID;
import entity.item.Hyperfire;
import entity.item.OrbitalAnnihilator;

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
	/** Average time between falling obstacle spawns, in milliseconds. */
	private static final int FALLING_OBSTACLE_INTERVAL = 17500;
	/** Spawn interval varies from fifteen to twenty seconds. */
	private static final int FALLING_OBSTACLE_VARIANCE = 2500;
	/**
	 * Enables the debug keys used to exercise player status by hand. Off
	 * unless the game is started with -Dinvaders.debugKeys=true, so a normal
	 * play session on main never has them.
	 */
	private static final boolean DEBUG_KEYS =
			Boolean.getBoolean("invaders.debugKeys");
	/** Height of the interface separation line. */
	private static final int SEPARATION_LINE_HEIGHT = 40;
	/** Damage dealt to the boss by one player shot (to confirm with the
	 * weapon developer). */
	private static final int BOSS_DAMAGE_PER_SHOT = 50;
	/** Time the boss explosion stays on screen. */
	private static final int BOSS_EXPLOSION_TIME = 500;
	/** Stages where a boss appears (agreed with the team, may change). */
	private static final int[] BOSS_LEVELS = { 3, 5, 7 };
	/** Time the space key must be held to trigger the Annihilator. */
	private static final int ANNIHILATOR_HOLD_TIME = 1000;

	/** Time the defeated boss stays on screen as an explosion. */
	private Cooldown bossExplosionCooldown;
	/** Boss of the level, its attacks, events and links to other systems.
	 * The only source of truth for the boss. */
	private BossManager bossManager;
	/** Set of all bullets fired by the boss. */
	private Set<BossBullet> bossBullets;

	private ItemManager itemManager;
	private final Random itemDropRandom = new Random();
	private final Random obstacleRandom = new Random();
	/**
	 * Active falling hazards for this level. Kept as a set so the collection
	 * can support multiple hazards if the spawn policy is expanded later.
	 */
	private Set<FallingObstacle> fallingObstacles;
	/** Time until the next falling hazard appears. */
	private Cooldown fallingObstacleCooldown;
	private boolean chameleonDroppedThisLevel = false;
	private boolean shieldDroppedThisLevel = false;

	/** Player ship type selected before the stage started. */
	private ShipType shipType;
	/** Current game difficulty settings. */
	private GameSettings gameSettings;
	/** Settings of every wave in this stage, in play order. */
	private List<GameSettings> waves;
	/** Index of the wave currently being played. */
	private int currentWave;
	/** Current difficulty level number. */
	private int level;
	/** Formation of enemy ships. */
	private EnemyShipFormation enemyShipFormation;
	/** Player's ship. */
	private Ship ship;
	/** Bonus enemy ship that appears sometimes. */
	private BonusEnemyManager bonusEnemyManager;
	/** Time from finishing the level to screen change. */
	private Cooldown screenFinishedCooldown;
	/** Set of all bullets fired by on screen ships. */
	private Set<Bullet> bullets;
		/** Explosion particles. */
	private ParticleSystem particleSystem = new ParticleSystem();
	/** Current score. */
	private int score;
	/** Player lives left. */
	private PlayerStatus playerStatus;
	/** State of the debug shield key on the previous frame. */
	private boolean shieldDebugKeyWasDown;
	/** Total bullets shot by the player. */
	private int bulletsShot;
	/** Total ships destroyed by the player. */
	private int shipsDestroyed;
	/** Moment the game starts. */
	private long gameStartTime;
	/** Checks if the stage was cleared. */
	private boolean stageCleared;
	/** Checks if the level is finished. */
	private boolean levelFinished;
	/** Checks if a bonus life is received. */
	private boolean bonusLife;
	/** Orbital Annihilator (Doomsday Laser), only available on level 7. */
	private OrbitalAnnihilator annihilator;
	/** Moment the space key started being held, 0 if not held. */
	private long spaceHeldSince;
	/** Last countdown number that played a sound. */
	private int lastCountdown = -1;
	private RunSession runSession;
    private RecordsService recordsService;
    private Achievement visibleAchievement;
    private long achievementNoticeUntil;
    private final GameplayEventPublisher recordsEvents = new GameplayEventPublisher();
    private final Map<EnemyShip,String> defeatIds = new IdentityHashMap<EnemyShip,String>();
	/** Prevents repeated activation while holding H. */
	private boolean hyperfireKeyPressed = false;
    /** Flag to track if the urgent low-health BGM is currently playing. */
    private boolean isLowHealthBGM = false;

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
		this(gameState, Collections.singletonList(gameSettings), bonusLife,
				width, height, fps, ShipType.STANDARD);
	}

	/**
	 * Constructor for a stage played with a selected player ship.
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
		this(gameState, Collections.singletonList(gameSettings), bonusLife,
				width, height, fps, shipType);
	}

	/**
	 * Constructor for a stage with one or more waves.
	 *
	 * @param gameState
	 * 			Current game state.
	 * @param waves
	 * 			Settings of each wave, in play order.
	 * @param bonusLife
	 * 			Checks if a bonus life is awarded this level.
	 * @param width
	 * 			Screen width.
	 * @param height
	 * 			Screen height.
	 * @param fps
	 * 			Frames per second, frame rate at which the game is run.
	 */
	public GameScreen(final GameState gameState,
					  final List<GameSettings> waves, final boolean bonusLife,
					  final int width, final int height, final int fps) {
		this(gameState, waves, bonusLife, width, height, fps,
				ShipType.STANDARD, null);
	}

	/**
	 * Constructor for a stage with one or more waves, played with a selected
	 * player ship.
	 *
	 * @param gameState
	 * 			Current game state.
	 * @param waves
	 * 			Settings of each wave, in play order.
	 * @param bonusLife
	 * 			Checks if a bonus life is awarded this level.
	 * @param width
	 * 			Screen width.
	 * @param height
	 * 			Screen height.
	 * @param fps
	 * 			Frames per second, frame rate at which the game is run.
	 * @param shipType
	 * 			Selected player ship type.
	 */
	public GameScreen(final GameState gameState,
					  final List<GameSettings> waves, final boolean bonusLife,
					  final int width, final int height, final int fps,
					  final ShipType shipType) {
		this(gameState, waves, bonusLife, width, height, fps, shipType, null);
	}

	/**
	 * Constructor for a single-wave stage that records its run.
	 *
	 * @param runSession
	 * 			Session the run is recorded under, or null for no records.
	 */
	public GameScreen(final GameState gameState,
					  final GameSettings gameSettings, final boolean bonusLife,
					  final int width, final int height, final int fps,
					  final RunSession runSession) {
		this(gameState, Collections.singletonList(gameSettings), bonusLife,
				width, height, fps, ShipType.STANDARD, runSession);
	}

	/**
	 * Constructor for a multi-wave stage that records its run.
	 *
	 * @param runSession
	 * 			Session the run is recorded under, or null for no records.
	 */
	public GameScreen(final GameState gameState,
					  final List<GameSettings> waves, final boolean bonusLife,
					  final int width, final int height, final int fps,
					  final RunSession runSession) {
		this(gameState, waves, bonusLife, width, height, fps,
				ShipType.STANDARD, runSession);
	}

	/**
	 * Constructor for a single-wave stage played with a selected ship and
	 * recorded under a run session.
	 */
	public GameScreen(final GameState gameState,
					  final GameSettings gameSettings, final boolean bonusLife,
					  final int width, final int height, final int fps,
					  final ShipType shipType, final RunSession runSession) {
		this(gameState, Collections.singletonList(gameSettings), bonusLife,
				width, height, fps, shipType, runSession);
	}

	/**
	 * Constructor for a stage with one or more waves, played with a selected
	 * player ship and recorded under a run session. Every other constructor
	 * reaches this one.
	 *
	 * @param gameState
	 * 			Current game state.
	 * @param waves
	 * 			Settings of each wave, in play order.
	 * @param bonusLife
	 * 			Checks if a bonus life is awarded this level.
	 * @param width
	 * 			Screen width.
	 * @param height
	 * 			Screen height.
	 * @param fps
	 * 			Frames per second, frame rate at which the game is run.
	 * @param shipType
	 * 			Selected player ship type.
	 * @param runSession
	 * 			Session the run is recorded under, or null for no records.
	 */
	public GameScreen(final GameState gameState,
					  final List<GameSettings> waves, final boolean bonusLife,
					  final int width, final int height, final int fps,
					  final ShipType shipType, final RunSession runSession) {
		super(width, height, fps);

		this.runSession = runSession;
		this.recordsService = Core.getRecordsService();
		this.recordsEvents.subscribe(EnemyDefeatedEvent.class,
				new GameplayEventListener<EnemyDefeatedEvent>() {
			public void onEvent(final EnemyDefeatedEvent event) {
				if (recordsService != null)
					recordsService.enemyDefeated(event.getRunId(),
							event.getEventId());
			}
		});

		if (shipType == null)
			throw new IllegalArgumentException("Ship type cannot be null.");

		this.shipType = shipType;
		this.waves = copyWaves(waves);
		this.currentWave = 0;
		this.gameSettings = this.waves.get(0);
		this.bonusLife = bonusLife;
		this.level = gameState.getLevel();
		this.score = gameState.getScore();
		this.playerStatus = new PlayerStatus(
				Math.min(gameState.getLivesRemaining(),
						this.shipType.getMaxLives()),
				this.shipType.getMaxLives());
		if (this.bonusLife)
			this.playerStatus.gainLife();
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
		this.itemManager = new ItemManager(this.height);

		this.shieldDroppedThisLevel = false;
		this.chameleonDroppedThisLevel = false;
		this.hyperfireKeyPressed = false;

		this.bonusEnemyManager = new BonusEnemyManager();
		this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);
		this.bullets = new HashSet<Bullet>();
		this.fallingObstacles = new HashSet<FallingObstacle>();
		this.fallingObstacleCooldown = Core.getVariableCooldown(
				FALLING_OBSTACLE_INTERVAL, FALLING_OBSTACLE_VARIANCE);
		this.fallingObstacleCooldown.reset();
		this.annihilator = new OrbitalAnnihilator();

		this.bossBullets = new HashSet<BossBullet>();
		this.bossManager = new BossManager();
		// Logs boss events until HUD and Visual Effect listeners are added.
		this.bossManager.getEventManager().addListener(
				new LoggingBossEventListener());
		// Same for the player's own status events.
		this.playerStatus.addListener(new LoggingPlayerStatusListener());
		// Forward them onto the shared gameplay bus as well, so a system that
		// already listens there, such as sound effects, does not have to know
		// about PlayerStatus to receive them.
		this.playerStatus.addListener(new PlayerStatusListener() {
			public void onPlayerStatusEvent(final PlayerStatusEvent event) {
				recordsEvents.publish(event);
			}
		});
		// The boss appears only on boss stages.
		if (isBossLevel(this.level))
			this.bossManager.spawn(new BossSpawnRequest(
					Boss.BossType.BOSS_A, this.width / 2 - Boss.WIDTH / 2, 78,
					this.level, 0));
		this.bossExplosionCooldown = Core.getCooldown(BOSS_EXPLOSION_TIME);

		// Special input delay / countdown.
		this.gameStartTime = System.currentTimeMillis();
		this.inputDelay = Core.getCooldown(INPUT_DELAY);
		this.inputDelay.reset();

        if (this.playerStatus.getLives() == 1) {
            AudioManager.playBGM("audio/bgm/music_game_urgent.wav");
            this.isLowHealthBGM = true;
        } else {
            AudioManager.playBGM("audio/bgm/music_game.wav");
            this.isLowHealthBGM = false;
        }
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code.
	 */
	public final int run() {
        try { super.run(); }
        finally { if (this.runSession != null) this.runSession.stopActiveSegment(System.nanoTime()); }

		// Life bonus is only awarded when the stage was cleared.
		if (this.stageCleared)
			this.score += LIFE_SCORE * (this.playerStatus.getLives() - 1);
		this.logger.info("Screen cleared with a score of " + this.score);

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();
        if (this.recordsService != null) {
            long now = System.currentTimeMillis();
            if (this.visibleAchievement == null || now >= this.achievementNoticeUntil) {
                this.visibleAchievement = this.recordsService.pollNotification();
                this.achievementNoticeUntil = this.visibleAchievement == null ? 0 : now + 3000;
            }
        }

		// Outside the input-delay check: a shield that runs out has to be
		// announced even while the level is finishing.
		this.playerStatus.update();
		// Reconcile shields active outside the item-collision path.
		clearSlowIfEnergyShieldActive();

        if (this.inputDelay.checkFinished() && !this.levelFinished) {
            if (this.runSession != null) this.runSession.startActiveSegment(System.nanoTime());
            if (this.runSession != null && this.recordsService != null)
                this.recordsService.activeDuration(this.runSession.getRunId(),
                        this.runSession.getActiveDurationMillis(System.nanoTime()));

			if (!this.ship.isDestroyed()) {
				boolean moveRight = inputManager.isKeyDown(
						ControlsConfig.getKey(ControlsConfig.RIGHT));
				boolean moveLeft = inputManager.isKeyDown(
						ControlsConfig.getKey(ControlsConfig.LEFT));

				boolean isRightBorder = this.ship.getPositionX()
						+ this.ship.getWidth() + this.ship.getSpeed() > this.width - 1;
				boolean isLeftBorder = this.ship.getPositionX()
						- this.ship.getSpeed() < 1;

				if (moveRight && !isRightBorder) {
					this.ship.moveRight();
					clampShipPosition();
				}
				if (moveLeft && !isLeftBorder) {
					this.ship.moveLeft();
					clampShipPosition();
				}

				// Normal shooting using configured controls.
				if (inputManager.isKeyDown(
						ControlsConfig.getKey(ControlsConfig.FIRE))) {
					if (!this.annihilator.isCharging()) {
						this.bulletsShot += this.ship.shoot(this.bullets);
					}
				}

				// Hyperfire test activation with the H key.
				boolean hyperfireKeyDown = inputManager.isKeyDown(KeyEvent.VK_H);
				if (hyperfireKeyDown && !this.hyperfireKeyPressed) {
					this.itemManager.getBuffManager().addBuff(
							new Hyperfire(0, 0), this.ship);
				}
				this.hyperfireKeyPressed = hyperfireKeyDown;

				// Orbital Annihilator charging mechanic.
				if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
					if (!this.annihilator.isReady()) {
						this.spaceHeldSince = 0;
					} else if (this.spaceHeldSince == 0) {
						this.spaceHeldSince = System.currentTimeMillis();
					}

					if (this.spaceHeldSince != 0
							&& this.level == OrbitalAnnihilator.REQUIRED_LEVEL
							&& System.currentTimeMillis() - this.spaceHeldSince >= ANNIHILATOR_HOLD_TIME) {
						this.annihilator.trigger();
						// Charging stops the ship shooting, so a burst that
						// is still owed is dropped rather than fired when
						// the beam is over.
						this.ship.cancelBurst();
					}
				} else {
					this.spaceHeldSince = 0;
				}

				// Temporary: the shield has no in-game source until B5 adds
				// the buff item, so it cannot be verified by playing without
				// this key. Removed in that PR.
				if (DEBUG_KEYS) {
					boolean shieldKeyDown = inputManager
							.isKeyDown(KeyEvent.VK_F2);
					// Fires on the press, not every frame the key is held,
					// so holding F2 cannot chain shields over the expiry and
					// hide the very timing this key exists to show.
					if (shieldKeyDown && !this.shieldDebugKeyWasDown) {
						this.playerStatus.activateShield();
						this.logger.info("Debug shield granted.");
					}
					this.shieldDebugKeyWasDown = shieldKeyDown;
				}
			} else {
				// Cancel charging when the ship is destroyed.
				this.annihilator.cancelCharge();
				this.spaceHeldSince = 0;
			}

			this.bonusEnemyManager.update(this.width);

			Boss boss = getBossOnScreen();
			if (boss != null) {
				boss.update(1, this.width - 1);
				this.bossManager.update(
						this.ship.getPositionX() + this.ship.getWidth() / 2,
						this.ship.getPositionY(), this.bossBullets);
			}

			if (!this.ship.isDestroyed() && !this.annihilator.isCharging())
				this.bulletsShot += this.ship.updateBurst(this.bullets);

			updateBallMovement();

			this.annihilator.update();
			this.enemyShipFormation.update();
			this.enemyShipFormation.shoot(this.bullets, this.ship);
		}

		this.ship.update();

		if (this.inputDelay.checkFinished() && !this.levelFinished) {
			this.itemManager.update(this.ship);

			if (!this.ship.isDestroyed()) {
				this.itemManager.checkCollisions(this.ship);
				// Clear in this frame if an Energy Shield item was just collected.
				clearSlowIfEnergyShieldActive();
			}

			clampShipPosition();
			clampBallPositionY();
		}

		updateFallingObstacles();
		manageFallingObstacleCollisions();
		manageCollisions();
		manageBallContact();

		if (this.annihilator.isFiring())
			manageAnnihilatorBeam();

		cleanBullets();
		this.particleSystem.update(); 
		draw();

		if (!this.levelFinished) {
			if (!this.playerStatus.isAlive()) {
				this.levelFinished = true;
				this.stageCleared = false;
				this.fallingObstacles.clear();
				this.screenFinishedCooldown.reset();
				this.logger.info("Stage " + this.level + " failed.");
			} else if (this.enemyShipFormation.isEmpty() && isBossCleared()) {
				if (this.currentWave < this.waves.size() - 1) {
					startNextWave();
				} else {
					this.levelFinished = true;
					this.stageCleared = true;
					this.fallingObstacles.clear();
					this.screenFinishedCooldown.reset();
					this.logger.info("Stage " + this.level + " cleared.");
				}
			}
		}

        if (this.levelFinished && this.runSession != null)
            this.runSession.stopActiveSegment(System.nanoTime());

		if (this.levelFinished && this.screenFinishedCooldown.checkFinished())
			this.isRunning = false;
	}

	/**
	 * Copies the wave list after checking that it can be played.
	 *
	 * @param waves
	 *            Settings of each wave, in play order.
	 * @return A copy of the wave list.
	 * @throws IllegalArgumentException
	 *             If the list is null or empty, or contains a null wave.
	 */
	static List<GameSettings> copyWaves(final List<GameSettings> waves) {
		if (waves == null || waves.isEmpty())
			throw new IllegalArgumentException(
					"A stage must contain at least one wave.");
		List<GameSettings> copy = new ArrayList<GameSettings>(waves);
		for (int i = 0; i < copy.size(); i++)
			if (copy.get(i) == null)
				throw new IllegalArgumentException(
						"Wave " + (i + 1) + " must not be null.");
		return copy;
	}

	/**
	 * Replaces the empty formation with the next wave's formation.
	 */
	private void startNextWave() {
		this.fallingObstacles.clear();
		this.fallingObstacleCooldown.reset();
		this.logger.info("Stage " + this.level + " wave " +
				(this.currentWave + 1) + " cleared.");
		this.currentWave++;
		this.gameSettings = this.waves.get(this.currentWave);
		this.enemyShipFormation = new EnemyShipFormation(this.gameSettings);
		this.enemyShipFormation.attach(this);
		this.logger.info("Stage " + this.level + " wave " + (this.currentWave + 1) +
				" of " + this.waves.size() + " started.");
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		if (this.annihilator.isCharging() || this.annihilator.isFiring())
			drawManager.drawOrbitalAnnihilator(
					this.ship,
					OrbitalAnnihilator.BEAM_WIDTH,
					SEPARATION_LINE_HEIGHT,
					this.annihilator.isFiring(),
					this.annihilator.getChargeProgress()
			);

		drawManager.drawEntity(this.ship, this.ship.getPositionX(),
				this.ship.getPositionY());

		for (FallingObstacle obstacle : this.fallingObstacles)
			drawManager.drawEntity(obstacle, obstacle.getPositionX(),
					obstacle.getPositionY());

		if (this.ship.isShieldActive() && !this.ship.isDestroyed()) {
			drawManager.drawShield(this, this.ship);
		}

		EnemyShip bonusShip = this.bonusEnemyManager.getShip();
		if (bonusShip != null)
			drawManager.drawEntity(bonusShip, bonusShip.getPositionX(),
					bonusShip.getPositionY());

		Boss boss = getBossOnScreen();
		if (boss != null)
			drawManager.drawEntity(boss, boss.getPositionX(),
					boss.getPositionY());

		for (BossBullet bossBullet : this.bossBullets)
			drawManager.drawEntity(bossBullet, bossBullet.getPositionX(),
					bossBullet.getPositionY());

		enemyShipFormation.draw();

		for (Bullet bullet : this.bullets)
			drawManager.drawEntity(bullet, bullet.getPositionX(),
					bullet.getPositionY());

		this.itemManager.draw(this.drawManager);

		// Interface.
		drawManager.drawScore(this, this.score);
		drawManager.drawLives(this, this.playerStatus.getLives());

		if (this.level == OrbitalAnnihilator.REQUIRED_LEVEL)
			drawManager.drawAnnihilatorGauge(
					this,
					this.annihilator.getRechargeProgress()
			);

		drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);

		// Countdown to game start.
		if (!this.inputDelay.checkFinished()) {
			int countdown = (int) ((INPUT_DELAY
					- (System.currentTimeMillis()
					- this.gameStartTime)) / 1000);
			playCountdownSound(countdown);
			drawManager.drawCountDown(this, this.level, countdown,
					this.bonusLife);
			drawManager.drawHorizontalLine(this, this.height / 2 - this.height
					/ 12);
			drawManager.drawHorizontalLine(this, this.height / 2 + this.height
					/ 12);
		}
		drawManager.drawParticles(this.particleSystem); 

        if (this.visibleAchievement != null)
            drawManager.drawAchievementNotice(this, this.visibleAchievement);
		drawManager.completeDrawing(this);
	}

	/**
	 * Cleans bullets that go off screen.
	 */
	private void cleanBullets() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets) {
			bullet.update();
			// An aimed shot can travel almost horizontally, so a bullet that
			// leaves sideways has to be retired as well. Checking only the Y
			// axis left a bullet with no vertical speed on screen for the
			// rest of the level, drawn and collision-checked every frame and
			// never returned to the pool.
			if (bullet.getPositionY() < SEPARATION_LINE_HEIGHT
					|| bullet.getPositionY() > this.height
					|| bullet.getPositionX() + bullet.getWidth() < 0
					|| bullet.getPositionX() > this.width)
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

	/** Creates, advances, and retires the non-damaging falling hazards. */
	private void updateFallingObstacles() {
		if (this.levelFinished) {
			this.fallingObstacles.clear();
			return;
		}
		if (!this.inputDelay.checkFinished())
			return;

		if (this.fallingObstacles.isEmpty()
				&& this.fallingObstacleCooldown.checkFinished()) {
			int rightmostX = this.width - FallingObstacle.WIDTH - 1;
			int positionX = 1 + this.obstacleRandom.nextInt(rightmostX);
			this.fallingObstacles.add(new FallingObstacle(positionX,
					SEPARATION_LINE_HEIGHT));
		}

		for (Iterator<FallingObstacle> iterator =
				this.fallingObstacles.iterator(); iterator.hasNext();) {
			FallingObstacle obstacle = iterator.next();
			obstacle.update();
			if (obstacle.hasLeftScreen(this.height)) {
				iterator.remove();
				this.fallingObstacleCooldown.reset();
			}
		}
	}

	/** Consumes a colliding hazard, applying only the existing slow effect. */
	private void manageFallingObstacleCollisions() {
		if (!this.inputDelay.checkFinished() || this.levelFinished)
			return;

		for (Iterator<FallingObstacle> iterator =
				this.fallingObstacles.iterator(); iterator.hasNext();) {
			FallingObstacle obstacle = iterator.next();
			if (checkCollision(obstacle, this.ship)) {
				// Remove it before applying the effect so one object can only
				// trigger a single collision, even if listeners inspect state.
				iterator.remove();
				this.fallingObstacleCooldown.reset();
				if (!this.ship.isDestroyed()
						&& !this.ship.isShieldActive())
					this.playerStatus.applySlow(this.ship);
			}
		}
	}

	/** Clears an existing slow as soon as the Energy Shield item is active. */
	private void clearSlowIfEnergyShieldActive() {
		if (this.ship.isShieldActive())
			this.playerStatus.clearSlowDebuff();
	}

	/**
	 * Manages collisions between bullets and ships.
	 */
	private void manageCollisions() {
        if (this.levelFinished) return;
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets)
			if (bullet.isEnemyBullet()) {
				if (checkCollision(bullet, this.ship) && !this.levelFinished) {
					recyclable.add(bullet);
					damagePlayer(1, "Hit on player ship");
				}
			} else {
				for (EnemyShip enemyShip : this.enemyShipFormation)
					if (!enemyShip.isDestroyed()
							&& checkCollision(bullet, enemyShip)) {
						this.score += enemyShip.getPointValue();
						this.shipsDestroyed++;
						recordEnemyDefeat(enemyShip);
						this.enemyShipFormation.destroy(enemyShip);
						this.particleSystem.spawnEnemyExplosion(
								enemyShip.getPositionX() + enemyShip.getWidth() / 2,
								enemyShip.getPositionY() + enemyShip.getHeight() / 2);
						tryDropChameleonHull(enemyShip);
						recyclable.add(bullet);

						// --- DROP DU BOUCLIER ---
						if (!this.shieldDroppedThisLevel) {
							boolean shouldDrop = (Math.random() < 0.15)
									|| (this.enemyShipFormation.isEmpty());
							if (shouldDrop) {
								this.itemManager.spawnItem(
										new ShieldItem(
												enemyShip.getPositionX(),
												enemyShip.getPositionY()
										)
								);
								this.shieldDroppedThisLevel = true;
								this.logger.info("Shield item dropped from enemy ship at ("
										+ enemyShip.getPositionX() + ", "
										+ enemyShip.getPositionY() + ").");
							}
						}
					}
				EnemyShip bonusShip = this.bonusEnemyManager.getShip();
				if (this.bonusEnemyManager.isActive()
						&& checkCollision(bullet, bonusShip)) {
					recordEnemyDefeat(bonusShip);
					this.score += this.bonusEnemyManager.destroy();
					this.shipsDestroyed++;
					this.particleSystem.spawnEnemyExplosion(
							bonusShip.getPositionX() + bonusShip.getWidth() / 2,
							bonusShip.getPositionY() + bonusShip.getHeight() / 2);
					tryDropChameleonHull(bonusShip);
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
				damagePlayer(bossBullet.getDamage(),
						"Hit on player ship by the boss");
			}
		this.bossBullets.removeAll(hitBossBullets);
	}

	/**
	 * Applies the Orbital Annihilator beam: destroys every enemy in the
	 * corridor and neutralizes enemy projectiles inside it.
	 */
	private void manageAnnihilatorBeam() {
        if (this.levelFinished) return;
		for (EnemyShip enemyShip : this.enemyShipFormation)
			if (!enemyShip.isDestroyed()
					&& OrbitalAnnihilator.hits(this.ship, enemyShip)) {
				this.score += enemyShip.getPointValue();
				this.shipsDestroyed++;
				recordEnemyDefeat(enemyShip);

				this.enemyShipFormation.destroy(enemyShip);
				tryDropChameleonHull(enemyShip);
			}

		EnemyShip bonusShip = this.bonusEnemyManager.getShip();
		if (this.bonusEnemyManager.isActive()
				&& OrbitalAnnihilator.hits(this.ship, bonusShip)) {
			recordEnemyDefeat(bonusShip);
			this.score += this.bonusEnemyManager.destroy();
			this.shipsDestroyed++;
			tryDropChameleonHull(bonusShip);
		}

		Set<Bullet> neutralized = new HashSet<Bullet>();

		for (Bullet bullet : this.bullets)
			if (bullet.isEnemyBullet()
					&& OrbitalAnnihilator.hits(this.ship, bullet))
				neutralized.add(bullet);

		this.bullets.removeAll(neutralized);
		BulletPool.recycle(neutralized);
	}

	/**
	 * Applies one hit to the player. Every source of damage goes through this
	 * method, so that the protections and the effects of a hit cannot differ
	 * between an enemy bullet and a boss bullet.
	 *
	 * <p>The order matters. A protection returns before any life is taken,
	 * and the ship is destroyed only once the damage was actually accepted,
	 * which also stops several bullets in the same frame from each costing a
	 * life: the first accepted hit starts the destruction cooldown and the
	 * rest are turned away by {@link Ship#isDestroyed()}.</p>
	 *
	 * @param damage
	 *            Amount of damage the source deals.
	 * @param logMessage
	 *            Description of the source, for the log line.
	 * @return Whether a life was lost.
	 */
	// The damage path is accessible to tests in the screen package.
	boolean damagePlayer(final int damage, final String logMessage) {
		// The Chameleon Hull makes the ship immune while it is active.
		if (this.ship.isChameleonHullActive())
			return false;
		// Still inside the recovery period from an earlier hit.
		if (this.ship.isDestroyed())
			return false;
		if (this.ship.isShieldActive()) {
			this.logger.info("Shot absorbed by Energy Shield.");
			// The Energy Shield belongs to Ship, so PlayerStatus cannot see
			// this hit by itself. Announce it anyway, or a listener would
			// hear a blocked hit only when the timed shield was the one that
			// blocked it.
			this.playerStatus.announceDamageBlocked(damage);
			return false;
		}
		// PlayerStatus owns the timed shield, so its answer decides whether
		// the hit lands at all; the effects below must not run otherwise.
		if (!this.playerStatus.takeDamage(damage))
			return false;

		this.ship.destroy();
		this.ship.triggerDamageFeedback();
		this.particleSystem.spawnPlayerExplosion(
				this.ship.getPositionX() + this.ship.getWidth() / 2,
				this.ship.getPositionY() + this.ship.getHeight() / 2);
		this.itemManager.getBuffManager().resetAllBuffs(this.ship);
		if (this.playerStatus.getLives() == 1 && !this.isLowHealthBGM) {
			AudioManager.playBGM("audio/bgm/music_game_urgent.wav");
			this.isLowHealthBGM = true;
		}
		this.logger.info(logMessage + ", " + this.playerStatus.getLives()
				+ " lives remaining.");
		return true;
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
	 * Checks if the stage was cleared.
	 *
	 * @return True if all enemies were defeated with the ship alive.
	 */
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
			return BOSS_DAMAGE_PER_SHOT / BulletType.RAPID.getShotCount();
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
	 * Checks whether the stage may end as far as the boss is concerned. The
	 * battle state comes from the BossManager, and a defeated boss leaves the
	 * screen only after its explosion has been shown.
	 *
	 * @return True when no boss is holding the stage open.
	 */
	private boolean isBossCleared() {
		return this.bossManager.getStatus().getBattleState()
				!= BossStatus.BattleState.IN_BATTLE
				&& getBossOnScreen() == null;
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

	public final boolean isStageCleared() {
		return this.stageCleared;
	}

	/**
	 * Gives the other systems the player's status, to read lives and shield
	 * state and to register a {@link entity.PlayerStatusListener}.
	 *
	 * @return Player status of the current game.
	 */
	public final PlayerStatus getPlayerStatus() {
		return this.playerStatus;
	}

	/**
	 * Returns a GameState object representing the status of the game.
	 *
	 * @return Current game state.
	 */
	public final GameState getGameState() {
		return new GameState(this.level, this.score,
				this.playerStatus.getLives(),
				this.bulletsShot, this.shipsDestroyed);
	}

	/**
	 * Plays the countdown sound once each time the countdown number changes.
	 *
	 * @param countdown
	 *            Number currently shown in the countdown.
	 */
	private void playCountdownSound(final int countdown) {
		if (countdown == this.lastCountdown)
			return;
		this.lastCountdown = countdown;
		if (countdown == 0)
			AudioManager.playSFX("audio/sfx/countdown_go.wav");
		else if (countdown <= 3)
			AudioManager.playSFX("audio/sfx/countdown_beep.wav");
	}

    private void recordEnemyDefeat(final EnemyShip enemy) {
        if (this.runSession == null) return;
        String id = this.defeatIds.get(enemy);
        if (id == null) { id=UUID.randomUUID().toString(); this.defeatIds.put(enemy,id); }
        this.recordsEvents.publish(new EnemyDefeatedEvent(this.runSession.getRunId(),id));
    }

    /**
     * Gives other systems the shared gameplay event bus. Player status events
     * are published here too, so a listener can subscribe with
     * {@code subscribe(PlayerStatusEvent.class, listener)} and read the type
     * from {@link entity.PlayerStatusEvent#getType()}.
     *
     * @return Shared gameplay event publisher.
     */
    public GameplayEventPublisher getGameplayEvents() { return this.recordsEvents; }

    public String getActiveRunId() { return this.runSession == null ? null : this.runSession.getRunId(); }

	/** Keep the extra movement inside the existing playfield borders. */
	private void clampShipPosition() {
		this.ship.setPositionX(clampShipPositionX(this.ship.getPositionX(),
				this.ship.getWidth(), this.width));
	}

	/**
	 * Keeps a ship position inside the playfield borders.
	 *
	 * @param positionX
	 *            Position to clamp, on the X axis.
	 * @param shipWidth
	 *            Width of the ship being moved.
	 * @param screenWidth
	 *            Width of the playfield.
	 * @return Clamped position on the X axis.
	 */
	static int clampShipPositionX(final int positionX, final int shipWidth,
			final int screenWidth) {
		final int leftBoundary = 1;
		final int rightBoundary = Math.max(leftBoundary,
				screenWidth - shipWidth - 1);
		return Math.max(leftBoundary, Math.min(positionX, rightBoundary));
	}

	private void tryDropChameleonHull(final EnemyShip enemy) {
		if (!this.chameleonDroppedThisLevel
				&& this.level >= ChameleonHull.REQUIRED_LEVEL
				&& !this.levelFinished
				&& this.playerStatus.isAlive()
				&& this.itemDropRandom.nextDouble() < ChameleonHull.DROP_CHANCE) {

			this.itemManager.spawnItem(
					new ChameleonHull(
							enemy.getPositionX(),
							enemy.getPositionY()
					)
			);
			this.chameleonDroppedThisLevel = true;
		}
	}

	/** Vertical keys are enabled only during the star transformation. */
	private void updateBallMovement() {
		if (!this.ship.isChameleonHullActive() || this.ship.isDestroyed()) {
			return;
		}

		boolean up = inputManager.isKeyDown(KeyEvent.VK_UP)
				|| inputManager.isKeyDown(KeyEvent.VK_W);
		boolean down = inputManager.isKeyDown(KeyEvent.VK_DOWN)
				|| inputManager.isKeyDown(KeyEvent.VK_S);

		if (up && !down) {
			this.ship.moveUp();
		}
		if (down && !up) {
			this.ship.moveDown();
		}

		clampBallPositionY();
	}

	private void clampBallPositionY() {
		if (this.ship.isChameleonHullActive()) {
			int minimumY = SEPARATION_LINE_HEIGHT + 1;
			int maximumY = this.height - this.ship.getHeight() - 1;

			this.ship.setPositionY(
					Math.max(
							minimumY,
							Math.min(maximumY, this.ship.getPositionY())
					)
			);
		}
	}

	/** Contact kills use the existing score, destruction and drop systems. */
	private void manageBallContact() {
		if (!this.inputDelay.checkFinished()
				|| this.levelFinished
				|| this.ship.isDestroyed()
				|| !this.ship.isChameleonHullActive()) {
			return;
		}

		for (EnemyShip enemy : this.enemyShipFormation) {
			if (!enemy.isDestroyed() && checkCollision(this.ship, enemy)) {
				this.score += enemy.getPointValue();
				this.shipsDestroyed++;
				recordEnemyDefeat(enemy);

				this.enemyShipFormation.destroy(enemy);
				tryDropChameleonHull(enemy);
			}
		}

		EnemyShip bonusShip = this.bonusEnemyManager.getShip();
		if (this.bonusEnemyManager.isActive()
				&& checkCollision(this.ship, bonusShip)) {
			recordEnemyDefeat(bonusShip);
			this.score += this.bonusEnemyManager.destroy();
			this.shipsDestroyed++;
			tryDropChameleonHull(bonusShip);
		}
	}
}
