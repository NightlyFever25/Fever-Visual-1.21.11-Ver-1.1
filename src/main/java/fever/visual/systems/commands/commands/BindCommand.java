package fever.visual.systems.commands.commands;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import fever.visual.FeverVisual;
import fever.visual.systems.commands.Command;
import fever.visual.systems.commands.CommandBuilder;
import fever.visual.systems.commands.CommandContext;
import fever.visual.systems.commands.ParameterBuilder;
import fever.visual.systems.commands.ValidationResult;
import fever.visual.systems.commands.ParameterValidator;
import fever.visual.systems.modules.Module;
import fever.visual.utility.game.MessageUtility;
import fever.visual.utility.game.TextUtility;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;


public class BindCommand {

   public Command command() {
      List<String> moduleNames = FeverVisual.getInstance().getModuleManager().getModules().stream()
            .map(module -> module.getName().replace(" ", "")).toList();
      List<String> keyNames = this.getAvailableKeyNames();
      return CommandBuilder
            .begin("bind", commandBuilder -> commandBuilder.aliases("binds", "бинд").desc("Бинд на модуль"))
            .param("action", p -> p.literal("add", "delete", "remove", "create", "list"))
            .param("module",
                  p -> p.optional().validator((ParameterValidator) ParameterBuilder.MODULE)
                        .suggests(moduleNames))
            .param(
                  "key",
                  p -> p.optional()
                        .validator(text -> (ValidationResult) (text.isBlank() ? ValidationResult.error("key is empty")
                              : ValidationResult.ok(text)))
                        .suggests(keyNames))
            .handler(this::handle)
            .build();
   }


   private void handle(CommandContext context) {
      String action = (String) context.arguments().getFirst();
      Module module = (Module) context.arguments().get(1);
      String keyStr = (String) context.arguments().get(2);
      if (action.equalsIgnoreCase("list")) {
         List<Module> modules = FeverVisual.getInstance().getModuleManager().getModules().stream()
               .filter(mx -> mx.getKey() != -1).toList();
         if (modules.isEmpty()) {
            MessageUtility.info(Text.of("Список биндов пуст"));
         } else {
            MessageUtility.info(Text.of("Список биндов:"));

            for (int i = 0; i < modules.size(); i++) {
               Module m = modules.get(i);
               MessageUtility.info(
                     Text.of(
                           Formatting.GRAY
                                 + "["
                                 + (i + 1)
                                 + "] "
                                 + Formatting.WHITE
                                 + m.getName()
                                 + Formatting.GRAY
                                 + " ("
                                 + TextUtility.getKeyName(m.getKey())
                                 + ")"));
            }
         }
      } else if (module == null) {
         MessageUtility.error(Text.of("Модуль не указан"));
      } else {
         if (!action.equalsIgnoreCase("add") && !action.equalsIgnoreCase("create")) {
            if (action.equalsIgnoreCase("delete") || action.equalsIgnoreCase("remove")) {
               module.setKey(-1);
               MessageUtility.info(Text.of("Бинд удален с модуля " + module.getName()));
            }
         } else {
            if (keyStr == null) {
               MessageUtility.error(Text.of("Клавиша не указана"));
               return;
            }

            int keyCode = this.getKeyCodeFromString(keyStr);
            if (keyCode == -1) {
               MessageUtility.error(Text.of("Неизвестная клавиша: " + keyStr));
               return;
            }

            module.setKey(keyCode);
            MessageUtility.info(Text.of("Бинд установлен на клавишу " + TextUtility.getKeyName(keyCode)));
         }
      }
   }

   private int getKeyCodeFromString(String input) {
      if (input != null && !input.isBlank()) {
         input = input.toUpperCase(Locale.ROOT).replace(" ", "_");

         try {
            return (Integer) GLFW.class.getField("GLFW_KEY_" + input).get(null);
         } catch (Exception var3) {
            return -1;
         }
      } else {
         return -1;
      }
   }


   private List<String> getAvailableKeyNames() {
      return Stream.of(GLFW.class.getFields())
            .map(Field::getName)
            .filter(name -> name.startsWith("GLFW_KEY_"))
            .map(name -> name.substring("GLFW_KEY_".length()))
            .filter(name -> !name.matches("LAST|UNKNOWN|WORLD_\\d+"))
            .collect(Collectors.toList());
   }
}
