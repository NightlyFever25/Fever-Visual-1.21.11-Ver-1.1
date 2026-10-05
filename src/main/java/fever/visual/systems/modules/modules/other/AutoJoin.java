package fever.visual.systems.modules.modules.other;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.network.ReceivePacketEvent;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.notifications.NotificationType;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.utility.game.server.ServerUtility;
import fever.visual.utility.inventory.group.SlotGroup;
import fever.visual.utility.inventory.group.SlotGroups;
import fever.visual.utility.inventory.slots.HotbarSlot;
import fever.visual.utility.time.Timer;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@ModuleInfo(name = "Auto Join", category = ModuleCategory.MISC, desc = "modules.descriptions.auto_join")
public class AutoJoin extends BaseModule {
   private final String griefString = "306";
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.auto_join.mode");
   private final ModeSetting.Value duels = new ModeSetting.Value(this.mode, "modules.settings.auto_join.mode.duels").select();
   private final ModeSetting.Value grief = new ModeSetting.Value(this.mode, "modules.settings.auto_join.mode.grief");
   private final Timer timer = new Timer();
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (ServerUtility.isST() && this.duels.isSelected()) {
         SlotGroup<HotbarSlot> search = SlotGroups.hotbar();
         HotbarSlot compass = search.findItem(Items.COMPASS);
         HotbarSlot sword = search.findItem(Items.DIAMOND_SWORD);
         if (compass != null && this.timer.finished(300L)) {
            mc.player.getInventory().setSelectedSlot(compass.getSlotId());
            mc.interactionManager
                    .sendSequencedPacket(mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch()));
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && mc.currentScreen.getTitle().getString().contains("Выберите режим")) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 14, 0, SlotActionType.QUICK_MOVE, mc.player);
            }

            this.timer.reset();
         }

         if (sword != null) {
            FeverVisual.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS, "modules.auto_join.success_title", "modules.auto_join.success_message");
            this.toggle();
         }
      }

      if ((ServerUtility.isFT() || ServerUtility.isRW() || ServerUtility.isST()) && this.grief.isSelected()) {
         mc.player.networkHandler.sendChatCommand("an306");
      }
   };
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet
              && (ServerUtility.isFT() || ServerUtility.isRW() || ServerUtility.isST())
              && this.grief.isSelected()) {
         String message = packet.content().getString().toLowerCase();
         if (ServerUtility.isFT() ? message.contains("вы уже подключены к этому серверву!") : message.contains("вы уже подключены на этот сервер!")) {
            this.toggle();
         }
      }
   };
}
