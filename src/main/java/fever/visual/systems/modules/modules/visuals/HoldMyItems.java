package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import com.hmi.HandMyItemsRuntime;

@ModuleInfo(
        name = "Hold My Items",
        category = ModuleCategory.VISUALS,
        desc = "modules.descriptions.hold_my_items"
)
public class HoldMyItems extends BaseModule {

    public final SliderSetting physicsSpeed = new SliderSetting(this, "modules.settings.hold_my_items.physics_speed")
            .min(10.0F)
            .max(60.0F)
            .currentValue(30.0F)
            .step(1.0F);

    public final BooleanSetting swimmingAnimation = new BooleanSetting(this, "modules.settings.hold_my_items.swimming_animation")
            .enabled(true);

    public final BooleanSetting climbAndCrawl = new BooleanSetting(this, "modules.settings.hold_my_items.climb_and_crawl")
            .enabled(true);

    public final SliderSetting swingSpeed = new SliderSetting(this, "modules.settings.hold_my_items.swing_speed")
            .min(4.0F)
            .max(20.0F)
            .currentValue(9.0F)
            .step(0.5F);

    public final SliderSetting viewmodelXOffset = new SliderSetting(this, "modules.settings.hold_my_items.viewmodel_x_offset")
            .min(-2.0F)
            .max(2.0F)
            .currentValue(0.0F)
            .step(0.05F);

    public final SliderSetting viewmodelYOffset = new SliderSetting(this, "modules.settings.hold_my_items.viewmodel_y_offset")
            .min(-2.0F)
            .max(2.0F)
            .currentValue(0.0F)
            .step(0.05F);

    public final SliderSetting viewmodelZOffset = new SliderSetting(this, "modules.settings.hold_my_items.viewmodel_z_offset")
            .min(-2.0F)
            .max(2.0F)
            .currentValue(0.0F)
            .step(0.05F);

    public final BooleanSetting mb3DCompat = new BooleanSetting(this, "modules.settings.hold_my_items.mb3d_compat")
            .enabled(false);

    private static HoldMyItems INSTANCE;

    public HoldMyItems() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        HandMyItemsRuntime.enable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        HandMyItemsRuntime.disable();
    }

    public static HoldMyItems getInstance() {
        return INSTANCE;
    }

    public float getViewmodelXOffset() { return viewmodelXOffset.getCurrentValue(); }
    public float getViewmodelYOffset() { return viewmodelYOffset.getCurrentValue(); }
    public float getViewmodelZOffset() { return viewmodelZOffset.getCurrentValue(); }
    public float getPhysicsSpeed() { return physicsSpeed.getCurrentValue(); }
    public int getSwingSpeed() { return Math.max(1, Math.round(swingSpeed.getCurrentValue())); }
    public boolean isSwimmingAnimation() { return swimmingAnimation.isEnabled(); }
    public boolean isClimbAndCrawl() { return climbAndCrawl.isEnabled(); }
    public boolean isMb3DCompat() { return mb3DCompat.isEnabled(); }
}
