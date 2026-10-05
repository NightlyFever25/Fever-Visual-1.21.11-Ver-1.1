package fever.visual.systems.modules.modules.other;

import lombok.Generated;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.network.ReceivePacketEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.sounds.ClientSounds;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "Sounds", enabledByDefault = true, category = ModuleCategory.MISC, desc = "modules.descriptions.sounds")
public class Sounds extends BaseModule {
   private final SliderSetting volume = new SliderSetting(this, "modules.settings.sounds.volume").step(0.1F).min(0.1F).max(1.0F).currentValue(1.0F);
   private final EventListener<ReceivePacketEvent> receivePacket = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet) {
         String msg = packet.content().getString();
         if (msg.contains("Вы успешно купили") || msg.contains("отправлено игроку")) {
            ClientSounds.APPLEPAY.play(this.volume.getCurrentValue(), 1.0F);
         }
      }
   };

   @Generated
   public SliderSetting getVolume() {
      return this.volume;
   }

   @Generated
   public EventListener<ReceivePacketEvent> getReceivePacket() {
      return this.receivePacket;
   }
}