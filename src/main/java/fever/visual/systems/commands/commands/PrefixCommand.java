package fever.visual.systems.commands.commands;

import fever.visual.FeverVisual;
import fever.visual.systems.commands.Command;
import fever.visual.systems.commands.CommandBuilder;
import fever.visual.systems.commands.CommandContext;
import fever.visual.systems.commands.CommandRegistry;
import fever.visual.systems.commands.ValidationResult;
import fever.visual.systems.localization.Localizator;
import fever.visual.utility.game.MessageUtility;
import net.minecraft.text.Text;


public class PrefixCommand {

   public Command command() {
      return CommandBuilder.begin(
            "prefix",
            b -> b.desc("commands.prefix.description")
               .param("action", p -> p.optional().literal("list", "clear", "default", "set", "create"))
               .param(
                  "new",
                  p -> p.optional()
                     .validator(
                        text -> (ValidationResult)(text.length() > 1
                           ? ValidationResult.error(Localizator.translate("commands.prefix.invalid_length"))
                           : ValidationResult.ok(text))
                     )
               )
               .handler(this::handle)
         )
         .build();
   }


   private void handle(CommandContext ctx) {
      String action = (String)ctx.arguments().get(0);
      String newPrefix = (String)ctx.arguments().get(1);
      CommandRegistry registry = FeverVisual.getInstance().getCommandManager();
      String current = registry.getPrefix();
      if (action == null) {
         MessageUtility.info(Text.of(Localizator.translate("commands.prefix.current", current)));
      } else {
         String var6 = action.toLowerCase();
         switch (var6) {
            case "list":
               MessageUtility.info(Text.of(Localizator.translate("commands.prefix.current", current)));
               break;
            case "clear":
            case "default":
            case "reset":
               registry.setPrefix(".");
               MessageUtility.info(Text.of(Localizator.translate("commands.prefix.reset")));
               break;
            case "set":
            case "create":
               if (newPrefix == null || newPrefix.isEmpty()) {
                  MessageUtility.error(Text.of(Localizator.translate("commands.prefix.empty")));
                  return;
               }

               registry.setPrefix(newPrefix);
               MessageUtility.info(Text.of(Localizator.translate("commands.prefix.set", newPrefix)));
         }
      }
   }
}
