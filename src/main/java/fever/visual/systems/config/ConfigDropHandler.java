package fever.visual.systems.config;

import fever.visual.FeverVisual;
import fever.visual.systems.file.FileManager;
import fever.visual.systems.notifications.NotificationType;
import fever.visual.utility.game.MessageUtility;
import fever.visual.utility.interfaces.IMinecraft;
import lombok.Generated;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWDropCallbackI;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class ConfigDropHandler implements IMinecraft {
   private static boolean initialized;

   public static void init() {
      if (!initialized) {
         initialized = true;
         long handle = mc.getWindow().getHandle();
         GLFWDropCallbackI[] previous = new GLFWDropCallbackI[1];
         GLFWDropCallbackI callback = (window, count, names) -> {
            if (previous[0] != null) {
               previous[0].invoke(window, count, names);
            }

            for (int i = 0; i < count; i++) {
               String path = GLFWDropCallback.getName(names, i);
               handleDrop(path);
            }
         };
         previous[0] = GLFW.glfwSetDropCallback(handle, callback);
      }
   }

   private static void handleDrop(String path) {
      try {
         File src = new File(path);
         if (!src.isFile()) {
            return;
         }

         String srcName = src.getName();
         String lowerName = srcName.toLowerCase(Locale.ROOT);
         boolean cfgExtension = lowerName.endsWith("." + ConfigFile.EXTENSION);
         boolean legacy = lowerName.endsWith("." + ConfigFile.LEGACY_EXTENSION);
         if (!cfgExtension && !legacy) {
            return;
         }

         File destDir = new File(FileManager.DIRECTORY, "configs");
         if (!destDir.exists() && !destDir.mkdirs()) {
            FeverVisual.LOGGER.error("Failed to create directory {}", destDir.getAbsolutePath());
            return;
         }

         String name = srcName.substring(0, srcName.lastIndexOf(46));
         File dest = new File(destDir, name + "." + ConfigFile.EXTENSION);
         Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
         ConfigManager manager = FeverVisual.getInstance().getConfigManager();
         manager.refresh();
         ConfigFile cfg = manager.getConfig(name);
         if (cfg == null) {
            cfg = new ConfigFile(name);
            manager.getConfigFiles().add(cfg);
         }

         cfg.load();
         MessageUtility.info(Text.of("Конфиг " + name + " загружен"));
         FeverVisual.getInstance().getNotificationManager().addNotification(NotificationType.SUCCESS, Text.translatable("configs.loaded").getString());
      } catch (Exception var71) {
         FeverVisual.LOGGER.error("Failed to load dropped config {}", path, var71);
      }
   }

   @Generated
   private ConfigDropHandler() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
