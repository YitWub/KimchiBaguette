package entity;

public enum BulletType {
    NORMAL(-12, 600, 1,0),
    RAPID(-6, 750,2,100),
    MULTI(-6, 1000,1,0);

    private final int speed;
    private final int shooting_interval;
    private final int shot_count;
    private final int shot_delay;

    BulletType(int speed, int shooting_interval, int shot_count, int shot_delay) {
        this.speed = speed;
        this.shooting_interval = shooting_interval;
        this.shot_count = shot_count;
        this.shot_delay = shot_delay;
    }

    public int getSpeed() {
        return speed;
    }

    public int getShootingInterval() {
        return shooting_interval;
    }

    public int getShotCount() {
        return shot_count;
    }

    public int getShotDelay() {
        return shot_delay;
    }
}