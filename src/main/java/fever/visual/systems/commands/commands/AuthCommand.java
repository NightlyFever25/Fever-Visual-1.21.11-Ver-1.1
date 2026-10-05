package fever.visual.systems.commands.commands;

import java.util.Map;
import java.util.Map.Entry;
import fever.visual.FeverVisual;
import fever.visual.systems.commands.Command;
import fever.visual.systems.commands.CommandBuilder;
import fever.visual.systems.commands.CommandContext;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.modules.other.AutoAuth;
import fever.visual.utility.game.MessageUtility;
import net.minecraft.text.Text;


public class AuthCommand {

   public Command command() {
      return CommandBuilder.begin("auth", b -> b.aliases("autoAuth", "пароли", "passwords").desc("commands.auth.description").handler(this::handle)).build();
   }


   private void handle(CommandContext ctx) {
      Map<String, String> map = FeverVisual.getInstance().getModuleManager().getModule(AutoAuth.class).listPassword();
      int counter = 1;
      if (map.isEmpty()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.auth.empty")));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.auth.passwords")));

         for (Entry<String, String> entry : map.entrySet()) {
            String nickname = entry.getKey();
            String password = entry.getValue();
            MessageUtility.info(Text.of(counter++ + ") Ник: " + nickname + " | Пароль: " + password));
         }
      }
   }
}
