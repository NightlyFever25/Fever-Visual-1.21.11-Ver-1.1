package fever.visual.systems.modules.modules.visuals;

import lombok.Generated;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.SelectSetting;

@ModuleInfo(name = "No Render", category = ModuleCategory.VISUALS, desc = "modules.descriptions.no_render")
public class NoRender extends BaseModule {
   private double oldFovEffectScale;
   private final SelectSetting effects = new SelectSetting(this, "modules.settings.no_render.effects");
   private final SelectSetting.Value hurtCam = new SelectSetting.Value(this.effects, "modules.settings.no_render.hurtCam").select();
   private final SelectSetting.Value scoreboard = new SelectSetting.Value(this.effects, "modules.settings.no_render.scoreboard");
   private final SelectSetting.Value bossBar = new SelectSetting.Value(this.effects, "modules.settings.no_render.bossBar");
   private final SelectSetting.Value portal = new SelectSetting.Value(this.effects, "modules.settings.no_render.portal").select();
   private final SelectSetting.Value pumpkin = new SelectSetting.Value(this.effects, "modules.settings.no_render.pumpkin");
   private final SelectSetting.Value fire = new SelectSetting.Value(this.effects, "modules.settings.no_render.fire").select();
   private final SelectSetting.Value breakParticles = new SelectSetting.Value(this.effects, "modules.settings.no_render.breakParticles");
   private final SelectSetting.Value water = new SelectSetting.Value(this.effects, "modules.settings.no_render.water");
   private final SelectSetting.Value nausea = new SelectSetting.Value(this.effects, "modules.settings.no_render.nausea").select();
   private final SelectSetting.Value fov = new SelectSetting.Value(this.effects, "modules.settings.no_render.fov").select();
   private final SelectSetting.Value weather = new SelectSetting.Value(this.effects, "modules.settings.no_render.weather");
   private final SelectSetting sounds = new SelectSetting(this, "modules.settings.no_render.sounds");
   private final SelectSetting.Value beacon = new SelectSetting.Value(this.sounds, "modules.settings.no_render.beacon");
   private final SelectSetting.Value phantoms = new SelectSetting.Value(this.sounds, "modules.settings.no_render.phantoms");
   private final SelectSetting.Value weatherSound = new SelectSetting.Value(this.sounds, "modules.settings.no_render.weatherSound");
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (this.fov.isSelected()) {
         mc.options.getFovEffectScale().setValue(0.0);
      }
   };

   @Override
   public void onEnable() {
      this.oldFovEffectScale = (Double)mc.options.getFovEffectScale().getValue();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      mc.options.getFovEffectScale().setValue(this.oldFovEffectScale);
      super.onDisable();
   }

   @Generated
   public double getOldFovEffectScale() {
      return this.oldFovEffectScale;
   }

   @Generated
   public SelectSetting getEffects() {
      return this.effects;
   }

   @Generated
   public SelectSetting.Value getHurtCam() {
      return this.hurtCam;
   }

   @Generated
   public SelectSetting.Value getScoreboard() {
      return this.scoreboard;
   }

   @Generated
   public SelectSetting.Value getBossBar() {
      return this.bossBar;
   }

   @Generated
   public SelectSetting.Value getPortal() {
      return this.portal;
   }

   @Generated
   public SelectSetting.Value getPumpkin() {
      return this.pumpkin;
   }

   @Generated
   public SelectSetting.Value getFire() {
      return this.fire;
   }


   @Generated
   public SelectSetting.Value getBreakParticles() {
      return this.breakParticles;
   }

   @Generated
   public SelectSetting.Value getWater() {
      return this.water;
   }

   @Generated
   public SelectSetting.Value getNausea() {
      return this.nausea;
   }

   @Generated
   public SelectSetting.Value getFov() {
      return this.fov;
   }

   @Generated
   public SelectSetting.Value getWeather() {
      return this.weather;
   }

   @Generated
   public SelectSetting getSounds() {
      return this.sounds;
   }

   @Generated
   public SelectSetting.Value getBeacon() {
      return this.beacon;
   }

   @Generated
   public SelectSetting.Value getPhantoms() {
      return this.phantoms;
   }

   @Generated
   public SelectSetting.Value getWeatherSound() {
      return this.weatherSound;
   }

   @Generated
   public EventListener<ClientPlayerTickEvent> getOnUpdateEvent() {
      return this.onUpdateEvent;
   }
}
