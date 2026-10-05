//Да давай пасти бездарность :)
//Made by FeverVisuals (NightlyFever)
package fever.visual.systems.modules.modules.visuals;

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
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.Utils;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleInfo(name = "Nimb", category = ModuleCategory.VISUALS, desc = "modules.descriptions.nimb")
public class Nimb extends BaseModule {


    private final SelectSetting targets = new SelectSetting(this, "modules.settings.model_changer.targets");
    private final SelectSetting.Value targetSelf = new SelectSetting.Value(targets, "modules.settings.chinahat.render_self").select();
    private final SelectSetting.Value targetFriends = new SelectSetting.Value(targets, "modules.settings.chinahat.render_others");

    private final SliderSetting speed3d = new SliderSetting(this, "modules.settings.nimb.speed_3d").min(1f).max(30f).step(1f).currentValue(3f);
    private final SliderSetting size3d = new SliderSetting(this, "modules.settings.nimb.size_3d").min(1f).max(7f).step(1f).currentValue(4f);
    private final SliderSetting distance3d = new SliderSetting(this, "modules.settings.nimb.distance_3d").min(5f).max(35f).step(1f).currentValue(15f);
    private final SliderSetting length3d = new SliderSetting(this, "modules.settings.nimb.length_3d").min(1f).max(50f).step(1f).currentValue(35f);
    private final SliderSetting length2_3d = new SliderSetting(this, "modules.settings.nimb.length2_3d").min(150f).max(255f).step(1f).currentValue(235f);
    private final SliderSetting alpha3d = new SliderSetting(this, "modules.settings.nimb.alpha_3d").min(1f).max(255f).step(1f).currentValue(255f);

    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.nimb.color_mode");
    private final ModeSetting.Value colorTheme = new ModeSetting.Value(colorMode, "modules.settings.nimb.color_mode.theme").select();
    private final ModeSetting.Value colorRainbow = new ModeSetting.Value(colorMode, "modules.settings.nimb.color_mode.rainbow", "", () -> true);
    private final ModeSetting.Value colorCustom = new ModeSetting.Value(colorMode, "modules.settings.nimb.color_mode.custom");
    private final ColorSetting customColorFirst = new ColorSetting(this, "modules.settings.nimb.custom_color", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(255, 100, 100, 255))
            .alpha(true);
    private final ColorSetting customColorSecond = new ColorSetting(this, "modules.settings.nimb.custom_color_second", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(100, 160, 255, 255))
            .alpha(true);

    private final SliderSetting opacity = new SliderSetting(this, "modules.settings.nimb.opacity").min(0.1f).max(1.0f).step(0.05f).currentValue(0.7f);
    private final BooleanSetting firstPerson = new BooleanSetting(this, "modules.settings.nimb.first_person").enabled(false);
    private final SliderSetting height = new SliderSetting(this, "modules.settings.nimb.height").min(-0.5f).max(2.0f).step(0.05f).currentValue(0.4f);
    private final SliderSetting radius = new SliderSetting(this, "modules.settings.nimb.radius").min(0.1f).max(2.0f).step(0.05f).currentValue(0.5f);

    private final Identifier fireflyTexture = FeverVisual.id("textures/particles/firefly.png");

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (mc.player == null || mc.world == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldRenderForPlayer(player)) continue;
            if (!shouldRenderForInvisible(player)) continue;
            if (player == mc.player && mc.options.getPerspective().isFirstPerson() && !firstPerson.isEnabled()) continue;
            if (player != mc.player) {
                Camera camera = mc.gameRenderer.getCamera();
                if (player.squaredDistanceTo(camera.getCameraPos()) > 4096.0
                        || !Utils.isInViewHemisphere(camera, player.getX(), player.getY() + player.getHeight(), player.getZ(), 3.0)) {
                    continue;
                }
            }

            Vec3d playerPos = interpolatePlayerPosition(player, event.getTickDelta());
            float playerHeight = player.getHeight();
            double centerY = playerPos.y + playerHeight + height.getCurrentValue();
            Vec3d center = new Vec3d(playerPos.x, centerY, playerPos.z);

            render3DEffect(player, event.getMatrices(), event.getTickDelta(), center);
        }
    };

    private boolean shouldRenderForPlayer(PlayerEntity player) {
        if (player == null || player.isRemoved()) return false;
        if (player == mc.player) {
            return targetSelf.isSelected();
        }
        boolean isFriend = FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString());
        return isFriend && targetFriends.isSelected();
    }

    private boolean shouldRenderForInvisible(PlayerEntity player) {
        if (player == mc.player) return true;
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) return false;
        return !player.isInvisible() && !player.isInvisibleTo(mc.player);
    }

    private int getColor(PlayerEntity player, int hue) {
        if (colorMode.is(colorCustom)) {
            return customColorFirst.getColor().mix(customColorSecond.getColor(), hue / 360.0F).getRGB();
        }

        return Colors.getAccentColor(hue).getRGB();
    }

    private void render3DEffect(PlayerEntity player, MatrixStack matrices, float tickDelta, Vec3d center) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        Vec3d relativeCenter = center.subtract(cameraPos);

        float speed = 31 - speed3d.getCurrentValue();
        float size = size3d.getCurrentValue() / 30f;
        double distance = distance3d.getCurrentValue();
        int length = (int) length3d.getCurrentValue();
        int maxAlpha = (int) alpha3d.getCurrentValue();
        int alphaFactor = 255 - (int) length2_3d.getCurrentValue();
        float actualRadius = radius.getCurrentValue();
        float actualOpacity = opacity.getCurrentValue();

        matrices.push();
        RenderUtility.setupRender3D(true);
        RenderSystem.setShaderTexture(0, fireflyTexture);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        long currentTime = System.currentTimeMillis();
        double angle = 0.15D * currentTime / speed;
        double angleStep = -0.15D * distance / speed;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double stepSin = Math.sin(angleStep);
        double stepCos = Math.cos(angleStep);
        Matrix4f baseMatrix = new Matrix4f(matrices.peek().getPositionMatrix());
        Matrix4f matrix = new Matrix4f();

        for (int i = 0; i < length; i++) {
            double s = cos * actualRadius;
            double c = sin * actualRadius;
            float alpha = MathHelper.clamp(maxAlpha - (i * alphaFactor), 0, maxAlpha) / 255f;
            alpha *= actualOpacity;
            int color = applyOpacity(getColor(player, i * 360 / length), alpha);

            matrix.set(baseMatrix)
                    .translate((float) (relativeCenter.x + s), (float) relativeCenter.y, (float) (relativeCenter.z - c))
                    .rotate(camera.getRotation());
            drawParticle(buffer, matrix, size, color);

            color = applyOpacity(getColor(player, i * 360 / length + 180), alpha);
            matrix.set(baseMatrix)
                    .translate((float) (relativeCenter.x - s), (float) relativeCenter.y, (float) (relativeCenter.z + c))
                    .rotate(camera.getRotation());
            drawParticle(buffer, matrix, size, color);

            double nextCos = cos * stepCos - sin * stepSin;
            sin = sin * stepCos + cos * stepSin;
            cos = nextCos;
        }

        RenderUtility.buildBuffer(buffer);
        RenderSystem.depthMask(true);
        RenderUtility.endRender3D();
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
        matrices.pop();
    }

    private void drawParticle(BufferBuilder buffer, Matrix4f matrix, float size, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = ((color >> 24) & 0xFF) / 255.0F;

        buffer.vertex(matrix, -size, size, 0).texture(0f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, size, size, 0).texture(1f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, 0).texture(1f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, -size, -size, 0).texture(0f, 0f).color(r, g, b, a);
    }

    private int applyOpacity(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private Vec3d interpolatePlayerPosition(PlayerEntity player, float tickDelta) {
        double x = player.lastX + (player.getX() - player.lastX) * tickDelta;
        double y = player.lastY + (player.getY() - player.lastY) * tickDelta;
        double z = player.lastZ + (player.getZ() - player.lastZ) * tickDelta;
        return new Vec3d(x, y, z);
    }
}
