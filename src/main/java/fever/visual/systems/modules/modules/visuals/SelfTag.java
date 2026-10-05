package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;

@ModuleInfo(
   name = "Self Tag",
   category = ModuleCategory.VISUALS,
   desc = "Shows your own nametag in third person"
)
public class SelfTag extends BaseModule {
   public static boolean isActive() {
      if (FeverVisual.getInstance() == null || FeverVisual.getInstance().getModuleManager() == null) {
         return false;
      }
      SelfTag module = FeverVisual.getInstance().getModuleManager().getModuleSafe(SelfTag.class);
      return module != null && module.isEnabled();
   }
}
