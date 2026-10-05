package fever.visual.systems.modules.modules.visuals.littlePet;

public class SmoothValue {
    private float current;
    private float target;
    private float startValue;
    private long startTime;
    private int speedMs;

    public SmoothValue(float initial, int speedMs) {
        this.current = initial;
        this.target = initial;
        this.startValue = initial;
        this.startTime = System.currentTimeMillis();
        this.speedMs = speedMs;
    }

    public void set(float newTarget) {
        if (newTarget == this.target) {
            return;
        }
        this.startValue = this.get();
        this.target = newTarget;
        this.startTime = System.currentTimeMillis();
    }

    public float get() {
        long elapsed = System.currentTimeMillis() - this.startTime;
        if (elapsed >= (long) this.speedMs) {
            this.current = this.target;
            return this.current;
        }
        float t = (float) elapsed / (float) this.speedMs;
        this.current = this.startValue + (this.target - this.startValue) * t;
        return this.current;
    }

    public boolean isFinished() {
        return System.currentTimeMillis() - this.startTime >= (long) this.speedMs;
    }
}