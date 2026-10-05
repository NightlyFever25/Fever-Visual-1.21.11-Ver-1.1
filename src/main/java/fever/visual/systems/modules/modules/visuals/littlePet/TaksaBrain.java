package fever.visual.systems.modules.modules.visuals.littlePet;

import fever.visual.systems.modules.modules.visuals.littlePet.SmoothValue;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public class TaksaBrain implements IMinecraft {

    private static final int SMOOTH_SPEED = 150;

    private Vec3d pos = null;
    private Vec3d motion = Vec3d.ZERO;
    private float direction = randomDirection();

    private SmoothValue sx = new SmoothValue(0.0f, SMOOTH_SPEED);
    private SmoothValue sy = new SmoothValue(0.0f, SMOOTH_SPEED);
    private SmoothValue sz = new SmoothValue(0.0f, SMOOTH_SPEED);

    private float yaw;
    private float body;
    private SmoothValue bodySmooth  = new SmoothValue(0.0f, SMOOTH_SPEED);
    private SmoothValue yawSmooth   = new SmoothValue(0.0f, SMOOTH_SPEED);
    private SmoothValue pitchSmooth = new SmoothValue(0.0f, SMOOTH_SPEED);

    private boolean lay;
    private long stayingStart = System.currentTimeMillis();

    public float prevLimbSwingAmount;
    public float limbSwingAmount;
    public float limbSwing;

    private PlayerEntity entity;
    private LivingEntity attackTarget;
    private long attackTargetExpiry = 0L;
    private static final long ATTACK_TARGET_TIMEOUT = 3000L;

    public void setEntity(PlayerEntity e) {
        this.entity = e;
    }

    public void setAttackTarget(LivingEntity target) {
        if (target != null && target != mc.player) {
            boolean isPlayer = target instanceof PlayerEntity;
            if (isInvisible(target)) {
                this.attackTarget = null;
                return;
            }
            if (isPlayer && target != mc.player) {
                this.attackTarget = null;
                return;
            }
        }

        this.attackTarget = target;
        this.attackTargetExpiry = System.currentTimeMillis() + ATTACK_TARGET_TIMEOUT;
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null) return false;
        if (target == mc.player) return false;
        if (target.isRemoved() || target.isDead()) return false;
        if (isInvisible(target)) return false;
        if (target instanceof PlayerEntity && target != mc.player) return false;

        return true;
    }

    private boolean isInvisible(LivingEntity target) {
        if (target.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
        if (target.isInvisible()) return true;
        if (mc.player != null && target.isInvisibleTo(mc.player)) return true;
        return false;
    }

    private static float randomDirection() {
        return (float)ThreadLocalRandom.current().nextDouble(360.0);
    }

    public void tick() {
        if (this.entity == null || mc.world == null) return;

        Vec3d playerPos = this.entity.getEntityPos();

        if (this.pos == null || this.pos.distanceTo(playerPos) > 10.0) {
            this.pos = playerPos;
            this.sx = new SmoothValue((float) this.pos.x, SMOOTH_SPEED);
            this.sy = new SmoothValue((float) this.pos.y, SMOOTH_SPEED);
            this.sz = new SmoothValue((float) this.pos.z, SMOOTH_SPEED);
        }

        this.motion = this.motion.add(0.0, -0.2, 0.0);
        Vec3d newPos = this.pos.add(this.motion);
        BlockPos bp = BlockPos.ofFloored(newPos.x, newPos.y - 0.1, newPos.z);
        if (!mc.world.getBlockState(bp).isAir()) {
            double correctedY = bp.getY() + 1 + 0.1;
            newPos = new Vec3d(newPos.x, correctedY, newPos.z);
            this.motion = new Vec3d(this.motion.x, 0.0, this.motion.z);
        }
        if (this.attackTarget != null) {
            if (System.currentTimeMillis() > this.attackTargetExpiry || !isValidTarget(this.attackTarget)) {
                this.attackTarget = null;
            }
        }

        LivingEntity target = this.attackTarget;
        double dist = newPos.distanceTo(playerPos);

        if (target != null && target.isAlive() && isValidTarget(target)) {
            Vec3d targetPos = target.getEntityPos();
            if (mc.world != null) {
                Vec3d dir = targetPos.subtract(newPos).normalize();
                BlockPos frontBp = BlockPos.ofFloored(
                        newPos.x + dir.x * 0.5,
                        newPos.y + 0.5,
                        newPos.z + dir.z * 0.5
                );
                if (!mc.world.getBlockState(frontBp).isAir()) {
                    this.motion = new Vec3d(this.motion.x, 0.62, this.motion.z);
                }
            }
            this.motion = this.motion.add(targetPos.subtract(newPos).normalize().multiply(0.6));
            Box taksaBox = new Box(newPos.subtract(0.4, 0.0, 0.4), newPos.add(0.4, 0.4, 0.4));
            Box targetBox = target.getBoundingBox().expand(-0.1, 0.0, -0.1);
            if (taksaBox.intersects(targetBox)) {
                this.motion = new Vec3d(-this.motion.x, this.motion.y, -this.motion.z);
            }

        } else if (dist > 2.0) {
            this.motion = this.motion.add(playerPos.subtract(newPos).normalize().multiply(0.7));
        }

        this.handleRotation(newPos, target);
        this.pos = newPos;
        if (dist < 0.1 && target == null) {
            this.direction = randomDirection();
            double xMot = -Math.sin(Math.toRadians(this.direction)) * 0.08;
            double zMot =  Math.cos(Math.toRadians(this.direction)) * 0.08;
            this.motion = this.motion.add(xMot, 0.0, zMot);
        }

        this.motion = this.motion.multiply(0.5);

        this.sx.set((float) this.pos.x);
        this.sy.set((float) this.pos.y);
        this.sz.set((float) this.pos.z);

        this.limbTick();

        float dx = this.sx.get() - (float) this.pos.x;
        float dz = this.sz.get() - (float) this.pos.z;
        boolean moving = Math.abs(dx) > 0.1f || Math.abs(dz) > 0.1f;

        if (moving) {
            this.stayingStart = System.currentTimeMillis();
        }
        this.lay = System.currentTimeMillis() - this.stayingStart > 1000L;
    }

    private void handleRotation(Vec3d currentPos, LivingEntity target) {
        if (this.motion.x != 0.0 || this.motion.z != 0.0) {
            double angle = Math.atan2(this.motion.z, this.motion.x);
            this.yaw = (float) Math.toDegrees(angle) - 90.0f;
            this.yaw %= 360.0f;
            if (this.yaw < 0.0f) this.yaw += 360.0f;
        }

        Vec3d lookAt;
        if (target != null && isValidTarget(target)) {
            lookAt = target.getEntityPos();
        } else {
            lookAt = this.entity.getEntityPos();
        }

        float[] rot = this.getRotation(currentPos, lookAt);
        float targetYawHead = rot[0];
        float targetPitch   = rot[1];

        float gradus  = this.lay ? 200.0f : 150.0f;
        float gradus1 = this.lay ? 100.0f : 50.0f;

        if (targetYawHead - this.yaw < -gradus || targetYawHead - this.yaw > gradus) {
            this.yaw = targetYawHead;
        }

        float shortestYawPath = ((this.yaw - this.body) % 360.0f + 540.0f) % 360.0f - 180.0f;

        this.bodySmooth.set(this.body + shortestYawPath);
        this.yawSmooth.set(MathHelper.clamp(targetYawHead - this.yaw, -gradus1, gradus1));
        this.pitchSmooth.set(targetPitch);
        this.body += shortestYawPath;
    }

    private float[] getRotation(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, hDist)));
        return new float[]{yaw, pitch};
    }

    private void limbTick() {
        this.prevLimbSwingAmount = this.limbSwingAmount;
        double d0 = (double) this.sx.get() - this.pos.x;
        double d2 = (double) this.sz.get() - this.pos.z;
        float f = MathHelper.sqrt((float) (d0 * d0 + d2 * d2)) * 4.0f;
        if (f > 1.0f) f = 1.0f;
        this.limbSwingAmount += (f - this.limbSwingAmount) * 0.4f;
        this.limbSwing += this.limbSwingAmount;
    }

    public Vec3d getPos() {
        if (this.pos == null) return Vec3d.ZERO;
        return new Vec3d(this.sx.get(), this.sy.get(), this.sz.get());
    }

    public float getBody()  { return this.bodySmooth.get(); }
    public float getYaw()   { return this.yawSmooth.get(); }
    public float getPitch() { return this.pitchSmooth.get(); }
    public boolean isLay()  { return this.lay; }
}
