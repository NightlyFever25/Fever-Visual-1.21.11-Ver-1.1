package fever.visual.systems.modules.modules.combat;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.window.KeyPressEvent;
import fever.visual.systems.event.impl.window.MouseEvent;
import fever.visual.systems.friends.FriendManager;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BindSetting;
import net.minecraft.entity.player.PlayerEntity;

@ModuleInfo(name = "MCF", category = ModuleCategory.COMBAT, desc = "modules.descriptions.mcf")
public class MCF extends BaseModule {
   private final BindSetting clickFriendKey = new BindSetting(this, "modules.settings.clickfriendkey");
   private final EventListener<KeyPressEvent> onKeyPressEvent = event -> this.handleKey(event.getKey(), event.getAction());
   private final EventListener<MouseEvent> onMouseEvent = event -> this.handleKey(event.getButton(), event.getAction());

   private void handleKey(int key, int action) {
      if (mc.currentScreen == null && action == 1) {
         if (this.clickFriendKey.isKey(key) && mc.targetedEntity instanceof PlayerEntity) {
            String nick = mc.targetedEntity.getName().getString();
            FriendManager friend = FeverVisual.getInstance().getFriendManager();
            if (friend.isFriend(nick)) {
               friend.remove(nick);
            } else {
               friend.add(nick);
            }
         }
      }
   }
}