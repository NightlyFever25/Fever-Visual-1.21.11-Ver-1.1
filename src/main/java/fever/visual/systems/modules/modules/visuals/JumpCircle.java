package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.GlStateManager;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.EntityJumpEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.systems.setting.settings.*;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ModuleInfo(name = "Jump Circle", category = ModuleCategory.VISUALS, desc = "modules.descriptions.jump_circle_name")
public class JumpCircle extends BaseModule {
    private static final Identifier CIRCLE_TEXTURE = FeverVisual.id("textures/circle.png");
    private static final Identifier CIRCLE_1_TEXTURE = FeverVisual.id("textures/circle1.png");
    private static final Identifier CIRCLE_2_TEXTURE = FeverVisual.id("textures/circle2.png");

    private final SelectSetting targets = new SelectSetting(this, "modules.settings.model_changer.targets");
    private final SelectSetting.Value targetSelf = new SelectSetting.Value(targets, "modules.settings.chinahat.render_self").select();
    private final SelectSetting.Value targetFriends = new SelectSetting.Value(targets, "modules.settings.chinahat.render_others");

    private final SliderSetting maxSize = new SliderSetting(this, "modules.settings.jump_circle.size").min(1.0f).max(5.0f).step(0.1f).currentValue(2.5f);
    private final SliderSetting speed = new SliderSetting(this, "modules.settings.jump_circle.speed").min(500f).max(3000f).step(50f).currentValue(1000f);

    private final ModeSetting mode = new ModeSetting(this, "modules.settings.jump_circle.mode");
    private final ModeSetting.Value def = new ModeSetting.Value(mode, "modules.settings.jump_circle.mode.default").select();
    private final ModeSetting.Value circle1 = new ModeSetting.Value(mode, "modules.settings.jump_circle.mode.circle1");
    private final ModeSetting.Value circle2 = new ModeSetting.Value(mode, "modules.settings.jump_circle.mode.circle2");

    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.jump_circle.color_mode");
    private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.jump_circle.color_mode.theme").select();
    private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.jump_circle.color_mode.custom");
    private final ColorSetting color = new ColorSetting(this, "modules.settings.jump_circle.color_1", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(151, 71, 255, 255))
            .alpha(true);
    private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.jump_circle.color_2", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(100, 160, 255, 255))
            .alpha(true);

    private final List<JumpCircleData> circles = new ArrayList<>();

    private Identifier getTexture() {
        if (circle1.isSelected()) return CIRCLE_TEXTURE;
        if (circle2.isSelected()) return CIRCLE_1_TEXTURE;
        return CIRCLE_2_TEXTURE;
    }

    private final EventListener<EntityJumpEvent> onJump = event -> {
        if (mc.player == null || mc.world == null) return;
        if (!(event.getEntity() instanceof PlayerEntity player)) return;

        if (!shouldRenderForPlayer(player)) return;

        Vec3d pos = new Vec3d(player.getX(), Math.floor(player.getY()) + 0.01, player.getZ());
        circles.add(new JumpCircleData(pos, System.currentTimeMillis(), player.getUuid()));
    };

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (circles.isEmpty()) return;

        long now = System.currentTimeMillis();
        float speedValue = speed.getCurrentValue();
        for (int i = circles.size() - 1; i >= 0; i--) {
            if (now - circles.get(i).startTime > speedValue) {
                circles.remove(i);
            }
        }

        RenderUtility.setupRender3D(true);
        RenderSystem.setShaderTexture(0, getTexture());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();

        for (JumpCircleData circle : circles) {
            float progress = (float) (now - circle.startTime) / speedValue;
            if (progress >= 1.0f) continue;

            float easedProgress = bounceOut(progress);
            float s = easedProgress * maxSize.getCurrentValue();
            float alpha = progress < 0.2f ? progress * 5.0f : (1.0f - progress) * 1.25f;
            int c = this.getCircleColor(progress * 360.0F).withAlpha(Math.max(0, Math.min(255, alpha * 255))).getRGB();

            MatrixStack ms = event.getMatrices();
            ms.push();
            ms.translate(circle.pos.x - camPos.x, circle.pos.y - camPos.y, circle.pos.z - camPos.z);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

            Matrix4f matrix = ms.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, -s/2, -s/2, 0).texture(0, 0).color(c);
            buffer.vertex(matrix, -s/2, s/2, 0).texture(0, 1).color(c);
            buffer.vertex(matrix, s/2, s/2, 0).texture(1, 1).color(c);
            buffer.vertex(matrix, s/2, -s/2, 0).texture(1, 0).color(c);
            RenderUtility.buildBuffer(buffer);

            ms.pop();
        }

        RenderSystem.defaultBlendFunc();
        RenderUtility.endRender3D();
    };

    private boolean shouldRenderForPlayer(PlayerEntity player) {
        if (player == mc.player) {
            return targetSelf.isSelected();
        }

        boolean isFriend = FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString());
        if (isFriend && targetFriends.isSelected()) {
            return !isInvisible(player);
        }

        return false;
    }

    private boolean isInvisible(PlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
        if (player.isInvisible()) return true;
        if (mc.player != null && player.isInvisibleTo(mc.player)) return true;
        return false;
    }

    private float bounceOut(float value) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (value < 1.0f / d1) return n1 * value * value;
        else if (value < 2.0f / d1) return n1 * (value -= 1.5f / d1) * value + 0.75f;
        else if (value < 2.5f / d1) return n1 * (value -= 2.25f / d1) * value + 0.9375f;
        else return n1 * (value -= 2.625f / d1) * value + 0.984375f;
    }

    private ColorRGBA getCircleColor(float index) {
        if (this.colorCustom.isSelected()) {
            float normalized = (index % 360.0F) / 180.0F;
            float mix = normalized > 1.0F ? 2.0F - normalized : normalized;
            return this.color.getColorSafe().mix(this.colorSecond.getColorSafe(), mix);
        }

        return Colors.getAccentColor(index);
    }

    private static record JumpCircleData(Vec3d pos, long startTime, UUID playerUuid) {}
}
