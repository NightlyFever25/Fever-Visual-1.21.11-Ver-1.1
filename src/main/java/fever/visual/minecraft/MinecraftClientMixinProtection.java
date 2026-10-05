package fever.visual.minecraft;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.ModuleManager;
import fever.visual.systems.modules.modules.other.ClientName;
import fever.visual.utility.game.PlatformUtility;
import fever.visual.utility.sounds.MusicTracker;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MinecraftClientMixinProtection {
   private static String cachedTitle = "Fever Visual -> 1.21.11 Release";
   private static volatile boolean isRunning = true;
   private static ScheduledExecutorService scheduler;
   private static MusicTracker musicTracker;

   public static void init() {
      FeverVisual.INSTANCE.initialize();
      startTitleUpdater();
   }

   public static void shutdown() {
      isRunning = false;
      if (musicTracker != null) {
         musicTracker.shutdown();
         musicTracker = null;
      }

      if (scheduler != null && !scheduler.isShutdown()) {
         scheduler.shutdownNow();
         try {
            scheduler.awaitTermination(500, TimeUnit.MILLISECONDS);
         } catch (InterruptedException ignored) {}
         scheduler = null;
      }

      FeverVisual.INSTANCE.shutdown();
   }

   private static void startTitleUpdater() {
      scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
         Thread t = new Thread(r, "FeverVisual-TitleUpdater");
         t.setDaemon(true);
         return t;
      });
      scheduler.scheduleAtFixedRate(() -> {
         if (isRunning && !FeverVisual.INSTANCE.isSafeMode()) {
            try {
               String newTitle = getWindowTitle();
               if (!newTitle.equals(cachedTitle)) {
                  cachedTitle = newTitle;
                  MinecraftClient client = MinecraftClient.getInstance();
                  if (client != null) {
                     client.execute(() -> applyWindowTitle(client, cachedTitle));
                  }
               } else if (PlatformUtility.isLunarClient()) {
                  MinecraftClient client = MinecraftClient.getInstance();
                  if (client != null) {
                     client.execute(() -> applyWindowTitle(client, cachedTitle));
                  }
               }
            } catch (Exception e) {
               if (isRunning) {
                  e.printStackTrace();
               }
            }
         }
      }, 0, 1, TimeUnit.SECONDS);
   }

   public static void updateTitle(CallbackInfoReturnable<String> cir) {
      if (!FeverVisual.INSTANCE.isSafeMode()) {
         cir.setReturnValue(cachedTitle);
      }
   }

   public static String getCachedTitle() {
      return cachedTitle;
   }

   private static String getWindowTitle() {
      try {
         ModuleManager moduleManager = FeverVisual.INSTANCE.getModuleManager();
         if (moduleManager == null) {
            return "Fever Visual -> 1.21.11 Release | " + getCurrentTime();
         }

         ClientName clientName = moduleManager.getModule(ClientName.class);
         if (clientName != null && clientName.isEnabled()) {
            String customName = clientName.getClientName();
            if (customName != null && !customName.isEmpty()) {
               return formatTitle(customName);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return "Fever Visual -> 1.21.11 Release | " + getCurrentTime();
   }

   private static String formatTitle(String baseTitle) {
      if (baseTitle.contains("%time%")) {
         return baseTitle.replace("%time%", getCurrentTime());
      }
      return baseTitle + " | " + getCurrentTime();
   }

   private static String getCurrentTime() {
      return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
   }

   public static void forceUpdateTitle() {
      if (!FeverVisual.INSTANCE.isSafeMode()) {
         try {
            String newTitle = getWindowTitle();
            cachedTitle = newTitle;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
               client.execute(() -> applyWindowTitle(client, cachedTitle));
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   private static void applyWindowTitle(MinecraftClient client, String title) {
      if (client == null || client.getWindow() == null) {
         return;
      }

      if (PlatformUtility.isLunarClient()) {
         client.getWindow().setTitle(title);
      } else {
         client.updateWindowTitle();
      }
   }
}
