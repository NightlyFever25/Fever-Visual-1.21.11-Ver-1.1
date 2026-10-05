package fever.visual.systems.modules.modules.other;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.waypoints.WayPointsManager;
import fever.visual.utility.game.MessageUtility;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;

@ModuleInfo(name = "Death Cords", category = ModuleCategory.MISC, desc = "modules.descriptions.death_cords")
public class DeathCords extends BaseModule {
   private boolean death;
   private final BooleanSetting wayDeath = new BooleanSetting(this, "modules.settings.death_cords.way_death");
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (!(mc.currentScreen instanceof DeathScreen) || mc.player == null) {
         this.death = true;
      } else if (this.death) {
         int xCord = (int)mc.player.getX();
         int yCord = (int)mc.player.getY();
         int zCord = (int)mc.player.getZ();
         MessageUtility.info(Text.of("modules.death_cords.message " + xCord + " " + yCord + " " + zCord));
         if (this.wayDeath.isEnabled()) {
            WayPointsManager wayPointsManager = FeverVisual.getInstance().getWayPointsManager();
            if (wayPointsManager.contains("Death")) {
               wayPointsManager.del("Death");
            }

            wayPointsManager.add("Death", xCord, yCord, zCord);
         }

         this.death = false;
      }
   };
}