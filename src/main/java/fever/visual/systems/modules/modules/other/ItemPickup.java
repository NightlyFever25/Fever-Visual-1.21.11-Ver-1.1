package fever.visual.systems.modules.modules.other;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.PickupEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.notifications.NotificationType;
import fever.visual.utility.game.ItemUtility;
import fever.visual.utility.game.MessageUtility;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.Map.Entry;

@ModuleInfo(name = "Item Pickup", category = ModuleCategory.MISC, desc = "Уведомляет вас при поднятии донатного предмета")
public class ItemPickup extends BaseModule {
   private final Map<String, String> don = Map.of(
      "krush-helmet",
      "Вы подобрали Шлем крушителя!",
      "krush-chestplate",
      "Вы подобрали Нагрудник крушителя!",
      "krush-leggings",
      "Вы подобрали Поножи крушителя!",
      "krush-boots",
      "Вы подобрали Ботинки крушителя!",
      "krush-sword",
      "Вы подобрали донатный предмет: Меч крушителя"
   );
   private final EventListener<PickupEvent> onPickupEvent = event -> {
      ItemStack stack = event.getItemStack();

      for (Entry<String, String> entry : this.don.entrySet()) {
         if (ItemUtility.checkDonItem(stack, entry.getKey())) {
            MessageUtility.info(Text.of(entry.getValue()));
            return;
         }
      }

      if (ItemUtility.isDonItem(stack)) {
         String name = stack.getName().getString();
         FeverVisual.getInstance()
            .getNotificationManager()
            .addNotificationOther(NotificationType.INFO, "Донатный предмет", "Вы подобрали донатный предмет: " + name);
      }
   };
}
