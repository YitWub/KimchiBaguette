package entity;

public enum BulletType {
    NORMAL(-12, 600),
    RAPID(-6, 750),
    MULTI(-6, 1000);

    private final int speed;
    private final int shooting_interval;

    BulletType(int speed, int shooting_interval) {
        this.speed = speed;
        this.shooting_interval = shooting_interval;
    }

    public int getSpeed() {
        return speed;
    }

    public int getShootingInterval() {
        return shooting_interval;
    }
}