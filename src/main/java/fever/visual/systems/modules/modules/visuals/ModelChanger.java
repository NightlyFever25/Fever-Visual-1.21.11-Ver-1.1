package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;

@ModuleInfo(name = "Model Changer", desc = "modules.descriptions.model_changer", category = ModuleCategory.VISUALS)
public class ModelChanger extends BaseModule {

    private final SelectSetting targets = new SelectSetting(this, "modules.settings.model_changer.targets");
    private final SelectSetting.Value friends = new SelectSetting.Value(targets, "modules.settings.model_changer.friends");
    private final SelectSetting.Value self = new SelectSetting.Value(targets, "modules.settings.model_changer.self");

    private final SliderSetting selfScale = new SliderSetting(
            this,
            "modules.settings.model_changer.self_scale"
    ).step(0.01f).min(0.1f).max(1.0f).currentValue(0.5f);

    private final SliderSetting friendsScale = new SliderSetting(
            this,
            "modules.settings.model_changer.friends_scale"
    ).step(0.01f).min(0.1f).max(1.0f).currentValue(0.5f);

    private static final ThreadLocal<Float> currentScale = ThreadLocal.withInitial(() -> 1.0f);
    private static final ThreadLocal<Float> currentOffset = ThreadLocal.withInitial(() -> 0.0f);

    @Override
    public void onEnable() {
        currentScale.set(1.0f);
        currentOffset.set(0.0f);
    }

    @Override
    public void onDisable() {
        currentScale.set(1.0f);
        currentOffset.set(0.0f);
    }

    public float getScaleForPlayer(String playerName, boolean isSelf) {
        if (!isEnabled()) {
            return 1.0f;
        }
        if (targets.getSelectedValues().contains(self) && isSelf) {
            return selfScale.getCurrentValue();
        }
        if (targets.getSelectedValues().contains(friends) &&
                FeverVisual.getInstance().getFriendManager().isFriend(playerName)) {
            return friendsScale.getCurrentValue();
        }

        return 1.0f;
    }

    public float getYOffsetForScale(float scaleValue) {
        if (!isEnabled() || scaleValue >= 1.0f) {
            return 0.0f;
        }
        return (1.0f - scaleValue) * 1.5f;
    }

    public static void setCurrentScale(float scale) {
        currentScale.set(scale);
    }

    public static void setCurrentOffset(float offset) {
        currentOffset.set(offset);
    }

    public static float getCurrentScale() {
        return currentScale.get();
    }

    public static float getCurrentOffset() {
        return currentOffset.get();
    }

    public SelectSetting getTargets() {
        return targets;
    }

    public SliderSetting getSelfScale() {
        return selfScale;
    }

    public SliderSetting getFriendsScale() {
        return friendsScale;
    }
}