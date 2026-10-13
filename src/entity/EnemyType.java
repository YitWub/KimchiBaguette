package entity;

import engine.DrawManager.SpriteType;

/**
 * Types of enemy ships that the Level Design system can spawn.
 */
public enum EnemyType {
    /** Weakest enemy, worth 10 points. */
    A(SpriteType.EnemyShipA1),
    /** Medium enemy, worth 20 points. */
    B(SpriteType.EnemyShipB1),
    /** Strongest enemy, worth 30 points. */
    C(SpriteType.EnemyShipC1);

    /** Sprite used to draw this type of enemy. */
    private final SpriteType spriteType;

    EnemyType(final SpriteType spriteType) {
        this.spriteType = spriteType;
    }

    /**
     * Returns the sprite used to draw this type of enemy.
     *
     * @return Sprite type.
     */
    public SpriteType getSpriteType() {
        return this.spriteType;
    }
}