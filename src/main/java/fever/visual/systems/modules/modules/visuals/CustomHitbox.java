package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.Utils;
import fever.visual.utility.game.EntityUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import fever.visual.utility.render.compat.BufferRenderer;
import net.minecraft.client.render.Camera;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
        name = "Custom Hitbox",
        category = ModuleCategory.COMBAT,
        desc = "modules.descriptions.custom_hitbox"
)
public class CustomHitbox extends BaseModule implements IMinecraft {

    private final ModeSetting hitboxColorMode = new ModeSetting(this, "modules.settings.custom_hitbox.hitbox_color_mode");
    private final ModeSetting.Value hitboxColorTheme = new ModeSetting.Value(this.hitboxColorMode, "modules.settings.custom_hitbox.color_mode.theme").select();
    private final ModeSetting.Value hitboxColorCustom = new ModeSetting.Value(this.hitboxColorMode, "modules.settings.custom_hitbox.color_mode.custom");
    private final ColorSetting hitboxColor = new ColorSetting(this, "modules.settings.custom_hitbox.hitbox_color", () -> !this.hitboxColorCustom.isSelected())
            .color(new ColorRGBA(253.0F, 0.0F, 0.0F, 144.0F))
            .alpha(true);
    private final ColorSetting hitboxColorSecond = new ColorSetting(this, "modules.settings.custom_hitbox.hitbox_color_second", () -> !this.hitboxColorCustom.isSelected())
            .color(new ColorRGBA(255.0F, 96.0F, 96.0F, 160.0F))
            .alpha(true);
    private final ModeSetting boxColorMode = new ModeSetting(this, "modules.settings.custom_hitbox.box_color_mode");
    private final ModeSetting.Value boxColorTheme = new ModeSetting.Value(this.boxColorMode, "modules.settings.custom_hitbox.color_mode.theme").select();
    private final ModeSetting.Value boxColorCustom = new ModeSetting.Value(this.boxColorMode, "modules.settings.custom_hitbox.color_mode.custom");
    private final ColorSetting boxColor1 = new ColorSetting(this, "modules.settings.custom_hitbox.box_color", () -> !this.boxColorCustom.isSelected())
            .color(new ColorRGBA(253.0F, 0.0F, 0.0F, 96.0F))
            .alpha(true);
    private final ColorSetting boxColor2 = new ColorSetting(this, "modules.settings.custom_hitbox.box_color_second", () -> !this.boxColorCustom.isSelected())
            .color(new ColorRGBA(255.0F, 96.0F, 96.0F, 160.0F))
            .alpha(true);
    private final SelectSetting entitySelection = new SelectSetting(this, "modules.settings.custom_hitbox.entity_types").min(1);
    private final SelectSetting.Value self        = new SelectSetting.Value(this.entitySelection, "modules.settings.custom_hitbox.entity_types.self").select();
    private final SelectSetting.Value players     = new SelectSetting.Value(this.entitySelection, "modules.settings.custom_hitbox.entity_types.players").select();
    private final SelectSetting.Value mobs        = new SelectSetting.Value(this.entitySelection, "modules.settings.custom_hitbox.entity_types.mobs").select();
    private final SelectSetting.Value animals     = new SelectSetting.Value(this.entitySelection, "modules.settings.custom_hitbox.entity_types.animals").select();
    private final SelectSetting.Value armorStands = new SelectSetting.Value(this.entitySelection, "modules.settings.custom_hitbox.entity_types.armor_stands").select();
    private final List<Entity> visibleEntities = new ArrayList<>();

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (!EntityUtility.isInGame()) return;
        if (this.hasVisionBlockingEffect()) return;

        MatrixStack matrices = event.getMatrices();
        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        this.visibleEntities.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (this.shouldRenderHitbox(entity) && this.isInFieldOfView(entity, event.getTickDelta())) {
                this.visibleEntities.add(entity);
            }
        }

        if (!this.visibleEntities.isEmpty()) {
            ColorRGBA[] gradientColors = this.createGradientColors();
            ColorRGBA[] hitboxColors = this.createHitboxColors();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder fillBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Entity entity : this.visibleEntities) {
                this.appendBox(matrices, entity, event.getTickDelta(), fillBuffer, gradientColors, false);
            }
            BufferRenderer.drawWithGlobalProgram(fillBuffer.end());

            RenderSystem.lineWidth(3.0F);
            BufferBuilder lineBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (Entity entity : this.visibleEntities) {
                this.appendBox(matrices, entity, event.getTickDelta(), lineBuffer, hitboxColors, true);
            }
            BufferRenderer.drawWithGlobalProgram(lineBuffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        matrices.pop();
    };

    public ColorRGBA getHitboxColor() {
        return this.getHitboxColor(0.0F);
    }

    public ColorRGBA getBoxColor1() {
        return this.getBoxColor(0.0F);
    }

    public ColorRGBA getBoxColor2() {
        return this.getBoxColor(20.0F);
    }

    public boolean shouldRenderHitbox(Entity entity) {
        if (entity instanceof LivingEntity living && entity != mc.player && this.isInvisible(living)) {
            return false;
        }

        if (entity instanceof PlayerEntity player) {
            if (entity == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
                return false;
            }
            return (this.self.isSelected() && entity == mc.player)
                    || (this.players.isSelected() && entity != mc.player);
        }
        if (entity instanceof PassiveEntity) return this.animals.isSelected();
        if (entity instanceof MobEntity)    return this.mobs.isSelected();
        if (entity instanceof ArmorStandEntity) return this.armorStands.isSelected();
        return false;
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

    @Nullable
    public static CustomHitbox getModule() {
        if (FeverVisual.getInstance().getModuleManager() == null) return null;
        return FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomHitbox.class);
    }

    private boolean isInFieldOfView(Entity entity, float partialTicks) {
        Camera camera = mc.gameRenderer.getCamera();
        double ex = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * partialTicks;
        double ey = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * partialTicks + entity.getHeight() * 0.5;
        double ez = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * partialTicks;
        return Utils.isInViewCone(camera, ex, ey, ez, 0.20, Math.max(1.0, entity.getWidth()));
    }

    private void appendBox(MatrixStack matrices, Entity target, float partialTicks, BufferBuilder buffer, ColorRGBA[] colors, boolean outline) {
        if (target == null) return;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        double x = target.lastRenderX + (target.getX() - target.lastRenderX) * partialTicks;
        double y = target.lastRenderY + (target.getY() - target.lastRenderY) * partialTicks;
        double z = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * partialTicks;

        Box bb = target.getBoundingBox();
        double minX = bb.minX - target.getX() + x - cameraPos.x;
        double minY = bb.minY - target.getY() + y - cameraPos.y;
        double minZ = bb.minZ - target.getZ() + z - cameraPos.z;
        double maxX = bb.maxX - target.getX() + x - cameraPos.x;
        double maxY = bb.maxY - target.getY() + y - cameraPos.y;
        double maxZ = bb.maxZ - target.getZ() + z - cameraPos.z;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        if (outline) {
            drawBoxOutline(buffer, matrix, minX, minY + 0.01, minZ, maxX, maxY, maxZ, colors);
        } else {
            drawBoxFill(buffer, matrix, minX, minY + 0.01, minZ, maxX, maxY, maxZ, colors, 85);
        }
    }

    private ColorRGBA[] createGradientColors() {
        ColorRGBA start = this.getBoxColor(0.0F);
        ColorRGBA end = this.getBoxColor(20.0F);
        return new ColorRGBA[] {
                start.withAlpha(start.getAlpha() * 0.35F), end.withAlpha(end.getAlpha() * 0.60F),
                start.withAlpha(start.getAlpha() * 0.35F), end.withAlpha(end.getAlpha() * 0.60F)
        };
    }

    private ColorRGBA[] createHitboxColors() {
        return new ColorRGBA[] {
                this.getHitboxColor(0.0F), this.getHitboxColor(20.0F),
                this.getHitboxColor(40.0F), this.getHitboxColor(60.0F)
        };
    }

    private ColorRGBA getHitboxColor(float index) {
        if (this.hitboxColorCustom.isSelected()) {
            return this.hitboxColor.getColorSafe().mix(this.hitboxColorSecond.getColorSafe(), (index % 60.0F) / 59.0F);
        }

        return Colors.getAccentColor(index * 6.0F);
    }

    private ColorRGBA getBoxColor(float index) {
        if (this.boxColorCustom.isSelected()) {
            return this.boxColor1.getColorSafe().mix(this.boxColor2.getColorSafe(), (index % 40.0F) / 39.0F);
        }

        return Colors.getAccentColor(index * 6.0F);
    }

    private void drawBoxFill(BufferBuilder buffer, Matrix4f matrix,
                             double minX, double minY, double minZ,
                             double maxX, double maxY, double maxZ,
                             ColorRGBA[] colors, int fillAlpha) {
        int c0 = replAlpha(colors[0].getRGB(), fillAlpha);
        int c1 = replAlpha(colors[1].getRGB(), fillAlpha);
        int c2 = replAlpha(colors[2].getRGB(), fillAlpha);
        int c3 = replAlpha(colors[3].getRGB(), fillAlpha);
        buffer.vertex(matrix, (float)minX, (float)minY, (float)minZ).color(c0);
        buffer.vertex(matrix, (float)maxX, (float)minY, (float)minZ).color(c1);
        buffer.vertex(matrix, (float)maxX, (float)minY, (float)maxZ).color(c2);
        buffer.vertex(matrix, (float)minX, (float)minY, (float)maxZ).color(c3);
        buffer.vertex(matrix, (float)minX, (float)maxY, (float)minZ).color(c0);
        buffer.vertex(matrix, (float)minX, (float)maxY, (float)maxZ).color(c3);
        buffer.vertex(matrix, (float)maxX, (float)maxY, (float)maxZ).color(c2);
        buffer.vertex(matrix, (float)maxX, (float)maxY, (float)minZ).color(c1);
        buffer.vertex(matrix, (float)minX, (float)minY, (float)maxZ).color(c3);
        buffer.vertex(matrix, (float)maxX, (float)minY, (float)maxZ).color(c2);
        buffer.vertex(matrix, (float)maxX, (float)maxY, (float)maxZ).color(c2);
        buffer.vertex(matrix, (float)minX, (float)maxY, (float)maxZ).color(c3);
        buffer.vertex(matrix, (float)maxX, (float)minY, (float)minZ).color(c1);
        buffer.vertex(matrix, (float)minX, (float)minY, (float)minZ).color(c0);
        buffer.vertex(matrix, (float)minX, (float)maxY, (float)minZ).color(c0);
        buffer.vertex(matrix, (float)maxX, (float)maxY, (float)minZ).color(c1);
        buffer.vertex(matrix, (float)minX, (float)minY, (float)minZ).color(c0);
        buffer.vertex(matrix, (float)minX, (float)minY, (float)maxZ).color(c3);
        buffer.vertex(matrix, (float)minX, (float)maxY, (float)maxZ).color(c3);
        buffer.vertex(matrix, (float)minX, (float)maxY, (float)minZ).color(c0);
        buffer.vertex(matrix, (float)maxX, (float)minY, (float)maxZ).color(c2);
        buffer.vertex(matrix, (float)maxX, (float)minY, (float)minZ).color(c1);
        buffer.vertex(matrix, (float)maxX, (float)maxY, (float)minZ).color(c1);
        buffer.vertex(matrix, (float)maxX, (float)maxY, (float)maxZ).color(c2);
    }

    private void drawBoxOutline(BufferBuilder buffer, Matrix4f matrix,
                                double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ,
                                ColorRGBA[] colors) {
        int c0 = replAlpha(colors[0].getRGB(), 255);
        int c1 = replAlpha(colors[1].getRGB(), 255);
        int c2 = replAlpha(colors[2].getRGB(), 255);
        int c3 = replAlpha(colors[3].getRGB(), 255);
        drawLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, c0, c1);
        drawLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, c1, c2);
        drawLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, c2, c3);
        drawLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, c3, c0);
        drawLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, c0, c1);
        drawLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, c1, c2);
        drawLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, c2, c3);
        drawLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, c3, c0);
        drawLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, c0, c0);
        drawLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, c1, c1);
        drawLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, c2, c2);
        drawLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, c3, c3);
    }

    private void drawLine(BufferBuilder buffer, Matrix4f matrix,
                          double x1, double y1, double z1,
                          double x2, double y2, double z2,
                          int color1, int color2) {
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1)
                .color(color1 >> 16 & 0xFF, color1 >> 8 & 0xFF, color1 & 0xFF, color1 >> 24 & 0xFF);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2)
                .color(color2 >> 16 & 0xFF, color2 >> 8 & 0xFF, color2 & 0xFF, color2 >> 24 & 0xFF);
    }

    private int replAlpha(int color, int alpha) {
        return alpha << 24 | (color >> 16 & 0xFF) << 16 | (color >> 8 & 0xFF) << 8 | (color & 0xFF);
    }
}
