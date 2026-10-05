package fever.visual;

import fever.visual.framework.base.FeverGuiElementRenderer;
import fever.visual.systems.policy.ServerPolicy;
import net.fabricmc.api.ClientModInitializer;
public final class FeverVisualClient implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      ServerPolicy.getInstance().start();
      FeverGuiElementRenderer.register();
   }
}
