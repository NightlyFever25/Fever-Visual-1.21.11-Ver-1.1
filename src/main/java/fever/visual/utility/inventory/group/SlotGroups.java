package fever.visual.utility.inventory.group;

import fever.visual.utility.inventory.group.impl.ArmorSlotsGroup;
import fever.visual.utility.inventory.group.impl.HotbarSlotsGroup;
import fever.visual.utility.inventory.group.impl.InventorySlotsGroup;
import fever.visual.utility.inventory.group.impl.OffhandSlotGroup;
import fever.visual.utility.inventory.slots.ArmorSlot;
import fever.visual.utility.inventory.slots.HotbarSlot;
import fever.visual.utility.inventory.slots.InventorySlot;
import fever.visual.utility.inventory.slots.OffhandSlot;

public class SlotGroups {
   private SlotGroups() {
   }

   public static SlotGroup<HotbarSlot> hotbar() {
      return new HotbarSlotsGroup();
   }

   public static SlotGroup<InventorySlot> inventory() {
      return new InventorySlotsGroup();
   }

   public static SlotGroup<ArmorSlot> armor() {
      return new ArmorSlotsGroup();
   }

   public static SlotGroup<OffhandSlot> offhand() {
      return new OffhandSlotGroup();
   }
}
