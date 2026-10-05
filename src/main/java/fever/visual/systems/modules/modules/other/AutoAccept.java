package fever.visual.systems.modules.modules.other;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.network.ReceivePacketEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.utility.game.server.ServerUtility;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "Auto Accept", category = ModuleCategory.MISC, desc = "modules.descriptions.auto_accept")
public class AutoAccept extends BaseModule {
   private final ModeSetting acceptMode = new ModeSetting(this, "modules.settings.auto_accept.mode");
   private final ModeSetting.Value acceptAll = new ModeSetting.Value(this.acceptMode, "modules.settings.auto_accept.mode.all");
   private final ModeSetting.Value friendsOnly = new ModeSetting.Value(this.acceptMode, "modules.settings.auto_accept.mode.friends_only");
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet
              && mc.player != null
              && packet.content().getString().contains("телепортироваться")
              && !ServerUtility.hasCT
              && this.canAccept(packet.content().getString())) {
         mc.player.networkHandler.sendChatCommand("tpaccept");
      }
   };

   private boolean canAccept(String message) {
      if (this.acceptMode.is(this.acceptAll)) {
         return true;
      } else {
         if (this.acceptMode.is(this.friendsOnly)) {
            if (FeverVisual.getInstance().getFriendManager().isFriend(message.split(" ")[1])
                    || FeverVisual.getInstance()
                    .getFriendManager()
                    .isFriend(message.replace("\u0a77 просит телепортироваться к Вам.\u0a77§l [ੲ§l✔\u0a77§l]\u0a77§l [\u0a7c§l✗\u0a77§l]", "").replace("੶", ""))
                    || FeverVisual.getInstance().getFriendManager().isFriend(message.replace("➝ Ник: ", ""))) {
               return true;
            }

            if (message.contains("телепортироваться")) {
               String[] parts = message.split(" ");
               return parts.length >= 2 && FeverVisual.getInstance().getFriendManager().isFriend(parts[2]);
            }
         }

         return false;
      }
   }
}