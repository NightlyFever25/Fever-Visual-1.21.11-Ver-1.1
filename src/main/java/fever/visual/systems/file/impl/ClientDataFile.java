package fever.visual.systems.file.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fever.visual.FeverVisual;
import fever.visual.systems.config.ConfigFile;
import fever.visual.systems.file.ClientFile;
import fever.visual.systems.file.FileManager;
import fever.visual.systems.file.api.FileInfo;
import fever.visual.systems.setting.Setting;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.components.ColorPicker;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.session.Session;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@FileInfo(
   name = "client"
)
public class ClientDataFile extends ClientFile implements IMinecraft {
   private String lastConfigName = null;

   public String getLastConfigName() {
      return this.lastConfigName;
   }

   @Override
   public void write() {
      JsonObject json = new JsonObject();
      json.addProperty("username", mc.getSession().getUsername());
      json.addProperty("theme", FeverVisual.getInstance().getThemeManager().getCurrentTheme().name());
      json.add("hudElements", this.getHudElementsJsonArray());
      json.add("friends", this.getFriendsJsonArray());
      json.add("colorPickerPresets", this.getColorPickerPresetsJsonArray());
      ConfigFile currentConfig = FeverVisual.getInstance().getConfigManager().getCurrent();
      if (currentConfig != null) {
         json.addProperty("lastConfig", currentConfig.getFileName());
      }

      try (FileWriter writer = new FileWriter(this.file)) {
         writer.write(FileManager.GSON.toJson(json));
      } catch (Exception var81) {
         var81.printStackTrace();
      }
   }

   @Override
   public void read() {
      try (FileReader reader = new FileReader(this.getFile())) {
         JsonObject object = (JsonObject)FileManager.GSON.fromJson(reader, JsonObject.class);
         if (object.has("username")) {
            String username = object.get("username").getAsString();
            new Session(username, UUID.randomUUID(), "", Optional.empty(), Optional.empty());
         }

         if (object.has("theme")) {
            String themeName = object.get("theme").getAsString();

            try {
               Theme theme = Theme.valueOf(themeName);
               FeverVisual.getInstance().getThemeManager().setCurrentTheme(theme);
            } catch (IllegalArgumentException var15) {
               FeverVisual.getInstance().getThemeManager().setCurrentTheme(Theme.DARK);
            }
         }

         if (object.has("friends")) {
            JsonArray friendsArray = object.getAsJsonArray("friends");
            FeverVisual.getInstance().getFriendManager().clear();

            for (JsonElement friendElement : friendsArray) {
               FeverVisual.getInstance().getFriendManager().add(friendElement.getAsString());
            }
         }

         if (object.has("colorPickerPresets")) {
            this.loadColorPickerPresets(object.getAsJsonArray("colorPickerPresets"));
         }

         if (object.has("hudElements")) {
            for (JsonElement elemObj : object.getAsJsonArray("hudElements")) {
               JsonObject elementObject = elemObj.getAsJsonObject();
               String name = elementObject.get("name").getAsString();
               float x = elementObject.get("x").getAsFloat();
               float y = elementObject.get("y").getAsFloat();
               boolean showing = elementObject.get("showing").getAsBoolean();
               HudElement element = FeverVisual.getInstance().getHud().getElementByName(name);
               if (element != null) {
                  element.setX(x);
                  element.setY(y);
                  element.setShowing(showing);
                  if (elementObject.has("settings")) {
                     JsonObject settingsObject = elementObject.getAsJsonObject("settings");

                     for (Setting setting : element.getSettings()) {
                        if (settingsObject.has(setting.getName())) {
                           setting.load(settingsObject.get(setting.getName()));
                        }
                     }
                  }
               }
            }
         }

         if (object.has("lastConfig")) {
            this.lastConfigName = object.get("lastConfig").getAsString();
         }
      } catch (Exception var17) {
         var17.printStackTrace();
      }
   }

   private JsonArray getHudElementsJsonArray() {
      JsonArray hudElementsArray = new JsonArray();

      for (HudElement element : FeverVisual.getInstance().getHud().getElements()) {
         JsonObject elementObject = new JsonObject();
         elementObject.addProperty("name", element.getName());
         elementObject.addProperty("x", element.getX());
         elementObject.addProperty("y", element.getY());
         elementObject.addProperty("showing", element.isShowing());
         elementObject.add("settings", this.getSettingsJsonObject(element));
         hudElementsArray.add(elementObject);
      }

      return hudElementsArray;
   }

   private JsonObject getSettingsJsonObject(HudElement element) {
      JsonObject settingsObject = new JsonObject();

      for (Setting setting : element.getSettings()) {
         settingsObject.add(setting.getName(), setting.save());
      }

      return settingsObject;
   }

   private JsonArray getFriendsJsonArray() {
      JsonArray friendsJsonArray = new JsonArray();

      for (String friendsName : FeverVisual.getInstance().getFriendManager().listFriends()) {
         friendsJsonArray.add(friendsName);
      }

      return friendsJsonArray;
   }

   private JsonArray getColorPickerPresetsJsonArray() {
      JsonArray presetsArray = new JsonArray();

      for (ColorPicker.Preset preset : ColorPicker.COLOR_PRESETS) {
         if (preset.isShowing()) {
            JsonObject presetObject = new JsonObject();
            ColorRGBA color = preset.getColor();
            presetObject.addProperty("red", color.getRed());
            presetObject.addProperty("green", color.getGreen());
            presetObject.addProperty("blue", color.getBlue());
            presetObject.addProperty("alpha", color.getAlpha());
            presetsArray.add(presetObject);
         }
      }

      return presetsArray;
   }

   private void loadColorPickerPresets(JsonArray presetsArray) {
      List<ColorPicker.Preset> loadedPresets = new ArrayList<>();

      for (JsonElement presetElement : presetsArray) {
         JsonObject presetObject = presetElement.getAsJsonObject();
         float red = presetObject.get("red").getAsFloat();
         float green = presetObject.get("green").getAsFloat();
         float blue = presetObject.get("blue").getAsFloat();
         float alpha = presetObject.get("alpha").getAsFloat();
         ColorRGBA color = new ColorRGBA(red, green, blue, alpha);
         loadedPresets.add(new ColorPicker.Preset(color));
      }

      ColorPicker.setColorPresets(loadedPresets);
   }
}
