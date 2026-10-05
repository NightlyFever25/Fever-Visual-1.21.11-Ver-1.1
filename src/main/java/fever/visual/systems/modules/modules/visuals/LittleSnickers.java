package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.AttackEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.modules.modules.visuals.littlePet.GhostWolfEntity;
import fever.visual.systems.setting.settings.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@ModuleInfo(name = "Little Pet", category = ModuleCategory.VISUALS, desc = "Пёс за вами")
@Environment(EnvType.CLIENT)
public class LittleSnickers extends BaseModule {

    private LivingEntity fakeEntity = null;
    private boolean active = false;
    private World lastWorld = null;

    private final BooleanSetting bumbum = new BooleanSetting(this, "modules.settings.bumbum", "Бегать за таргетом").enabled(false);

    private int ticksAway = 0;
    private int stuckTicks = 0;

    private LivingEntity currentTarget = null;

    private final EventListener<AttackEvent> onAttack = event -> {
        if (!bumbum.isEnabled()) return;

        Entity attacked = event.getEntity();
        if (attacked instanceof LivingEntity living &&
                living != mc.player &&
                !this.isInvisible(living)) {
            currentTarget = living;
        }
    };


    private LivingEntity createEntity() {
        var entity = new GhostWolfEntity(EntityType.WOLF, mc.world);
        if (entity == null) return null;

        entity.refreshPositionAndAngles(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch());
        entity.setInvisible(false);
        entity.setCustomNameVisible(false);
        entity.setHealth(20.0F);

        if (entity instanceof TameableEntity tameable) {
            tameable.setTamed(true, false);
            tameable.setOwner(mc.player);
        }

        return entity;
    }

    private boolean isSolid(BlockPos pos) {
        if (mc.world == null) return false;
        BlockState blockState = mc.world.getBlockState(pos);
        return !blockState.isAir() && !blockState.getCollisionShape(mc.world, pos).isEmpty();
    }

    private void moveEntityTowardsTarget(Vec3d targetPos) {
        if (fakeEntity == null) return;

        Vec3d entityPos = fakeEntity.getEntityPos();
        double dx = targetPos.x - entityPos.x;
        double dz = targetPos.z - entityPos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        double dy = targetPos.y - entityPos.y;

        boolean isMoving = false;
        boolean isJumping = false;

        BlockPos entityBlockPos = new BlockPos(
                (int) Math.floor(entityPos.x),
                (int) Math.floor(entityPos.y - 0.1),
                (int) Math.floor(entityPos.z)
        );
        boolean onGround = isSolid(entityBlockPos);

        if (Math.abs(dy) >= 12 || distanceXZ > 20) {
            fakeEntity.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, fakeEntity.getYaw(), fakeEntity.getPitch());
            fakeEntity.setVelocity(Vec3d.ZERO);
            stuckTicks = 0;
            return;
        }

        if (distanceXZ > 1.2) {
            double speed = 0.21;
            double normX = dx / distanceXZ;
            double normZ = dz / distanceXZ;

            BlockPos frontPos = new BlockPos(
                    (int) Math.floor(entityPos.x + normX * 0.6),
                    (int) Math.floor(entityPos.y),
                    (int) Math.floor(entityPos.z + normZ * 0.6)
            );
            BlockPos aboveFrontPos = frontPos.up();
            BlockPos blockBelowFront = frontPos.down();
            BlockPos blockBelow = entityBlockPos.down();

            boolean frontSolid = isSolid(frontPos);
            boolean aboveClear = !isSolid(aboveFrontPos);
            boolean belowSolid = isSolid(blockBelow);
            boolean belowFrontSolid = isSolid(blockBelowFront);

            if (frontSolid && aboveClear && onGround) {
                int blockHeight = frontPos.getY() - entityBlockPos.getY();
                if (blockHeight <= 1) {
                    Vec3d vel = fakeEntity.getVelocity();
                    fakeEntity.setVelocity(vel.x, 0.5, vel.z);
                    isJumping = true;
                    onGround = false;
                } else {
                    stuckTicks++;
                    return;
                }
            }

            if (!frontSolid && !belowFrontSolid) {
                boolean foundGround = false;
                for (int drop = 1; drop <= 3; drop++) {
                    BlockPos check = frontPos.down(drop);
                    if (isSolid(check)) {
                        Vec3d vel = fakeEntity.getVelocity();
                        fakeEntity.setVelocity(vel.x, -0.1, vel.z);
                        foundGround = true;
                        break;
                    }
                }
                if (!foundGround) {
                    stuckTicks++;
                    return;
                }
            }

            if (!isJumping && (belowSolid || onGround)) {
                fakeEntity.move(MovementType.SELF, new Vec3d(normX * speed, 0, normZ * speed));
                isMoving = true;
                stuckTicks = 0;
            } else if (!isJumping) {
                stuckTicks++;
            }

            if (isMoving) {
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                fakeEntity.setYaw(yaw);
                fakeEntity.setBodyYaw(yaw);
                if (fakeEntity instanceof MobEntity mob) mob.setHeadYaw(yaw);
            }
        }

        if (!onGround) {
            Vec3d velocity = fakeEntity.getVelocity();
            fakeEntity.setVelocity(
                    velocity.x * 0.91,
                    Math.max(velocity.y - 0.08, -0.5),
                    velocity.z * 0.91
            );
            fakeEntity.move(MovementType.SELF, fakeEntity.getVelocity());
        }

        if (!isMoving && onGround) {
            fakeEntity.setVelocity(Vec3d.ZERO);
        }

        if (fakeEntity instanceof WolfEntity wolf) {
            wolf.setSprinting(isMoving && !isJumping);
            wolf.setSitting(!isMoving && onGround);
        }
    }

    private void updateEntityRotation(Entity target) {
        if (fakeEntity == null || target == null) return;

        Vec3d entityEyes = fakeEntity.getEntityPos().add(0, fakeEntity.getStandingEyeHeight(), 0);
        Vec3d targetEyes = target.getEntityPos().add(0, target.getStandingEyeHeight(), 0);
        Vec3d diff = targetEyes.subtract(entityEyes);

        double flatDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(diff.y, flatDist));

        fakeEntity.setYaw(yaw);
        fakeEntity.setPitch(pitch);
        fakeEntity.setBodyYaw(yaw);

        if (fakeEntity instanceof MobEntity mob) {
            mob.setHeadYaw(yaw);
        }
    }

    private void updateEntityRotationSafe(Object target) {
        if (target instanceof Entity entity) {
            updateEntityRotation(entity);
        }
    }

    private final ClientTickEvents.EndTick tickListener = client -> {
        if (!active || mc.player == null || mc.world == null) return;

        if (fakeEntity == null) {
            fakeEntity = createEntity();
            if (fakeEntity != null) {
                mc.world.addEntity(fakeEntity);
                lastWorld = mc.world;
            }
            return;
        }

        if (mc.world != lastWorld) {
            if (fakeEntity != null)
                mc.world.removeEntity(fakeEntity.getId(), Entity.RemovalReason.DISCARDED);
            fakeEntity = createEntity();
            if (fakeEntity == null) return;
            mc.world.addEntity(fakeEntity);
            lastWorld = mc.world;
        }

        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d entityPos = fakeEntity.getEntityPos();
        double distToPlayer = playerPos.distanceTo(entityPos);

        if (distToPlayer > 10.0 || stuckTicks > 40) {
            ticksAway++;

            if (ticksAway >= 60 || stuckTicks > 40) {
                double targetX = playerPos.x;
                double targetY = playerPos.y;
                double targetZ = playerPos.z;

                if (mc.player.isGliding() || !mc.player.isOnGround()) {
                    BlockPos.Mutable posBelow = new BlockPos.Mutable(
                            (int) Math.floor(playerPos.x),
                            (int) Math.floor(playerPos.y),
                            (int) Math.floor(playerPos.z)
                    );

                    for (int yOffset = 0; yOffset < 64; yOffset++) {
                        if (isSolid(posBelow)) {
                            targetY = posBelow.getY() + 1.0;
                            break;
                        }
                        posBelow.move(0, -1, 0);
                    }
                }

                fakeEntity.refreshPositionAndAngles(
                        targetX, targetY, targetZ,
                        fakeEntity.getYaw(), fakeEntity.getPitch()
                );
                fakeEntity.setVelocity(Vec3d.ZERO);
                ticksAway = 0;
                stuckTicks = 0;
            }
        } else {
            ticksAway = 0;
        }

        if (currentTarget != null &&
                (currentTarget.isRemoved() ||
                        currentTarget.isDead() ||
                        this.isInvisible(currentTarget))) {
            currentTarget = null;
        }

        Vec3d moveTo = playerPos;
        Object lookTarget = mc.player;

        if (bumbum.isEnabled() && currentTarget != null) {
            double distToTarget = currentTarget.getEntityPos().distanceTo(entityPos);

            if (distToTarget <= 12) {
                moveTo = currentTarget.getEntityPos();
                lookTarget = currentTarget;
            } else {
                currentTarget = null;
                moveTo = playerPos;
                lookTarget = mc.player;
            }
        }

        moveEntityTowardsTarget(moveTo);
        fakeEntity.tickMovement();
        updateEntityRotationSafe(lookTarget);
    };

    private boolean isInvisible(LivingEntity entity) {
        if (entity.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
        if (entity.isInvisible()) return true;
        if (mc.player != null && entity.isInvisibleTo(mc.player)) return true;
        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ClientTickEvents.END_CLIENT_TICK.register(tickListener);
        active = true;
        lastWorld = mc.world;

        if (mc.player != null && mc.world != null) {
            fakeEntity = createEntity();
            if (fakeEntity != null) {
                mc.world.addEntity(fakeEntity);
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!active) return;

        if (mc.world != null && fakeEntity != null) {
            mc.world.removeEntity(fakeEntity.getId(), Entity.RemovalReason.DISCARDED);
            fakeEntity = null;
        }

        active = false;
        ticksAway = 0;
        stuckTicks = 0;
        currentTarget = null;
    }
}
