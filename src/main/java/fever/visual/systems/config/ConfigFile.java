package fever.visual.systems.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fever.visual.FeverVisual;
import fever.visual.systems.file.FileManager;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.Module;
import fever.visual.systems.modules.exception.UnknownModuleException;
import fever.visual.systems.modules.modules.other.Sounds;
import fever.visual.systems.modules.modules.visuals.MenuModule;
import fever.visual.systems.notifications.NotificationType;
import fever.visual.systems.policy.ServerPolicy;
import fever.visual.systems.setting.Setting;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.game.MessageUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.sounds.ClientSounds;
import lombok.Generated;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public class ConfigFile implements IMinecraft {
   public static final String EXTENSION = "cfg";
   public static final String LEGACY_EXTENSION = "fever";

   private final List<Module> modules = FeverVisual.getInstance().getModuleManager().getModules();
   private final File file;
   private final File legacyFile;
   private final String fileName;

   public ConfigFile(String fileName) {
      this.fileName = normalizeFileName(fileName);
      File configsFolder = new File(FileManager.DIRECTORY, "configs");
      if (!configsFolder.exists() && !configsFolder.mkdirs()) {
         FeverVisual.LOGGER.error("Failed to create configs folder: {}", configsFolder.getAbsolutePath());
      }

      this.file = new File(configsFolder, this.fileName + "." + EXTENSION);
      this.legacyFile = new File(configsFolder, this.fileName + "." + LEGACY_EXTENSION);
   }

   public void load() {
      this.load(false);
   }

   public void load(boolean silent) {
      File readableFile = this.getReadableFile();
      if (!readableFile.exists()) {
         FeverVisual.LOGGER.warn("Config file not found: {}", this.file.getAbsolutePath());
         return;
      }

      try (BufferedReader reader = Files.newBufferedReader(readableFile.toPath(), StandardCharsets.UTF_8)) {
         JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
         int loadedModules = this.loadModules(jsonObject);
         this.loadHudElements(jsonObject);
          this.loadTheme(jsonObject);
          ServerPolicy.getInstance().enforceBlocked();
          FeverVisual.getInstance().getConfigManager().setCurrent(this);

         if (!silent) {
            this.playLoadFeedback();
         }

         FeverVisual.LOGGER.info("Loaded {} modules from config {}", loadedModules, this.fileName);
      } catch (Exception exception) {
         FeverVisual.LOGGER.error("Failed to load config file {}: {}", this.fileName, exception.getMessage());
      }
   }

   public void save() {
      try {
         Files.createDirectories(this.file.toPath().getParent());
         JsonObject json = new JsonObject();
         json.addProperty("format", 2);
         json.addProperty("theme", FeverVisual.getInstance().getThemeManager().getCurrentTheme().name());
         json.add("modules", this.getModulesJsonArray());
         json.add("hudElements", this.getHudElementsJsonArray());

         File tempFile = new File(this.file.getParentFile(), this.file.getName() + ".tmp");
         try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
            writer.write(FileManager.GSON.toJson(json));
         }

         try {
            Files.move(tempFile.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tempFile.toPath(), this.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
         }

         FeverVisual.getInstance().getConfigManager().setCurrent(this);
         FeverVisual.LOGGER.info("Successfully saved config {}", this.fileName);
      } catch (IOException exception) {
         FeverVisual.LOGGER.error("Failed to save config file", exception);
      }
   }

   public void delete() {
      boolean deleted = this.deleteFile(this.file) | this.deleteFile(this.legacyFile);
      if (deleted) {
         FeverVisual.getInstance().getConfigManager().getConfigFiles().remove(this);
         if (FeverVisual.getInstance().getConfigManager().getCurrent() == this) {
            FeverVisual.getInstance().getConfigManager().setCurrent(null);
         }

         MessageUtility.info(Text.of(Localizator.translate("commands.config.deleted", this.fileName)));
         FeverVisual.LOGGER.info("Config file deleted: {}", this.fileName);
      } else {
         MessageUtility.error(Text.of(Localizator.translate("commands.config.delete_error")));
         FeverVisual.LOGGER.warn("Failed to delete config file: {}", this.fileName);
      }
   }

   public boolean exists() {
      return this.file.exists() || this.legacyFile.exists();
   }

   private int loadModules(JsonObject jsonObject) {
      if (!jsonObject.has("modules") || !jsonObject.get("modules").isJsonArray()) {
         FeverVisual.LOGGER.warn("Invalid config format: missing 'modules' array in {}", this.fileName);
         return 0;
      }

      int loadedModules = 0;
      JsonArray modulesArray = jsonObject.getAsJsonArray("modules");
      for (JsonElement moduleElement : modulesArray) {
         if (!moduleElement.isJsonObject()) {
            continue;
         }

         JsonObject moduleObject = moduleElement.getAsJsonObject();
         if (!moduleObject.has("name")) {
            continue;
         }

         String moduleName = moduleObject.get("name").getAsString();
         boolean enabled = moduleObject.has("enabled") && moduleObject.get("enabled").getAsBoolean();
         int key = moduleObject.has("key") ? moduleObject.get("key").getAsInt() : 0;

         try {
            Module module = FeverVisual.getInstance().getModuleManager().getModule(moduleName);
            if (!(module instanceof MenuModule)) {
               module.setEnabled(enabled, true);
               module.setKey(key);
            }

            if (moduleObject.has("settings") && moduleObject.get("settings").isJsonObject()) {
               this.loadSettings(module.getSettings(), moduleObject.getAsJsonObject("settings"), module.getName());
            }

            loadedModules++;
         } catch (UnknownModuleException ignored) {
         } catch (Exception exception) {
            FeverVisual.LOGGER.warn("Failed to load module {} from config {}: {}", moduleName, this.fileName, exception.getMessage());
         }
      }

      return loadedModules;
   }

   private void loadHudElements(JsonObject jsonObject) {
      if (!jsonObject.has("hudElements") || !jsonObject.get("hudElements").isJsonArray()) {
         return;
      }

      for (JsonElement element : jsonObject.getAsJsonArray("hudElements")) {
         if (!element.isJsonObject()) {
            continue;
         }

         JsonObject elementObject = element.getAsJsonObject();
         if (!elementObject.has("name")) {
            continue;
         }

         HudElement hudElement = FeverVisual.getInstance().getHud().getElementByName(elementObject.get("name").getAsString());
         if (hudElement == null) {
            continue;
         }

         if (elementObject.has("x")) {
            hudElement.setX(elementObject.get("x").getAsFloat());
         }

         if (elementObject.has("y")) {
            hudElement.setY(elementObject.get("y").getAsFloat());
         }

         if (elementObject.has("showing")) {
            hudElement.setShowing(elementObject.get("showing").getAsBoolean());
         }

         if (elementObject.has("settings") && elementObject.get("settings").isJsonObject()) {
            this.loadSettings(hudElement.getSettings(), elementObject.getAsJsonObject("settings"), hudElement.getName());
         }
      }
   }

   private void loadTheme(JsonObject jsonObject) {
      if (!jsonObject.has("theme")) {
         return;
      }

      try {
         Theme theme = Theme.valueOf(jsonObject.get("theme").getAsString());
         FeverVisual.getInstance().getThemeManager().setCurrentTheme(theme);
      } catch (IllegalArgumentException exception) {
         FeverVisual.LOGGER.warn("Unknown theme in config {}: {}", this.fileName, jsonObject.get("theme").getAsString());
      }
   }

   private void loadSettings(List<Setting> settings, JsonObject settingsObject, String ownerName) {
      for (Setting setting : settings) {
         if (!settingsObject.has(setting.getName())) {
            continue;
         }

         try {
            setting.load(settingsObject.get(setting.getName()));
         } catch (Exception exception) {
            FeverVisual.LOGGER.warn("Failed to load setting {} for {}: {}", setting.getName(), ownerName, exception.getMessage());
         }
      }
   }

   private JsonArray getModulesJsonArray() {
      JsonArray modulesJsonArray = new JsonArray();

      for (Module module : this.modules) {
         JsonObject moduleObject = new JsonObject();
         moduleObject.addProperty("name", module.getName());
         moduleObject.addProperty("enabled", module.isEnabled());
         moduleObject.addProperty("key", module.getKey());
         moduleObject.add("settings", this.getSettingsJsonObject(module.getSettings()));
         modulesJsonArray.add(moduleObject);
      }

      return modulesJsonArray;
   }

   private JsonArray getHudElementsJsonArray() {
      JsonArray hudElementsArray = new JsonArray();

      for (HudElement element : FeverVisual.getInstance().getHud().getElements()) {
         JsonObject elementObject = new JsonObject();
         elementObject.addProperty("name", element.getName());
         elementObject.addProperty("x", element.getX());
         elementObject.addProperty("y", element.getY());
         elementObject.addProperty("showing", element.isShowing());
         elementObject.add("settings", this.getSettingsJsonObject(element.getSettings()));
         hudElementsArray.add(elementObject);
      }

      return hudElementsArray;
   }

   private JsonObject getSettingsJsonObject(List<Setting> settings) {
      JsonObject settingsObject = new JsonObject();

      for (Setting setting : settings) {
         settingsObject.add(setting.getName(), setting.save());
      }

      return settingsObject;
   }

   private File getReadableFile() {
      return this.file.exists() ? this.file : this.legacyFile;
   }

   private boolean deleteFile(File target) {
      return target.exists() && target.delete();
   }

   private void playLoadFeedback() throws UnknownModuleException {
      ClientSounds.MODULE.play(FeverVisual.getInstance().getModuleManager().getModule(Sounds.class).getVolume().getCurrentValue(), 1.0F);
      FeverVisual.getInstance().getNotificationManager().addNotification(NotificationType.SUCCESS, Localizator.translate("configs.loaded"));
   }

   public static String normalizeFileName(String fileName) {
      if (fileName == null) {
         return "";
      }

      String normalized = fileName.trim();
      String lower = normalized.toLowerCase(Locale.ROOT);
      if (lower.endsWith("." + EXTENSION)) {
         normalized = normalized.substring(0, normalized.length() - EXTENSION.length() - 1);
      } else if (lower.endsWith("." + LEGACY_EXTENSION)) {
         normalized = normalized.substring(0, normalized.length() - LEGACY_EXTENSION.length() - 1);
      }

      return normalized;
   }

   @Generated
   public String getFileName() {
      return this.fileName;
   }
}
