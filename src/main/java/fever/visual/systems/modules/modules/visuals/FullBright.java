package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

@ModuleInfo(name = "FullBright", category = ModuleCategory.VISUALS, desc = "modules.descriptions.fullbright")
public class FullBright extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.fullbright.mode");
   private final ModeSetting.Value gamma = new ModeSetting.Value(this.mode, "modules.settings.fullbright.mode.gamma").select();
   private final ModeSetting.Value nightVision = new ModeSetting.Value(this.mode, "modules.settings.fullbright.mode.night_vision");
   private boolean appliedNightVision;

   @Override
   public void tick() {
      if (mc.player == null) {
         return;
      }

      if (this.isNightVisionMode() && !this.shouldKeepVisionEffect()) {
         mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
         this.appliedNightVision = true;
      } else if (this.appliedNightVision) {
         mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
         this.appliedNightVision = false;
      }
   }

   @Override
   public void onDisable() {
      if (this.appliedNightVision && mc.player != null) {
         mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
      }

      this.appliedNightVision = false;
   }

   public boolean isGammaMode() {
      return this.isEnabled() && this.gamma.isSelected() && !this.shouldKeepVisionEffect();
   }

   public boolean isNightVisionMode() {
      return this.isEnabled() && this.nightVision.isSelected();
   }

   private boolean shouldKeepVisionEffect() {
      return mc.player != null
              && (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
              || mc.player.hasStatusEffect(StatusEffects.DARKNESS));
   }
}
