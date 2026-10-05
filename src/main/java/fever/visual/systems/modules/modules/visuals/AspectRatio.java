package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;

@ModuleInfo(name = "Aspect Ratio", category = ModuleCategory.VISUALS, desc = "modules.descriptions.aspect_ratio")
public class AspectRatio extends BaseModule {

    private final ModeSetting mode = new ModeSetting(this, "modules.settings.aspect_ratio.mode");

    private final ModeSetting.Value custom = new ModeSetting.Value(this.mode, "modules.settings.aspect_ratio.mode.custom").select();
    private final ModeSetting.Value ratio16_9 = new ModeSetting.Value(this.mode, "16:9").select();
    private final ModeSetting.Value ratio16_10 = new ModeSetting.Value(this.mode, "16:10").select();
    private final ModeSetting.Value ratio4_3 = new ModeSetting.Value(this.mode, "4:3").select();
    private final ModeSetting.Value ratio21_9 = new ModeSetting.Value(this.mode, "21:9").select();
    private final ModeSetting.Value ratio5_4 = new ModeSetting.Value(this.mode, "5:4").select();

    private final SliderSetting customRatio = new SliderSetting(
            this,
            "Кастомное соотношение",
            () -> !this.custom.isSelected()
    ).step(0.001f).min(0.5f).max(5.0f).currentValue(1.7777f);

    private static final ThreadLocal<Boolean> renderingHands = ThreadLocal.withInitial(() -> false);

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public float getRatio() {
        if (ratio16_9.isSelected()) {
            return 16.0f / 9.0f;
        } else if (ratio16_10.isSelected()) {
            return 16.0f / 10.0f;
        } else if (ratio4_3.isSelected()) {
            return 4.0f / 3.0f;
        } else if (ratio21_9.isSelected()) {
            return 21.0f / 9.0f;
        } else if (ratio5_4.isSelected()) {
            return 5.0f / 4.0f;
        } else {
            return this.customRatio.getCurrentValue();
        }
    }

    public static void setRenderingHands(boolean rendering) {
        renderingHands.set(rendering);
    }

    public static boolean isRenderingHands() {
        return renderingHands.get();
    }

    public ModeSetting getMode() {
        return mode;
    }

    public SliderSetting getCustomRatio() {
        return customRatio;
    }
}