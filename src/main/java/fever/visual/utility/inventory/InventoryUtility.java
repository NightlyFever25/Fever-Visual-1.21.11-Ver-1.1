package fever.visual.utility.inventory;

import java.util.function.Predicate;
import lombok.Generated;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.inventory.group.impl.HotbarSlotsGroup;
import fever.visual.utility.inventory.slots.ArmorSlot;
import fever.visual.utility.inventory.slots.HotbarSlot;
import fever.visual.utility.inventory.slots.InventorySlot;
import fever.visual.utility.inventory.slots.OffhandSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.NotNull;

public final class InventoryUtility implements IMinecraft {
   public static HotbarSlot getHotbarSlot(int slotId) {
      return new HotbarSlot(slotId);
   }


   public static OffhandSlot getOffHandSlot() {
      return new OffhandSlot();
   }

   public static void moveItem(ItemSlot from, ItemSlot to) {
      if (mc.getNetworkHandler() != null) {
         from.click();
         to.click();
         if (!to.isEmpty()) {
            from.click();
         }

         mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
      }
   }

   public static void moveToOffHand(ItemSlot fromSlot) {
      OffhandSlot offHandSlot = getOffHandSlot();
      moveItem(fromSlot, offHandSlot);
   }

   public static void selectHotbarSlot(int slotId) {
      if (mc.player != null && mc.player.getInventory() != null && mc.getNetworkHandler() != null) {
         if (slotId >= 0 && slotId <= 8) {
            mc.player.getInventory().setSelectedSlot(slotId);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
         } else {
            throw new IllegalArgumentException("Hotbar slot ID must be between 0 and 8");
         }
      }
   }

   public static void selectHotbarSlot(HotbarSlot slot) {
      selectHotbarSlot(slot.getSlotId());
   }


   @Generated
   private InventoryUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
