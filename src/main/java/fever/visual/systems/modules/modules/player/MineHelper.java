package fever.visual.systems.modules.modules.player;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.StartBreakBlockEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

@ModuleInfo(name = "Mine Helper", category = ModuleCategory.MISC, desc = "modules.descriptions.mine_helper")
public class MineHelper extends BaseModule {
   private final BooleanSetting save = new BooleanSetting(this, "modules.settings.mine_helper.save", "modules.settings.mine_helper.save.description").enable();
   public final SliderSetting percent = new SliderSetting(this, "modules.settings.mine_helper.percent").step(1.0F).min(1.0F).max(70.0F).currentValue(10.0F).suffix("%");
   private final EventListener<StartBreakBlockEvent> onStartBreakBlockEvent = event -> {
      if (mc.player != null) {
         ItemStack currentStack = mc.player.getMainHandStack();
         if (this.isValidPickaxe(currentStack)) {
            double durabilityPercent = this.getDurabilityPercent(currentStack);
            if (this.save.isEnabled() && !(durabilityPercent >= this.percent.getCurrentValue())) {
               event.cancel();
               this.handleLowDurability(currentStack);
            }
         }
      }
   };

   private void handleLowDurability(ItemStack currentStack) {
   }

   private boolean isValidPickaxe(ItemStack stack) {
      return stack != null && stack.isDamageable() && stack.isIn(ItemTags.PICKAXES);
   }

   private double getDurabilityPercent(ItemStack stack) {
      return (double)(stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage() * 100.0;
   }
}
