package fever.visual.systems.modules.modules.other;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.network.ReceivePacketEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.StringSetting;
import fever.visual.utility.game.TextUtility;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ModuleInfo(name = "Auto Auth", category = ModuleCategory.MISC, desc = "modules.descriptions.auto_auth")
public class AutoAuth extends BaseModule {
   private final BooleanSetting random = new BooleanSetting(this, "modules.settings.auto_auth.random");
   private final StringSetting password = new StringSetting(this, "modules.settings.auto_auth.password", this.random::isEnabled).text("123123");
   private final Map<String, String> nickAndPassword = new HashMap<>();
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet && mc.player != null) {
         String message = packet.content().getString().toLowerCase();
         String randomPass = TextUtility.getRandomNick();
         String password = this.random.isEnabled() ? randomPass : this.password.getText();
         this.nickAndPassword.put(mc.player.getDisplayName().getString(), " " + randomPass);
         if (!message.contains("зарегистрируйтесь") && !message.contains("/reg")) {
            if (message.contains("авторизуйтесь") || message.contains("/login") || message.contains("/l") && message.matches("/l(\\s|$)")) {
               mc.player.networkHandler.sendChatCommand(String.format("l %s", password));
            }
         } else {
            mc.player.networkHandler.sendChatCommand(String.format("reg %s %s", password, password));
         }
      }
   };

   public Map<String, String> listPassword() {
      return Collections.unmodifiableMap(this.nickAndPassword);
   }

   public void put(String key, String value) {
      this.nickAndPassword.put(key, value);
   }
}