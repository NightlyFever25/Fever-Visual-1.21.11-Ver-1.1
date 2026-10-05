package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;

import fever.visual.utility.render.compat.GlStateManager;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.EntityUtility;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.Utils;
import net.minecraft.client.MinecraftClient;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

@ModuleInfo(name = "Trails", category = ModuleCategory.VISUALS, desc = "modules.descriptions.trails")
public class Trails extends BaseModule {

    private final SelectSetting targets = new SelectSetting(this, "modules.descriptions.badtrip.targets").min(0);

    private final SelectSetting.Value targetFriends = new SelectSetting.Value(targets, "modules.descriptions.badtrip.friends");
    private final SelectSetting.Value targetSelf = new SelectSetting.Value(targets, "modules.descriptions.badtrip.self");

    private final ModeSetting mode = new ModeSetting(this, "Mode");
    private final ModeSetting.Value stripMode = new ModeSetting.Value(this.mode, "Strip").select();
    private final ModeSetting.Value glowMode = new ModeSetting.Value(this.mode, "Glow");
    private final ModeSetting.Value shardsMode = new ModeSetting.Value(this.mode, "Shards");
    private final ModeSetting.Value energyMode = new ModeSetting.Value(this.mode, "Energy");
    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.move_particles.color_mode");
    private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.trails.sync_with_theme").select();
    private final ModeSetting.Value colorRainbow = new ModeSetting.Value(this.colorMode, "modules.settings.move_particles.color_mode.rainbow", "", () -> true);
    private final ModeSetting.Value colorFriends = new ModeSetting.Value(this.colorMode, "modules.settings.target_esp.color_mode.friends", "", () -> true);
    private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.move_particles.color_mode.custom");
    private final ColorSetting customColorFirst = new ColorSetting(this, "modules.settings.trails.custom_color", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(255, 100, 100, 255))
            .alpha(true);
    private final ColorSetting customColorSecond = new ColorSetting(this, "modules.settings.trails.custom_color_second", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(100, 160, 255, 255))
            .alpha(true);

    private final SliderSetting length = new SliderSetting(this, "modules.settings.trails.length")
            .min(50).max(1000).currentValue(750).step(10);
    private final SliderSetting opacity = new SliderSetting(this, "modules.settings.nimb.opacity")
            .min(0.1F).max(1.0F).currentValue(0.7F).step(0.05F);
    private final SliderSetting shardLength = new SliderSetting(this, "Shards Length", () -> !this.shardsMode.isSelected())
            .min(0.0F).max(8.0F).currentValue(3.0F).step(0.25F);
    private final SliderSetting shardDensity = new SliderSetting(this, "Shards Density", () -> !this.shardsMode.isSelected())
            .min(1.0F).max(10.0F).currentValue(6.0F).step(1.0F);
    private final SliderSetting shardAlpha = new SliderSetting(this, "Shards Alpha", () -> !this.shardsMode.isSelected())
            .min(0.1F).max(1.0F).currentValue(0.7F).step(0.05F);
    private final SliderSetting shardTint = new SliderSetting(this, "Shards Tint", () -> !this.shardsMode.isSelected())
            .min(0.0F).max(100.0F).currentValue(100.0F).step(1.0F);
    private final SliderSetting shardDistort = new SliderSetting(this, "Shards Distortion", () -> !this.shardsMode.isSelected())
            .min(0.0F).max(3.0F).currentValue(1.0F).step(0.1F);
    private final SliderSetting shardReflect = new SliderSetting(this, "Shards Reflect", () -> !this.shardsMode.isSelected())
            .min(0.0F).max(1.0F).currentValue(0.4F).step(0.05F);
    private final BooleanSetting shardSparks = new BooleanSetting(this, "Shards Sparks", () -> !this.shardsMode.isSelected()).enable();
    private final SliderSetting energyLength = new SliderSetting(this, "Energy Length", () -> !this.energyMode.isSelected())
            .min(875.0F).max(1625.0F).currentValue(1200.0F).step(25.0F);
    private final SliderSetting energyShrink = new SliderSetting(this, "Energy Shrink", () -> !this.energyMode.isSelected())
            .min(0.0F).max(100.0F).currentValue(35.0F).step(5.0F);
    private final SliderSetting energySpread = new SliderSetting(this, "Energy Spread", () -> !this.energyMode.isSelected())
            .min(15.0F).max(30.0F).currentValue(16.0F).step(0.5F);
    private final SliderSetting energyRise = new SliderSetting(this, "Energy Rise", () -> !this.energyMode.isSelected())
            .min(-12.0F).max(12.0F).currentValue(-0.5F).step(0.5F);
    private final BooleanSetting energyGlow = new BooleanSetting(this, "Energy Glow", () -> !this.energyMode.isSelected()).enable();
    private final BooleanSetting energyWalls = new BooleanSetting(this, "Energy Through Walls", () -> !this.energyMode.isSelected()).enabled(false);
    private final Map<PlayerEntity, List<TrailPoint>> trailsMap = new HashMap<>();
    private final Map<PlayerEntity, List<ShardBurst>> shardBursts = new HashMap<>();
    private final Map<PlayerEntity, Vec3d> lastShardPositions = new HashMap<>();
    private final Map<PlayerEntity, EnergyState> energyStates = new HashMap<>();
    private long lastUpdateTime = 0;

    private final EventListener<Render3DEvent> onRender3D = new EventListener<>() {
        @Override
        public void onEvent(Render3DEvent event) {
            if (!EntityUtility.isInGame()) return;

            long now = System.currentTimeMillis();
            long trailLifetimeMs = (long) length.getCurrentValue();
            long renderLifetime = Trails.this.energyMode.isSelected() ? (long)Trails.this.energyLength.getCurrentValue() : trailLifetimeMs;
            long updateInterval = Trails.this.energyMode.isSelected() ? 8L : 50L;
            if (now - lastUpdateTime > updateInterval) {
                updateTrails(now, renderLifetime);
                lastUpdateTime = now;
            }
            renderTrails(event, now, renderLifetime);
        }

        @Override
        public int getPriority() {
            return -100;
        }
    };

    @Override
    public void onEnable() {
        super.onEnable();
        trailsMap.clear();
        shardBursts.clear();
        lastShardPositions.clear();
        energyStates.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        trailsMap.clear();
        shardBursts.clear();
        lastShardPositions.clear();
        energyStates.clear();
    }

    private void updateTrails(long now, long trailLifetimeMs) {
        if (mc.world == null) {
            trailsMap.clear();
            return;
        }

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == null || entity == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
                continue;
            }

            if (!shouldRenderTrails(entity)) {
                trailsMap.remove(entity);
                continue;
            }

            List<TrailPoint> trails = trailsMap.computeIfAbsent(entity, k -> new ArrayList<>());
            for (int i = trails.size() - 1; i >= 0; i--) {
                if (trails.get(i).isExpired(now, trailLifetimeMs)) {
                    trails.remove(i);
                }
            }
            Vec3d currentPos = entity.getEntityPos();
            if (this.shardsMode.isSelected()) {
                this.updateShardBursts(entity, currentPos, now);
            }
            if (this.energyMode.isSelected()) {
                this.energyStates.computeIfAbsent(entity, ignored -> new EnergyState()).sample(entity, currentPos, now, trailLifetimeMs);
            }
            if (trails.isEmpty()) {
                trails.add(new TrailPoint(currentPos, getTrailColor(entity, 0).getRGB(), now));
            } else {
                TrailPoint last = trails.get(trails.size() - 1);
                double distance = last.pos.distanceTo(currentPos);
                if (distance > 8.0) {
                    trails.clear();
                    trails.add(new TrailPoint(currentPos, getTrailColor(entity, 0).getRGB(), now));
                    continue;
                }
                if (distance >= 0.05f) {
                    int steps = Math.min(24, Math.max(1, (int) (distance / 0.05f)));
                    for (int i = 1; i <= steps; i++) {
                        double t = (double) i / steps;
                        Vec3d interpolated = new Vec3d(
                                MathHelper.lerp(t, last.pos.x, currentPos.x),
                                MathHelper.lerp(t, last.pos.y, currentPos.y),
                                MathHelper.lerp(t, last.pos.z, currentPos.z)
                        );
                        trails.add(new TrailPoint(interpolated,
                                getTrailColor(entity, trails.size()).getRGB(),
                                now - (long)((1 - t) * (now - last.time))));
                    }
                }
            }
        }

        trailsMap.entrySet().removeIf(entry -> {
            PlayerEntity player = entry.getKey();
            return player == null || !player.isAlive() || !mc.world.getPlayers().contains(player) || !shouldRenderTrails(player);
        });
        this.shardBursts.entrySet().removeIf(entry -> !shouldRenderTrails(entry.getKey()));
        this.lastShardPositions.keySet().removeIf(player -> !shouldRenderTrails(player));
        this.energyStates.entrySet().removeIf(entry -> !shouldRenderTrails(entry.getKey()));

        long shardLife = Math.max(250L, trailLifetimeMs);
        this.shardBursts.values().forEach(list -> list.removeIf(burst -> now - burst.spawnTime > shardLife));
        if (!this.shardsMode.isSelected()) {
            this.shardBursts.clear();
            this.lastShardPositions.clear();
        }
        if (!this.energyMode.isSelected()) {
            this.energyStates.clear();
        }
    }

    private void updateShardBursts(PlayerEntity entity, Vec3d currentPos, long now) {
        Vec3d previous = this.lastShardPositions.put(entity, currentPos);
        if (previous == null || previous.squaredDistanceTo(currentPos) > 64.0D) {
            return;
        }
        Vec3d movement = currentPos.subtract(previous);
        if (movement.horizontalLengthSquared() < 0.04D) {
            return;
        }
        Vec3d direction = new Vec3d(movement.x, 0.0D, movement.z).normalize();
        List<ShardBurst> bursts = this.shardBursts.computeIfAbsent(entity, ignored -> new ArrayList<>());
        int count = MathHelper.clamp(Math.round(this.shardDensity.getCurrentValue() * 0.65F), 2, 8);
        long seed = now ^ ((long)entity.getId() << 32) ^ bursts.size() * 0x9E3779B97F4A7C15L;
        bursts.add(new ShardBurst(currentPos, direction, now, seed, getTrailColor(entity, bursts.size()).getRGB(), count));
        while (bursts.size() > 64) {
            bursts.remove(0);
        }
    }

    private void renderTrails(Render3DEvent event, long now, long trailLifetimeMs) {
        MatrixStack matrices = event.getMatrices();
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        try {
            if (this.shardsMode.isSelected()) {
                this.renderShardBursts(matrices, cameraPos, now, trailLifetimeMs);
                return;
            }
            if (this.energyMode.isSelected()) {
                this.renderEnergyStates(matrices, cameraPos, now, trailLifetimeMs);
                return;
            }
            for (Map.Entry<PlayerEntity, List<TrailPoint>> entry : trailsMap.entrySet()) {
                PlayerEntity entity = entry.getKey();
                List<TrailPoint> trails = entry.getValue();

                if (trails.size() < 2 || !shouldRenderTrails(entity)
                        || !Utils.isInViewHemisphere(camera, entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ(), 4.0)) continue;

                if (this.stripMode.isSelected()) {
                    renderTrailStrip(matrices, cameraPos, entity.getHeight(), trails, now, trailLifetimeMs);
                } else if (this.glowMode.isSelected()) {
                    renderGlowTrail(matrices, event.getCamera(), trails, now, trailLifetimeMs);
                }
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void renderShardBursts(MatrixStack matrices, Vec3d cameraPos, long now, long lifetime) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, FeverVisual.id("textures/bloom.png"));
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        boolean emitted = false;
        for (List<ShardBurst> bursts : this.shardBursts.values()) {
            for (ShardBurst burst : bursts) {
                float age = MathHelper.clamp((now - burst.spawnTime) / (float)Math.max(1L, lifetime), 0.0F, 1.0F);
                float appear = MathHelper.clamp(age / 0.08F, 0.0F, 1.0F);
                float fade = MathHelper.clamp((1.0F - age) / 0.28F, 0.0F, 1.0F);
                float alpha = appear * fade * this.opacity.getCurrentValue() * this.shardAlpha.getCurrentValue();
                if (alpha <= 0.01F) continue;
                Vec3d side = new Vec3d(-burst.direction.z, 0.0D, burst.direction.x);
                for (int i = 0; i < burst.count; i++) {
                    float r1 = hash01(burst.seed, i * 5 + 1);
                    float r2 = hash01(burst.seed, i * 5 + 2);
                    float r3 = hash01(burst.seed, i * 5 + 3);
                    double behind = 0.08D + r1 * 0.42D;
                    double lateral = (r2 - 0.5D) * (0.55D + this.shardDistort.getCurrentValue() * 0.14D);
                    double height = 0.14D + r3 * 1.55D;
                    // The tiny forward drift belongs to the burst itself and never follows the player.
                    Vec3d center = burst.origin.subtract(burst.direction.multiply(behind + age * age * 0.28D))
                            .add(side.multiply(lateral)).add(0.0D, height, 0.0D).subtract(cameraPos);
                    float halfHeight = 0.014F + 0.012F * this.shardReflect.getCurrentValue();
                    float halfLength = halfHeight * (5.0F + this.shardLength.getCurrentValue());
                    ColorRGBA source = ColorRGBA.fromInt(burst.color);
                    float tint = MathHelper.clamp(this.shardTint.getCurrentValue() / 100.0F, 0.0F, 1.0F);
                    ColorRGBA color = new ColorRGBA(
                            255.0F + (source.getRed() - 255.0F) * tint,
                            255.0F + (source.getGreen() - 255.0F) * tint,
                            255.0F + (source.getBlue() - 255.0F) * tint,
                            220.0F * alpha);
                    emitWorldDash(buffer, matrix, center, burst.direction, halfLength, halfHeight, color);
                    if (this.shardSparks.isEnabled() && (i & 1) == 0) {
                        Vec3d spark = center.add(side.multiply((r1 - 0.5D) * 0.24D)).add(0.0D, (r2 - 0.5D) * 0.2D, 0.0D);
                        emitWorldDash(buffer, matrix, spark, burst.direction, halfLength * 0.22F, halfHeight * 1.25F, source.withAlpha(170.0F * alpha));
                    }
                    emitted = true;
                }
            }
        }
        BuiltBuffer built = buffer.endNullable();
        if (emitted && built != null) BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }

    private void renderEnergyStates(MatrixStack matrices, Vec3d cameraPos, long now, long lifetime) {
        if (this.energyWalls.isEnabled()) RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, this.energyGlow.isEnabled()
                ? GlStateManager.DstFactor.ONE : GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        boolean emitted = false;
        for (EnergyState state : this.energyStates.values()) {
            for (EnergyRibbon ribbon : state.ribbons) {
                ribbon.prune(now, lifetime);
                if (ribbon.points.size() < 2) continue;
                emitted |= this.renderEnergyRibbon(buffer, matrix, cameraPos, ribbon, now, lifetime);
            }
        }
        BuiltBuffer built = buffer.endNullable();
        if (emitted && built != null) BufferRenderer.drawWithGlobalProgram(built);
        if (this.energyWalls.isEnabled()) RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }

    private boolean renderEnergyRibbon(BufferBuilder buffer, Matrix4f matrix, Vec3d cameraPos, EnergyRibbon ribbon,
                                       long now, long lifetime) {
        List<EnergyPoint> points = new ArrayList<>(ribbon.points);
        if (ribbon.livePosition != null && !points.isEmpty()) {
            points.add(new EnergyPoint(ribbon.livePosition, now, ribbon.liveColor, Vec3d.ZERO, 0.0F));
        }
        boolean emitted = false;
        for (int i = 0; i + 1 < points.size(); i++) {
            EnergyPoint a = points.get(i);
            EnergyPoint b = points.get(i + 1);
            float ageA = MathHelper.clamp((now - a.time) / (float)Math.max(1L, lifetime), 0.0F, 1.0F);
            float ageB = MathHelper.clamp((now - b.time) / (float)Math.max(1L, lifetime), 0.0F, 1.0F);
            Vec3d pa = energyPosition(a, ageA).subtract(cameraPos);
            Vec3d pb = energyPosition(b, ageB).subtract(cameraPos);
            Vec3d tangent = pb.subtract(pa);
            if (tangent.lengthSquared() < 1.0E-8D) continue;
            tangent = tangent.normalize();
            Vec3d viewA = pa.lengthSquared() < 1.0E-8D ? new Vec3d(0.0D, 0.0D, 1.0D) : pa.normalize();
            Vec3d viewB = pb.lengthSquared() < 1.0E-8D ? viewA : pb.normalize();
            Vec3d sideA = tangent.crossProduct(viewA);
            Vec3d sideB = tangent.crossProduct(viewB);
            if (sideA.lengthSquared() < 1.0E-8D) sideA = new Vec3d(1.0D, 0.0D, 0.0D);
            if (sideB.lengthSquared() < 1.0E-8D) sideB = sideA;
            sideA = sideA.normalize();
            sideB = sideB.normalize();

            float restA = 1.0F - ageA;
            float restB = 1.0F - ageB;
            float growA = MathHelper.clamp((now - a.time) / 120.0F, 0.0F, 1.0F);
            float growB = MathHelper.clamp((now - b.time) / 120.0F, 0.0F, 1.0F);
            growA = growA * growA * (3.0F - 2.0F * growA);
            growB = growB * growB * (3.0F - 2.0F * growB);
            float alphaA = 0.05F * 0.75F * a.power * ribbon.brightness * growA * restA * restA;
            float alphaB = 0.05F * 0.75F * b.power * ribbon.brightness * growB * restB * restB;
            if (alphaA <= 0.002F && alphaB <= 0.002F) continue;
            float shrink = MathHelper.clamp(this.energyShrink.getCurrentValue() / 100.0F, 0.0F, 1.0F);
            float widthA = ribbon.width * (1.0F - shrink * ageA);
            float widthB = ribbon.width * (1.0F - shrink * ageB);
            ColorRGBA ca = ColorRGBA.fromInt(a.color).withAlpha(255.0F * alphaA);
            ColorRGBA cb = ColorRGBA.fromInt(b.color).withAlpha(255.0F * alphaB);
            emitEnergySpan(buffer, matrix, pa, pb, sideA, sideB, widthA * 1.25F, widthB * 1.25F,
                    ca.withAlpha(ca.getAlpha() * 0.45F), cb.withAlpha(cb.getAlpha() * 0.45F));
            emitEnergySpan(buffer, matrix, pa, pb, sideA, sideB, widthA * 0.5F, widthB * 0.5F, ca, cb);
            emitted = true;
        }
        return emitted;
    }

    private Vec3d energyPosition(EnergyPoint point, float age) {
        double travel = this.energySpread.getCurrentValue() * 0.0625D * age * Math.sqrt(age);
        return point.pos.add(point.drift.multiply(travel)).add(0.0D, this.energyRise.getCurrentValue() * 0.0625D * age, 0.0D);
    }

    private static void emitEnergySpan(BufferBuilder buffer, Matrix4f matrix, Vec3d a, Vec3d b,
                                       Vec3d sideA, Vec3d sideB, float halfA, float halfB,
                                       ColorRGBA colorA, ColorRGBA colorB) {
        Vec3d al = a.subtract(sideA.multiply(halfA));
        Vec3d ar = a.add(sideA.multiply(halfA));
        Vec3d bl = b.subtract(sideB.multiply(halfB));
        Vec3d br = b.add(sideB.multiply(halfB));
        buffer.vertex(matrix, (float)al.x, (float)al.y, (float)al.z).color(colorA.getRGB());
        buffer.vertex(matrix, (float)ar.x, (float)ar.y, (float)ar.z).color(colorA.getRGB());
        buffer.vertex(matrix, (float)br.x, (float)br.y, (float)br.z).color(colorB.getRGB());
        buffer.vertex(matrix, (float)bl.x, (float)bl.y, (float)bl.z).color(colorB.getRGB());
    }

    private static float hash01(long seed, int salt) {
        long value = seed + salt * 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return ((value ^ (value >>> 31)) >>> 40) / (float)(1L << 24);
    }

    private void renderGlowTrail(MatrixStack matrices, Camera camera, List<TrailPoint> trails, long now, long trailLifetimeMs) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, FeverVisual.id("textures/bloom.png"));
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean hasVertices = false;
        for (int i = 0; i < trails.size(); i++) {
            TrailPoint point = trails.get(i);
            if (point.isExpired(now, trailLifetimeMs)) continue;
            float ageFrac = (float)(now - point.time) / (float)trailLifetimeMs;
            float alpha = (1.0F - Math.min(1.0F, ageFrac)) * opacity.getCurrentValue();
            float size = 0.18F + 0.28F * (1.0F - ageFrac);
            ColorRGBA color = ColorRGBA.fromInt(point.color).withAlpha(255.0F * alpha);
            matrices.push();
            RenderUtility.prepareMatrices(matrices, point.pos.add(0.0D, 0.72D + (i % 3) * 0.08D, 0.0D));
            matrices.multiply(camera.getRotation());
            DrawUtility.drawImage(matrices, buffer, -size, -size, 0.0D, size * 2.0F, size * 2.0F, color);
            matrices.pop();
            hasVertices = true;
        }
        BuiltBuffer built = buffer.endNullable();
        if (hasVertices && built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }

    private void renderShardTrail(MatrixStack matrices, Vec3d cameraPos, List<TrailPoint> trails, long now, long trailLifetimeMs) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, FeverVisual.id("textures/bloom.png"));
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        boolean hasVertices = false;
        int stride = Math.max(1, 12 - Math.round(this.shardDensity.getCurrentValue()));
        for (int i = 0; i < trails.size(); i += stride) {
            TrailPoint point = trails.get(i);
            if (point.isExpired(now, trailLifetimeMs)) continue;
            float ageFrac = (float)(now - point.time) / (float)trailLifetimeMs;
            float alpha = (1.0F - Math.min(1.0F, ageFrac)) * opacity.getCurrentValue() * shardAlpha.getCurrentValue();
            double angle = i * 2.399963229728653D;
            double burst = MathHelper.clamp(ageFrac / 0.22F, 0.0F, 1.0F);
            double spread = (0.12D + this.shardDistort.getCurrentValue() * 0.08D) * burst;
            double side = Math.cos(angle) * spread;
            double up = 0.22D + (i % 5) * 0.22D + Math.sin(angle * 1.7D) * 0.05D;
            Vec3d center = point.pos.add(side, up, Math.sin(angle) * spread);
            float h = (0.018F + 0.018F * this.shardReflect.getCurrentValue()) * opacity.getCurrentValue();
            float w = h * (5.0F + this.shardLength.getCurrentValue() * 1.1F);
            ColorRGBA source = ColorRGBA.fromInt(point.color);
            float tint = MathHelper.clamp(this.shardTint.getCurrentValue() / 100.0F, 0.0F, 1.0F);
            ColorRGBA color = new ColorRGBA(
                    255.0F + (source.getRed() - 255.0F) * tint,
                    255.0F + (source.getGreen() - 255.0F) * tint,
                    255.0F + (source.getBlue() - 255.0F) * tint,
                    180.0F * alpha
            );
            Vec3d direction = trailDirection(trails, i);
            emitWorldDash(buffer, matrix, center.subtract(cameraPos), direction, w, h, color);
            hasVertices = true;
            if (this.shardSparks.isEnabled() && (i % (stride * 2) == 0)) {
                float sparkSize = Math.max(h * 1.8F, 0.025F);
                ColorRGBA sparkColor = source.withAlpha(190.0F * alpha);
                Vec3d sparkCenter = center.add(Math.sin(angle * 1.7D) * 0.18D, Math.cos(angle) * 0.06D, Math.cos(angle * 1.3D) * 0.18D).subtract(cameraPos);
                emitWorldDash(buffer, matrix, sparkCenter, direction, sparkSize * 1.8F, sparkSize, sparkColor);
            }
        }
        BuiltBuffer built = buffer.endNullable();
        if (hasVertices && built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }

    private void renderEnergyTrail(MatrixStack matrices, Vec3d cameraPos, float playerHeight, List<TrailPoint> trails, long now, long trailLifetimeMs) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, FeverVisual.id("textures/bloom.png"));
        if (this.energyGlow.isEnabled()) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        } else {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        }
        if (this.energyWalls.isEnabled()) {
            RenderSystem.disableDepthTest();
        }
        BufferBuilder glow = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        boolean hasGlow = false;
        int stride = 3;
        float shrink = MathHelper.clamp(this.energyShrink.getCurrentValue() / 100.0F, 0.0F, 1.0F);
        float spread = this.energySpread.getCurrentValue() / 16.0F;
        float rise = this.energyRise.getCurrentValue() / 12.0F;
        for (int i = 0; i < trails.size(); i += stride) {
            TrailPoint point = trails.get(i);
            if (point.isExpired(now, trailLifetimeMs)) continue;
            float ageFrac = (float)(now - point.time) / (float)trailLifetimeMs;
            float alpha = (1.0F - Math.min(1.0F, ageFrac)) * opacity.getCurrentValue();
            float tailScale = 1.0F - shrink * ageFrac;
            Vec3d direction = trailDirection(trails, i);
            Vec3d sideDirection = new Vec3d(-direction.z, 0.0D, direction.x);
            for (int layer = 0; layer < 5; layer++) {
                double phase = i * 0.37D + layer * 2.11D + now * 0.001D;
                double side = Math.sin(phase) * (0.10D + layer * 0.07D) * spread;
                double y = playerHeight * 0.5D + Math.cos(phase * 0.7D) * 0.04D + rise * ageFrac;
                float halfLength = (0.42F + layer * 0.16F + 0.35F * (1.0F - ageFrac)) * tailScale;
                float halfHeight = (playerHeight * (0.48F + layer * 0.025F)) * tailScale;
                ColorRGBA color = ColorRGBA.fromInt(point.color).mix(Colors.getAccentColor(i * 9.0F + layer * 58.0F), 0.65F)
                        .withAlpha((82.0F - layer * 9.0F) * alpha);
                Vec3d center = point.pos.add(sideDirection.multiply(side)).add(0.0D, y, 0.0D).subtract(cameraPos);
                emitVerticalGlow(glow, matrix, center, direction, halfLength, halfHeight, color);
                hasGlow = true;
            }
        }
        BuiltBuffer glowBuilt = glow.endNullable();
        if (hasGlow && glowBuilt != null) {
            BufferRenderer.drawWithGlobalProgram(glowBuilt);
        }
        if (this.energyWalls.isEnabled()) {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }

    private static Vec3d trailDirection(List<TrailPoint> trails, int index) {
        Vec3d before = trails.get(Math.max(0, index - 2)).pos;
        Vec3d after = trails.get(Math.min(trails.size() - 1, index + 2)).pos;
        Vec3d direction = new Vec3d(after.x - before.x, 0.0D, after.z - before.z);
        return direction.lengthSquared() < 1.0E-6D ? new Vec3d(1.0D, 0.0D, 0.0D) : direction.normalize();
    }

    private static void emitWorldDash(BufferBuilder buffer, Matrix4f matrix, Vec3d center, Vec3d direction, float halfLength, float halfThickness, ColorRGBA color) {
        Vec3d along = direction.multiply(halfLength);
        Vec3d up = new Vec3d(0.0D, halfThickness, 0.0D);
        texturedQuad(buffer, matrix, center.subtract(along).subtract(up), center.add(along).subtract(up), center.add(along).add(up), center.subtract(along).add(up), color);

        Vec3d side = new Vec3d(-direction.z, 0.0D, direction.x).multiply(halfThickness);
        texturedQuad(buffer, matrix, center.subtract(along).subtract(side), center.add(along).subtract(side), center.add(along).add(side), center.subtract(along).add(side), color.withAlpha(color.getAlpha() * 0.72F));
    }

    private static void emitVerticalGlow(BufferBuilder buffer, Matrix4f matrix, Vec3d center, Vec3d direction, float halfLength, float halfHeight, ColorRGBA color) {
        Vec3d along = direction.multiply(halfLength);
        Vec3d up = new Vec3d(0.0D, halfHeight, 0.0D);
        texturedQuad(buffer, matrix, center.subtract(along).subtract(up), center.add(along).subtract(up), center.add(along).add(up), center.subtract(along).add(up), color);

        Vec3d sideDirection = new Vec3d(-direction.z, 0.0D, direction.x);
        Vec3d side = sideDirection.multiply(halfLength * 0.45F);
        texturedQuad(buffer, matrix, center.subtract(side).subtract(up), center.add(side).subtract(up), center.add(side).add(up), center.subtract(side).add(up), color.withAlpha(color.getAlpha() * 0.55F));
    }

    private static void texturedQuad(BufferBuilder buffer, Matrix4f matrix, Vec3d a, Vec3d b, Vec3d c, Vec3d d, ColorRGBA color) {
        int rgba = color.getRGB();
        buffer.vertex(matrix, (float)a.x, (float)a.y, (float)a.z).texture(0.0F, 1.0F).color(rgba);
        buffer.vertex(matrix, (float)b.x, (float)b.y, (float)b.z).texture(1.0F, 1.0F).color(rgba);
        buffer.vertex(matrix, (float)c.x, (float)c.y, (float)c.z).texture(1.0F, 0.0F).color(rgba);
        buffer.vertex(matrix, (float)d.x, (float)d.y, (float)d.z).texture(0.0F, 0.0F).color(rgba);
    }

    private void renderTrailStrip(MatrixStack matrices, Vec3d cameraPos, float playerHeight, List<TrailPoint> trails, long now, long trailLifetimeMs) {
        BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        float actualOpacity = opacity.getCurrentValue();
        boolean hasVertices = false;

        for (TrailPoint point : trails) {
            if (point.isExpired(now, trailLifetimeMs)) continue;

            float ageFrac = (float) (now - point.time) / (float) trailLifetimeMs;
            float alpha = 1f - Math.min(1f, ageFrac);
            alpha = Math.max(0.01f, alpha) * actualOpacity;

            int color = (MathHelper.clamp(Math.round(alpha * 255.0F), 0, 255) << 24) | (point.color & 0x00FFFFFF);
            float x = (float) (point.pos.x - cameraPos.x);
            float yBottom = (float) (point.pos.y - cameraPos.y);
            float yTop = yBottom + playerHeight;
            float z = (float) (point.pos.z - cameraPos.z);

            buffer.vertex(positionMatrix, x, yTop, z)
                    .color(color);
            buffer.vertex(positionMatrix, x, yBottom, z)
                    .color(color);
            hasVertices = true;
        }

        if (hasVertices) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } else {
            buffer.endNullable();
        }
    }

    private ColorRGBA getTrailColor(PlayerEntity entity, int index) {
        if (colorCustom.isSelected()) {
            return customColorFirst.getColor().mix(customColorSecond.getColor(), (index % 40) / 39.0F);
        }

        return Colors.getAccentColor(index * 6.0F);
    }

    private boolean shouldRenderTrails(PlayerEntity entity) {
        if (entity == mc.player) {
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
                return false;
            }
            return targetSelf.isSelected();
        }
        if (targetFriends.isSelected() && FeverVisual.getInstance().getFriendManager().isFriend(entity.getName().getString())) {
            return !isPlayerInvisible(entity);
        }
        return false;
    }

    private boolean isPlayerInvisible(PlayerEntity player) {
        if (player == mc.player) return false;
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
        if (player.isInvisible()) return true;
        if (player.isInvisibleTo(mc.player)) return true;
        return false;
    }

    private record ShardBurst(Vec3d origin, Vec3d direction, long spawnTime, long seed, int color, int count) {
    }

    private static final List<SurfaceSample> ENERGY_SURFACE = createEnergySurface();

    private static List<SurfaceSample> createEnergySurface() {
        List<SurfaceSample> samples = new ArrayList<>();
        addBoxSurface(samples, 0.0D, 1.55D, 0.0D, 0.25D, 0.25D, 0.25D);
        addBoxSurface(samples, 0.0D, 1.05D, 0.0D, 0.25D, 0.375D, 0.125D);
        addBoxSurface(samples, -0.375D, 1.05D, 0.0D, 0.125D, 0.375D, 0.125D);
        addBoxSurface(samples, 0.375D, 1.05D, 0.0D, 0.125D, 0.375D, 0.125D);
        addBoxSurface(samples, -0.125D, 0.375D, 0.0D, 0.125D, 0.375D, 0.125D);
        addBoxSurface(samples, 0.125D, 0.375D, 0.0D, 0.125D, 0.375D, 0.125D);
        return List.copyOf(samples);
    }

    private static void addBoxSurface(List<SurfaceSample> out, double cx, double cy, double cz,
                                      double hx, double hy, double hz) {
        addFace(out, cx, cy, cz, hx, hy, hz, 0, -1.0D);
        addFace(out, cx, cy, cz, hx, hy, hz, 0, 1.0D);
        addFace(out, cx, cy, cz, hx, hy, hz, 1, -1.0D);
        addFace(out, cx, cy, cz, hx, hy, hz, 1, 1.0D);
        addFace(out, cx, cy, cz, hx, hy, hz, 2, -1.0D);
        addFace(out, cx, cy, cz, hx, hy, hz, 2, 1.0D);
    }

    private static void addFace(List<SurfaceSample> out, double cx, double cy, double cz,
                                double hx, double hy, double hz, int axis, double sign) {
        double spacing = 1.25D / 16.0D;
        double uHalf = axis == 0 ? hz : hx;
        double vHalf = axis == 1 ? hz : hy;
        int stepsU = MathHelper.clamp((int)Math.round(uHalf * 2.0D / spacing), 1, 24);
        int stepsV = MathHelper.clamp((int)Math.round(vHalf * 2.0D / spacing), 1, 24);
        Vec3d normal = axis == 0 ? new Vec3d(sign, 0.0D, 0.0D)
                : axis == 1 ? new Vec3d(0.0D, sign, 0.0D) : new Vec3d(0.0D, 0.0D, sign);
        for (int u = 0; u < stepsU; u++) {
            double fu = ((u + 0.5D) / stepsU * 2.0D - 1.0D) * uHalf;
            for (int v = 0; v < stepsV; v++) {
                double fv = ((v + 0.5D) / stepsV * 2.0D - 1.0D) * vHalf;
                Vec3d offset;
                if (axis == 0) offset = new Vec3d(cx + sign * hx, cy + fv, cz + fu);
                else if (axis == 1) offset = new Vec3d(cx + fu, cy + sign * hy, cz + fv);
                else offset = new Vec3d(cx + fu, cy + fv, cz + sign * hz);
                out.add(new SurfaceSample(offset.add(normal.multiply(0.02D)), normal));
            }
        }
    }

    private final class EnergyState {
        private static final int SAMPLE_COUNT = 20;
        private final EnergyRibbon[] ribbons = new EnergyRibbon[ENERGY_SURFACE.size()];
        private float smoothPower;
        private long lastSample;
        private long lastPower;

        private EnergyState() {
            for (int i = 0; i < this.ribbons.length; i++) {
                this.ribbons[i] = new EnergyRibbon(i);
            }
        }

        private void sample(PlayerEntity entity, Vec3d position, long now, long lifetime) {
            double moveX = entity.getX() - entity.lastRenderX;
            double moveZ = entity.getZ() - entity.lastRenderZ;
            float speed = (float)Math.sqrt(moveX * moveX + moveZ * moveZ);
            float targetPower = MathHelper.clamp(speed / 0.25F, 0.0F, 1.0F);
            long elapsed = this.lastPower == 0L ? 0L : MathHelper.clamp(now - this.lastPower, 0L, 250L);
            this.lastPower = now;
            float rate = 1.0F - (float)Math.exp(-elapsed / 190.0D);
            this.smoothPower += (targetPower - this.smoothPower) * rate;
            boolean push = this.smoothPower > 0.01F && now - this.lastSample >= Math.max(8L, lifetime / SAMPLE_COUNT);
            if (push) this.lastSample = now;

            float yaw = (float)Math.toRadians(entity.bodyYaw);
            Vec3d right = new Vec3d(Math.cos(yaw), 0.0D, Math.sin(yaw));
            Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            for (int i = 0; i < this.ribbons.length; i++) {
                SurfaceSample sample = ENERGY_SURFACE.get(i);
                Vec3d offset = sample.offset;
                Vec3d world = position.add(right.multiply(offset.x)).add(0.0D, offset.y, 0.0D).add(forward.multiply(offset.z));
                Vec3d localNormal = sample.normal;
                Vec3d normal = right.multiply(localNormal.x).add(0.0D, localNormal.y, 0.0D).add(forward.multiply(localNormal.z));
                EnergyRibbon ribbon = this.ribbons[i];
                ribbon.livePosition = world;
                ribbon.liveColor = getTrailColor(entity, i * 7 + ribbon.points.size()).getRGB();
                if (!push) continue;
                double fx = Math.sin(world.y * 0.85D * 1.7D + world.z * 0.85D * 0.9D)
                        + 0.5D * Math.cos(world.z * 0.85D * 2.3D - world.x * 0.85D * 1.1D);
                double fy = Math.sin(world.z * 0.85D * 1.3D + world.x * 0.85D * 1.9D)
                        + 0.5D * Math.cos(world.x * 0.85D * 2.1D - world.y * 0.85D * 0.7D);
                double fz = Math.sin(world.x * 0.85D * 1.1D + world.y * 0.85D * 2.1D)
                        + 0.5D * Math.cos(world.y * 0.85D * 1.9D - world.z * 0.85D * 1.3D);
                Vec3d drift = new Vec3d(fx, fy, fz).multiply(0.9D).add(normal.multiply(0.65D));
                if (drift.lengthSquared() > 1.0E-6D) drift = drift.normalize();
                ribbon.points.addLast(new EnergyPoint(world, now, ribbon.liveColor, drift, this.smoothPower));
                ribbon.prune(now, lifetime);
                while (ribbon.points.size() > SAMPLE_COUNT) ribbon.points.removeFirst();
            }
        }
    }

    private static final class EnergyRibbon {
        private final Deque<EnergyPoint> points = new ArrayDeque<>();
        private final float width;
        private final float brightness;
        private Vec3d livePosition;
        private int liveColor;

        private EnergyRibbon(int index) {
            this.width = 0.15625F * (0.72F + hash01(index * 0x9E3779B9L, 2) * 0.56F);
            this.brightness = 0.72F + hash01(index * 0xC2B2AE35L, 5) * 0.56F;
        }

        private void prune(long now, long lifetime) {
            while (!this.points.isEmpty() && now - this.points.peekFirst().time > lifetime) {
                this.points.removeFirst();
            }
        }
    }

    private record SurfaceSample(Vec3d offset, Vec3d normal) {
    }

    private record EnergyPoint(Vec3d pos, long time, int color, Vec3d drift, float power) {
    }

    private static class TrailPoint {
        public final Vec3d pos;
        public final int color;
        public final long time;

        public TrailPoint(Vec3d pos, int color, long time) {
            this.pos = pos;
            this.color = color;
            this.time = time;
        }

        public boolean isExpired(long now, long trailLifetimeMs) {
            return (now - time) > trailLifetimeMs;
        }
    }
}
