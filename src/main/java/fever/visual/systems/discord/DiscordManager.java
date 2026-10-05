package fever.visual.systems.discord;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.RichPresence;
import fever.visual.utility.interfaces.IMinecraft;

public class DiscordManager implements IMinecraft {
   private final IPCClient client = new IPCClient(1529556061760258070L);
   private final long startTime = System.currentTimeMillis() / 1000L;

   private RichPresence getPresence() {
      return new RichPresence()
              .setDetails("Build: Release v1.1")
              .setState("Telegram: @FeverVisuals")
              .setStartTimestamp(this.startTime)
              .setLargeImageKey("animlogo_v2")
              .setLargeImageText("67")
              .setButtons(new RichPresence.Button("Download", "https://t.me/FeverVisuals"));
   }

   public void connect() {
      this.client.setListener(new IPCListener() {
         @Override
         public void onReady(IPCClient client) {
            client.sendRichPresence(DiscordManager.this.getPresence());
         }
      });
      this.client.connect();
   }
}
