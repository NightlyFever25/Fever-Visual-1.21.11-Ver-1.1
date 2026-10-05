package fever.visual.systems.commands.commands;

import java.util.List;

import fever.visual.systems.commands.ParameterValidator;
import fever.visual.FeverVisual;
import fever.visual.systems.commands.Command;
import fever.visual.systems.commands.CommandBuilder;
import fever.visual.systems.commands.ParameterBuilder;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.Module;
import fever.visual.utility.game.MessageUtility;
import net.minecraft.text.Text;


public class ToggleCommand {
   
      public Command command() {
            List<String> moduleNames = FeverVisual.getInstance().getModuleManager().getModules().stream()
                        .map(module -> module.getName().replace(" ", "")).toList();
            return CommandBuilder.begin("toggle")
                        .aliases("t")
                        .desc("commands.toggle.description")
                        .param("module",
                                    p -> p.validator(
                                                (ParameterValidator) ParameterBuilder.MODULE)
                                                .suggests(moduleNames))
                        .handler(context -> {
                              Module module = (Module) context.arguments().getFirst();
                              module.toggle();
                              MessageUtility.info(Text.of(Localizator
                                          .translate("commands.toggle." + (module.isEnabled() ? "enabled" : "disabled"),
                                                      module.getName())));
                        })
                        .build();
      }
}
