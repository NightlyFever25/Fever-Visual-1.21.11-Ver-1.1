package fever.visual.utility.game;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.CustomChat;
import fever.visual.ui.hud.impl.CustomScoreboard;
import fever.visual.ui.hud.impl.CustomTab;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;

public final class LabyModUtility {
   private static final ManagedBoolean ADVANCED_CHAT = new ManagedBoolean();
   private static final ManagedBoolean CUSTOM_PLAYER_LIST = new ManagedBoolean();
   private static final ManagedBoolean SCOREBOARD_WIDGET = new ManagedBoolean();
   private static boolean warned;

   private LabyModUtility() {
   }

   public static void tickOverlayReplacement() {
      if (!PlatformUtility.isLabyMod()) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      boolean inGame = client != null && client.player != null && client.world != null;
      boolean replaceChat = inGame && CustomChat.isActive();
      boolean replaceTab = inGame && CustomTab.shouldReplaceVanilla();
      boolean replaceScoreboard = inGame && CustomScoreboard.shouldReplaceVanilla();

      try {
         Object api = getLabyApi();
         if (api == null) {
            return;
         }

         Object config = invoke(api, "config");
         Object ingame = invoke(config, "ingame");
         Object multiplayer = invoke(config, "multiplayer");

         boolean hudChanged = false;
         ADVANCED_CHAT.sync(invoke(invoke(ingame, "advancedChat"), "enabled"), replaceChat);
         CUSTOM_PLAYER_LIST.sync(invoke(multiplayer, "customPlayerList"), replaceTab);
         hudChanged |= SCOREBOARD_WIDGET.syncConfigEnabled(getHudWidgetConfig(api, "scoreboard"), replaceScoreboard);

         if (hudChanged) {
            updateHudWidgets(api);
         }
      } catch (Throwable throwable) {
         if (!warned) {
            warned = true;
            FeverVisual.LOGGER.warn("Failed to synchronize LabyMod overlay visibility", throwable);
         }
      }
   }

   public static void restoreOverlayReplacement() {
      if (!PlatformUtility.isLabyMod()) {
         return;
      }

      try {
         Object api = getLabyApi();
         if (api == null) {
            return;
         }

         Object config = invoke(api, "config");
         Object ingame = invoke(config, "ingame");
         Object multiplayer = invoke(config, "multiplayer");

         boolean hudChanged = false;
         ADVANCED_CHAT.restore(invoke(invoke(ingame, "advancedChat"), "enabled"));
         CUSTOM_PLAYER_LIST.restore(invoke(multiplayer, "customPlayerList"));
         hudChanged |= SCOREBOARD_WIDGET.restoreConfigEnabled(getHudWidgetConfig(api, "scoreboard"));

         if (hudChanged) {
            updateHudWidgets(api);
         }
      } catch (Throwable ignored) {
      }
   }

   private static Object getLabyApi() throws Exception {
      Class<?> labyClass = Class.forName("net.labymod.api.Laby");
      Object initialized = invokeStatic(labyClass, "isInitialized");
      if (initialized instanceof Boolean ready && !ready) {
         return null;
      }
      return invokeStatic(labyClass, "labyAPI");
   }

   private static Object getHudWidgetConfig(Object api, String widgetId) throws Exception {
      Object registry = invoke(api, "hudWidgetRegistry");
      if (registry == null) {
         return null;
      }

      Object widget = invoke(registry, "getById", new Class<?>[]{String.class}, widgetId);
      return widget == null ? null : invoke(widget, "getConfig");
   }

   private static void updateHudWidgets(Object api) throws Exception {
      Object registry = invoke(api, "hudWidgetRegistry");
      if (registry != null) {
         invoke(registry, "updateHudWidgets");
      }
   }

   private static Boolean getBooleanProperty(Object property) throws Exception {
      if (property == null) {
         return null;
      }

      Object value = invoke(property, "get");
      return value instanceof Boolean booleanValue ? booleanValue : null;
   }

   private static boolean withPseudoBoolean(Object property, boolean value) throws Exception {
      if (property == null) {
         return false;
      }

      Boolean current = getBooleanProperty(property);
      if (current != null && current == value) {
         return false;
      }

      invoke(property, "withPseudoValue", new Class<?>[]{Object.class}, value);
      return true;
   }

   private static boolean restorePseudoValue(Object property, boolean hadPseudoValue, Object pseudoValue) throws Exception {
      if (property == null) {
         return false;
      }

      if (hadPseudoValue) {
         invoke(property, "withPseudoValue", new Class<?>[]{Object.class}, pseudoValue);
      } else {
         invoke(property, "withoutPseudoValue");
      }
      return true;
   }

   private static boolean hasPseudoValue(Object property) throws Exception {
      if (property == null) {
         return false;
      }

      Object value = invoke(property, "hasPseudoValue");
      return value instanceof Boolean booleanValue && booleanValue;
   }

   private static Boolean getConfigEnabled(Object config) throws Exception {
      if (config == null) {
         return null;
      }

      Object value = invoke(config, "isEnabled");
      return value instanceof Boolean booleanValue ? booleanValue : null;
   }

   private static boolean setConfigEnabled(Object config, boolean value) throws Exception {
      if (config == null) {
         return false;
      }

      Boolean current = getConfigEnabled(config);
      if (current != null && current == value) {
         return false;
      }

      invoke(config, "setEnabled", new Class<?>[]{boolean.class}, value);
      return true;
   }

   private static Object invokeStatic(Class<?> owner, String methodName) throws Exception {
      Method method = owner.getMethod(methodName);
      method.setAccessible(true);
      return method.invoke(null);
   }

   private static Object invoke(Object target, String methodName) throws Exception {
      if (target == null) {
         return null;
      }

      Method method = target.getClass().getMethod(methodName);
      method.setAccessible(true);
      return method.invoke(target);
   }

   private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
      if (target == null) {
         return null;
      }

      Method method = target.getClass().getMethod(methodName, parameterTypes);
      method.setAccessible(true);
      return method.invoke(target, args);
   }

   private static final class ManagedBoolean {
      private Boolean original;
      private Object originalPseudo;
      private boolean hadPseudo;
      private boolean overridden;

      boolean sync(Object property, boolean forceDisabled) throws Exception {
         if (!forceDisabled) {
            restore(property);
            return false;
         }

         if (!overridden) {
            hadPseudo = hasPseudoValue(property);
            originalPseudo = hadPseudo ? getBooleanProperty(property) : null;
            overridden = true;
         }

         return withPseudoBoolean(property, false);
      }

      void restore(Object property) throws Exception {
         if (!overridden) {
            return;
         }

         restorePseudoValue(property, hadPseudo, originalPseudo);
         originalPseudo = null;
         hadPseudo = false;
         original = null;
         overridden = false;
      }

      boolean syncConfigEnabled(Object config, boolean forceDisabled) throws Exception {
         if (!forceDisabled) {
            return restoreConfigEnabled(config);
         }

         Boolean current = getConfigEnabled(config);
         if (!overridden) {
            original = current;
            overridden = true;
         }

         return setConfigEnabled(config, false);
      }

      boolean restoreConfigEnabled(Object config) throws Exception {
         if (!overridden) {
            return false;
         }

         boolean changed = original != null && setConfigEnabled(config, original);
         original = null;
         overridden = false;
         return changed;
      }
   }
}
