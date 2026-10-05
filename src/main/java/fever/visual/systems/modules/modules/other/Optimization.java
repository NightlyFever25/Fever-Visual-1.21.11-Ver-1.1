package fever.visual.systems.modules.modules.other;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.ChunkBuilderMode;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

@ModuleInfo(name = "Optimization", category = ModuleCategory.MISC, desc = "modules.descriptions.optimization")
public class Optimization extends BaseModule {
    private final BooleanSetting vanillaParticles = new BooleanSetting(this, "modules.settings.optimization.vanilla_particles").enable();
    private final BooleanSetting farParticles = new BooleanSetting(this, "modules.settings.optimization.far_particles").enable();
    private final SliderSetting particleDistance = new SliderSetting(this, "modules.settings.optimization.particle_distance", () -> !this.farParticles.isEnabled())
            .min(16.0F).max(128.0F).step(1.0F).currentValue(48.0F).suffix("m");
    private final BooleanSetting rainParticles = new BooleanSetting(this, "modules.settings.optimization.rain_particles").enable();
    private final SliderSetting rainParticleDensity = new SliderSetting(this, "modules.settings.optimization.rain_particle_density", () -> !this.rainParticles.isEnabled())
            .min(5.0F).max(100.0F).step(5.0F).currentValue(35.0F).suffix("%");
    private final BooleanSetting cullFarEntities = new BooleanSetting(this, "modules.settings.optimization.cull_far_entities").enabled(false);
    private final SliderSetting entityDistance = new SliderSetting(this, "modules.settings.optimization.entity_distance", () -> !this.cullFarEntities.isEnabled())
            .min(32.0F).max(192.0F).step(1.0F).currentValue(96.0F).suffix("m");
    private final BooleanSetting wallEntityCulling = new BooleanSetting(this, "modules.settings.optimization.wall_entity_culling").enabled(false);
    private final SliderSetting wallEntityDistance = new SliderSetting(this, "modules.settings.optimization.wall_entity_distance", () -> !this.wallEntityCulling.isEnabled())
            .min(8.0F).max(96.0F).step(1.0F).currentValue(48.0F).suffix("m");
    private final BooleanSetting entityDistanceOption = new BooleanSetting(this, "modules.settings.optimization.entity_distance_option").enabled(false);
    private final SliderSetting entityDistanceScale = new SliderSetting(this, "modules.settings.optimization.entity_distance_scale", () -> !this.entityDistanceOption.isEnabled())
            .min(0.5F).max(1.0F).step(0.05F).currentValue(0.75F);
    private final BooleanSetting disableEntityShadows = new BooleanSetting(this, "modules.settings.optimization.entity_shadows").enabled(false);
    private final BooleanSetting fastClouds = new BooleanSetting(this, "modules.settings.optimization.fast_clouds").enabled(false);
    private final BooleanSetting asynchronousChunkUpdates = new BooleanSetting(this, "modules.settings.optimization.async_chunk_updates").enable();
    private final BooleanSetting reduceViewDistance = new BooleanSetting(this, "modules.settings.optimization.view_distance").enabled(false);
    private final SliderSetting viewDistance = new SliderSetting(this, "modules.settings.optimization.view_distance_value", () -> !this.reduceViewDistance.isEnabled())
            .min(4.0F).max(16.0F).step(1.0F).currentValue(10.0F);
    private final BooleanSetting reduceSimulationDistance = new BooleanSetting(this, "modules.settings.optimization.simulation_distance").enabled(false);
    private final SliderSetting simulationDistance = new SliderSetting(this, "modules.settings.optimization.simulation_distance_value", () -> !this.reduceSimulationDistance.isEnabled())
            .min(5.0F).max(16.0F).step(1.0F).currentValue(6.0F);
    private final BooleanSetting hideWeather = new BooleanSetting(this, "modules.settings.optimization.hide_weather").enabled(false);

    private ParticlesMode oldParticles;
    private Double oldEntityDistanceScale;
    private Boolean oldEntityShadows;
    private CloudRenderMode oldCloudRenderMode;
    private ChunkBuilderMode oldChunkBuilderMode;
    private Integer oldViewDistance;
    private Integer oldSimulationDistance;
    private boolean appliedParticles;
    private boolean appliedEntityDistance;
    private boolean appliedEntityShadows;
    private boolean appliedClouds;
    private boolean appliedChunkBuilderMode;
    private boolean appliedViewDistance;
    private boolean appliedSimulationDistance;
    private int particleCounter;
    private final Map<Integer, OcclusionCacheEntry> occlusionCache = new HashMap<>();

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        this.applyOptions();
        if (!this.wallEntityCulling.isEnabled() || this.occlusionCache.size() > 2048) {
            this.occlusionCache.clear();
        }
    };

    @Override
    public void onEnable() {
        this.captureOptions();
        this.applyOptions();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.occlusionCache.clear();
        this.restoreOptions();
        super.onDisable();
    }

    private void captureOptions() {
        if (mc.options == null) {
            return;
        }

        GameOptions options = mc.options;
        this.oldParticles = options.getParticles().getValue();
        this.oldEntityDistanceScale = options.getEntityDistanceScaling().getValue();
        this.oldEntityShadows = options.getEntityShadows().getValue();
        this.oldCloudRenderMode = options.getCloudRenderMode().getValue();
        this.oldChunkBuilderMode = options.getChunkBuilderMode().getValue();
        this.oldViewDistance = options.getViewDistance().getValue();
        this.oldSimulationDistance = options.getSimulationDistance().getValue();
    }

    private void applyOptions() {
        if (mc.options == null) {
            return;
        }

        GameOptions options = mc.options;
        if (this.vanillaParticles.isEnabled()) {
            this.appliedParticles = true;
            if (options.getParticles().getValue() == ParticlesMode.ALL) {
                options.getParticles().setValue(ParticlesMode.DECREASED);
            }
        } else {
            this.restoreParticles(options);
        }

        if (this.entityDistanceOption.isEnabled()) {
            this.appliedEntityDistance = true;
            double target = this.entityDistanceScale.getCurrentValue();
            if (Math.abs(options.getEntityDistanceScaling().getValue() - target) > 0.001D) {
                options.getEntityDistanceScaling().setValue(target);
            }
        } else {
            this.restoreEntityDistance(options);
        }

        if (this.disableEntityShadows.isEnabled()) {
            this.appliedEntityShadows = true;
            if (options.getEntityShadows().getValue()) {
                options.getEntityShadows().setValue(false);
            }
        } else {
            this.restoreEntityShadows(options);
        }

        if (this.fastClouds.isEnabled()) {
            this.appliedClouds = true;
            if (options.getCloudRenderMode().getValue() == CloudRenderMode.FANCY) {
                options.getCloudRenderMode().setValue(CloudRenderMode.FAST);
            }
        } else {
            this.restoreClouds(options);
        }

        if (this.asynchronousChunkUpdates.isEnabled()) {
            if (options.getChunkBuilderMode().getValue() == ChunkBuilderMode.NEARBY) {
                this.appliedChunkBuilderMode = true;
                options.getChunkBuilderMode().setValue(ChunkBuilderMode.PLAYER_AFFECTED);
            }
        } else {
            this.restoreChunkBuilderMode(options);
        }

        if (this.reduceViewDistance.isEnabled()) {
            this.appliedViewDistance = true;
            int target = (int)this.viewDistance.getCurrentValue();
            if (options.getViewDistance().getValue() > target) {
                options.getViewDistance().setValue(target);
            }
        } else {
            this.restoreViewDistance(options);
        }

        if (this.reduceSimulationDistance.isEnabled()) {
            this.appliedSimulationDistance = true;
            int target = (int)this.simulationDistance.getCurrentValue();
            if (options.getSimulationDistance().getValue() > target) {
                options.getSimulationDistance().setValue(target);
            }
        } else {
            this.restoreSimulationDistance(options);
        }
    }

    private void restoreOptions() {
        if (mc.options == null) {
            return;
        }

        GameOptions options = mc.options;
        this.restoreParticles(options);
        this.restoreEntityDistance(options);
        this.restoreEntityShadows(options);
        this.restoreClouds(options);
        this.restoreChunkBuilderMode(options);
        this.restoreViewDistance(options);
        this.restoreSimulationDistance(options);
    }

    private void restoreParticles(GameOptions options) {
        if (this.appliedParticles && this.oldParticles != null) {
            options.getParticles().setValue(this.oldParticles);
        }
        this.appliedParticles = false;
    }

    private void restoreEntityDistance(GameOptions options) {
        if (this.appliedEntityDistance && this.oldEntityDistanceScale != null) {
            options.getEntityDistanceScaling().setValue(this.oldEntityDistanceScale);
        }
        this.appliedEntityDistance = false;
    }

    private void restoreEntityShadows(GameOptions options) {
        if (this.appliedEntityShadows && this.oldEntityShadows != null) {
            options.getEntityShadows().setValue(this.oldEntityShadows);
        }
        this.appliedEntityShadows = false;
    }

    private void restoreClouds(GameOptions options) {
        if (this.appliedClouds && this.oldCloudRenderMode != null) {
            options.getCloudRenderMode().setValue(this.oldCloudRenderMode);
        }
        this.appliedClouds = false;
    }

    private void restoreChunkBuilderMode(GameOptions options) {
        if (this.appliedChunkBuilderMode && this.oldChunkBuilderMode != null) {
            options.getChunkBuilderMode().setValue(this.oldChunkBuilderMode);
        }
        this.appliedChunkBuilderMode = false;
    }

    private void restoreViewDistance(GameOptions options) {
        if (this.appliedViewDistance && this.oldViewDistance != null) {
            options.getViewDistance().setValue(this.oldViewDistance);
        }
        this.appliedViewDistance = false;
    }

    private void restoreSimulationDistance(GameOptions options) {
        if (this.appliedSimulationDistance && this.oldSimulationDistance != null) {
            options.getSimulationDistance().setValue(this.oldSimulationDistance);
        }
        this.appliedSimulationDistance = false;
    }

    public boolean shouldSkipParticle(ParticleEffect parameters, double x, double y, double z) {
        if (!this.isEnabled() || mc.player == null || mc.world == null) {
            return false;
        }

        if (parameters.getType() == ParticleTypes.FIREWORK) {
            return false;
        }

        if (this.farParticles.isEnabled()) {
            double distance = this.particleDistance.getCurrentValue();
            if (mc.player.squaredDistanceTo(x, y, z) > distance * distance) {
                return true;
            }
        }

        if (this.rainParticles.isEnabled() && parameters.getType() == ParticleTypes.RAIN) {
            this.particleCounter = (this.particleCounter + 1) & 1023;
            float density = Math.max(0.05F, this.rainParticleDensity.getCurrentValue() / 100.0F);
            int keepEvery = Math.max(1, Math.round(1.0F / density));
            return this.particleCounter % keepEvery != 0;
        }

        return false;
    }

    public boolean shouldSkipWeatherRendering() {
        return this.isEnabled() && this.hideWeather.isEnabled();
    }

    public boolean shouldSkipEntity(Entity entity, double cameraX, double cameraY, double cameraZ) {
        if (!this.isEnabled() || mc.player == null || entity == null || entity == mc.player) {
            return false;
        }
        if (!this.cullFarEntities.isEnabled() && !this.wallEntityCulling.isEnabled()) {
            return false;
        }
        if (entity.hasPassengerDeep(mc.player)) {
            return false;
        }

        double squaredDistance = entity.squaredDistanceTo(cameraX, cameraY, cameraZ);
        double farDistance = this.entityDistance.getCurrentValue();
        if (this.cullFarEntities.isEnabled() && squaredDistance > farDistance * farDistance) {
            return true;
        }

        return this.shouldSkipOccludedEntity(entity, cameraX, cameraY, cameraZ);
    }

    private boolean shouldSkipOccludedEntity(Entity entity, double cameraX, double cameraY, double cameraZ) {
        if (!this.wallEntityCulling.isEnabled() || mc.world == null || mc.player == null) {
            return false;
        }

        double distance = this.wallEntityDistance.getCurrentValue();
        if (entity.squaredDistanceTo(cameraX, cameraY, cameraZ) > distance * distance) {
            return false;
        }

        Vec3d cameraPos = mc.gameRenderer != null && mc.gameRenderer.getCamera() != null
                ? mc.gameRenderer.getCamera().getCameraPos()
                : new Vec3d(cameraX, cameraY, cameraZ);
        if (cameraPos.squaredDistanceTo(entity.getEntityPos()) < 4.0D) {
            return false;
        }

        int tick = mc.player.age;
        int entityId = entity.getId();
        OcclusionCacheEntry cached = this.occlusionCache.get(entityId);
        double entityX = entity.getX();
        double entityY = entity.getY();
        double entityZ = entity.getZ();
        if (cached != null
                && tick - cached.tick <= 3
                && squaredDistance(cameraPos.x, cameraPos.y, cameraPos.z, cached.cameraX, cached.cameraY, cached.cameraZ) < 0.25D
                && squaredDistance(entityX, entityY, entityZ, cached.entityX, cached.entityY, cached.entityZ) < 0.25D) {
            return cached.occluded;
        }

        boolean occluded = this.isEntityFullyBlocked(entity, cameraPos);
        this.occlusionCache.put(entityId, new OcclusionCacheEntry(
                tick, occluded,
                cameraPos.x, cameraPos.y, cameraPos.z,
                entityX, entityY, entityZ
        ));
        return occluded;
    }

    private static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double x = x1 - x2;
        double y = y1 - y2;
        double z = z1 - z2;
        return x * x + y * y + z * z;
    }

    private boolean isEntityFullyBlocked(Entity entity, Vec3d cameraPos) {
        Box box = entity.getBoundingBox();
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerY = (box.minY + box.maxY) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double insetX = Math.min((box.maxX - box.minX) * 0.25D, 0.35D);
        double insetZ = Math.min((box.maxZ - box.minZ) * 0.25D, 0.35D);

        return this.isBlocked(cameraPos, new Vec3d(centerX, centerY, centerZ))
                && this.isBlocked(cameraPos, new Vec3d(centerX, box.maxY - 0.05D, centerZ))
                && this.isBlocked(cameraPos, new Vec3d(centerX, box.minY + 0.1D, centerZ))
                && this.isBlocked(cameraPos, new Vec3d(centerX + insetX, centerY, centerZ + insetZ))
                && this.isBlocked(cameraPos, new Vec3d(centerX - insetX, centerY, centerZ - insetZ));
    }

    private boolean isBlocked(Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        double length = delta.length();
        if (length <= 0.001D) {
            return false;
        }

        Vec3d step = delta.normalize().multiply(0.25D);
        int steps = Math.max(1, (int)Math.ceil(length / 0.25D));
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;

        for (int i = 1; i < steps; i++) {
            Vec3d sample = from.add(step.multiply(i));
            pos.set(sample.x, sample.y, sample.z);
            if (pos.getX() == lastX && pos.getY() == lastY && pos.getZ() == lastZ) {
                continue;
            }

            lastX = pos.getX();
            lastY = pos.getY();
            lastZ = pos.getZ();
            BlockState state = mc.world.getBlockState(pos);
            if (this.blocksEntityView(state, pos)) {
                return true;
            }
        }

        return false;
    }

    private boolean blocksEntityView(BlockState state, BlockPos pos) {
        return !state.isAir()
                && state.isOpaqueFullCube()
                && state.shouldBlockVision(mc.world, pos)
                && state.isFullCube(mc.world, pos);
    }

    public static Optimization getInstanceSafe() {
        if (FeverVisual.getInstance() == null || FeverVisual.getInstance().getModuleManager() == null) {
            return null;
        }
        return FeverVisual.getInstance().getModuleManager().getModuleSafe(Optimization.class);
    }

    private record OcclusionCacheEntry(
            int tick, boolean occluded,
            double cameraX, double cameraY, double cameraZ,
            double entityX, double entityY, double entityZ
    ) {
    }
}
