package fever.visual.utility.game;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public final class PlatformUtility {
   private static Boolean labyMod;
   private static Boolean lunarClient;
   private static boolean labyChatInputOpen;

   private PlatformUtility() {
   }
   public static boolean isLabyMod() {
      if (Boolean.TRUE.equals(labyMod)) {
         return true;
      }

      boolean detected = FabricLoader.getInstance().isModLoaded("labymod") || isClassPresent("net.labymod.api.Laby");
      labyMod = detected;
      return detected;
   }

   public static boolean isLunarClient() {
      if (Boolean.TRUE.equals(lunarClient)) {
         return true;
      }

      boolean detected = FabricLoader.getInstance().isModLoaded("ichor")
         || FabricLoader.getInstance().getAllMods().stream().anyMatch(mod -> {
            String id = lower(mod.getMetadata().getId());
            String name = lower(mod.getMetadata().getName());
            return id.contains("lunar") || name.contains("lunar");
         })
         || isClassPresent("com.moonsworth.lunar.genesis.Genesis")
         || isClassPresent("com.moonsworth.lunar.client.LunarClient")
         || runtimeContains("lunarclient");
      lunarClient = detected;
      return detected;
   }

   public static boolean isChatEditorOpen() {
      Screen screen = MinecraftClient.getInstance().currentScreen;
      if (screen instanceof ChatScreen) {
         return true;
      }

      if (screen != null && classHierarchyContains(screen.getClass(), "chat")) {
         return true;
      }

      return isLabyMod() && (labyChatInputOpen || isLabyChatOpen());
   }

   public static void markChatEditorOpen() {
      if (isLabyMod()) {
         labyChatInputOpen = true;
      }
   }

   public static void markChatEditorClosed() {
      labyChatInputOpen = false;
   }

   private static boolean isClassPresent(String className) {
      try {
         Class.forName(className, false, PlatformUtility.class.getClassLoader());
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }

   private static boolean runtimeContains(String needle) {
      String lowerNeedle = lower(needle);
      return lower(System.getProperty("java.class.path", "")).contains(lowerNeedle)
         || lower(System.getProperty("sun.java.command", "")).contains(lowerNeedle)
         || lower(FabricLoader.getInstance().getGameDir().toString()).contains(lowerNeedle);
   }

   private static String lower(String value) {
      return value == null ? "" : value.toLowerCase(Locale.ROOT);
   }

   private static boolean classHierarchyContains(Class<?> type, String needle) {
      for (Class<?> current = type; current != null; current = current.getSuperclass()) {
         if (current.getName().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
         }
      }
      return false;
   }

   private static boolean isLabyChatOpen() {
      try {
         Class<?> labyClass = Class.forName("net.labymod.api.Laby");
         Object initialized = invokeStatic(labyClass, "isInitialized");
         if (initialized instanceof Boolean ready && !ready) {
            return false;
         }

         Object api = invokeStatic(labyClass, "labyAPI");
         if (hasOpenChatActivity(invoke(invoke(api, "ingameOverlay"), "getActivities"))) {
            return true;
         }

         Object screenOverlayHandler = invoke(api, "screenOverlayHandler");
         if (hasOpenChatActivity(invoke(screenOverlayHandler, "overlays"))) {
            return true;
         }
      } catch (Throwable ignored) {
      }

      return false;
   }

   private static boolean hasOpenChatActivity(Object activities) throws Exception {
      if (!(activities instanceof List<?> list)) {
         return false;
      }

      for (Object activity : list) {
         if (activity != null && classHierarchyContains(activity.getClass(), "chat") && isAcceptingChatInput(activity)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isAcceptingChatInput(Object activity) throws Exception {
      return invokeBoolean(activity, "isChatOpen")
         || invokeBoolean(activity, "isAcceptingInput")
         || invokeBoolean(activity, "isFocused")
         || invokeBoolean(activity, "isActive")
         || invokeBoolean(activity, "isOpen");
   }

   private static boolean invokeBoolean(Object target, String methodName) {
      try {
         Object value = invoke(target, methodName);
         return value instanceof Boolean booleanValue && booleanValue;
      } catch (Throwable ignored) {
         return false;
      }
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
}
