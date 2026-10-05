package fever.visual.ui.hud.inline.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.ui.hud.inline.InlineElement;
import fever.visual.ui.hud.inline.InlineValue;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.game.server.ServerUtility;

public class WorldElement extends InlineElement {
   private final InlineValue cords = new InlineValue(this.elements, "coords");
   private final InlineValue server = new InlineValue(this.elements, "server");
   private final InlineValue tps = new InlineValue(this.elements, "TPS", "TPS");
   private final BooleanSetting shortName = new BooleanSetting(this, "hud.world.compact_server").enable();
   private long lastSlowUpdate;
   private int lastX = Integer.MIN_VALUE;
   private int lastY = Integer.MIN_VALUE;
   private int lastZ = Integer.MIN_VALUE;

   public WorldElement() {
      super("hud.world", "icons/hud/world.png");
   }

   @Override
   public void update(UIContext context) {
      super.update(context);
      int x = Math.round((float)mc.player.getX());
      int y = Math.round((float)mc.player.getY());
      int z = Math.round((float)mc.player.getZ());
      if (x != this.lastX || y != this.lastY || z != this.lastZ) {
         this.cords.update(x + " " + y + " " + z);
         this.lastX = x;
         this.lastY = y;
         this.lastZ = z;
      }

      long now = System.currentTimeMillis();
      if (now - this.lastSlowUpdate >= 500L) {
         this.server.update(ServerUtility.getServerName(this.shortName.isEnabled()), ServerUtility.getIP());
         this.tps.update(TextUtility.formatNumber(FeverVisual.getInstance().getTpsHandler().getTPS()).replace(",", ".").replace(".0", ""));
         this.lastSlowUpdate = now;
      }
   }
}
