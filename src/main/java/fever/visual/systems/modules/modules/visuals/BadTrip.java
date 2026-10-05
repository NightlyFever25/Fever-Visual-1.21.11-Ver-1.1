package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.modules.modules.other.Sounds;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.utility.sounds.ClientSounds;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

@ModuleInfo(name = "Bad Trip", category = ModuleCategory.VISUALS, desc = "modules.descriptions.bad_trip")
public class BadTrip extends BaseModule {

    private final SelectSetting targets = new SelectSetting(this, "modules.descriptions.badtrip.targets");
    private final SelectSetting.Value targetSelf = new SelectSetting.Value(targets, "modules.descriptions.badtrip.self").select();
    private final SelectSetting.Value targetFriends = new SelectSetting.Value(targets, "modules.descriptions.badtrip.friends");
    private final SelectSetting.Value targetEnemies = new SelectSetting.Value(targets, "modules.descriptions.badtrip.enemies");

    private static final float PULSE_SPEED = 0.5f;
    private static final float PULSE_INTENSITY = 0.5f;
    private static final float Y_INTENSITY = 0.3f;

    private static final ThreadLocal<Float> currentTimeFactor = ThreadLocal.withInitial(() -> 0.0f);
    private static final ThreadLocal<Float> currentScaleX = ThreadLocal.withInitial(() -> 1.0f);
    private static final ThreadLocal<Float> currentScaleY = ThreadLocal.withInitial(() -> 1.0f);
    private static final ThreadLocal<Float> currentScaleZ = ThreadLocal.withInitial(() -> 1.0f);

    private boolean soundPlayed = false;

    @Override
    public void onEnable() {
        currentTimeFactor.set(0.0f);
        currentScaleX.set(1.0f);
        currentScaleY.set(1.0f);
        currentScaleZ.set(1.0f);
        soundPlayed = false;
        playBadTripSound();
    }

    @Override
    public void onDisable() {
        currentTimeFactor.set(0.0f);
        currentScaleX.set(1.0f);
        currentScaleY.set(1.0f);
        currentScaleZ.set(1.0f);
        soundPlayed = false;
        stopBadTripSound();
    }

    private boolean hasAnyTarget() {
        return targetSelf.isSelected() || targetFriends.isSelected() || targetEnemies.isSelected();
    }

    private void playBadTripSound() {
        if (!hasAnyTarget()) {
            return;
        }
        if (soundPlayed) {
            return;
        }

        Sounds soundsModule = FeverVisual.getInstance().getModuleManager().getModule(Sounds.class);
        if (soundsModule != null && soundsModule.isEnabled()) {
            float volume = soundsModule.getVolume().getCurrentValue();
            ClientSounds.BADTRIP.play(volume);
        } else {
            ClientSounds.BADTRIP.play(1.0F);
        }

        soundPlayed = true;
    }

    private void stopBadTripSound() {
        ClientSounds.BADTRIP.stop();
    }

    public boolean shouldAffectPlayer(PlayerEntity player) {
        if (!isEnabled() || player == null) {
            return false;
        }

        boolean isSelf = player == mc.player;
        String playerName = player.getName().getString();
        boolean isFriend = FeverVisual.getInstance().getFriendManager().isFriend(playerName);
        if (!isSelf) {
            if (isInvisible(player)) {
                return false;
            }
        }

        if (isSelf && targetSelf.isSelected()) {
            return true;
        }

        if (isFriend && targetFriends.isSelected()) {
            return true;
        }

        if (!isSelf && !isFriend && targetEnemies.isSelected()) {
            return true;
        }

        return false;
    }

    private boolean isInvisible(PlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
        if (player.isInvisible()) return true;
        if (mc.player != null && player.isInvisibleTo(mc.player)) return true;
        return false;
    }

    public float getTimeFactor() {
        if (!isEnabled()) {
            return 0.0f;
        }
        long time = System.currentTimeMillis();
        return (float) (Math.sin((double) time / (200.0 / PULSE_SPEED) * Math.PI) * 2 + 0.5);
    }

    public float getWidthHeightScale() {
        if (!isEnabled()) {
            return 1.0f;
        }
        float timeFactor = getTimeFactor();
        return 1.0f + (PULSE_INTENSITY * timeFactor);
    }

    public float getYScale() {
        if (!isEnabled()) {
            return 1.0f;
        }
        float timeFactor = getTimeFactor();
        float baseYScale = 1.0f;
        float minY = 1.0f - (PULSE_INTENSITY * Y_INTENSITY);
        float maxY = 1.0f + (PULSE_INTENSITY * Y_INTENSITY * 0.5f);

        return maxY - (maxY - minY) * (timeFactor);
    }

    public void updateScales() {
        if (!isEnabled()) {
            currentScaleX.set(1.0f);
            currentScaleY.set(1.0f);
            currentScaleZ.set(1.0f);
            currentTimeFactor.set(0.0f);
            return;
        }

        currentTimeFactor.set(getTimeFactor());
        currentScaleX.set(getWidthHeightScale());
        currentScaleY.set(getYScale());
        currentScaleZ.set(getWidthHeightScale());
    }

    public static float getCurrentScaleX() {
        return currentScaleX.get();
    }

    public static float getCurrentScaleY() {
        return currentScaleY.get();
    }

    public static float getCurrentScaleZ() {
        return currentScaleZ.get();
    }

    public static float getCurrentTimeFactor() {
        return currentTimeFactor.get();
    }

    public SelectSetting getTargets() {
        return targets;
    }
}