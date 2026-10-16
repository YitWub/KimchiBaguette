
package engine;

import java.awt.Color;
/** Shared color palette for the whole game (requirement 2.1). */

public final class GameColors {


    public static final Color BACKGROUND = new Color(8, 10, 28);

    public static final Color TEXT = new Color(232, 234, 246);

    public static final Color TEXT_DIM = new Color(128, 134, 160);

    public static final Color HIGHLIGHT = new Color(72, 220, 120);


    public static final Color PLAYER = new Color(72, 220, 120);

    public static final Color PLAYER_BULLET = new Color(120, 200, 255);

    public static final Color ENEMY = new Color(232, 234, 246);

    public static final Color ENEMY_SPECIAL = new Color(235, 64, 64);

    public static final Color ENEMY_BULLET = new Color(255, 150, 40);


    public static final Color DAMAGE = new Color(235, 40, 40);

    public static final Color EXPLOSION_RED = new Color(235, 64, 40);

    public static final Color EXPLOSION_ORANGE = new Color(255, 150, 40);

    public static final Color EXPLOSION_YELLOW = new Color(255, 220, 70);


    public static final Color POWERUP_SHIELD = new Color(80, 170, 255);

    public static final Color POWERUP_LIFE = new Color(72, 220, 120);

    public static final Color POWERUP_RAPID_FIRE = new Color(190, 110, 255);


    private GameColors() {
    }
}