package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@ModuleInfo(name = "Cape", category = ModuleCategory.VISUALS, desc = "modules.descriptions.cape")
public class Cape extends BaseModule {
    private static final Identifier FEVER_CAPE_TEXTURE = FeverVisual.id("textures/cape/cape.png");
    private static final Identifier DEFAULT_CAPE_TEXTURE = FeverVisual.id("textures/cape/cape2.png");

    private final SelectSetting targets = new SelectSetting(this, "modules.settings.cape.targets");
    private final SelectSetting.Value targetSelf = new SelectSetting.Value(targets, "modules.settings.cape.targets.self").select();
    private final SelectSetting.Value targetFriends = new SelectSetting.Value(targets, "modules.settings.cape.targets.friends");

    private final ModeSetting style = new ModeSetting(this, "modules.settings.cape.style");
    private final ModeSetting.Value defaultCape = new ModeSetting.Value(style, "modules.settings.cape.style.default").select();
    private final ModeSetting.Value FeverCape = new ModeSetting.Value(style, "modules.settings.cape.style.white");

    private final BooleanSetting clothPhysics = new BooleanSetting(this, "modules.settings.cape.cloth_physics").enable();

    private final SliderSetting physicsStiffness = new SliderSetting(this, "modules.settings.cape.physics_stiffness", () -> !this.clothPhysics.isEnabled())
            .min(0.05F)
            .max(0.45F)
            .step(0.01F)
            .currentValue(0.22F);

    private final SliderSetting physicsDamping = new SliderSetting(this, "modules.settings.cape.physics_damping", () -> !this.clothPhysics.isEnabled())
            .min(0.55F)
            .max(0.95F)
            .step(0.01F)
            .currentValue(0.82F);

    private final SliderSetting motionInfluence = new SliderSetting(this, "modules.settings.cape.motion_influence", () -> !this.clothPhysics.isEnabled())
            .min(0.2F)
            .max(3.0F)
            .step(0.05F)
            .currentValue(1.0F);

    private final SliderSetting gravityInfluence = new SliderSetting(this, "modules.settings.cape.gravity_influence", () -> !this.clothPhysics.isEnabled())
            .min(0.0F)
            .max(3.0F)
            .step(0.05F)
            .currentValue(1.0F);

    private final SliderSetting waveAmplitude = new SliderSetting(this, "modules.settings.cape.wave_amplitude", () -> !this.clothPhysics.isEnabled())
            .min(0.0F)
            .max(7.0F)
            .step(0.1F)
            .currentValue(2.0F);

    private final SliderSetting waveSpeed = new SliderSetting(this, "modules.settings.cape.wave_speed", () -> !this.clothPhysics.isEnabled())
            .min(0.1F)
            .max(4.0F)
            .step(0.05F)
            .currentValue(1.0F);

    public Identifier getCapeTexture() {
        if (FeverCape.isSelected()) {
            return FEVER_CAPE_TEXTURE;
        }
        return DEFAULT_CAPE_TEXTURE;
    }

    public boolean shouldRenderForPlayer(PlayerEntity player) {
        if (player == null) return false;

        if (player == mc.player) {
            return targetSelf.isSelected();
        }

        boolean isFriend = FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString());
        if (isFriend && targetFriends.isSelected()) {
            return !isInvisible(player);
        }

        return false;
    }

    private boolean isInvisible(PlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
        if (player.isInvisible()) return true;
        if (mc.player != null && player.isInvisibleTo(mc.player)) return true;
        return false;
    }

    public boolean isClothPhysicsEnabled() {
        return this.clothPhysics.isEnabled();
    }

    public float getPhysicsStiffness() {
        return this.physicsStiffness.getCurrentValue();
    }

    public float getPhysicsDamping() {
        return this.physicsDamping.getCurrentValue();
    }

    public float getMotionInfluence() {
        return this.motionInfluence.getCurrentValue();
    }

    public float getGravityInfluence() {
        return this.gravityInfluence.getCurrentValue();
    }

    public float getWaveAmplitude() {
        return this.waveAmplitude.getCurrentValue();
    }

    public float getWaveSpeed() {
        return this.waveSpeed.getCurrentValue();
    }

    @Nullable
    public static Cape getModule() {
        if (FeverVisual.getInstance().getModuleManager() == null) {
            return null;
        }
        return FeverVisual.getInstance().getModuleManager().getModuleSafe(Cape.class);
    }
}
