package fever.visual.ui.hud.impl.island.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.ui.hud.impl.island.DynamicIsland;
import fever.visual.ui.hud.impl.island.IslandStatus;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;

public class DefaultStatus extends IslandStatus {
   public DefaultStatus(SelectSetting setting) {
      super(setting, "default");
   }

   @Override
   public void draw(CustomDrawContext context) {
      DynamicIsland island = FeverVisual.getInstance().getHud().getIsland();
      float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
      float y = 7.0F;
      float width = this.size.width = 20.0F + Fonts.MEDIUM.getFont(7.0F).width("FeverVisual");
      float height = this.size.height = 15.0F;
      context.drawRoundedRect(x - 6.0F + 10.0F * this.animation.getValue(), y + 4.0F, 7.0F, 7.0F, BorderRadius.all(3.0F), Colors.getAccentColor(0.0F));
      context.drawText(Fonts.MEDIUM.getFont(7.0F), "FeverVisual", x + 25.0F - 10.0F * this.animation.getValue(), y + 5.0F, Colors.getTextColor());
   }

   @Override
   public boolean canShow() {
      return true;
   }
}