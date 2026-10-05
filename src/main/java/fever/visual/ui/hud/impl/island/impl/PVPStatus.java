package fever.visual.ui.hud.impl.island.impl;

import fever.visual.framework.base.CustomDrawContext;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.ui.hud.impl.island.TimerStatus;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.game.server.ServerUtility;

public class PVPStatus extends TimerStatus {
   public PVPStatus(SelectSetting setting) {
      super(setting, "pvp");
   }

   @Override
   public void draw(CustomDrawContext context) {
      this.update("s", ServerUtility.ctTime, "Вы в PVP режиме", new ColorRGBA(185.0F, 28.0F, 28.0F));
      super.draw(context);
   }

   @Override
   public boolean canShow() {
      return ServerUtility.hasCT;
   }
}
