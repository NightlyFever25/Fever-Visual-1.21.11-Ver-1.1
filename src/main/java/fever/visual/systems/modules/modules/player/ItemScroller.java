package fever.visual.systems.modules.modules.player;

import lombok.Generated;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.SliderSetting;

@ModuleInfo(name = "Item Scroller", category = ModuleCategory.MISC,desc = "Быстрый скрол предметов")
public class ItemScroller extends BaseModule {
   private final SliderSetting scrollDelay = new SliderSetting(this, "delay").currentValue(0.0F).max(150.0F).min(0.0F).step(1.0F);

   @Generated
   public SliderSetting getScrollDelay() {
      return this.scrollDelay;
   }

}
