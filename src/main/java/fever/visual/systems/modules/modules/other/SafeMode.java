package fever.visual.systems.modules.modules.other;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.Module;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.utility.game.TitleBarHelper;
import net.minecraft.client.util.Icons;

import java.util.LinkedHashMap;
import java.util.Map;

@ModuleInfo(name = "Safe Mode", category = ModuleCategory.MISC, desc = "modules.descriptions.SafeMode")
public class SafeMode extends BaseModule {
   private final Map<Module, ModuleState> savedStates = new LinkedHashMap<>();

   @Override
   public void onEnable() {
      this.captureState();
      TitleBarHelper.setLightTitleBar();
      FeverVisual.getInstance().getFileManager().saveClientFiles();
      FeverVisual.getInstance().startSafeModeCountdown(this::activateSafeMode, this::restoreSafeMode);
      super.onEnable();
   }

   private void activateSafeMode() {
      for (Module module : FeverVisual.getInstance().getModuleManager().getModules()) {
         module.setKey(-1);
         module.setEnabled(false, true);
      }

      try {
         mc.getWindow().setIcon(mc.getDefaultResourcePack(), Icons.RELEASE);
      } catch (Exception ignored) {
      }
   }

   private void restoreSafeMode() {
      for (Map.Entry<Module, ModuleState> entry : this.savedStates.entrySet()) {
         Module module = entry.getKey();
         ModuleState state = entry.getValue();
         module.setKey(state.key());
         module.setEnabled(state.enabled(), true);
      }

      this.savedStates.clear();
      TitleBarHelper.setDarkTitleBar();
   }

   private void captureState() {
      this.savedStates.clear();
      for (Module module : FeverVisual.getInstance().getModuleManager().getModules()) {
         boolean enabled = module != this && module.isEnabled();
         this.savedStates.put(module, new ModuleState(enabled, module.getKey()));
      }
   }

   private record ModuleState(boolean enabled, int key) {
   }
}
