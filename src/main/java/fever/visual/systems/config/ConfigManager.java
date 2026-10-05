package fever.visual.systems.config;

import fever.visual.FeverVisual;
import fever.visual.systems.file.FileManager;
import fever.visual.systems.localization.Localizator;
import fever.visual.utility.game.MessageUtility;
import lombok.Generated;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class ConfigManager {
   private static final long AUTO_SAVE_INTERVAL_MS = 5L * 60L * 1000L;

   private final List<ConfigFile> configFiles = new ArrayList<>();
   private ConfigFile current;
   private boolean initialized = false;
   private long lastAutoSave = System.currentTimeMillis();

   public void handle() {
      if (!this.initialized) {
         this.scanConfigDirectory();
         this.initialized = true;
      }

      if (this.getConfig("autosave", false) == null) {
         this.createConfig("autosave");
      }
   }

   public void directionConfig() {
      try {
         File configDir = new File(FileManager.DIRECTORY, "configs");
         if (!configDir.exists()) {
            Files.createDirectories(configDir.toPath());
         }

         String[] commands = new String[]{"explorer", configDir.getAbsolutePath()};
         Runtime.getRuntime().exec(commands);
      } catch (Exception exception) {
         FeverVisual.LOGGER.error("Failed to open configs folder: {}", exception.getMessage());
      }
   }

   public void createConfig(String name) {
      String normalizedName = ConfigFile.normalizeFileName(name);
      if (normalizedName.isBlank()) {
         return;
      }

      this.refresh();
      ConfigFile config = this.getConfig(normalizedName);
      if (config == null) {
         config = new ConfigFile(normalizedName);
         this.configFiles.add(config);
      }

      config.save();
   }

   public void listConfigs() {
      this.refresh();
      MessageUtility.info(Text.of(Localizator.translate("configs.list")));

      for (ConfigFile configFile : this.configFiles) {
         int idx = this.configFiles.indexOf(configFile) + 1;
         MessageUtility.info(Text.of("[" + idx + "] " + configFile.getFileName()));
      }
   }

   public void refresh() {
      this.scanConfigDirectory();
   }

   public void saveCurrent() {
      ConfigFile config = this.getAutoSaveConfig();
      if (config != null) {
         config.save();
         this.lastAutoSave = System.currentTimeMillis();
      }
   }

   public void tickAutoSave() {
      long now = System.currentTimeMillis();
      if (now - this.lastAutoSave >= AUTO_SAVE_INTERVAL_MS) {
         this.saveCurrent();
      }
   }

   public void loadLastConfig(String name) {
      this.refresh();
      ConfigFile config = null;
      String normalizedName = ConfigFile.normalizeFileName(name);
      if (!normalizedName.isBlank()) {
         config = this.getConfig(normalizedName);
      }

      if (config == null) {
         config = this.getConfig("autosave");
      }

      if (config != null && config.exists()) {
         config.load(true);
      }
   }

   private void scanConfigDirectory() {
      this.configFiles.clear();
      Path configPath = Paths.get(FileManager.DIRECTORY.getPath(), "configs");
      if (!Files.exists(configPath)) {
         try {
            Files.createDirectories(configPath);
         } catch (IOException exception) {
            FeverVisual.LOGGER.error("Failed to create configs directory: {}", exception.getMessage());
         }

         return;
      }

      Map<String, ConfigFile> foundConfigs = new LinkedHashMap<>();
      try (Stream<Path> stream = Files.list(configPath)) {
         stream.filter(Files::isRegularFile).filter(this::isConfigPath).forEach(path -> {
            String fileName = path.getFileName().toString();
            String name = fileName.substring(0, fileName.lastIndexOf('.'));
            String normalizedName = ConfigFile.normalizeFileName(name);
            boolean cfgFile = fileName.toLowerCase(Locale.ROOT).endsWith("." + ConfigFile.EXTENSION);
            if (cfgFile || !foundConfigs.containsKey(normalizedName.toLowerCase(Locale.ROOT))) {
               foundConfigs.put(normalizedName.toLowerCase(Locale.ROOT), new ConfigFile(normalizedName));
            }
         });
      } catch (IOException exception) {
         FeverVisual.LOGGER.error("Failed to scan configs directory: {}", exception.getMessage());
      }

      this.configFiles.addAll(foundConfigs.values());
   }

   private boolean isConfigPath(Path path) {
      String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
      return lower.endsWith("." + ConfigFile.EXTENSION) || lower.endsWith("." + ConfigFile.LEGACY_EXTENSION);
   }

   public ConfigFile getConfig(String name, boolean rescan) {
      if (rescan) {
         this.scanConfigDirectory();
      }

      String normalizedName = ConfigFile.normalizeFileName(name);
      return this.configFiles.stream().filter(configFile -> configFile.getFileName().equalsIgnoreCase(normalizedName)).findFirst().orElse(null);
   }

   public ConfigFile getConfig(String name) {
      return this.getConfig(name, false);
   }

   public ConfigFile getAutoSaveConfig() {
      ConfigFile config = this.current != null ? this.current : this.getConfig("autosave", false);
      if (config == null && this.initialized) {
         this.createConfig("autosave");
         config = this.getConfig("autosave", true);
      }

      return config;
   }

   @Generated
   public List<ConfigFile> getConfigFiles() {
      return this.configFiles;
   }

   @Generated
   public ConfigFile getCurrent() {
      return this.current;
   }

   @Generated
   public boolean isInitialized() {
      return this.initialized;
   }

   @Generated
   public void setCurrent(ConfigFile current) {
      this.current = current;
   }
}
