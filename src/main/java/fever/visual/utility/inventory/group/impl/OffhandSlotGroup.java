package fever.visual.utility.inventory.group.impl;

import java.util.List;
import fever.visual.utility.inventory.group.SlotGroup;
import fever.visual.utility.inventory.slots.OffhandSlot;

public class OffhandSlotGroup extends SlotGroup<OffhandSlot> {
   public OffhandSlotGroup() {
      super(List.of(new OffhandSlot()));
   }
}
