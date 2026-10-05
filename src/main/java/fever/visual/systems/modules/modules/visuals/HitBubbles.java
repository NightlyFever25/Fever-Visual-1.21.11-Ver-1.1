package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.utility.render.compat.GlStateManager;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.AttackEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(name = "Hit Bubbles", category = ModuleCategory.VISUALS, desc = "Пузырьки при ударе")
@Environment(EnvType.CLIENT)
public class HitBubbles extends BaseModule {

    private static final Identifier BUBBLE_TEX = FeverVisual.id("textures/bubble.png");
    private static final long GROW_MS = 400L;
    private static final long COOLDOWN_MS = 100L;
    private static final float MAX_ROTATION = 45.0f;

    private final ModeSetting mode = new ModeSetting(this, "Mode");
    private final ModeSetting.Value textureMode = new ModeSetting.Value(this.mode, "Texture").select();
    private final ModeSetting.Value rippleMode = new ModeSetting.Value(this.mode, "Ripple");

    private final SliderSetting size = new SliderSetting(this, "modules.settings.hit_bubbles.size", "Размер пузырька")
            .min(0.3f).max(2.0f).step(0.05f).currentValue(1.0f);
    private final SliderSetting lifeTime = new SliderSetting(this, "modules.settings.hit_bubbles.life_time", "Время жизни (сек)")
            .min(0.1f).max(0.6f).step(0.05f).currentValue(0.2f);
    private final SliderSetting strength = new SliderSetting(this, "Strength", () -> !this.rippleMode.isSelected())
            .min(0.2f).max(3.0f).step(0.1f).currentValue(1.5f);
    private final BooleanSetting warp = new BooleanSetting(this, "Warp", () -> !this.rippleMode.isSelected());
    private final SliderSetting warpStrength = new SliderSetting(this, "Warp Strength", () -> !this.rippleMode.isSelected() || !this.warp.isEnabled())
            .min(0.1f).max(2.0f).step(0.1f).currentValue(0.5f);
    private final SliderSetting saturation = new SliderSetting(this, "Saturation", () -> !this.rippleMode.isSelected())
            .min(-1.0f).max(1.0f).step(0.05f).currentValue(1.0f);
    private final BooleanSetting tint = new BooleanSetting(this, "Tint", () -> !this.rippleMode.isSelected()).enable();
    private final SliderSetting tintStrength = new SliderSetting(this, "Tint Strength", () -> !this.rippleMode.isSelected())
            .min(0.0f).max(100.0f).step(1.0f).currentValue(100.0f);

    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.hit_bubbles.color_mode");
    private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.hit_bubbles.color_mode.theme").select();
    private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.hit_bubbles.color_mode.custom");
    private final ColorSetting colorFirst = new ColorSetting(this, "modules.settings.hit_bubbles.color", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(255, 80, 120, 255))
            .alpha(true);
    private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.hit_bubbles.color_second", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(100, 160, 255, 255))
            .alpha(true);

    private final List<HitCircle> hitCircles = new ArrayList<>();
    private final ConcurrentHashMap<Integer, Long> lastHitTime = new ConcurrentHashMap<>();

    private final EventListener<AttackEvent> onAttack = event -> {
        if (mc.player == null || mc.world == null) return;

        Entity target = event.getEntity();
        if (target == mc.player) return;
        if (!(target instanceof LivingEntity entity)) return;
        if (!entity.isAlive()) return;
        if (this.hasVisionBlockingEffect() || this.isInvisible(entity)) return;

        int entityId = entity.getId();
        long now = System.currentTimeMillis();
        Long lastHit = lastHitTime.get(entityId);

        if (!this.rippleMode.isSelected() && lastHit != null && (now - lastHit) < COOLDOWN_MS) {
            return;
        }

        lastHitTime.put(entityId, now);

        Vec3d hitPos = this.rippleMode.isSelected() ? entity.getEntityPos().add(0, entity.getHeight() * 0.5F, 0) : computeHitPos(entity);
        if (hitPos == null) {
            hitPos = entity.getEntityPos().add(0, entity.getHeight() / 2f, 0);
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d toCamera = cameraPos.subtract(hitPos).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(toCamera.z, toCamera.x));
        float pitch = (float) Math.toDegrees(Math.atan2(-toCamera.y, Math.sqrt(toCamera.x * toCamera.x + toCamera.z * toCamera.z)));

        hitCircles.add(new HitCircle(hitPos, (long) (lifeTime.getCurrentValue() * 1000), yaw, pitch));

        if (hitCircles.size() % 20 == 0) {
            lastHitTime.entrySet().removeIf(entry -> entry.getValue() == null || now - entry.getValue() > 10000L);
        }
    };

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        for (int i = hitCircles.size() - 1; i >= 0; i--) {
            HitCircle circle = hitCircles.get(i);
            if ((now - circle.spawnTime) > circle.ttl) {
                hitCircles.remove(i);
            }
        }

        if (this.rippleMode.isSelected()) {
            for (HitCircle circle : hitCircles) {
                long age = now - circle.spawnTime;
                float progress = MathHelper.clamp(age / (float)Math.max(1L, circle.ttl), 0.0F, 1.0F);
                float fadeIn = MathHelper.clamp(progress / 0.1F, 0.0F, 1.0F);
                float fadeOut = MathHelper.clamp((1.0F - progress) / 0.3F, 0.0F, 1.0F);
                float envelope = fadeIn * fadeOut;
                if (envelope <= 0.001F) continue;
                float radius = this.size.getCurrentValue() * (0.08F + 0.92F * (1.0F - (1.0F - progress) * (1.0F - progress)));
                this.renderRipple(event.getMatrices(), circle.origin, radius, this.getRippleColor(age * 0.35F, envelope), envelope, age);
            }
            return;
        }

        for (HitCircle c : hitCircles) {
            long age = now - c.spawnTime;

            float growVal;
            if (age >= GROW_MS) {
                growVal = 1.0f;
            } else {
                growVal = (float) age / GROW_MS;
                growVal = (float) (1 - Math.pow(1 - growVal, 2));
            }

            float rotation;
            if (age < GROW_MS) {
                rotation = MAX_ROTATION * growVal;
            } else {
                float fadeProgress = Math.min(1.0f, Math.max(0f, (age - GROW_MS) / (float) Math.max(1, c.ttl - GROW_MS)));
                rotation = MAX_ROTATION * (1 - fadeProgress);
            }

            float timeFade = 1.0f - Math.min(1.0f, Math.max(0f, (age - GROW_MS) / (float) Math.max(1, c.ttl - GROW_MS)));
            if (age < GROW_MS) {
                timeFade = Math.min(1.0f, age / (float) GROW_MS);
            }

            float alphaK = Math.max(0f, Math.min(1f, growVal * timeFade));
            int alpha = (int) (255 * alphaK);
            if (alpha <= 2) continue;

            float r = size.getCurrentValue() * (0.6f + 0.4f * growVal);
            ColorRGBA drawColor = this.getBubbleColor(age * 0.35f).withAlpha(alpha);

            renderBubble(event.getMatrices(), c.origin, r, drawColor, c.rotationYaw, c.rotationPitch, rotation);
        }
    };

    public HitBubbles() {
    }

    private Vec3d computeHitPos(LivingEntity entity) {
        HitResult crosshairTarget = mc.crosshairTarget;
        if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult ehr = (EntityHitResult) crosshairTarget;
            if (ehr.getEntity() == entity) {
                return ehr.getPos();
            }
        }
        return computeHitOnEntityAABB(entity);
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

    private Vec3d computeHitOnEntityAABB(LivingEntity entity) {
        Vec3d start = mc.player.getEyePos();
        Vec3d dir = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(6.0));
        Box bb = entity.getBoundingBox();
        Optional<Vec3d> res = bb.raycast(start, end);
        return res.orElse(null);
    }

    private ColorRGBA getBubbleColor(float index) {
        if (this.colorCustom.isSelected()) {
            return this.colorFirst.getColor().mix(this.colorSecond.getColor(), this.getCustomMix(index));
        }

        return Colors.getAccentColor(index);
    }

    private float getCustomMix(float index) {
        float normalized = (index % 360.0f) / 180.0f;
        return normalized > 1.0f ? 2.0f - normalized : normalized;
    }

    private void renderBubble(MatrixStack ms, Vec3d origin, float size, ColorRGBA color, float rotationYaw, float rotationPitch, float additionalRotation) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, BUBBLE_TEX);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
        );
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        ms.push();
        ms.translate(origin.x - cameraPos.x, origin.y - cameraPos.y, origin.z - cameraPos.z);

        double yawRadians = Math.toRadians(rotationYaw);
        double pitchRadians = Math.toRadians(rotationPitch);
        double rotRadians = Math.toRadians(additionalRotation);

        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);
        double cosPitch = Math.cos(pitchRadians);
        double sinPitch = Math.sin(pitchRadians);
        double cosRot = Math.cos(rotRadians);
        double sinRot = Math.sin(rotRadians);

        Vec3d forward = new Vec3d(
                cosYaw * cosPitch,
                -sinPitch,
                sinYaw * cosPitch
        ).normalize();

        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right = worldUp.crossProduct(forward).normalize();
        Vec3d up = forward.crossProduct(right).normalize();

        Vec3d rotatedRight = right.multiply(cosRot).add(up.multiply(sinRot));
        Vec3d rotatedUp = up.multiply(cosRot).subtract(right.multiply(sinRot));

        Vec3d scaledRight = rotatedRight.multiply(size);
        Vec3d scaledUp = rotatedUp.multiply(size);

        Vec3d topLeft = scaledUp.subtract(scaledRight);
        Vec3d topRight = scaledUp.add(scaledRight);
        Vec3d bottomLeft = scaledRight.multiply(-1).subtract(scaledUp);
        Vec3d bottomRight = scaledRight.subtract(scaledUp);

        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float red = color.getRed() / 255.0f;
        float green = color.getGreen() / 255.0f;
        float blue = color.getBlue() / 255.0f;
        float alphaValue = color.getAlpha() / 255.0f;

        buffer.vertex(matrix, (float) bottomLeft.x, (float) bottomLeft.y, (float) bottomLeft.z)
                .texture(0.0f, 1.0f).color(red, green, blue, alphaValue);
        buffer.vertex(matrix, (float) bottomRight.x, (float) bottomRight.y, (float) bottomRight.z)
                .texture(1.0f, 1.0f).color(red, green, blue, alphaValue);
        buffer.vertex(matrix, (float) topRight.x, (float) topRight.y, (float) topRight.z)
                .texture(1.0f, 0.0f).color(red, green, blue, alphaValue);
        buffer.vertex(matrix, (float) topLeft.x, (float) topLeft.y, (float) topLeft.z)
                .texture(0.0f, 0.0f).color(red, green, blue, alphaValue);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        ms.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private ColorRGBA getRippleColor(float index, float alphaK) {
        ColorRGBA base = this.getBubbleColor(index);
        if (this.tint.isEnabled()) {
            float tintK = MathHelper.clamp(this.tintStrength.getCurrentValue() / 100.0F, 0.0F, 1.0F);
            base = base.mix(Colors.getAccentColor(index + 60.0F), tintK * 0.35F);
        } else {
            float gray = (base.getRed() + base.getGreen() + base.getBlue()) / 3.0F;
            float sat = this.saturation.getCurrentValue();
            base = new ColorRGBA(
                    gray + (base.getRed() - gray) * sat,
                    gray + (base.getGreen() - gray) * sat,
                    gray + (base.getBlue() - gray) * sat,
                    base.getAlpha()
            );
        }
        return base.withAlpha(230.0F * alphaK);
    }

    private void renderRipple(MatrixStack ms, Vec3d origin, float radius, ColorRGBA color, float alphaK, long age) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        Matrix4f matrix = ms.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Vec3d center = origin.subtract(cameraPos);
        int segments = 96;
        float warpAmount = this.warp.isEnabled() ? 0.035F * this.warpStrength.getCurrentValue() : 0.0F;
        float halfWidth = MathHelper.clamp(0.008F * this.strength.getCurrentValue(), 0.006F, 0.035F);
        for (int ring = 0; ring < 3; ring++) {
            float ringRadius = radius * (0.55F + ring * 0.28F + (age % 650L) / 650.0F * 0.16F);
            float ringAlpha = alphaK * (0.8F - ring * 0.18F);
            ColorRGBA ringColor = color.withAlpha(210.0F * ringAlpha);
            for (int i = 0; i < segments; i++) {
                double a1 = Math.PI * 2.0D * i / segments;
                double a2 = Math.PI * 2.0D * (i + 1) / segments;
                float y1 = (float)Math.sin(a1 * 3.0D + age * 0.012D + ring) * warpAmount;
                float y2 = (float)Math.sin(a2 * 3.0D + age * 0.012D + ring) * warpAmount;
                float inner = Math.max(0.0F, ringRadius - halfWidth);
                float outer = ringRadius + halfWidth;
                buffer.vertex(matrix, (float)(center.x + Math.cos(a1) * inner), (float)center.y + y1, (float)(center.z + Math.sin(a1) * inner)).color(ringColor.getRGB());
                buffer.vertex(matrix, (float)(center.x + Math.cos(a1) * outer), (float)center.y + y1, (float)(center.z + Math.sin(a1) * outer)).color(ringColor.getRGB());
                buffer.vertex(matrix, (float)(center.x + Math.cos(a2) * outer), (float)center.y + y2, (float)(center.z + Math.sin(a2) * outer)).color(ringColor.getRGB());
                buffer.vertex(matrix, (float)(center.x + Math.cos(a2) * inner), (float)center.y + y2, (float)(center.z + Math.sin(a2) * inner)).color(ringColor.getRGB());
            }
        }

        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        hitCircles.clear();
        lastHitTime.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        hitCircles.clear();
        lastHitTime.clear();
    }

    private static class HitCircle {
        final Vec3d origin;
        final long spawnTime;
        final long ttl;
        final float rotationYaw;
        final float rotationPitch;

        HitCircle(Vec3d origin, long ttl, float rotationYaw, float rotationPitch) {
            this.origin = origin;
            this.spawnTime = System.currentTimeMillis();
            this.ttl = ttl;
            this.rotationYaw = rotationYaw;
            this.rotationPitch = rotationPitch;
        }
    }
}
