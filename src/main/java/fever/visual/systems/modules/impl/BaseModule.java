package fever.visual.systems.modules.impl;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.systems.localization.Language;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.Module;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.modules.other.Sounds;
import fever.visual.systems.modules.modules.visuals.MenuModule;
import fever.visual.systems.notifications.NotificationType;
import fever.visual.systems.policy.ServerPolicy;
import fever.visual.systems.setting.Setting;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.sounds.ClientSounds;

public abstract class BaseModule implements Module {
   private final ModuleInfo info = this.getClass().getAnnotation(ModuleInfo.class);
   private int key;
   private ModuleCategory category;
   private boolean enabled;
   private boolean hidden;
   private String name;
   private List<Setting> settings = new ArrayList<>();
   private final Animation keybindsAnimation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   public BaseModule() {
      this.name = this.info.name();
      this.category = this.info.category();
      this.key = this.info.key();
   }

   @Override
   public void toggle() {
      this.setEnabled(!this.enabled, false);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void tick() {
   }

   @Override
   public void disable() {
      this.setEnabled(false, false);
   }

   @Override
   public void enable() {
      this.setEnabled(true, false);
   }

   @Override
   public void setEnabled(boolean newState, boolean silent) {
      if (newState && !ServerPolicy.getInstance().canEnable(this)) {
         return;
      }
      if (this.enabled != newState) {
         this.enabled = newState;
         if (!(this instanceof MenuModule) && FeverVisual.getInstance().getModuleManager().getModule(Sounds.class).isEnabled() && !silent) {
            ClientSounds.MODULE
               .play(FeverVisual.getInstance().getModuleManager().getModule(Sounds.class).getVolume().getCurrentValue(), this.enabled ? 1.1F : 1.0F);
         }

         if (this.enabled) {
            FeverVisual.getInstance().getEventManager().subscribe(this);
            if (!silent) {
               FeverVisual.getInstance()
                  .getNotificationManager()
                  .addNotification(
                     NotificationType.SUCCESS,
                     this.name.replace(" ", "")
                        + " "
                        + Localizator.translate("enabled")
                        + (Localizator.getCurrentLanguage() == Language.RU_RU ? TextUtility.makeGender(this.name) : "")
                  );
            }

            this.onEnable();
         } else {
            FeverVisual.getInstance().getEventManager().unsubscribe(this);
            if (!silent) {
               FeverVisual.getInstance()
                  .getNotificationManager()
                  .addNotification(
                     NotificationType.ERROR,
                     this.name.replace(" ", "")
                        + " "
                        + Localizator.translate("disabled")
                        + (Localizator.getCurrentLanguage() == Language.RU_RU ? TextUtility.makeGender(this.name) : "")
                  );
            }

            this.onDisable();
         }
      }
   }

   public String getSettingName(String key) {
      return "modules.settings." + this.getName().toLowerCase().replace(" ", "_") + "." + key;
   }

   @Generated
   @Override
   public ModuleInfo getInfo() {
      return this.info;
   }

   @Generated
   @Override
   public int getKey() {
      return this.key;
   }

   @Generated
   @Override
   public ModuleCategory getCategory() {
      return this.category;
   }

   @Generated
   @Override
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   @Override
   public boolean isHidden() {
      return this.hidden;
   }

   @Generated
   @Override
   public String getName() {
      return this.name;
   }

   @Generated
   @Override
   public List<Setting> getSettings() {
      return this.settings;
   }

   @Generated
   @Override
   public Animation getKeybindsAnimation() {
      return this.keybindsAnimation;
   }

   @Generated
   @Override
   public void setKey(int key) {
      this.key = key;
   }

   @Generated
   public void setCategory(ModuleCategory category) {
      this.category = category;
   }

   public void setEnabled(boolean enabled) {
      this.setEnabled(enabled, true);
   }

   @Generated
   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   @Generated
   public void setName(String name) {
      this.name = name;
   }

   @Generated
   public void setSettings(List<Setting> settings) {
      this.settings = settings;
   }
}
