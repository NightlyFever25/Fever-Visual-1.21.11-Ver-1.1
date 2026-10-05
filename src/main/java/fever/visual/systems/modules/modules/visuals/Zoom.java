package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.window.KeyPressEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BindSetting;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;

@ModuleInfo(
        name = "Zoom",
        category = ModuleCategory.VISUALS,
        desc = "modules.descriptions.zoom"
)
public class Zoom extends BaseModule {
    private final BindSetting zoomKey = new BindSetting(this, "modules.settings.zoom.key");
    private final SliderSetting zoomLevel = new SliderSetting(this, "modules.settings.zoom.level")
            .step(0.1f).min(1.0f).max(10.0f).currentValue(4.0f);
    private final BooleanSetting smooth = new BooleanSetting(this, "modules.settings.zoom.smooth")
            .enabled(true);
    private final SliderSetting smoothSpeed = new SliderSetting(this, "modules.settings.zoom.smooth_speed",
            "modules.settings.zoom.smooth_speed.desc", () -> !this.smooth.isEnabled())
            .step(50.0f).min(100.0f).max(1000.0f).currentValue(300.0f);

    private boolean isZooming = false;
    private final Animation zoomAnimation = new Animation(300L, 0.0f, Easing.FIGMA_EASE_IN_OUT);

    private final EventListener<KeyPressEvent> onKeyPress = event -> {
        if (this.zoomKey.isKey(event.getKey()) && mc.currentScreen == null) {
            if (event.getAction() == 1) {
                this.isZooming = true;
            } else if (event.getAction() == 0) {
                this.isZooming = false;
            }
        }
    };

    @Override
    public void onEnable() {
        super.onEnable();
        this.zoomAnimation.setDuration((long) this.smoothSpeed.getCurrentValue());
    }

    public void updateZoom() {
        if (this.smooth.isEnabled()) {
            this.zoomAnimation.setDuration((long) this.smoothSpeed.getCurrentValue());
            this.zoomAnimation.update(this.isZooming);
        }
    }

    public float getZoomMultiplier() {
        if (!this.isEnabled()) {
            return 1.0f;
        }
        if (this.smooth.isEnabled()) {
            float progress = this.zoomAnimation.getValue();
            return 1.0f + (this.zoomLevel.getCurrentValue() - 1.0f) * progress;
        }
        return this.isZooming ? this.zoomLevel.getCurrentValue() : 1.0f;
    }

    public boolean isZooming() {
        return this.isZooming;
    }
}