package fever.visual.utility.inventory.group.impl;

import java.util.ArrayList;
import java.util.List;
import fever.visual.utility.inventory.group.SlotGroup;
import fever.visual.utility.inventory.slots.InventorySlot;

public class InventorySlotsGroup extends SlotGroup<InventorySlot> {
   public InventorySlotsGroup() {
      super(createSlots());
   }

   private static List<InventorySlot> createSlots() {
      List<InventorySlot> slots = new ArrayList<>();

      for (int i = 0; i < 27; i++) {
         slots.add(new InventorySlot(i));
      }

      return slots;
   }
}
