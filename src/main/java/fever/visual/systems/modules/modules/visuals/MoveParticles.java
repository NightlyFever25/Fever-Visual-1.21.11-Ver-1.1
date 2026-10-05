package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;

import fever.visual.utility.render.compat.GlStateManager.DstFactor;
import fever.visual.utility.render.compat.GlStateManager.SrcFactor;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.*;
import fever.visual.systems.setting.settings.*;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import fever.visual.utility.render.Utils;
import net.minecraft.client.render.*;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;

@ModuleInfo(
        name = "Move Particles",
        category = ModuleCategory.VISUALS,
        desc = "modules.descriptions.move_particles"
)
public class MoveParticles extends BaseModule implements IMinecraft {
    private static final int MAX_ACTIVE_PARTICLES = 600;
    private static final Identifier CROWN = Identifier.of("fevervisual", "textures/particles/crown.png");
    private static final Identifier DOLLAR = Identifier.of("fevervisual", "textures/particles/dollar.png");
    private static final Identifier FIREFLY = Identifier.of("fevervisual", "textures/particles/firefly.png");
    private static final Identifier GENSHIN = Identifier.of("fevervisual", "textures/particles/genshin.png");
    private static final Identifier HEART = Identifier.of("fevervisual", "textures/particles/heart.png");
    private static final Identifier THOR = Identifier.of("fevervisual", "textures/particles/thor.png");
    private static final Identifier LINE = Identifier.of("fevervisual", "textures/particles/line.png");
    private static final Identifier POINT = Identifier.of("fevervisual", "textures/particles/point.png");
    private static final Identifier RHOMBUS = Identifier.of("fevervisual", "textures/particles/rhombus.png");
    private static final Identifier SNOWFLAKE = Identifier.of("fevervisual", "textures/particles/snowflake.png");
    private static final Identifier SPARK = Identifier.of("fevervisual", "textures/particles/spark.png");
    private static final Identifier STAR = Identifier.of("fevervisual", "textures/particles/star.png");
    private static final Identifier MOON = Identifier.of("fevervisual", "textures/particles/moon.png");
    private static final Identifier FEATHER = Identifier.of("fevervisual", "textures/particles/feather.png");
    private static final Identifier[] RANDOM_TEXTURES = {
            CROWN, DOLLAR, FIREFLY, GENSHIN, HEART, THOR, LINE, POINT, RHOMBUS, SNOWFLAKE, SPARK, STAR, MOON, FEATHER
    };

    private final SelectSetting targets = new SelectSetting(this, "modules.settings.model_changer.targets");
    private final SelectSetting.Value targetSelf = new SelectSetting.Value(targets, "modules.settings.move_particles.render_self").select();
    private final SelectSetting.Value targetFriends = new SelectSetting.Value(targets, "modules.settings.move_particles.render_friends");

    private final ModeSetting particleType = new ModeSetting(this, "modules.settings.move_particles.particle_type");
    private final ModeSetting.Value star = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.star").select();
    private final ModeSetting.Value crown = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.crown");
    private final ModeSetting.Value dollar = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.dollar");
    private final ModeSetting.Value firefly = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.firefly");
    private final ModeSetting.Value heart = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.heart");
    private final ModeSetting.Value lightning = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.lightning");
    private final ModeSetting.Value line = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.line");
    private final ModeSetting.Value point = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.point");
    private final ModeSetting.Value rhombus = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.rhombus");
    private final ModeSetting.Value snowflake = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.snowflake");
    private final ModeSetting.Value spark = new ModeSetting.Value(this.particleType, "modules.settings.move_par ticles.particle_type.spark");
    private final ModeSetting.Value moon = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.moon");
    private final ModeSetting.Value feather = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.feather");
    private final ModeSetting.Value genshin = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.genshin");
    private final ModeSetting.Value randomType = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.random");

    private final ModeSetting movementMode = new ModeSetting(this, "modules.settings.move_particles.movement_mode", "modules.settings.move_particles.movement_mode.description");
    private final ModeSetting.Value normalMode = new ModeSetting.Value(this.movementMode, "modules.settings.move_particles.movement_mode.normal").select();
    private final ModeSetting.Value bounceMode = new ModeSetting.Value(this.movementMode, "modules.settings.move_particles.movement_mode.bounce");
    private final ModeSetting.Value gentleMode = new ModeSetting.Value(this.movementMode, "modules.settings.move_particles.movement_mode.gentle");
    private final ModeSetting.Value featherMode = new ModeSetting.Value(this.movementMode, "modules.settings.move_particles.movement_mode.feather").select();

    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.move_particles.color_mode");
    private final ModeSetting.Value themeColor = new ModeSetting.Value(this.colorMode, "modules.settings.move_particles.color_mode.theme").select();
    private final ModeSetting.Value rainbowColor = new ModeSetting.Value(this.colorMode, "modules.settings.move_particles.color_mode.rainbow", "", () -> true);
    private final ModeSetting.Value velocityColor = new ModeSetting.Value(this.colorMode, "modules.settings.move_particles.color_mode.velocity", "", () -> true);
    private final ModeSetting.Value customColor = new ModeSetting.Value(this.colorMode, "modules.settings.move_particles.color_mode.custom");

    private final ColorSetting customColorFirst = new ColorSetting(this, "modules.settings.move_particles.custom_color", () -> !customColor.isSelected())
            .color(new ColorRGBA(255, 100, 100, 255))
            .alpha(true);
    private final ColorSetting customColorSecond = new ColorSetting(this, "modules.settings.move_particles.custom_color_second", () -> !customColor.isSelected())
            .color(new ColorRGBA(100, 160, 255, 255))
            .alpha(true);

    private final SliderSetting particleCount = new SliderSetting(this, "modules.settings.move_particles.count", "modules.settings.move_particles.count.description")
            .min(5.0F).max(50.0F).step(1.0F).currentValue(20.0F);
    private final SliderSetting particleSize = new SliderSetting(this, "modules.settings.move_particles.size", "modules.settings.move_particles.size.description")
            .min(0.1F).max(1.5F).step(0.05F).currentValue(0.4F);
    private final SliderSetting particleLife = new SliderSetting(this, "modules.settings.move_particles.life", "modules.settings.move_particles.life.description")
            .min(500.0F).max(3000.0F).step(100.0F).currentValue(1000.0F);
    private final SliderSetting speedThreshold = new SliderSetting(this, "modules.settings.move_particles.speed_threshold", "modules.settings.move_particles.speed_threshold.description")
            .min(0.05F).max(0.5F).step(0.01F).currentValue(0.12F);
    private final SliderSetting spawnInterval = new SliderSetting(this, "modules.settings.move_particles.spawn_interval", "modules.settings.move_particles.spawn_interval.description")
            .min(1.0F).max(10.0F).step(1.0F).currentValue(2.0F);
    private final SliderSetting jumpHeight = new SliderSetting(this, "modules.settings.move_particles.jump_height", "modules.settings.move_particles.jump_height.description")
            .min(0.05F).max(0.5F).step(0.01F).currentValue(0.15F);
    private final SliderSetting colorSpeedMultiplier = new SliderSetting(this, "modules.settings.move_particles.color_speed_multiplier", "modules.settings.move_particles.color_speed_multiplier.description", () -> true)
            .min(0.5F).max(5.0F).step(0.1F).currentValue(2.0F);

    private final BooleanSetting trailingEffect = new BooleanSetting(this, "modules.settings.move_particles.trailing_effect", "modules.settings.move_particles.trailing_effect.description").enabled(true);
    private final BooleanSetting disableFirstPerson = new BooleanSetting(this, "modules.settings.move_particles.disable_first_person", "modules.settings.move_particles.disable_first_person.description").enabled(true);

    private final List<MoveParticle> particles = new ArrayList<>();
    private final Map<Identifier, List<MoveParticle>> particlesByTexture = new HashMap<>();
    private final Random rand = new Random();
    private final Map<UUID, PlayerMovementData> playerData = new HashMap<>();
    private long lastUpdateTime = System.nanoTime();

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        updateParticles();
        spawnParticlesForPlayers();
    };

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (particles.isEmpty()) return;

        long now = System.nanoTime();
        double deltaTime = Math.min((now - lastUpdateTime) / 1.0E9, 0.05);
        lastUpdateTime = now;

        updateParticlePositions(deltaTime);
        renderParticles(event.getMatrices(), event.getTickDelta());
    };

    @Override
    public void onEnable() {
        super.onEnable();
        particles.clear();
        playerData.clear();
        lastUpdateTime = System.nanoTime();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
        playerData.clear();
    }

    private void updateParticles() {
        long now = System.currentTimeMillis();
        float maxLife = particleLife.getCurrentValue();
        for (int i = particles.size() - 1; i >= 0; i--) {
            if (now - particles.get(i).spawnTime > maxLife) {
                particles.remove(i);
            }
        }
    }

    private void spawnParticlesForPlayers() {
        if (mc.world == null || mc.player == null) return;
        if (disableFirstPerson.isEnabled() && mc.options.getPerspective().isFirstPerson()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.isRemoved()) continue;
            if (!shouldRenderForPlayer(player)) continue;

            spawnParticlesForPlayer(player);
        }
    }

    private boolean shouldRenderForPlayer(PlayerEntity player) {
        if (player == mc.player) {
            return targetSelf.isSelected();
        }

        boolean isFriend = isFriend(player);
        if (isFriend && targetFriends.isSelected()) {
            return !isInvisible(player);
        }

        return false;
    }

    private void spawnParticlesForPlayer(PlayerEntity player) {
        UUID uuid = player.getUuid();
        PlayerMovementData data = playerData.computeIfAbsent(uuid, k -> new PlayerMovementData());

        Vec3d currentPos = player.getEntityPos();
        double speed = currentPos.distanceTo(data.lastPos);

        data.lastPos = currentPos;
        data.ticksSinceLastSpawn++;
        if (speed < speedThreshold.getCurrentValue()) {
            return;
        }
        if (data.ticksSinceLastSpawn < spawnInterval.getCurrentValue()) {
            return;
        }

        data.ticksSinceLastSpawn = 0;
        Vec3d velocity = currentPos.subtract(data.lastPosBefore);
        data.lastPosBefore = currentPos;
        int count = (int) (particleCount.getCurrentValue() * Math.min(1.0, speed * 2.0));
        count = Math.max(1, Math.min(count, 15));
        for (int i = 0; i < count; i++) {
            if (particles.size() >= MAX_ACTIVE_PARTICLES) {
                break;
            }
            Identifier texture = getParticleTexture();
            ColorRGBA color = getParticleColor(speed, i);
            if (color == null || color.getAlpha() <= 0) {
                color = Colors.getAccentColor();
            }
            Vec3d spawnPos;
            if (trailingEffect.isEnabled() && velocity.length() > 0.01) {
                Vec3d direction = velocity.normalize();
                double offset = -0.3 - rand.nextDouble() * 0.4;
                spawnPos = currentPos.add(
                        direction.x * offset + (rand.nextDouble() - 0.5) * 0.3,
                        rand.nextDouble() * player.getHeight() * 0.4,
                        direction.z * offset + (rand.nextDouble() - 0.5) * 0.3
                );
            } else {
                spawnPos = new Vec3d(
                        currentPos.x + (rand.nextDouble() - 0.5) * 0.6,
                        currentPos.y + rand.nextDouble() * player.getHeight() * 0.4,
                        currentPos.z + (rand.nextDouble() - 0.5) * 0.6
                );
            }
            MoveParticle particle = createParticleWithMovement(texture, spawnPos, velocity, color, speed, i);
            particles.add(particle);
        }
    }

    private MoveParticle createParticleWithMovement(Identifier texture, Vec3d spawnPos, Vec3d playerVelocity, ColorRGBA color, double playerSpeed, int index) {
        String mode = movementMode.getValue().getName();
        float jumpH = jumpHeight.getCurrentValue();
        if (featherMode.isSelected()) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = 0.05 + rand.nextDouble() * 0.2;
            double horizontalX = Math.cos(angle) * speed;
            double horizontalZ = Math.sin(angle) * speed;
            double upVelocity = -0.01 + rand.nextDouble() * 0.025;

            double forwardVelocity = horizontalX;
            double sideVelocity = horizontalZ;

            if (trailingEffect.isEnabled() && playerVelocity.length() > 0.01) {
                Vec3d oppositeDir = playerVelocity.normalize();
                forwardVelocity = -oppositeDir.x * 0.2 + horizontalX * 0.8;
                sideVelocity = -oppositeDir.z * 0.2 + horizontalZ * 0.8;
            }

            Vec3d particleVelocity = new Vec3d(forwardVelocity, upVelocity, sideVelocity);
            float size = particleSize.getCurrentValue() * (0.5F + rand.nextFloat() * 0.6F);

            return new MoveParticle(texture, spawnPos, particleVelocity, color, size, System.currentTimeMillis(), false, true);

        } else if (bounceMode.isSelected()) {
            double upVelocity = 0.12 + rand.nextDouble() * jumpH;
            double forwardVelocity = (rand.nextDouble() - 0.5) * 0.15;
            double sideVelocity = (rand.nextDouble() - 0.5) * 0.15;

            if (trailingEffect.isEnabled() && playerVelocity.length() > 0.01) {
                forwardVelocity = -playerVelocity.x * 0.3 + (rand.nextDouble() - 0.5) * 0.1;
                sideVelocity = -playerVelocity.z * 0.3 + (rand.nextDouble() - 0.5) * 0.1;
            }

            Vec3d particleVelocity = new Vec3d(forwardVelocity, upVelocity, sideVelocity);
            float size = particleSize.getCurrentValue() * (0.6F + rand.nextFloat() * 0.6F);

            return new MoveParticle(texture, spawnPos, particleVelocity, color, size, System.currentTimeMillis(), true, false);

        } else if (gentleMode.isSelected()) {
            double upVelocity = 0.04 + rand.nextDouble() * 0.08;
            double forwardVelocity = (rand.nextDouble() - 0.5) * 0.08;
            double sideVelocity = (rand.nextDouble() - 0.5) * 0.08;

            if (trailingEffect.isEnabled() && playerVelocity.length() > 0.01) {
                forwardVelocity = -playerVelocity.x * 0.2 + (rand.nextDouble() - 0.5) * 0.05;
                sideVelocity = -playerVelocity.z * 0.2 + (rand.nextDouble() - 0.5) * 0.05;
            }

            Vec3d particleVelocity = new Vec3d(forwardVelocity, upVelocity, sideVelocity);
            float size = particleSize.getCurrentValue() * (0.7F + rand.nextFloat() * 0.5F);

            return new MoveParticle(texture, spawnPos, particleVelocity, color, size, System.currentTimeMillis(), false, false);

        } else {
            Vec3d particleVelocity = new Vec3d(
                    (rand.nextDouble() - 0.5) * 0.2 + (trailingEffect.isEnabled() ? -playerVelocity.x * 0.3 : 0),
                    rand.nextDouble() * 0.08 + 0.02,
                    (rand.nextDouble() - 0.5) * 0.2 + (trailingEffect.isEnabled() ? -playerVelocity.z * 0.3 : 0)
            );
            float size = particleSize.getCurrentValue() * (0.5F + rand.nextFloat() * 0.6F);

            return new MoveParticle(texture, spawnPos, particleVelocity, color, size, System.currentTimeMillis(), false, false);
        }
    }

    private void updateParticlePositions(double deltaTime) {
        Iterator<MoveParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            MoveParticle particle = iterator.next();
            particle.update(deltaTime);
            if (particle.position.y < -0.5 || Math.abs(particle.position.y) > 256) {
                iterator.remove();
            }
        }
    }

    private void renderParticles(MatrixStack matrix, float tickDelta) {
        if (particles.isEmpty()) return;

        setupRenderState();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();

        Tessellator tessellator = Tessellator.getInstance();

        for (List<MoveParticle> bucket : particlesByTexture.values()) {
            bucket.clear();
        }

        for (MoveParticle particle : particles) {
            if (Utils.isInViewHemisphere(camera, particle.position.x, particle.position.y, particle.position.z, 2.0)) {
                particlesByTexture.computeIfAbsent(particle.texture, key -> new ArrayList<>()).add(particle);
            }
        }

        float maxLife = particleLife.getCurrentValue();

        for (Map.Entry<Identifier, List<MoveParticle>> entry : particlesByTexture.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            RenderSystem.setShaderTexture(0, entry.getKey());
            BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (MoveParticle particle : entry.getValue()) {
                renderParticle(matrix, particle, cameraPos, buffer, maxLife);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        resetRenderState();
    }

    private void renderParticle(MatrixStack matrix, MoveParticle particle, Vec3d cameraPos, BufferBuilder buffer, float maxLife) {
        matrix.push();

        matrix.translate(
                particle.position.x - cameraPos.x,
                particle.position.y - cameraPos.y,
                particle.position.z - cameraPos.z
        );
        matrix.multiply(mc.gameRenderer.getCamera().getRotation());

        if (particle.rotate180) {
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
        }

        Matrix4f m = matrix.peek().getPositionMatrix();

        float halfSize = particle.size / 2.0F;
        float alpha = particle.getAlpha(maxLife);

        int color = (MathHelper.clamp((int)(alpha * 255.0F), 0, 255) << 24) | (particle.color.getRGB() & 0x00FFFFFF);

        drawQuad(buffer, m, halfSize, color);

        matrix.pop();
    }

    private void drawQuad(BufferBuilder buffer, Matrix4f matrix, float size, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = ((color >> 24) & 0xFF) / 255.0F;
        if (a <= 0.01f) a = 0.01f;

        buffer.vertex(matrix, -size, -size, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
        buffer.vertex(matrix, size, size, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
    }

    private void setupRenderState() {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ONE, DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
    }

    private void resetRenderState() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private Identifier getParticleTexture() {
        if (randomType.isSelected()) {
            return RANDOM_TEXTURES[this.rand.nextInt(RANDOM_TEXTURES.length)];
        } else if (crown.isSelected()) {
            return CROWN;
        } else if (dollar.isSelected()) {
            return DOLLAR;
        } else if (heart.isSelected()) {
            return HEART;
        } else if (firefly.isSelected()) {
            return FIREFLY;
        } else if (lightning.isSelected()) {
            return THOR;
        } else if (line.isSelected()) {
            return LINE;
        } else if (point.isSelected()) {
            return POINT;
        } else if (rhombus.isSelected()) {
            return RHOMBUS;
        } else if (snowflake.isSelected()) {
            return SNOWFLAKE;
        } else if (spark.isSelected()) {
            return SPARK;
        } else if (moon.isSelected()) {
            return MOON;
        } else if (feather.isSelected()) {
            return FEATHER;
        } else if (genshin.isSelected()) {
            return GENSHIN;
        } else {
            return STAR;
        }
    }

    private ColorRGBA getParticleColor(double speed, int index) {
        if (customColor.isSelected()) {
            ColorRGBA first = customColorFirst.getColor();
            ColorRGBA second = customColorSecond.getColor();
            return first.mix(second, (index % 20) / 19.0F);
        }

        return Colors.getAccentColor(index * 12.0F);
    }
    private boolean isFriend(PlayerEntity player) {
        if (FeverVisual.getInstance().getFriendManager() == null) return false;
        return FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString());
    }

    private boolean isInvisible(LivingEntity entity) {
        if (entity.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
        if (entity.isInvisible()) return true;
        if (mc.player != null && entity.isInvisibleTo(mc.player)) return true;
        return false;
    }


    private static class PlayerMovementData {
        Vec3d lastPos = Vec3d.ZERO;
        Vec3d lastPosBefore = Vec3d.ZERO;
        int ticksSinceLastSpawn = 0;
    }

    private static class MoveParticle {
        private final Identifier texture;
        private Vec3d position;
        private Vec3d velocity;
        private final ColorRGBA color;
        private final float size;
        private final long spawnTime;
        private final boolean bounceMode;
        private final boolean featherMode;
        private final boolean rotate180;
        private double groundY;

        public MoveParticle(Identifier texture, Vec3d position, Vec3d velocity, ColorRGBA color, float size, long spawnTime, boolean bounceMode, boolean featherMode) {
            this.texture = texture;
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.spawnTime = spawnTime;
            this.bounceMode = bounceMode;
            this.featherMode = featherMode;
            this.rotate180 = texture.equals(HEART);
            this.groundY = position.y;
        }

        public void update(double deltaTime) {
            if (featherMode) {
                velocity = velocity.multiply(0.98);
                velocity = velocity.add(0, -0.08 * deltaTime, 0);
                position = position.add(velocity.multiply(deltaTime * 60.0));

            } else if (bounceMode) {
                velocity = velocity.add(0, -0.8 * deltaTime, 0);
                position = position.add(velocity.multiply(deltaTime * 60.0));

                if (position.y <= groundY - 0.05) {
                    position = new Vec3d(position.x, groundY - 0.05, position.z);
                    velocity = new Vec3d(velocity.x * 0.6, -velocity.y * 0.4, velocity.z * 0.6);
                    if (Math.abs(velocity.y) < 0.02) {
                        velocity = new Vec3d(velocity.x * 0.95, 0, velocity.z * 0.95);
                    }
                }
            } else {
                velocity = velocity.multiply(0.98);
                velocity = velocity.add(0, -0.15 * deltaTime, 0);
                position = position.add(velocity.multiply(deltaTime * 60.0));
            }
        }

        public float getAlpha(float maxLifeTime) {
            float age = System.currentTimeMillis() - spawnTime;
            if (age >= maxLifeTime) {
                return 0.0F;
            }
            if (age < 50.0F) {
                return age / 50.0F;
            }
            if (age > maxLifeTime - 200.0F) {
                return (maxLifeTime - age) / 200.0F;
            }
            return 1.0F;
        }
    }
}
