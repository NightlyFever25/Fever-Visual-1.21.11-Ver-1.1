package fever.visual.systems.commands.commands;

import java.util.List;
import fever.visual.FeverVisual;
import fever.visual.systems.commands.Command;
import fever.visual.systems.commands.CommandBuilder;
import fever.visual.systems.commands.CommandContext;
import fever.visual.systems.commands.ValidationResult;
import fever.visual.systems.friends.FriendManager;
import fever.visual.systems.localization.Localizator;
import fever.visual.utility.game.MessageUtility;
import net.minecraft.text.Text;


public class FriendCommand {

   public Command command() {
      return CommandBuilder.begin(
            "friend",
            b -> b.aliases("friends")
               .desc("commands.friends.description")
               .param("action", p -> p.literal("add", "remove", "del", "delete", "clear", "list"))
               .param("id", p -> p.optional().validator(ValidationResult::ok))
               .handler(this::handle)
         )
         .build();
   }


   private void handle(CommandContext ctx) {
      String action = (String)ctx.arguments().get(0);
      String id = (String)ctx.arguments().get(1);
      FriendManager fm = FeverVisual.getInstance().getFriendManager();
      String var5 = action.toLowerCase();
      switch (var5) {
         case "add":
            if (this.requireName(id)) {
               return;
            }

            fm.add(id);
            break;
         case "remove":
         case "del":
         case "delete":
            if (this.requireName(id)) {
               return;
            }

            fm.remove(id);
            break;
         case "clear":
            fm.clear();
            break;
         case "list":
            this.printList();
      }
   }

   private boolean requireName(String id) {
      if (id == null || id.isBlank()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.name_required")));
         return true;
      }

      return false;
   }

   private void printList() {
      List<String> friends = FeverVisual.getInstance().getFriendManager().listFriends();
      if (friends.isEmpty()) {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.empty")));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.list")));

         for (int i = 0; i < friends.size(); i++) {
            MessageUtility.info(Text.of("[" + (i + 1) + "] " + friends.get(i)));
         }
      }
   }
}
