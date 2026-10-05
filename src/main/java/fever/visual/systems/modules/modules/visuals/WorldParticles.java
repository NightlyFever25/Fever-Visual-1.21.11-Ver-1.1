package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;

import fever.visual.utility.render.compat.GlStateManager.DstFactor;
import fever.visual.utility.render.compat.GlStateManager.SrcFactor;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import fever.visual.utility.render.Utils;
import net.minecraft.client.render.*;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.*;
//Да давай пасти бездарность :)
//Made by FeverVisuals (NightlyFever)
@ModuleInfo(name = "World Particles", category = ModuleCategory.VISUALS, desc = "modules.descriptions.world_particles")
public class WorldParticles extends BaseModule implements IMinecraft {

    private final ModeSetting particleType = new ModeSetting(this, "modules.settings.world_particles.particle_type");
    private final ModeSetting.Value star = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.star").select();
    private final ModeSetting.Value crown = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.crown");
    private final ModeSetting.Value dollar = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.dollar");
    private final ModeSetting.Value firefly = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.firefly");
    private final ModeSetting.Value heart = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.heart");
    private final ModeSetting.Value lightning = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.lightning");
    private final ModeSetting.Value line = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.line");
    private final ModeSetting.Value point = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.point");
    private final ModeSetting.Value rhombus = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.rhombus");
    private final ModeSetting.Value snowflake = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.snowflake");
    private final ModeSetting.Value spark = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.spark");
    private final ModeSetting.Value moon = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.moon");
    private final ModeSetting.Value feather = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.feather");
    private final ModeSetting.Value genshin = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.genshin");
    private final ModeSetting.Value randomType = new ModeSetting.Value(this.particleType, "modules.settings.world_particles.particle_type.random");

    private final ModeSetting animationMode = new ModeSetting(this, "modules.settings.world_particles.animation_mode", "Режим анимации частиц");
    private final ModeSetting.Value floatMode = new ModeSetting.Value(this.animationMode, "modules.settings.world_particles.animation_mode.float", "Парение").select();
    private final ModeSetting.Value snowMode = new ModeSetting.Value(this.animationMode, "modules.settings.world_particles.animation_mode.snow", "Снегопад");
    private final ModeSetting.Value rainMode = new ModeSetting.Value(this.animationMode, "modules.settings.world_particles.animation_mode.rain", "Дождь");

    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.world_particles.color_mode");
    private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.world_particles.color_mode.theme").select();
    private final ModeSetting.Value colorRainbow = new ModeSetting.Value(this.colorMode, "modules.settings.world_particles.color_mode.rainbow", "", () -> true);
    private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.world_particles.color_mode.custom");

    private final ColorSetting customColor = new ColorSetting(this, "modules.settings.world_particles.custom_color", () -> !colorCustom.isSelected())
            .color(new ColorRGBA(255, 100, 100, 255))
            .alpha(true);
    private final ColorSetting customColorSecond = new ColorSetting(this, "modules.settings.world_particles.custom_color_second", () -> !colorCustom.isSelected())
            .color(new ColorRGBA(100, 160, 255, 255))
            .alpha(true);

    private final SliderSetting floatSpeed = new SliderSetting(this, "modules.settings.world_particles.float_speed", "Скорость парения")
            .min(0.1F).max(2.0F).step(0.05F).currentValue(0.5F);
    private final SliderSetting snowSpeed = new SliderSetting(this, "modules.settings.world_particles.snow_speed", "Скорость снегопада")
            .min(0.05F).max(1.0F).step(0.05F).currentValue(0.2F);
    private final SliderSetting rainSpeed = new SliderSetting(this, "modules.settings.world_particles.rain_speed", "Скорость дождя")
            .min(0.05F).max(0.8F).step(0.05F).currentValue(0.2F);

    private final SliderSetting horizontalDrift = new SliderSetting(this, "modules.settings.world_particles.horizontal_drift", "Горизонтальный дрейф")
            .min(0.0F).max(1.0F).step(0.05F).currentValue(0.3F);

    private final SliderSetting particleOpacity = new SliderSetting(this, "modules.settings.world_particles.opacity", "Прозрачность частиц")
            .min(0.1F).max(1.0F).step(0.05F).currentValue(0.8F);

    private final SliderSetting particleCount = new SliderSetting(this, "modules.settings.world_particles.count", "Количество частиц")
            .min(50.0F).max(500.0F).step(10.0F).currentValue(200.0F);
    private final SliderSetting particleSize = new SliderSetting(this, "modules.settings.world_particles.size", "Размер частиц")
            .min(0.1F).max(2.0F).step(0.05F).currentValue(0.8F);
    private final SliderSetting particleLife = new SliderSetting(this, "modules.settings.world_particles.life", "Время жизни (мс)")
            .min(500.0F).max(10000.0F).step(100.0F).currentValue(2000.0F);
    private final SliderSetting spawnRadius = new SliderSetting(this, "modules.settings.world_particles.spawn_radius", "Радиус спавна")
            .min(10.0F).max(64.0F).step(1.0F).currentValue(32.0F);
    private final SliderSetting yOffset = new SliderSetting(this, "modules.settings.world_particles.y_offset", "Высота спавна")
            .min(0.0F).max(20.0F).step(0.5F).currentValue(5.0F);

    private final SliderSetting bounceDamping = new SliderSetting(this, "modules.settings.world_particles.bounce_damping", "Затухание отскока")
            .min(0.3F).max(0.9F).step(0.05F).currentValue(0.6F);
    private final SliderSetting bounceThreshold = new SliderSetting(this, "modules.settings.world_particles.bounce_threshold", "Порог отскока")
            .min(0.05F).max(0.3F).step(0.01F).currentValue(0.1F);

    private final BooleanSetting useGravity = new BooleanSetting(this, "modules.settings.world_particles.use_gravity", "Частицы падают вниз").enabled(true);
    private final BooleanSetting fadeOut = new BooleanSetting(this, "modules.settings.world_particles.fade_out", "Частицы исчезают плавно").enabled(true);
    private final BooleanSetting enableBounce = new BooleanSetting(this, "modules.settings.world_particles.enable_bounce", "Частицы отскакивают от земли").enabled(true);
    private final BooleanSetting collisionDetection = new BooleanSetting(this, "modules.settings.world_particles.collision_detection", "Частицы не застревают в блоках").enabled(true);

    private final List<WorldParticle> particles = new ArrayList<>();
    private final Map<Identifier, List<WorldParticle>> particlesByTexture = new HashMap<>();
    private final Random rand = new Random();
    private long lastRenderTime = System.currentTimeMillis();
    private long lastUpdateTime = System.currentTimeMillis();
    private boolean tickFloatMode;
    private boolean tickSnowMode;
    private boolean tickRainMode;
    private boolean tickGravity;
    private boolean tickBounce;
    private boolean tickCollision;
    private float tickFloatSpeed;
    private float tickSnowSpeed;
    private float tickRainSpeed;
    private float tickDrift;
    private float tickBounceDamping;
    private float tickBounceThreshold;
    private float tickSpawnRadius;
    private float tickYOffset;

    private static final Map<String, Identifier> TEXTURES = new HashMap<>();
    private static final List<Identifier> RANDOM_TEXTURES = new ArrayList<>();

    static {
        TEXTURES.put("crown", Identifier.of("fevervisual", "textures/particles/crown.png"));
        TEXTURES.put("dollar", Identifier.of("fevervisual", "textures/particles/dollar.png"));
        TEXTURES.put("firefly", Identifier.of("fevervisual", "textures/particles/firefly.png"));
        TEXTURES.put("heart", Identifier.of("fevervisual", "textures/particles/heart.png"));
        TEXTURES.put("thor", Identifier.of("fevervisual", "textures/particles/thor.png"));
        TEXTURES.put("line", Identifier.of("fevervisual", "textures/particles/line.png"));
        TEXTURES.put("point", Identifier.of("fevervisual", "textures/particles/point.png"));
        TEXTURES.put("rhombus", Identifier.of("fevervisual", "textures/particles/rhombus.png"));
        TEXTURES.put("snowflake", Identifier.of("fevervisual", "textures/particles/snowflake.png"));
        TEXTURES.put("spark", Identifier.of("fevervisual", "textures/particles/spark.png"));
        TEXTURES.put("star", Identifier.of("fevervisual", "textures/particles/star.png"));
        TEXTURES.put("moon", Identifier.of("fevervisual", "textures/particles/moon.png"));
        TEXTURES.put("feather", Identifier.of("fevervisual", "textures/particles/feather.png"));
        TEXTURES.put("genshin", Identifier.of("fevervisual", "textures/particles/genshin.png"));
        RANDOM_TEXTURES.addAll(TEXTURES.values());
    }

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        float deltaTime = Math.min(1.0F, (now - lastUpdateTime) / 1000.0F);
        lastUpdateTime = now;

        cacheTickSettings();

        updateParticles(deltaTime);
        spawnParticles();
    };

    private void cacheTickSettings() {
        this.tickFloatMode = this.floatMode.isSelected();
        this.tickSnowMode = this.snowMode.isSelected();
        this.tickRainMode = this.rainMode.isSelected();
        this.tickGravity = this.useGravity.isEnabled();
        this.tickBounce = this.enableBounce.isEnabled();
        this.tickCollision = this.collisionDetection.isEnabled();
        this.tickFloatSpeed = this.floatSpeed.getCurrentValue();
        this.tickSnowSpeed = this.snowSpeed.getCurrentValue();
        this.tickRainSpeed = this.rainSpeed.getCurrentValue();
        this.tickDrift = this.horizontalDrift.getCurrentValue();
        this.tickBounceDamping = this.bounceDamping.getCurrentValue();
        this.tickBounceThreshold = this.bounceThreshold.getCurrentValue();
        this.tickSpawnRadius = this.spawnRadius.getCurrentValue();
        this.tickYOffset = this.yOffset.getCurrentValue();
    }

    private final EventListener<Render3DEvent> onRender3D = new EventListener<>() {
        @Override
        public void onEvent(Render3DEvent event) {
            if (particles.isEmpty()) return;

            renderParticles(event.getMatrices(), event.getTickDelta());
        }

        @Override
        public int getPriority() {
            return 100;
        }
    };

    @Override
    public void onEnable() {
        super.onEnable();
        particles.clear();
        lastRenderTime = System.currentTimeMillis();
        lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
    }

    private void updateParticles(float deltaTime) {
        long currentTime = System.currentTimeMillis();

        for (WorldParticle particle : particles) {
            particle.update(deltaTime);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            WorldParticle particle = particles.get(i);
            if (currentTime - particle.spawnTime > particleLife.getCurrentValue()) {
                particles.remove(i);
            }
        }
    }

    private void spawnParticles() {
        int targetCount = (int) particleCount.getCurrentValue();
        int toSpawn = targetCount - particles.size();

        if (toSpawn <= 0) return;

        toSpawn = Math.min(toSpawn, 8);

        for (int i = 0; i < toSpawn; i++) {
            Identifier texture = getParticleTexture();
            ColorRGBA color = getParticleColor(i);

            float x, y, z;
            float radius = tickSpawnRadius;
            float angle = (float) (rand.nextDouble() * Math.PI * 2);
            float distance = rand.nextFloat() * radius;

            x = (float) (mc.player.getX() + Math.cos(angle) * distance);
            z = (float) (mc.player.getZ() + Math.sin(angle) * distance);

            if (tickRainMode) {
                y = (float) (mc.player.getY() + tickYOffset + 10.0F + rand.nextFloat() * 15.0F);
            } else {
                y = (float) (mc.player.getY() + tickYOffset + rand.nextFloat() * 8.0F - 4.0F);
            }

            float size = particleSize.getCurrentValue() * (0.5F + rand.nextFloat() * 0.8F);

            WorldParticle particle = new WorldParticle(
                    texture, x, y, z, color, size,
                    animationMode.getValue().getName()
            );
            particles.add(particle);
        }
    }

    private void renderParticles(MatrixStack matrix, float tickDelta) {
        if (particles.isEmpty()) return;

        setupRenderState();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        Quaternionf cameraRotation = camera.getRotation();
        long now = System.currentTimeMillis();
        float opacity = particleOpacity.getCurrentValue();
        float life = particleLife.getCurrentValue();
        boolean shouldFadeOut = fadeOut.isEnabled();

        Tessellator tessellator = Tessellator.getInstance();

        for (List<WorldParticle> bucket : particlesByTexture.values()) {
            bucket.clear();
        }

        for (WorldParticle particle : particles) {
            if (Utils.isInViewHemisphere(camera, particle.position.x, particle.position.y, particle.position.z, 2.0)) {
                particlesByTexture.computeIfAbsent(particle.texture, k -> new ArrayList<>()).add(particle);
            }
        }

        for (Map.Entry<Identifier, List<WorldParticle>> entry : particlesByTexture.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            RenderSystem.setShaderTexture(0, entry.getKey());
            BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (WorldParticle particle : entry.getValue()) {
                renderParticle(buffer, matrix, particle, cameraPos, cameraRotation, tickDelta, now, opacity, life, shouldFadeOut);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        resetRenderState();
    }

    private void renderParticle(BufferBuilder buffer, MatrixStack matrix, WorldParticle particle, Vec3d cameraPos, Quaternionf cameraRotation, float tickDelta, long now, float opacity, float life, boolean shouldFadeOut) {
        matrix.push();

        double px = MathHelper.lerp(tickDelta, particle.prevPosition.x, particle.position.x) - cameraPos.x;
        double py = MathHelper.lerp(tickDelta, particle.prevPosition.y, particle.position.y) - cameraPos.y;
        double pz = MathHelper.lerp(tickDelta, particle.prevPosition.z, particle.position.z) - cameraPos.z;
        matrix.translate(px, py, pz);
        matrix.multiply(cameraRotation);

        Matrix4f m = matrix.peek().getPositionMatrix();
        float halfSize = particle.size / 2.0F;

        float alpha = opacity;
        if (shouldFadeOut) {
            long age = now - particle.spawnTime;
            if (age < 100) {
                alpha *= age / 100.0F;
            } else if (age > life - 200) {
                alpha *= (life - age) / 200.0F;
            }
            alpha = MathHelper.clamp(alpha, 0.0F, 1.0F);
        }

        int color = (MathHelper.clamp((int) (alpha * 255), 0, 255) << 24) | (particle.color.getRGB() & 0x00FFFFFF);

        if (tickSnowMode && particle.rotatesSnowflake) {
            float rotation = (now + particle.spawnTime) % 360;
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        }
        if (particle.rotate180) {
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
        }

        drawQuad(buffer, m, halfSize, color);

        matrix.pop();
    }

    private void drawQuad(BufferBuilder buffer, Matrix4f matrix, float size, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = ((color >> 24) & 0xFF) / 255.0F;

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
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
    }

    private Identifier getParticleTexture() {
        if (randomType.isSelected()) {
            return RANDOM_TEXTURES.get(rand.nextInt(RANDOM_TEXTURES.size()));
        } else if (crown.isSelected()) return TEXTURES.get("crown");
        else if (dollar.isSelected()) return TEXTURES.get("dollar");
        else if (firefly.isSelected()) return TEXTURES.get("firefly");
        else if (heart.isSelected()) return TEXTURES.get("heart");
        else if (lightning.isSelected()) return TEXTURES.get("thor");
        else if (line.isSelected()) return TEXTURES.get("line");
        else if (point.isSelected()) return TEXTURES.get("point");
        else if (rhombus.isSelected()) return TEXTURES.get("rhombus");
        else if (snowflake.isSelected()) return TEXTURES.get("snowflake");
        else if (spark.isSelected()) return TEXTURES.get("spark");
        else if (star.isSelected()) return TEXTURES.get("star");
        else if (moon.isSelected()) return TEXTURES.get("moon");
        else if (feather.isSelected()) return TEXTURES.get("feather");
        else if (genshin.isSelected()) return TEXTURES.get("genshin");
        else return TEXTURES.get("star");
    }

    private ColorRGBA getParticleColor(int index) {
        if (colorCustom.isSelected()) {
            return customColor.getColor().mix(customColorSecond.getColor(), (index % 20) / 19.0F);
        }

        return Colors.getAccentColor(index * 12.0F);
    }

    private class WorldParticle {
        private final Identifier texture;
        private Vec3d position;
        private Vec3d velocity;
        private final ColorRGBA color;
        private final float size;
        private final long spawnTime;
        private Vec3d prevPosition;
        private final String animationType;
        private final boolean rotatesSnowflake;
        private final boolean rotate180;
        private double groundY = -Double.MAX_VALUE;

        public WorldParticle(Identifier texture, float x, float y, float z, ColorRGBA color, float size, String animationType) {
            this.texture = texture;
            this.position = new Vec3d(x, y, z);
            this.prevPosition = this.position;
            this.color = color;
            this.size = size;
            this.spawnTime = System.currentTimeMillis();
            this.animationType = animationType;
            this.rotatesSnowflake = texture.equals(TEXTURES.get("snowflake"));
            this.rotate180 = texture.equals(TEXTURES.get("heart"))
                    || texture.equals(TEXTURES.get("crown"))
                    || texture.equals(TEXTURES.get("moon"))
                    || texture.equals(TEXTURES.get("thor"))
                    || texture.equals(TEXTURES.get("dollar"));

            if (tickFloatMode) {
                float speed = tickFloatSpeed;
                this.velocity = new Vec3d(
                        (rand.nextDouble() - 0.5) * 0.4 * speed,
                        (rand.nextDouble() - 0.5) * 0.3 * speed,
                        (rand.nextDouble() - 0.5) * 0.4 * speed
                );
            } else if (tickSnowMode) {
                float speed = tickSnowSpeed;
                float drift = tickDrift;
                this.velocity = new Vec3d(
                        (rand.nextDouble() - 0.5) * 0.2 * drift,
                        -0.12 * speed - rand.nextDouble() * 0.03,
                        (rand.nextDouble() - 0.5) * 0.2 * drift
                );
            } else if (tickRainMode) {
                float speed = tickRainSpeed;
                float drift = tickDrift * 0.3f;
                this.velocity = new Vec3d(
                        (rand.nextDouble() - 0.5) * 0.08 * drift,
                        -0.15 * speed - rand.nextDouble() * 0.03,
                        (rand.nextDouble() - 0.5) * 0.08 * drift
                );
            } else {
                float speed = tickFloatSpeed;
                this.velocity = new Vec3d(
                        (rand.nextDouble() - 0.5) * 0.4 * speed,
                        (rand.nextDouble() - 0.5) * 0.3 * speed,
                        (rand.nextDouble() - 0.5) * 0.4 * speed
                );
            }
        }

        private boolean isInsideBlock(Vec3d pos) {
            if (mc.world == null) return false;
            BlockPos blockPos = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
            return !mc.world.isAir(blockPos) && !mc.world.getBlockState(blockPos).isLiquid();
        }

        private Vec3d pushOutOfBlock(Vec3d pos) {
            if (mc.world == null) return pos;

            BlockPos blockPos = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
            if (!mc.world.isAir(blockPos) && !mc.world.getBlockState(blockPos).isLiquid()) {
                double newY = blockPos.getY() + 0.5;
                return new Vec3d(pos.x, newY, pos.z);
            }
            return pos;
        }

        public void update(float deltaTime) {
            prevPosition = position;

            if (tickFloatMode && tickCollision) {
                if (isInsideBlock(position)) {
                    position = pushOutOfBlock(position);
                    velocity = new Vec3d(
                            velocity.x * -0.5,
                            Math.abs(velocity.y) * 0.5,
                            velocity.z * -0.5
                    );
                }
            }

            if (groundY == -Double.MAX_VALUE && mc.world != null && !tickFloatMode) {
                BlockPos blockPos = BlockPos.ofFloored(position);
                groundY = blockPos.getY();
                int searchBottom = Math.max(mc.world.getBottomY(), (int)position.y - 48);
                for (int y = (int) position.y; y >= searchBottom; y--) {
                    BlockPos checkPos = new BlockPos((int) position.x, y, (int) position.z);
                    if (!mc.world.isAir(checkPos)) {
                        groundY = y + 1.0;
                        break;
                    }
                }
            }

            if (tickGravity) {
                if (tickFloatMode) {
                    velocity = velocity.add(0, -0.05 * deltaTime, 0);
                } else if (tickSnowMode) {
                    velocity = velocity.add(0, -0.4 * deltaTime, 0);
                } else if (tickRainMode) {
                    velocity = velocity.add(0, -0.35 * deltaTime, 0);
                }
            }

            Vec3d newPosition = position.add(velocity.multiply(deltaTime * 60.0));

            if (tickFloatMode && tickCollision) {
                if (isInsideBlock(newPosition)) {
                    newPosition = pushOutOfBlock(newPosition);
                    velocity = new Vec3d(
                            velocity.x * -0.3,
                            Math.abs(velocity.y) * 0.4,
                            velocity.z * -0.3
                    );
                }
            }

            position = newPosition;

            if (tickBounce && groundY != -Double.MAX_VALUE && !tickFloatMode) {
                if (position.y <= groundY + 0.05) {
                    position = new Vec3d(position.x, groundY + 0.05, position.z);

                    if (Math.abs(velocity.y) > tickBounceThreshold) {
                        velocity = new Vec3d(
                                velocity.x * tickBounceDamping,
                                -velocity.y * tickBounceDamping,
                                velocity.z * tickBounceDamping
                        );

                        if (Math.abs(velocity.y) < tickBounceThreshold) {
                            velocity = new Vec3d(velocity.x, 0, velocity.z);
                        }
                    } else {
                        velocity = new Vec3d(velocity.x * 0.95, 0, velocity.z * 0.95);
                    }
                }
            }

            if (tickFloatMode) {
                if (rand.nextDouble() < 0.02) {
                    float speed = tickFloatSpeed;
                    velocity = velocity.add(
                            (rand.nextDouble() - 0.5) * 0.05 * speed,
                            (rand.nextDouble() - 0.5) * 0.03 * speed,
                            (rand.nextDouble() - 0.5) * 0.05 * speed
                    );
                    velocity = velocity.multiply(0.98);
                }

                double maxSpeed = 0.5 * tickFloatSpeed;
                if (velocity.length() > maxSpeed) {
                    velocity = velocity.normalize().multiply(maxSpeed);
                }

                if (mc.player != null) {
                    double minY = mc.player.getY() - 5;
                    double maxY = mc.player.getY() + 8;
                    if (position.y < minY) {
                        position = new Vec3d(position.x, minY, position.z);
                        velocity = new Vec3d(velocity.x, Math.abs(velocity.y) * 0.5, velocity.z);
                    } else if (position.y > maxY) {
                        position = new Vec3d(position.x, maxY, position.z);
                        velocity = new Vec3d(velocity.x, -Math.abs(velocity.y) * 0.5, velocity.z);
                    }
                }

            } else if (tickSnowMode) {
                if (rand.nextDouble() < 0.03) {
                    float drift = tickDrift;
                    velocity = velocity.add(
                            (rand.nextDouble() - 0.5) * 0.03 * drift,
                            0,
                            (rand.nextDouble() - 0.5) * 0.03 * drift
                    );
                }

                velocity = velocity.multiply(0.995);

                if (mc.player != null && position.y < mc.player.getY() - 5) {
                    position = new Vec3d(
                            position.x,
                            mc.player.getY() + tickYOffset + 15,
                            position.z
                    );
                    float speed = tickSnowSpeed;
                    float drift = tickDrift;
                    velocity = new Vec3d(
                            velocity.x * 0.8,
                            -0.12 * speed,
                            velocity.z * 0.8
                    );
                }

            } else if (tickRainMode) {
                velocity = velocity.multiply(0.999);

                if (position.y <= groundY + 0.1 && mc.player != null) {
                    position = new Vec3d(
                            mc.player.getX() + (rand.nextDouble() - 0.5) * tickSpawnRadius,
                            mc.player.getY() + tickYOffset + 15,
                            mc.player.getZ() + (rand.nextDouble() - 0.5) * tickSpawnRadius
                    );
                    float speed = tickRainSpeed;
                    float drift = tickDrift * 0.3f;
                    velocity = new Vec3d(
                            (rand.nextDouble() - 0.5) * 0.08 * drift,
                            -0.15 * speed,
                            (rand.nextDouble() - 0.5) * 0.08 * drift
                    );
                }
            }

            if (mc.player != null && !tickFloatMode) {
                double distToPlayer = position.distanceTo(mc.player.getEntityPos());
                if (distToPlayer > tickSpawnRadius * 2) {
                    position = new Vec3d(
                            mc.player.getX() + (rand.nextDouble() - 0.5) * tickSpawnRadius,
                            mc.player.getY() + tickYOffset + (rand.nextDouble() - 0.5) * 8,
                            mc.player.getZ() + (rand.nextDouble() - 0.5) * tickSpawnRadius
                    );
                }
            }
        }

        public Vec3d getInterpolatedPosition(float tickDelta) {
            double x = prevPosition.x + (position.x - prevPosition.x) * tickDelta;
            double y = prevPosition.y + (position.y - prevPosition.y) * tickDelta;
            double z = prevPosition.z + (position.z - prevPosition.z) * tickDelta;
            return new Vec3d(x, y, z);
        }
    }
}
