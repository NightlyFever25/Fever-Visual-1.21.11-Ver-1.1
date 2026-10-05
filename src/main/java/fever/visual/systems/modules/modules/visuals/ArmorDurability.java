package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

@ModuleInfo(
        name = "Armor Durability",
        category = ModuleCategory.DISPLAY,
        desc = "modules.descriptions.armor_durability"
)
public class ArmorDurability extends BaseModule {

    private final SliderSetting lowThreshold = new SliderSetting(this, "modules.settings.armor_durability.low_threshold")
            .min(1.0F)
            .max(50.0F)
            .step(1.0F)
            .suffix("%")
            .currentValue(30.0F);

    private final SliderSetting midThreshold = new SliderSetting(this, "modules.settings.armor_durability.mid_threshold")
            .min(20.0F)
            .max(90.0F)
            .step(1.0F)
            .suffix("%")
            .currentValue(65.0F);

    private final ColorSetting lowColor = new ColorSetting(this, "modules.settings.armor_durability.low_color")
            .color(new ColorRGBA(255.0F, 85.0F, 85.0F, 255.0F))
            .alpha(false);

    private final ColorSetting midColor = new ColorSetting(this, "modules.settings.armor_durability.mid_color")
            .color(new ColorRGBA(255.0F, 187.0F, 85.0F, 255.0F))
            .alpha(false);

    private final ColorSetting highColor = new ColorSetting(this, "modules.settings.armor_durability.high_color")
            .color(new ColorRGBA(85.0F, 255.0F, 85.0F, 255.0F))
            .alpha(false);

    public ColorRGBA getColorByPercent(float durabilityPercent) {
        float percent = MathHelper.clamp(durabilityPercent, 0.0F, 1.0F);
        float low = MathHelper.clamp(this.lowThreshold.getCurrentValue() / 100.0F, 0.01F, 0.99F);
        float mid = MathHelper.clamp(this.midThreshold.getCurrentValue() / 100.0F, 0.02F, 0.99F);
        if (mid <= low) {
            mid = Math.min(0.99F, low + 0.01F);
        }

        if (percent <= low) {
            return this.lowColor.getColorSafe();
        }
        if (percent < mid) {
            float factor = (percent - low) / (mid - low);
            return this.lowColor.getColorSafe().mix(this.midColor.getColorSafe(), factor);
        }

        float factor = (percent - mid) / (1.0F - mid);
        return this.midColor.getColorSafe().mix(this.highColor.getColorSafe(), factor);
    }

    public int getColorIntByPercent(float durabilityPercent) {
        return this.getColorByPercent(durabilityPercent).getRGB();
    }

    @Nullable
    public static ArmorDurability getModule() {
        if (FeverVisual.getInstance().getModuleManager() == null) {
            return null;
        }
        return FeverVisual.getInstance().getModuleManager().getModuleSafe(ArmorDurability.class);
    }
}
