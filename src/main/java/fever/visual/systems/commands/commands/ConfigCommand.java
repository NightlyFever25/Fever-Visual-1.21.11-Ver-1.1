package fever.visual.systems.commands.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import fever.visual.FeverVisual;
import fever.visual.systems.commands.*;
import fever.visual.systems.config.ConfigFile;
import fever.visual.systems.config.ConfigManager;
import fever.visual.systems.localization.Localizator;
import fever.visual.utility.game.MessageUtility;
import net.minecraft.text.Text;

public final class ConfigCommand {
   private static final ParameterValidator<String> CONFIG_NAME = ValidationResult::ok;
   public Command command() {
      List<String> configNames = FeverVisual.getInstance().getConfigManager().getConfigFiles().stream()
              .map(ConfigFile::getFileName).toList();
      return CommandBuilder.begin(
                      "config",
                      b -> b.aliases("cfg", "кфг", "конфиг")
                              .desc("commands.config.description")
                              .param(
                                      "action",
                                      p -> p.validator(
                                                      text -> ConfigCommand.Action.from(text)
                                                              .map(a -> (ValidationResult) ValidationResult.ok(a))
                                                              .orElseGet(() -> ValidationResult
                                                                      .error(Localizator.translate("commands.config.invalid_action"))))
                                              .suggests(ConfigCommand.Action.allNames()))
                              .param("id", p -> p.optional().validator((ParameterValidator) CONFIG_NAME).suggests(configNames))
                              .handler(this::handle))
              .build();
   }
   private void handle(CommandContext ctx) {
      ConfigCommand.Action action = (ConfigCommand.Action) ctx.arguments().get(0);
      String id = (String) ctx.arguments().get(1);
      action.createHandler().accept(id);
   }

   private static enum Action {
      SAVE("save", "create", "add", "сохранить", "ыфму"),
      REMOVE("delete", "remove", "del", "удалить", "вудуеу"),
      LIST("list", "дшые"),
      LOAD("load", "use", "использовать", "дщфв"),
      DIR("dir", "direction");

      private final List<String> names;

      private Action(String... names) {
         this.names = Arrays.stream(names).map(String::toLowerCase).collect(Collectors.toList());
      }

      private Consumer<String> createHandler() {
         return switch (this) {
            case SAVE -> this::saveConfig;
            case REMOVE -> s -> {
               if (s != null) {
                  ConfigFile config = FeverVisual.getInstance().getConfigManager().getConfig(s, true);
                  if (config != null) {
                     config.delete();
                  }
               }
            };
            case LIST -> s -> FeverVisual.getInstance().getConfigManager().listConfigs();
            case LOAD -> s -> {
               ConfigManager configManager = FeverVisual.getInstance().getConfigManager();
               configManager.saveCurrent();
               configManager.refresh();
               if (s != null && configManager.getConfig(s) != null) {
                  configManager.getConfig(s).load();
               }
            };
            case DIR -> s -> FeverVisual.getInstance().getConfigManager().directionConfig();
         };
      }
      private void saveConfig(String configName) {
         if (configName != null) {
            ConfigManager configManager = FeverVisual.getInstance().getConfigManager();
            configManager.createConfig(configName);
            MessageUtility.info(Text.of(Localizator.translate("commands.config.saved", configName)));
         }
      }
      static Optional<ConfigCommand.Action> from(String input) {
         String key = input.toLowerCase();
         return Arrays.stream(values()).filter(a -> a.names.contains(key)).findFirst();
      }
      static List<String> allNames() {
         return Arrays.stream(values()).map(a -> a.names.getFirst()).collect(Collectors.toList());
      }
   }
}
