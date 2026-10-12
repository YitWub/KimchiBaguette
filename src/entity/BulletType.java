package entity;

public enum BulletType {
    NORMAL(-12, 1,0),
    RAPID(-6,2,100),
    MULTI(-6,3,0);

    private final int speed;
    private final int shot_count;
    private final int shot_delay;

    BulletType(int speed, int shot_count, int shot_delay) {
        this.speed = speed;
        this.shot_count = shot_count;
        this.shot_delay = shot_delay;
    }

    public int getSpeed() {
        return speed;
    }

    public int getShotCount() {
        return shot_count;
    }

    public int getShotDelay() {
        return shot_delay;
    }
}