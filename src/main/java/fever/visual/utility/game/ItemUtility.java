package fever.visual.utility.game;
import lombok.Generated;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
public final class ItemUtility implements IMinecraft {
   public static NbtCompound getNBT(ItemStack stack) {
      NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
      return component == null ? null : component.copyNbt();
   }
   public static boolean checkDonItem(ItemStack itemStack, String startWith) {
      NbtCompound customData = getNBT(itemStack);
      if (customData == null) {
         return false;
      } else if (customData.contains("don-item")) {
         String donItemName = customData.getString("don-item", "");
         return donItemName.contains(startWith);
      } else {
         return false;
      }
   }
   public static boolean isDonItem(ItemStack itemStack) {
      NbtCompound customData = getNBT(itemStack);
      return customData == null ? false : customData.contains("don-item");
   }
   @Generated
   private ItemUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
