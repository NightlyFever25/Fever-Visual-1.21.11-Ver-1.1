package fever.visual.systems.friends;

import fever.visual.FeverVisual;
import fever.visual.systems.localization.Localizator;
import fever.visual.utility.game.EntityUtility;
import fever.visual.utility.game.MessageUtility;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FriendManager implements IMinecraft {
   private final List<String> friends = new ArrayList<>();

   public void add(String name) {
      if (name == null || name.isBlank()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.name_required")));
         return;
      }

      if (this.friends.contains(name)) {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.exists", name)));
      } else if (name.equalsIgnoreCase(mc.getSession().getUsername())) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.self")));
      } else {
         this.friends.add(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.added", name)));
         if (EntityUtility.isInGame()) {
            FeverVisual.getInstance().getFileManager().writeFile("client");
         }
      }
   }

   public void remove(String name) {
      if (name == null || name.isBlank()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.name_required")));
         return;
      }

      if (this.friends.contains(name)) {
         this.friends.remove(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.removed", name)));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.not_exists", name)));
      }

      FeverVisual.getInstance().getFileManager().writeFile("client");
   }

   public void clear() {
      if (this.friends.isEmpty()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.empty")));
      } else {
         this.friends.clear();
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.cleared")));
         FeverVisual.getInstance().getFileManager().writeFile("client");
      }
   }

   public List<String> listFriends() {
      return Collections.unmodifiableList(this.friends);
   }

   public boolean isFriend(String name) {
      return name != null && this.friends.contains(name);
   }

   public void addFriend(String name) {
      this.add(name);
   }

   public void removeFriend(String name) {
      this.remove(name);
   }
}
