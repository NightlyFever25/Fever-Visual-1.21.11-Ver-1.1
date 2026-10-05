package fever.visual.systems.modules.modules.visuals;

import lombok.Generated;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.network.ReceivePacketEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.game.EntityUtility;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

@ModuleInfo(name = "Ambience", category = ModuleCategory.VISUALS, desc = "modules.descriptions.ambience")
public class   Ambience extends BaseModule {
   public final BooleanSetting endSky = new BooleanSetting(this, "modules.settings.ambience.end_sky");
   private final BooleanSetting customTime = new BooleanSetting(this, "modules.settings.ambience.custom_time");
   private final SliderSetting time = new SliderSetting(this, "modules.settings.ambience.time", () -> !this.customTime.isEnabled())
      .step(1000.0F)
      .min(0.0F)
      .max(24000.0F)
      .currentValue(12000.0F);
   private long oldTime;
   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && this.customTime.isEnabled()) {
         event.cancel();
      }
   };

   @Override
   public void tick() {
      if (mc.world != null) {
         if (this.customTime.isEnabled()) {
            mc.world.getLevelProperties().setTimeOfDay((long)this.time.getCurrentValue());
         }

         super.tick();
      }
   }

   @Override
   public void onEnable() {
      if (EntityUtility.isInGame() && mc.world != null) {
         this.oldTime = mc.world.getTime();
         super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (EntityUtility.isInGame() && mc.world != null) {
         mc.world.getLevelProperties().setTimeOfDay(this.oldTime);
         super.onDisable();
      }
   }

   @Generated
   public BooleanSetting getEndSky() {
      return this.endSky;
   }

   @Generated
   public BooleanSetting getCustomTime() {
      return this.customTime;
   }

   @Generated
   public SliderSetting getTime() {
      return this.time;
   }

   @Generated
   public long getOldTime() {
      return this.oldTime;
   }

   @Generated
   public EventListener<ReceivePacketEvent> getOnReceivePacket() {
      return this.onReceivePacket;
   }
}
