package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.modules.modules.visuals.littlePet.ChinaHat;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.Util;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ModuleInfo(
        name = "China Hat",
        desc = "modules.descriptions.china_hat",
        category = ModuleCategory.VISUALS
)
public class ChinaHatModule extends BaseModule {
    private static final float BASE_HEAD_Y_OFFSET = 0.2F;
    private static final long VISIBILITY_CACHE_MS = 50L;
    private final Map<UUID, VisibilityEntry> visibilityCache = new HashMap<>();
    private ModelChanger modelChanger;

    private final SelectSetting targets = new SelectSetting(this, "modules.settings.model_changer.targets");
    private final SelectSetting.Value targetSelf = new SelectSetting.Value(targets, "modules.settings.chinahat.render_self").select();
    private final SelectSetting.Value targetFriends = new SelectSetting.Value(targets, "modules.settings.chinahat.render_others");

    private final SliderSetting width = new SliderSetting(this, "modules.settings.chinahat.width", "modules.settings.chinahat.width").min(1.2F).max(2.1F).step(0.1F).currentValue(1.2F);
    private final SliderSetting height = new SliderSetting(this, "modules.settings.chinahat.height", "modules.settings.chinahat.height").min(0.05F).max(0.8F).step(0.05F).currentValue(0.25F);
    private final BooleanSetting useTheme = new BooleanSetting(this, "modules.settings.chinahat.use_theme_color").enable();
    private final ColorSetting color = new ColorSetting(this, "modules.settings.chinahat.color", () -> this.useTheme.isEnabled()).color(new ColorRGBA(100, 150, 200, 255)).alpha(true);
    private final SliderSetting alpha = new SliderSetting(this, "modules.settings.chinahat.alpha", "modules.settings.chinahat.alpha").min(0.0F).max(1.0F).step(0.05F).currentValue(0.85F);

    private final SliderSetting widthFriends = new SliderSetting(this, "modules.settings.chinahat.width_others", "modules.settings.chinahat.width_others").min(1.2F).max(2.1F).step(0.1F).currentValue(1.2F);
    private final SliderSetting heightFriends = new SliderSetting(this, "modules.settings.chinahat.height_others", "modules.settings.chinahat.height_others").min(0.05F).max(0.8F).step(0.05F).currentValue(0.25F);
    private final ColorSetting colorFriends = new ColorSetting(this, "modules.settings.chinahat.color_others", () -> this.useTheme.isEnabled()).color(new ColorRGBA(100, 150, 200, 255)).alpha(true);
    private final SliderSetting alphaFriends = new SliderSetting(this, "modules.settings.chinahat.alpha_others", "modules.settings.chinahat.alpha_others").min(0.0F).max(1.0F).step(0.05F).currentValue(0.85F);

    public boolean shouldRenderForPlayer(AbstractClientPlayerEntity player) {
        if (!this.isEnabled() || mc.world == null || mc.player == null) {
            return false;
        }
        if (player == null || player.isRemoved()) return false;

        if (player == mc.player) {
            return targetSelf.isSelected();
        }

        boolean isFriend = isFriend(player);
        if (isFriend && targetFriends.isSelected()) {
            return !isInvisible(player) && canSeePlayer(player);
        }

        return false;
    }

    public void renderHeadAnchored(AbstractClientPlayerEntity player, MatrixStack matrices) {
        if (player == null || matrices == null) {
            return;
        }
        if (player == mc.player && mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        boolean isSelf = player == mc.player;
        float width = isSelf ? this.width.getCurrentValue() : this.widthFriends.getCurrentValue();
        float height = isSelf ? this.height.getCurrentValue() : this.heightFriends.getCurrentValue();
        float modelScale = getModelScale(player, isSelf);
        float finalYOffset = BASE_HEAD_Y_OFFSET * modelScale;
        ColorRGBA color = isSelf ? getHatColor() : getHatColorFriends();
        float alpha = isSelf ? this.alpha.getCurrentValue() : this.alphaFriends.getCurrentValue();

        ChinaHat.renderAttachedToHead(
                matrices,
                width,
                height,
                finalYOffset,
                color,
                0.3F + alpha * 0.7F
        );
    }

    private float getModelScale(AbstractClientPlayerEntity player, boolean isSelf) {
        ModelChanger modelChanger = this.modelChanger;
        if (modelChanger == null) {
            this.modelChanger = modelChanger = FeverVisual.getInstance().getModuleManager().getModuleSafe(ModelChanger.class);
        }
        if (modelChanger == null || !modelChanger.isEnabled() || player == null) {
            return 1.0F;
        }

        String playerName = player.getName().getString();
        return modelChanger.getScaleForPlayer(playerName, isSelf);
    }

    private boolean isFriend(AbstractClientPlayerEntity player) {
        if (FeverVisual.getInstance().getFriendManager() == null) {
            return false;
        }
        String playerName = player.getName().getString();
        return FeverVisual.getInstance().getFriendManager().isFriend(playerName);
    }

    private boolean isInvisible(AbstractClientPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            return true;
        }
        if (player.isInvisible()) {
            return true;
        }
        if (player.isInvisibleTo(mc.player)) {
            return true;
        }
        return false;
    }

    private boolean canSeePlayer(AbstractClientPlayerEntity player) {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        long now = Util.getMeasuringTimeMs();
        VisibilityEntry cached = this.visibilityCache.get(player.getUuid());
        if (cached != null && now < cached.expiresAt()) {
            return cached.visible();
        }
        HitResult hitResult = mc.world.raycast(
                new RaycastContext(
                        mc.player.getEyePos(),
                        player.getEyePos(),
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                )
        );

        boolean visible = hitResult.getType() != HitResult.Type.BLOCK;
        this.visibilityCache.put(player.getUuid(), new VisibilityEntry(now + VISIBILITY_CACHE_MS, visible));
        return visible;
    }

    @Override
    public void onDisable() {
        this.visibilityCache.clear();
        super.onDisable();
    }

    private record VisibilityEntry(long expiresAt, boolean visible) {
    }

    private ColorRGBA getHatColor() {
        if (this.useTheme.isEnabled()) {
            return Colors.getAccentColor();
        }
        return this.color.getColor();
    }

    private ColorRGBA getHatColorFriends() {
        if (this.useTheme.isEnabled()) {
            return Colors.getAccentColor();
        }
        return this.colorFriends.getColor();
    }
}
