package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.render.pipeline.GlowEspCompositePipeline;
import fever.visual.utility.render.pipeline.KawaseBlurPipeline;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.EntityVisibility;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ModuleInfo(name = "Glow ESP", category = ModuleCategory.VISUALS, desc = "Entity glow")
public class GlowEsp extends BaseModule {
    private static GlowEsp instance;

    private final SelectSetting targets = new SelectSetting(this, "Targets").min(1);
    private final SelectSetting.Value players = new SelectSetting.Value(this.targets, "Players").select();
    private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets, "Mobs").select();
    private final SelectSetting.Value self = new SelectSetting.Value(this.targets, "Self");
    private final SelectSetting.Value friends = new SelectSetting.Value(this.targets, "Friends").select();
    private final ModeSetting colorMode = new ModeSetting(this, "Color Mode");
    private final ModeSetting.Value theme = new ModeSetting.Value(this.colorMode, "Theme").select();
    private final ModeSetting.Value rainbow = new ModeSetting.Value(this.colorMode, "Rainbow");
    private final ModeSetting.Value custom = new ModeSetting.Value(this.colorMode, "Custom");
    private final ColorSetting firstColor = new ColorSetting(this, "Color 1", () -> !this.custom.isSelected())
            .color(new ColorRGBA(120, 170, 255, 255)).alpha(true);
    private final ColorSetting secondColor = new ColorSetting(this, "Color 2", () -> !this.custom.isSelected())
            .color(new ColorRGBA(255, 110, 205, 255)).alpha(true);
    private final SliderSetting glowStrength = new SliderSetting(this, "Glow Strength")
            .min(0.25F).max(2.5F).step(0.05F).currentValue(1.15F);
    private final ModeSetting renderMode = new ModeSetting(this, "Render Mode");
    private final ModeSetting.Value outer = new ModeSetting.Value(this.renderMode, "Outer").select();
    private final ModeSetting.Value inner = new ModeSetting.Value(this.renderMode, "Inner");
    private final ModeSetting.Value both = new ModeSetting.Value(this.renderMode, "Outer and Inner");
    private final SliderSetting blurPasses = new SliderSetting(this, "Iterations")
            .min(1.0F).max(5.0F).step(1.0F).currentValue(3.0F);
    private final SliderSetting divider = new SliderSetting(this, "Blur Divider")
            .min(1.0F).max(8.0F).step(0.1F).currentValue(8.0F);
    private final BooleanSetting chams = new BooleanSetting(this, "Chams");
    private final BooleanSetting outline = new BooleanSetting(this, "Outline").enable();

    private final Map<Integer, Boolean> previousGlow = new HashMap<>();
    private final Set<Integer> activeIds = new HashSet<>();
    private final KawaseBlurPipeline blur = new KawaseBlurPipeline();
    private final GlowEspCompositePipeline composite = new GlowEspCompositePipeline();

    public GlowEsp() {
        instance = this;
    }

    private final EventListener<Render3DEvent> onRender = event -> {
        this.renderGlowPost();
    };

    public static void prepareFrame(float tickDelta) {
        GlowEsp module = instance;
        if (module != null && module.isEnabled()) {
            module.updateGlowTargets(tickDelta);
        }
    }

    private void renderGlowPost() {
        if (this.activeIds.isEmpty() || mc.worldRenderer == null || mc.getFramebuffer() == null) return;
        net.minecraft.client.gl.Framebuffer outline = mc.worldRenderer.getEntityOutlinesFramebuffer();
        net.minecraft.client.gl.Framebuffer main = mc.getFramebuffer();
        if (outline == null || outline.getColorAttachment() == null || outline.getColorAttachmentView() == null
                || main.getColorAttachmentView() == null) return;
        com.mojang.blaze3d.textures.GpuTextureView glow = this.blur.blur(
                outline.getColorAttachment(), outline.getColorAttachmentView(),
                outline.textureWidth, outline.textureHeight,
                Math.round(this.blurPasses.getCurrentValue()), 1.25F
        );
        this.composite.composite(
                main.getColorAttachmentView(), outline.getColorAttachmentView(), glow,
                this.glowStrength.getCurrentValue(), this.renderModeId(), this.divider.getCurrentValue(),
                this.chams.isEnabled(), this.outline.isEnabled()
        );
    }

    private int renderModeId() {
        if (this.inner.isSelected()) return 1;
        if (this.both.isSelected()) return 2;
        return 0;
    }

    private void updateGlowTargets(float tickDelta) {
        if (mc.world == null || mc.player == null) {
            this.restoreAll();
            return;
        }
        Set<Integer> next = new HashSet<>();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || !this.shouldRender(living)
                    || !EntityVisibility.isRenderedAndVisible(living, tickDelta)) continue;
            next.add(entity.getId());
            this.previousGlow.putIfAbsent(entity.getId(), entity.isGlowing());
            entity.setGlowing(true);
        }
        for (Integer id : new HashSet<>(this.activeIds)) {
            if (!next.contains(id)) this.restore(id);
        }
        this.activeIds.clear();
        this.activeIds.addAll(next);
    }

    private boolean shouldRender(LivingEntity entity) {
        if (!entity.isAlive() || entity.isRemoved() || isInvisible(entity)) return false;
        if (entity == mc.player) {
            return this.self.isSelected() && mc.options.getPerspective() != Perspective.FIRST_PERSON;
        }
        if (entity instanceof PlayerEntity player) {
            boolean friend = FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString());
            return friend ? this.friends.isSelected() : this.players.isSelected();
        }
        return entity instanceof MobEntity && this.mobs.isSelected() && !entity.isSpectator();
    }

    private static boolean isInvisible(LivingEntity entity) {
        return entity.hasStatusEffect(StatusEffects.INVISIBILITY) || entity.isInvisible()
                || mc.player != null && entity.isInvisibleTo(mc.player);
    }

    private void restore(int id) {
        if (mc.world != null) {
            Entity entity = mc.world.getEntityById(id);
            if (entity != null) entity.setGlowing(this.previousGlow.getOrDefault(id, false));
        }
        this.previousGlow.remove(id);
        this.activeIds.remove(id);
    }

    private void restoreAll() {
        for (Integer id : new HashSet<>(this.activeIds)) this.restore(id);
        this.previousGlow.clear();
        this.activeIds.clear();
    }

    @Override
    public void onDisable() {
        this.restoreAll();
    }

    public static Integer outlineColorFor(Entity entity) {
        GlowEsp module = instance;
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
        if (module == null || !module.isEnabled() || !(entity instanceof LivingEntity living)
                || !module.shouldRender(living) || !EntityVisibility.isRenderedAndVisible(living, tickDelta)) {
            return null;
        }
        float phase = (System.currentTimeMillis() % 3500L) / 3500.0F * 360.0F
                + entity.getId() * 31.0F + (float)entity.getY() * 12.0F;
        ColorRGBA color;
        if (module.custom.isSelected()) {
            float mix = (float)(Math.sin(Math.toRadians(phase)) * 0.5D + 0.5D);
            color = module.firstColor.getColorSafe().mix(module.secondColor.getColorSafe(), mix);
        } else if (module.rainbow.isSelected()) {
            color = java.awt.Color.getHSBColor((phase % 360.0F) / 360.0F, 0.82F, 1.0F) == null
                    ? ColorRGBA.WHITE : ColorRGBA.fromInt(java.awt.Color.HSBtoRGB((phase % 360.0F) / 360.0F, 0.82F, 1.0F));
        } else {
            color = Colors.getAccentColor(phase);
        }
        if (entity instanceof PlayerEntity player
                && FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString())) {
            color = new ColorRGBA(70, 235, 120, 255);
        }
        if (living.hurtTime > 0) {
            color = color.mix(new ColorRGBA(255, 60, 60, 255), Math.min(1.0F, living.hurtTime / 10.0F));
        }
        return color.getRGB();
    }
}
