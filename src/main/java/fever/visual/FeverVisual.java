package fever.visual;

import lombok.Generated;
import fever.visual.framework.shader.GlProgram;
import fever.visual.systems.commands.CommandRegistry;
import fever.visual.systems.config.ConfigDropHandler;
import fever.visual.systems.config.ConfigManager;
import fever.visual.systems.discord.DiscordManager;
import fever.visual.systems.event.EventManager;
import fever.visual.systems.event.handlers.ServerConnectionHandler;
import fever.visual.systems.file.ClientFile;
import fever.visual.systems.file.FileManager;
import fever.visual.systems.file.impl.ClientDataFile;
import fever.visual.systems.friends.FriendManager;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.ModuleManager;
import fever.visual.systems.modules.listeners.ModuleTickListener;
import fever.visual.systems.modules.listeners.ModuleWidgetRenderer;
import fever.visual.systems.modules.modules.other.SafeMode;
import fever.visual.systems.notifications.NotificationManager;
import fever.visual.systems.theme.ThemeManager;
import fever.visual.systems.waypoints.WayPointsManager;
import fever.visual.ui.hud.Hud;
import fever.visual.utility.game.LabyModUtility;
import fever.visual.utility.game.LunarClientUtility;
import fever.visual.ui.mainmenu.AccountManager;
import fever.visual.ui.menu.MenuScreen;
import fever.visual.utility.game.TitleBarHelper;
import fever.visual.utility.game.MessageUtility;
import fever.visual.utility.game.server.TPSHandler;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.sounds.MusicTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum FeverVisual implements IMinecraft {
   INSTANCE;

   public static final String NAME = "Fever Visual";
   public static final String BUILD_TYPE = "1.21.11 Release";
   public static final String VERSION = "0.9";
   public static final String MOD_ID = "fevervisual";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   private EventManager eventManager;
   private ThemeManager themeManager;
   private ModuleManager moduleManager;
   private CommandRegistry commandManager;
   private FriendManager friendManager;
   private DiscordManager discordManager;
   private MusicTracker musicTracker;
   private FileManager fileManager;
   private NotificationManager notificationManager;
   private ConfigManager configManager;
   private TPSHandler tpsHandler;
   private Hud hud;
   private ServerConnectionHandler serverConnectionHandler;
   private WayPointsManager wayPointsManager;
   private MenuScreen menuScreen;
   private AccountManager accountManager;
   private boolean SafeMode;
   private boolean safeModePending;
   private long safeModeActivationTime;
   private int safeModeLastCountdown = -1;
   private Runnable safeModeActivationAction;
   private Runnable safeModeReturnAction;

   public void initialize() {
      LOGGER.info("Initializing {}...", "FeverVisual");
      this.musicTracker = new MusicTracker();
      this.wayPointsManager = new WayPointsManager();
      this.eventManager = new EventManager();
      this.friendManager = new FriendManager();
      this.themeManager = new ThemeManager();
      this.discordManager = new DiscordManager();
      this.fileManager = new FileManager();
      this.moduleManager = new ModuleManager(new ModuleTickListener(), new ModuleWidgetRenderer());
      this.hud = new Hud();
      this.tpsHandler = new TPSHandler();
      this.notificationManager = new NotificationManager();
      this.accountManager = new AccountManager();
      this.fileManager.registerClientFiles();
      this.moduleManager.registerModules();
      this.moduleManager.enableModules();
      this.configManager = new ConfigManager();
      this.configManager.handle();
      ClientTickEvents.END_CLIENT_TICK.register(client -> {
         if (!this.isSafeMode() && this.configManager != null) {
            this.configManager.tickAutoSave();
         }

         LabyModUtility.tickOverlayReplacement();
         LunarClientUtility.tickOverlayReplacement();
         this.tickSafeModeCountdown();
      });
      this.commandManager = new CommandRegistry();
      this.commandManager.initCommands();
      this.fileManager.loadClientFiles();
      ClientFile clientFile = this.fileManager.getClientFile("client");
      if (clientFile instanceof ClientDataFile clientDataFile) {
         this.configManager.loadLastConfig(clientDataFile.getLastConfigName());
      }
      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         public Identifier getFabricId() {
            return FeverVisual.id("after_shader_load");
         }

         public void reload(ResourceManager manager) {
            DrawUtility.initializeShaders();
            GlProgram.loadAndSetupPrograms();
         }
      });
      Localizator.loadTranslations();
      this.serverConnectionHandler = new ServerConnectionHandler();
      String osName = System.getProperty("os.name");
      String pcName = System.getProperty("user.name");
      if (osName.toLowerCase().contains("windows") && !pcName.equals("sheluvparis")) {
         this.discordManager.connect();
      }

      ConfigDropHandler.init();
      TitleBarHelper.setDarkTitleBar();
      LOGGER.info("{} initialized", "FeverVisual");
   }

   public void shutdown() {
      LOGGER.info("Shutting down...");
      if (!this.isSafeMode() && this.configManager != null) {
         this.configManager.saveCurrent();
      }

      if (this.fileManager != null) {
         this.fileManager.saveClientFiles();
      }

      if (this.musicTracker != null) {
         this.musicTracker.shutdown();
         this.musicTracker = null;
      }
      LabyModUtility.restoreOverlayReplacement();
      LunarClientUtility.restoreOverlayReplacement();
      this.setSafeMode(false);
   }

   public void startSafeModeCountdown(Runnable activationAction, Runnable returnAction) {
      if (this.SafeMode) {
         return;
      }

      this.safeModePending = true;
      this.safeModeActivationTime = System.currentTimeMillis() + 10000L;
      this.safeModeLastCountdown = -1;
      this.safeModeActivationAction = activationAction;
      this.safeModeReturnAction = returnAction;
      this.sendSafeModeCountdown(10);
   }

   public boolean handleSafeModeReturn(String message) {
      if (message == null || !message.trim().equalsIgnoreCase("Return")) {
         return false;
      }

      if (!this.safeModePending && !this.SafeMode) {
         return false;
      }

      this.returnFromSafeMode();
      MessageUtility.info(Text.of("SafeMode отключен. Все функции возвращены."));
      return true;
   }

   private void tickSafeModeCountdown() {
      if (!this.safeModePending) {
         return;
      }

      long remainingMs = this.safeModeActivationTime - System.currentTimeMillis();
      if (remainingMs <= 0L) {
         this.activateSafeModeNow();
         return;
      }

      int remainingSeconds = (int)Math.ceil(remainingMs / 1000.0);
      if (remainingSeconds != this.safeModeLastCountdown) {
         this.sendSafeModeCountdown(remainingSeconds);
      }
   }

   private void activateSafeModeNow() {
      Runnable action = this.safeModeActivationAction;
      this.safeModePending = false;
      this.safeModeActivationAction = null;
      this.safeModeLastCountdown = -1;
      this.SafeMode = true;
      MessageUtility.warn(Text.of("SafeMode включен. Чтобы вернуть напиши в чат Return"));
      if (action != null) {
         action.run();
      }
   }

   private void cancelSafeModeCountdown() {
      this.safeModePending = false;
      this.safeModeActivationAction = null;
      this.safeModeReturnAction = null;
      this.safeModeLastCountdown = -1;
      this.SafeMode = false;
      if (this.moduleManager != null) {
         SafeMode safeMode = this.moduleManager.getModuleSafe(SafeMode.class);
         if (safeMode != null && safeMode.isEnabled()) {
            safeMode.setEnabled(false, true);
         }
      }
   }

   private void returnFromSafeMode() {
      Runnable returnAction = this.safeModeReturnAction;
      this.safeModePending = false;
      this.safeModeActivationAction = null;
      this.safeModeReturnAction = null;
      this.safeModeLastCountdown = -1;
      this.SafeMode = false;
      if (returnAction != null) {
         returnAction.run();
      }

      if (this.moduleManager != null) {
         SafeMode safeMode = this.moduleManager.getModuleSafe(SafeMode.class);
         if (safeMode != null && safeMode.isEnabled()) {
            safeMode.setEnabled(false, true);
         }
      }
   }

   private void sendSafeModeCountdown(int seconds) {
      this.safeModeLastCountdown = seconds;
      MessageUtility.warn(Text.of("SafeMode: отключение через " + seconds + " сек. Чтобы вернуть напиши в чат Return"));
   }

   public static FeverVisual getInstance() {
      return INSTANCE;
   }

   public static Identifier id(String path) {
      return Identifier.of(MOD_ID, path);
   }

   @Generated
   public EventManager getEventManager() {
      return this.eventManager;
   }

   @Generated
   public ThemeManager getThemeManager() {
      return this.themeManager;
   }

   @Generated
   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   @Generated
   public CommandRegistry getCommandManager() {
      return this.commandManager;
   }

   @Generated
   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   @Generated
   public DiscordManager getDiscordManager() {
      return this.discordManager;
   }


   @Generated
   public MusicTracker getMusicTracker() {
      return this.musicTracker;
   }

   @Generated
   public FileManager getFileManager() {
      return this.fileManager;
   }

   @Generated
   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Generated
   public TPSHandler getTpsHandler() {
      return this.tpsHandler;
   }

   @Generated
   public Hud getHud() {
      return this.hud;
   }

   @Generated
   public ServerConnectionHandler getServerConnectionHandler() {
      return this.serverConnectionHandler;
   }

   @Generated
   public WayPointsManager getWayPointsManager() {
      return this.wayPointsManager;
   }

   @Generated
   public MenuScreen getMenuScreen() {
      return this.menuScreen;
   }


   @Generated
   public AccountManager getAccountManager() {
      return this.accountManager;
   }

   @Generated
   public boolean isSafeMode() {
      return this.SafeMode;
   }

   @Generated
   public void setMenuScreen(MenuScreen menuScreen) {
      this.menuScreen = menuScreen;
   }

   @Generated
   public void setSafeMode(boolean SafeMode) {
      if (!SafeMode) {
         this.cancelSafeModeCountdown();
      } else {
         this.SafeMode = true;
      }
   }
}
