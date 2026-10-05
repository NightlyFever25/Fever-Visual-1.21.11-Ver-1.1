package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.SliderSetting;

@ModuleInfo(name = "Better Tab", category = ModuleCategory.VISUALS, desc = "modules.descriptions.better_tab")
public class BetterTab extends BaseModule {
    private final SliderSetting playerLimit = new SliderSetting(this, "better_tab.player_limit")
            .min(80.0f).max(300.0f).step(10.0f).currentValue(160.0f);
    private final SliderSetting maxRows = new SliderSetting(this, "better_tab.max_rows")
            .min(20.0f).max(40.0f).step(1.0f).currentValue(30.0f);
    private final SliderSetting sideMargin = new SliderSetting(this, "better_tab.side_margin")
            .min(0.0f).max(50.0f).step(1.0f).currentValue(10.0f).suffix(" px");

    public static long getPlayerLimit(long vanillaLimit) {
        BetterTab module = getInstance();
        if (module == null || !module.isEnabled()) {
            return vanillaLimit;
        }
        return Math.max(vanillaLimit, (long) module.playerLimit.getCurrentValue());
    }

    public static int getMaxRows(int vanillaRows) {
        BetterTab module = getInstance();
        if (module == null || !module.isEnabled()) {
            return vanillaRows;
        }
        return Math.max(vanillaRows, Math.round(module.maxRows.getCurrentValue()));
    }

    public static int getSideMargin(int vanillaMargin) {
        BetterTab module = getInstance();
        if (module == null || !module.isEnabled()) {
            return vanillaMargin;
        }
        return Math.min(vanillaMargin, Math.max(0, Math.round(module.sideMargin.getCurrentValue())));
    }

    private static BetterTab getInstance() {
        return FeverVisual.getInstance().getModuleManager().getModuleSafe(BetterTab.class);
    }
}
