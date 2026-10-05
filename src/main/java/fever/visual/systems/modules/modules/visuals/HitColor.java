package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.OverlayTextureReloadBridge;
import org.jetbrains.annotations.Nullable;

@ModuleInfo(
        name = "Hit Color",
        category = ModuleCategory.VISUALS,
        desc = "modules.descriptions.hit_color"
)
public class HitColor extends BaseModule {
    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.hit_color.color_mode");
    private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.hit_color.color_mode.theme").select();
    private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.hit_color.color_mode.custom");
    private final ColorSetting hitColor = new ColorSetting(this, "modules.settings.hit_color.color", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(255.0F, 0.0F, 0.0F, 128.0F))
            .alpha(true);
    private final ColorSetting hitColorSecond = new ColorSetting(this, "modules.settings.hit_color.color_second", () -> !this.colorCustom.isSelected())
            .color(new ColorRGBA(151.0F, 71.0F, 255.0F, 128.0F))
            .alpha(true);

    private int lastColorArgb = Integer.MIN_VALUE;

    @Override
    public void onEnable() {
        OverlayTextureReloadBridge.reloadAll();
        this.lastColorArgb = this.getOverlayArgb();
    }

    @Override
    public void onDisable() {
        OverlayTextureReloadBridge.reloadAll();
    }

    @Override
    public void tick() {
        int current = this.getOverlayArgb();
        if (current != this.lastColorArgb) {
            this.lastColorArgb = current;
            OverlayTextureReloadBridge.reloadAll();
        }
    }

    public ColorRGBA getHitColor() {
        float index = (System.currentTimeMillis() % 2600L) / 2600.0F * 360.0F;
        if (this.colorCustom.isSelected()) {
            return this.hitColor.getColorSafe().mix(this.hitColorSecond.getColorSafe(), this.getColorMix(index));
        }

        return Colors.getAccentColor(index).withAlpha(this.hitColor.getColorSafe().getAlpha());
    }

    private float getColorMix(float index) {
        float normalized = (index % 360.0F) / 180.0F;
        return normalized > 1.0F ? 2.0F - normalized : normalized;
    }

    public int getOverlayArgb() {
        ColorRGBA color = this.getHitColor();
        int alpha = 255 - Math.round(color.getAlpha());
        int red = Math.round(color.getRed());
        int green = Math.round(color.getGreen());
        int blue = Math.round(color.getBlue());
        return net.minecraft.util.math.ColorHelper.getArgb(alpha, red, green, blue);
    }

    @Nullable
    public static HitColor getModule() {
        if (FeverVisual.getInstance().getModuleManager() == null) {
            return null;
        }
        return FeverVisual.getInstance().getModuleManager().getModuleSafe(HitColor.class);
    }
}
