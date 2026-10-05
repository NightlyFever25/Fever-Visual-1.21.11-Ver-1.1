//Да давай пасти бездарность :)
//Made by FeverVisuals (NightlyFever)
package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.modules.modules.visuals.cosmetic.CustomModelType;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
//Да давай пасти бездарность :)
//Made by FeverVisuals (NightlyFever)
@ModuleInfo(name = "Custom Models", category = ModuleCategory.VISUALS, desc = "modules.descriptions.custom_models")
public final class CustomModels extends BaseModule {

    public static final CustomModels INSTANCE = new CustomModels();

    private final ModeSetting selfModel = new ModeSetting(this, "modules.settings.models.self_model");
    private final ModeSetting.Value selfRabbit = new ModeSetting.Value(selfModel, "modules.settings.models.crazy_rabbit");
    private final ModeSetting.Value selfWhiteDemon = new ModeSetting.Value(selfModel, "modules.settings.models.white_demon");
    private final ModeSetting.Value selfRedDemon = new ModeSetting.Value(selfModel, "modules.settings.models.red_demon");
    private final ModeSetting.Value selfFreddy = new ModeSetting.Value(selfModel, "modules.settings.models.freddy_bear");
    private final ModeSetting.Value selfAmogus = new ModeSetting.Value(selfModel, "modules.settings.models.amogus");

    private final ModeSetting friendsModel = new ModeSetting(this, "modules.settings.models.friends_model");
    private final ModeSetting.Value friendsRabbit = new ModeSetting.Value(friendsModel, "modules.settings.models.crazy_rabbit");
    private final ModeSetting.Value friendsWhiteDemon = new ModeSetting.Value(friendsModel, "modules.settings.models.white_demon");
    private final ModeSetting.Value friendsRedDemon = new ModeSetting.Value(friendsModel, "modules.settings.models.red_demon");
    private final ModeSetting.Value friendsFreddy = new ModeSetting.Value(friendsModel, "modules.settings.models.freddy_bear");
    private final ModeSetting.Value friendsAmogus = new ModeSetting.Value(friendsModel, "modules.settings.models.amogus");


    public CustomModels() {
        selfRabbit.select();
        friendsRabbit.select();
    }

    public CustomModelType getSelectedTypeFor(LivingEntity entity) {
        if (entity == mc.player) {
            return getModelFromSetting(selfModel);
        } else if (isFriend(entity)) {
            return getModelFromSetting(friendsModel);
        }
        return null;
    }

    private CustomModelType getModelFromSetting(ModeSetting setting) {
        String selectedName = setting.getValue().getName();
        if (selectedName.equals("modules.settings.models.crazy_rabbit")) return CustomModelType.CRAZY_RABBIT;
        if (selectedName.equals("modules.settings.models.white_demon")) return CustomModelType.WHITE_DEMON;
        if (selectedName.equals("modules.settings.models.red_demon")) return CustomModelType.RED_DEMON;
        if (selectedName.equals("modules.settings.models.freddy_bear")) return CustomModelType.FREDDY_BEAR;
        if (selectedName.equals("modules.settings.models.amogus")) return CustomModelType.AMOGUS;
        return CustomModelType.CRAZY_RABBIT;
    }

    public boolean shouldApplyTo(LivingEntity entity) {
        if (!this.isEnabled() || entity == null) return false;
        if (!(entity instanceof PlayerEntity player)) return false;
        if (isInvisible(player)) return false;
        if (player == mc.player) return true;
        return isFriend(player);
    }


    private boolean isFriend(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity player)) return false;
        if (FeverVisual.getInstance().getFriendManager() == null) return false;
        return FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString());
    }

    private boolean isInvisible(PlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
        if (player.isInvisible()) return true;
        if (mc.player != null && player.isInvisibleTo(mc.player)) return true;
        return false;
    }
}