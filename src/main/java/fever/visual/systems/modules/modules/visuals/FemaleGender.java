package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

@ModuleInfo(name = "Female Gender", category = ModuleCategory.VISUALS, desc = "modules.descriptions.female_gender")
public class FemaleGender extends BaseModule {
   private static FemaleGender INSTANCE;

   public final SliderSetting selfSize = new SliderSetting(this, "female_gender.self_size")
      .min(0.0F)
      .max(1.6F)
      .step(0.05F)
      .currentValue(1.0F);

   public final SliderSetting friendsSize = new SliderSetting(this, "female_gender.friends_size")
      .min(0.0F)
      .max(1.6F)
      .step(0.05F)
      .currentValue(1.0F);

   public final SliderSetting othersSize = new SliderSetting(this, "female_gender.others_size")
      .min(0.0F)
      .max(1.6F)
      .step(0.05F)
      .currentValue(0.85F);

   public FemaleGender() {
      INSTANCE = this;
   }

   public static FemaleGender getInstance() {
      return INSTANCE;
   }

   public float getSizeFor(PlayerEntity player) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && player == client.player) {
         return Math.clamp(this.selfSize.getCurrentValue(), 0.0F, 1.6F);
      }

      if (isInvisible(player, client.player)) {
         return 0.0F;
      }

      if (FeverVisual.getInstance().getFriendManager() != null && FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString())) {
         return Math.clamp(this.friendsSize.getCurrentValue(), 0.0F, 1.6F);
      }

      return Math.clamp(this.othersSize.getCurrentValue(), 0.0F, 1.6F);
   }

   private boolean isInvisible(PlayerEntity player, PlayerEntity viewer) {
      if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
      if (player.isInvisible()) return true;
      if (viewer != null && player.isInvisibleTo(viewer)) return true;
      return false;
   }
}
