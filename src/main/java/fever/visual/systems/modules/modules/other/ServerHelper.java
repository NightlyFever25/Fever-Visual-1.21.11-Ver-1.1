package fever.visual.systems.modules.modules.other;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.network.ReceivePacketEvent;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.event.impl.player.InputEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.notifications.NotificationType;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.utility.game.ItemUtility;
import fever.visual.utility.game.server.ServerUtility;
import fever.visual.utility.inventory.InventoryUtility;
import fever.visual.utility.inventory.ItemSlot;
import fever.visual.utility.inventory.slots.HotbarSlot;
import fever.visual.utility.time.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler.ConfirmServerResourcePackScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.util.Hand;

import java.util.function.BooleanSupplier;

@ModuleInfo(name = "Server Helper", category = ModuleCategory.MISC, desc = "modules.descriptions.server_helper")
public class ServerHelper extends BaseModule {
   private final BooleanSupplier rwCondition = () -> ServerUtility.isHW()
           || ServerUtility.isPastaFT()
           || ServerUtility.isCM()
           || ServerUtility.isSaturn()
           || ServerUtility.isIntave();
   private final BooleanSupplier ftCondition = () -> ServerUtility.isHW()
           || ServerUtility.isRW()
           || ServerUtility.isCM()
           || ServerUtility.isSaturn()
           || ServerUtility.isIntave();
   private final BooleanSupplier hwCondition = () -> ServerUtility.isPastaFT()
           || ServerUtility.isRW()
           || ServerUtility.isCM()
           || ServerUtility.isSaturn()
           || ServerUtility.isIntave();
   private final BooleanSetting spoof = new BooleanSetting(this, "modules.settings.server_helper.spoof", "modules.settings.server_helper.spoof.description", this.rwCondition);
   private final BooleanSetting closeMenu = new BooleanSetting(this, "modules.settings.server_helper.close_menu", "modules.settings.server_helper.close_menu.description", this.rwCondition)
           .enable();
   private final BooleanSetting autoFix = new BooleanSetting(this, "modules.settings.server_helper.auto_fix", this.rwCondition);
   private final BooleanSetting warnArmor = new BooleanSetting(this, "modules.settings.server_helper.warn_armor", "modules.settings.server_helper.warn_armor.description");
   private final BooleanSetting autoZako = new BooleanSetting(this, "modules.settings.server_helper.auto_zako", this.hwCondition);
   private final Timer timer = new Timer();
   private boolean zakoCommandSent = true;
   private boolean visible;
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (this.autoZako.isEnabled() && event.getPacket() instanceof GameMessageS2CPacket packetx) {
         String message = packetx.content().getString();
         if (message.contains("Вы уже активировали этот промокод")) {
            this.zakoCommandSent = false;
            this.timer.reset();
         } else if (message.contains("Прямо сейчас идет набор")) {
            this.zakoCommandSent = true;
         }
      }

      if (this.closeMenu.isEnabled() && event.getPacket() instanceof OpenScreenS2CPacket packetxxx) {
         String title = packetxxx.getName().getString();
         if (title.contains("Меню") || title.contains("ꈁꀀꈂꌁꈂꀁ")) {
            mc.player.closeScreen();
            event.cancel();
         }
      }
   };
   private Timer chargeTimer = new Timer();
   private ItemSlot previousSlot = null;
   private boolean isCharging = false;
   private boolean needsSlotSwapBack = false;
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {};

   @Override
   public void tick() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (this.warnArmor.isEnabled()) {
            float armorPoint = 1.0F;

            for (int armorSlot = 36; armorSlot < 40; armorSlot++) {
               ItemStack stack = mc.player.getInventory().getStack(armorSlot);
               if (!stack.isEmpty()) {
                  float maxDamage = stack.getMaxDamage();
                  float currentDamage = maxDamage - stack.getDamage();
                  armorPoint = currentDamage / maxDamage;
               }
            }

            if (armorPoint < 0.36) {
               if (this.visible) {
                  FeverVisual.getInstance().getNotificationManager().addNotificationOther(NotificationType.INFO, "modules.server_helper.armor_break_title", "modules.server_helper.armor_break_message");
                  this.visible = false;
               }}}
         if (this.autoFix.isEnabled() && !ServerUtility.hasCT) {
            PlayerInventory inventory = mc.player.getInventory();

            for (int i = 0; i < inventory.size(); i++) {
               ItemStack stackx = inventory.getStack(i);
               if (!stackx.isEmpty() && stackx.isDamageable()) {
                  float maxDamage = stackx.getMaxDamage();
                  float currentDamage = maxDamage - stackx.getDamage();
                  if (currentDamage / maxDamage > 0.5F && mc.player.age % 25 == 0) {
                     mc.player.networkHandler.sendChatCommand("fix all");
                     break;
                  }
               }
            }
         }

         if (this.spoof.isEnabled() && this.spoof.isVisible() && mc.player.age > 20 && mc.currentScreen instanceof ConfirmServerResourcePackScreen) {
            mc.player.networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), Status.ACCEPTED));
            mc.player.networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), Status.SUCCESSFULLY_LOADED));
            mc.player.closeScreen();
         }

         if (this.autoZako.isEnabled() && this.timer.finished(500L) && this.zakoCommandSent) {
            this.zakoCommandSent = false;
            mc.player.networkHandler.sendChatCommand("zako");
         }

         super.tick();
      }
   }

   public void stop() {
      if (this.isCharging) {
         MinecraftClient.getInstance().options.useKey.setPressed(false);
         this.isCharging = false;
      }

      if (this.previousSlot != null && this.previousSlot instanceof HotbarSlot hotbarSlot) {
         InventoryUtility.selectHotbarSlot(hotbarSlot);
         this.previousSlot = null;
      }

      this.needsSlotSwapBack = false;
   }
}
