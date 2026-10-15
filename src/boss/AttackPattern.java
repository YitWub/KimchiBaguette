package boss;

import java.util.ArrayList;
import java.util.List;

import entity.BossBullet;

/**
 * Basic boss attack pattern (PRD 2.3): single, multi and spread shot.
 *
 * Boss Developer 2.
 */
public interface AttackPattern {

    /** @return Name of the pattern, used in BossAttack events. */
    String getName();

    /**
     * Creates the bullets of one attack.
     *
     * @param originX
     *            Horizontal center of the boss.
     * @param originY
     *            Bottom of the boss.
     * @param targetX
     *            Current X of the player.
     * @param targetY
     *            Current Y of the player.
     * @param damage
     *            Lives the player loses per bullet.
     * @return Bullets fired by this attack.
     */
    List<BossBullet> fire(int originX, int originY, int targetX, int targetY,
            int damage);

    /**
     * Single shot: one bullet aimed at the player's current position,
     * or fired in a fixed direction.
     */
    final class SingleShot implements AttackPattern {

        /** Speed in pixels per frame. */
        private final double speed;
        /** True to aim at the player. */
        private final boolean aimed;
        /** Direction used when not aimed, in degrees (90 is down). */
        private final double fixedAngle;

        /**
         * Creates a single shot aimed at the player.
         *
         * @param speed
         *            Speed in pixels per frame.
         */
        public SingleShot(final double speed) {
            this(speed, true, 90);
        }

        /**
         * Creates a single shot.
         *
         * @param speed
         *            Speed in pixels per frame.
         * @param aimed
         *            True to aim at the player.
         * @param fixedAngle
         *            Direction used when not aimed.
         */
        public SingleShot(final double speed, final boolean aimed,
                final double fixedAngle) {
            this.speed = speed;
            this.aimed = aimed;
            this.fixedAngle = fixedAngle;
        }

        @Override
        public String getName() {
            return "single";
        }

        @Override
        public List<BossBullet> fire(final int originX, final int originY,
                final int targetX, final int targetY, final int damage) {
            double angle = this.aimed
                    ? BossBullet.angleTo(originX, originY, targetX, targetY)
                    : this.fixedAngle;
            List<BossBullet> bullets = new ArrayList<BossBullet>();
            bullets.add(BossBullet.fromAngle(originX, originY, angle,
                    this.speed, damage, false));
            return bullets;
        }
    }

    /**
     * Multi shot: several bullets at the same time in preset directions,
     * which reduces the space the player can dodge to.
     */
    final class MultiShot implements AttackPattern {

        /** Speed in pixels per frame. */
        private final double speed;
        /** Directions of the bullets in degrees (90 is down). */
        private final double[] angles;

        /**
         * Creates a multi shot.
         *
         * @param speed
         *            Speed in pixels per frame.
         * @param angles
         *            Directions of the bullets in degrees.
         */
        public MultiShot(final double speed, final double... angles) {
            if (angles.length == 0)
                throw new IllegalArgumentException(
                        "Multi shot needs at least one direction.");
            this.speed = speed;
            this.angles = angles.clone();
        }

        @Override
        public String getName() {
            return "multi";
        }

        @Override
        public List<BossBullet> fire(final int originX, final int originY,
                final int targetX, final int targetY, final int damage) {
            List<BossBullet> bullets = new ArrayList<BossBullet>();
            for (double angle : this.angles)
                bullets.add(BossBullet.fromAngle(originX, originY, angle,
                        this.speed, damage, false));
            return bullets;
        }
    }

    /**
     * Spread shot: bullets fanned out at evenly spaced angles around the
     * direction of the player.
     */
    final class SpreadShot implements AttackPattern {

        /** Speed in pixels per frame. */
        private final double speed;
        /** Number of bullets. */
        private final int count;
        /** Total angle covered by the fan, in degrees. */
        private final double spreadAngle;

        /**
         * Creates a spread shot.
         *
         * @param speed
         *            Speed in pixels per frame.
         * @param count
         *            Number of bullets, at least 2.
         * @param spreadAngle
         *            Total angle of the fan in degrees.
         */
        public SpreadShot(final double speed, final int count,
                final double spreadAngle) {
            if (count < 2)
                throw new IllegalArgumentException(
                        "Spread shot needs at least two bullets.");
            this.speed = speed;
            this.count = count;
            this.spreadAngle = spreadAngle;
        }

        @Override
        public String getName() {
            return "spread";
        }

        @Override
        public List<BossBullet> fire(final int originX, final int originY,
                final int targetX, final int targetY, final int damage) {
            double center = BossBullet.angleTo(originX, originY, targetX,
                    targetY);
            double step = this.spreadAngle / (this.count - 1);
            double first = center - this.spreadAngle / 2;
            List<BossBullet> bullets = new ArrayList<BossBullet>();
            for (int i = 0; i < this.count; i++)
                bullets.add(BossBullet.fromAngle(originX, originY,
                        first + i * step, this.speed, damage, false));
            return bullets;
        }
    }
}
