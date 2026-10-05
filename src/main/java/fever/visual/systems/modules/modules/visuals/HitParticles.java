package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;

import fever.visual.utility.render.compat.GlStateManager.DstFactor;
import fever.visual.utility.render.compat.GlStateManager.SrcFactor;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.AttackEvent;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@ModuleInfo(
        name = "Hit Particles",
        category = ModuleCategory.VISUALS,
        desc = "modules.descriptions.hit_particles"
)
public class HitParticles extends BaseModule implements IMinecraft {
    private static final int MAX_ACTIVE_PARTICLES = 500;
    private static final Identifier CROWN = Identifier.of("fevervisual", "textures/particles/crown.png");
    private static final Identifier DOLLAR = Identifier.of("fevervisual", "textures/particles/dollar.png");
    private static final Identifier FIREFLY = Identifier.of("fevervisual", "textures/particles/firefly.png");
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
    private static final Identifier GENSHIN = Identifier.of("fevervisual", "textures/particles/genshin.png");
    private static final Identifier[] RANDOM_TEXTURES = {
            CROWN, DOLLAR, FIREFLY, HEART, THOR, LINE, POINT, RHOMBUS, SNOWFLAKE, SPARK, STAR, MOON, FEATHER, GENSHIN
    };

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
    private final ModeSetting.Value spark = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.spark");
    private final ModeSetting.Value moon = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.moon");
    private final ModeSetting.Value feather = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.feather");
    private final ModeSetting.Value genshin = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.genshin");
    private final ModeSetting.Value random = new ModeSetting.Value(this.particleType, "modules.settings.move_particles.particle_type.random");

    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.move_particles.color_mode");
    private final ModeSetting.Value themeColor = new ModeSetting.Value(this.colorMode, "modules.settings.move_particles.color_mode.theme").select();
    private final ModeSetting.Value rainbowColor = new ModeSetting.Value(this.colorMode, "modules.settings.move_particles.color_mode.rainbow", "", () -> true);
    private final ModeSetting.Value customColorMode = new ModeSetting.Value(this.colorMode, "modules.settings.move_particles.color_mode.custom");

    private final ColorSetting customColor = new ColorSetting(this, "modules.settings.move_particles.custom_color", () -> !customColorMode.isSelected())
            .color(new ColorRGBA(255, 100, 100, 255))
            .alpha(true);
    private final ColorSetting customColorSecond = new ColorSetting(this, "modules.settings.move_particles.custom_color_second", () -> !customColorMode.isSelected())
            .color(new ColorRGBA(100, 160, 255, 255))
            .alpha(true);

    private final SliderSetting speed = new SliderSetting(this, "modules.settings.hit_particles.speed").min(0.1F).max(3.0F).step(0.1F).currentValue(1.5F);
    private final SliderSetting size = new SliderSetting(this, "modules.settings.hit_particles.size").min(0.0F).max(3.0F).step(0.1F).currentValue(0.8F);
    private final SliderSetting count = new SliderSetting(this, "modules.settings.hit_particles.count").min(5.0F).max(50.0F).step(1.0F).currentValue(30.0F);
    private final List<Particle> particles = new ArrayList<>();
    private final Map<Identifier, List<Particle>> particlesByTexture = new HashMap<>();
    private final Random rand = new Random();
    private long lastUpdateTime = System.nanoTime();

    private final EventListener<AttackEvent> onAttack = event -> {
        Entity target = event.getEntity();
        if (target != null && !this.hasVisionBlockingEffect()) {
            if (target instanceof LivingEntity living && this.isInvisible(living)) {
                return;
            }
            for (int i = 0; i < (int) this.count.getCurrentValue(); i++) {
                if (this.particles.size() >= MAX_ACTIVE_PARTICLES) {
                    break;
                }
                Identifier texture = this.getParticleTexture();
                Vec3d position = new Vec3d(
                        target.getX() + this.randomRange(-0.4, 0.4),
                        target.getY() + this.randomRange(0.0, target.getHeight()),
                        target.getZ() + this.randomRange(-0.4, 0.4)
                );
                Vec3d velocity = new Vec3d(this.randomRange(-1.35, 1.35), this.randomRange(-1.25, 1.25), this.randomRange(-1.35, 1.35));
                ColorRGBA color = this.getParticleColor(i);
                this.particles
                        .add(new Particle(texture, position, velocity, color, 0.15F + this.size.getCurrentValue() * 0.3F, this.speed.getCurrentValue()));
            }
        }
    };

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        for (int i = this.particles.size() - 1; i >= 0; i--) {
            if (this.particles.get(i).time > 1000L) {
                this.particles.remove(i);
            }
        }
    };

    private final EventListener<Render3DEvent> on3DRender = event -> {
        if (!this.particles.isEmpty()) {
            long now = System.nanoTime();
            if (this.lastUpdateTime == 0) {
                this.lastUpdateTime = now;
            }
            double deltaTime = Math.min((now - this.lastUpdateTime) / 1.0E9, 0.05);
            this.lastUpdateTime = now;
            MatrixStack matrix = event.getMatrices();
            this.setupRenderState();
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d cameraPos = camera.getCameraPos();

            for (List<Particle> bucket : this.particlesByTexture.values()) {
                bucket.clear();
            }

            for (Particle particle : this.particles) {
                particle.update(deltaTime);
                if (Utils.isInViewHemisphere(camera, particle.position.x, particle.position.y, particle.position.z, 2.0)) {
                    this.particlesByTexture.computeIfAbsent(particle.texture, key -> new ArrayList<>()).add(particle);
                }
            }

            Tessellator tessellator = Tessellator.getInstance();
            for (Map.Entry<Identifier, List<Particle>> entry : this.particlesByTexture.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }

                RenderSystem.setShaderTexture(0, entry.getKey());
                BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Particle particle : entry.getValue()) {
                    this.renderParticle(buffer, matrix, particle, cameraPos);
                }
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }

            this.resetRenderState();
        }
    };

    @Override
    public void onEnable() {
        super.onEnable();
        this.particles.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.particles.clear();
    }

    private void setupRenderState() {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE, SrcFactor.ONE, DstFactor.ONE);
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

    private void renderParticle(BufferBuilder buffer, MatrixStack matrix, Particle particle, Vec3d cameraPos) {
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
        int color = (MathHelper.clamp((int)(particle.alpha * 255.0F), 0, 255) << 24) | (particle.color.getRGB() & 0x00FFFFFF);
        this.drawQuad(buffer, m, halfSize, color);
        matrix.pop();
    }

    private void drawQuad(BufferBuilder buffer, Matrix4f matrix, float size, int color) {
        buffer.vertex(matrix, -size, -size, 0.0F).texture(0.0F, 0.0F).color(color);
        buffer.vertex(matrix, size, -size, 0.0F).texture(1.0F, 0.0F).color(color);
        buffer.vertex(matrix, size, size, 0.0F).texture(1.0F, 1.0F).color(color);
        buffer.vertex(matrix, -size, size, 0.0F).texture(0.0F, 1.0F).color(color);
    }

    private Identifier getParticleTexture() {
        if (this.random.isSelected()) {
            return RANDOM_TEXTURES[this.rand.nextInt(RANDOM_TEXTURES.length)];
        } else if (this.crown.isSelected()) {
            return CROWN;
        } else if (this.dollar.isSelected()) {
            return DOLLAR;
        } else if (this.heart.isSelected()) {
            return HEART;
        } else if (this.firefly.isSelected()) {
            return FIREFLY;
        } else if (this.lightning.isSelected()) {
            return THOR;
        } else if (this.line.isSelected()) {
            return LINE;
        } else if (this.point.isSelected()) {
            return POINT;
        } else if (this.rhombus.isSelected()) {
            return RHOMBUS;
        } else if (this.snowflake.isSelected()) {
            return SNOWFLAKE;
        } else if (this.spark.isSelected()) {
            return SPARK;
        } else if (this.moon.isSelected()) {
            return MOON;
        } else if (this.feather.isSelected()) {
            return FEATHER;
        } else if (this.genshin.isSelected()) {
            return GENSHIN;
        } else {
            return STAR;
        }
    }

    private ColorRGBA getParticleColor(int index) {
        if (this.customColorMode.isSelected()) {
            return this.customColor.getColor().mix(this.customColorSecond.getColor(), (index % 20) / 19.0F);
        }

        return Colors.getAccentColor(index * 12.0F);
    }

    private double randomRange(double min, double max) {
        return min + (max - min) * this.rand.nextDouble();
    }

    private boolean isInvisible(LivingEntity entity) {
        if (entity.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
        if (entity.isInvisible()) return true;
        if (mc.player != null && entity.isInvisibleTo(mc.player)) return true;
        return false;
    }

    private boolean hasVisionBlockingEffect() {
        return mc.player != null
                && (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(StatusEffects.DARKNESS));
    }

    private class Particle {
        private final Identifier texture;
        private Vec3d position;
        private Vec3d velocity;
        private final ColorRGBA color;
        private final float size;
        private final float speedMultiplier;
        private final boolean rotate180;
        private long time = 0L;
        private float alpha = 1.0F;

        public Particle(Identifier texture, Vec3d position, Vec3d velocity, ColorRGBA color, float size, float speedMultiplier) {
            this.texture = texture;
            this.position = position;
            this.velocity = velocity.multiply(0.05);
            this.color = color;
            this.size = size;
            this.speedMultiplier = speedMultiplier;
            this.rotate180 = texture.equals(HEART);
        }

        public void update(double deltaTime) {
            this.velocity = this.velocity.multiply(Math.pow(0.999, deltaTime * 60.0));
            this.position = this.position.add(this.velocity.multiply(deltaTime * 60.0 * this.speedMultiplier));
            this.time += (long)(deltaTime * 1000.0);
            if (this.time > 600L) {
                this.alpha = 1.0F - (float)(this.time - 600L) / 400.0F;
                this.alpha = Math.max(0.0F, Math.min(1.0F, this.alpha));
            }
        }
    }
}
