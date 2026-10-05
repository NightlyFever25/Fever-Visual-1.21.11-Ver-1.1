package fever.visual.ui.menu.api;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.base.UIContext;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.systems.modules.modules.visuals.MenuModule;
import fever.visual.ui.menu.MenuScreen;
import fever.visual.ui.menu.dropdown.DropDownScreen;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;

public class MenuCloseListener implements IMinecraft {
   private final EventListener<HudRenderEvent> onHudRender = (event) -> {
      MenuScreen menuScreen = FeverVisual.getInstance().getMenuScreen();
      if (mc.currentScreen == null) {
      }

      if (!(menuScreen instanceof DropDownScreen)) {
         FeverVisual.getInstance().setMenuScreen(new DropDownScreen());
      }

      if (menuScreen != null) {
         menuScreen.getMenuAnimation().update(menuScreen.isClosing() ? 0.0F : 1.0F);
         if (!(mc.currentScreen instanceof MenuScreen) && ((MenuModule)FeverVisual.getInstance().getModuleManager().getModule(MenuModule.class)).isEnabled()) {
            ((MenuModule)FeverVisual.getInstance().getModuleManager().getModule(MenuModule.class)).setEnabled(false);
         }

         if (menuScreen.getMenuAnimation().getValue() > 0.1F && !(mc.currentScreen instanceof MenuScreen) && menuScreen.isClosing()) {
            UIContext context = UIContext.of((CustomDrawContext)event.getContext(), -1, -1, MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false));
            menuScreen.render(context);
         }
      }

   };

   public MenuCloseListener() {
      FeverVisual.getInstance().getEventManager().subscribe(this);
   }
}
