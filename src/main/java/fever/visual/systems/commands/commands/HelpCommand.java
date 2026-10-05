package fever.visual.systems.commands.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import fever.visual.FeverVisual;
import fever.visual.systems.commands.Command;
import fever.visual.systems.commands.CommandBuilder;
import fever.visual.systems.commands.CommandContext;
import fever.visual.systems.localization.Localizator;
import fever.visual.utility.game.MessageUtility;
import net.minecraft.text.Text;


public class HelpCommand {

   public Command command() {
      return CommandBuilder.begin("help", b -> b.aliases("помощь", "команды", "commands", "helpme").desc("commands.help.description").handler(this::handle))
         .build();
   }


   private void handle(CommandContext ctx) {
      List<Command> list = new ArrayList<>(FeverVisual.getInstance().getCommandManager().commands());
      list.sort(Comparator.comparing(c -> c.names().getFirst(), String.CASE_INSENSITIVE_ORDER));
      List<String> infos = new ArrayList<>();
      int counter = 1;

      for (Command command : list) {
         infos.add(
            String.format(
               "%d) %s%s - %s",
               counter++,
               FeverVisual.getInstance().getCommandManager().getPrefix(),
               command.names().getFirst(),
               Localizator.translate(command.description())
            )
         );
      }

      MessageUtility.info(Text.of("Доступные команды:\n" + String.join("\n", infos)));
   }
}
