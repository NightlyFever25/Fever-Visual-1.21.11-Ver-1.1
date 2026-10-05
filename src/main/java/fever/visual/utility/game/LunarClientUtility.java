package fever.visual.utility.game;

import fever.visual.FeverVisual;
import fever.visual.ui.hud.impl.CustomScoreboard;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class LunarClientUtility {
   private static final ManagedOption SCOREBOARD_ENABLED = new ManagedOption("com.lunarclient.apollo.mods.impl.ModScoreboard", "ENABLED");
   private static final ManagedOption SCOREBOARD_HIDE = new ManagedOption("com.lunarclient.apollo.mods.impl.ModScoreboard", "HIDE_SCOREBOARD");
   private static boolean warned;

   private LunarClientUtility() {
   }

   public static void tickOverlayReplacement() {
      if (!PlatformUtility.isLunarClient()) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      boolean inGame = client != null && client.player != null && client.world != null;
      boolean replaceScoreboard = inGame && CustomScoreboard.shouldReplaceVanilla();

      try {
         Object options = getApolloOptions();
         if (options == null) {
            return;
         }
         SCOREBOARD_ENABLED.sync(options, replaceScoreboard, Boolean.TRUE);
         SCOREBOARD_HIDE.sync(options, replaceScoreboard, Boolean.TRUE);
      } catch (Throwable throwable) {
         if (!warned) {
            warned = true;
            FeverVisual.LOGGER.warn("Failed to synchronize Lunar Client overlay visibility", throwable);
         }
      }
   }

   public static void restoreOverlayReplacement() {
      if (!PlatformUtility.isLunarClient()) {
         return;
      }

      try {
         Object options = getApolloOptions();
         if (options != null) {
            SCOREBOARD_ENABLED.restore(options);
            SCOREBOARD_HIDE.restore(options);
         }
      } catch (Throwable ignored) {
      }
   }

   private static Object getApolloOptions() throws Exception {
      Class<?> apollo = Class.forName("com.lunarclient.apollo.Apollo");
      Object platform = invokeStatic(apollo, "getPlatform");
      return invoke(platform, "getOptions");
   }

   private static Object getOption(String ownerClass, String fieldName) throws Exception {
      Field field = Class.forName(ownerClass).getField(fieldName);
      field.setAccessible(true);
      return field.get(null);
   }

   private static Object getDirectValue(Object options, Object option) throws Exception {
      Class<?> optionClass = Class.forName("com.lunarclient.apollo.option.Option");
      Object value = invoke(options, "getDirect", new Class<?>[]{optionClass}, option);
      if (value instanceof Optional<?> optional) {
         return optional.orElse(null);
      }

      return null;
   }

   private static Object getValue(Object options, Object option) throws Exception {
      Class<?> optionClass = Class.forName("com.lunarclient.apollo.option.Option");
      return invoke(options, "get", new Class<?>[]{optionClass}, option);
   }

   private static void setValue(Object options, Object option, Object value) throws Exception {
      Class<?> optionClass = Class.forName("com.lunarclient.apollo.option.Option");
      invoke(options, "set", new Class<?>[]{optionClass, Object.class}, option, value);
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

   private static final class ManagedOption {
      private final String ownerClass;
      private final String fieldName;
      private Object original;
      private boolean hadDirectOriginal;
      private boolean overridden;

      private ManagedOption(String ownerClass, String fieldName) {
         this.ownerClass = ownerClass;
         this.fieldName = fieldName;
      }

      private void sync(Object options, boolean forceValue, Object value) throws Exception {
         Object option = getOption(this.ownerClass, this.fieldName);
         if (!forceValue) {
            this.restore(options);
            return;
         }

         if (!this.overridden) {
            this.original = getDirectValue(options, option);
            this.hadDirectOriginal = this.original != null;
            if (!this.hadDirectOriginal) {
               this.original = getValue(options, option);
            }
            this.overridden = true;
         }

         setValue(options, option, value);
      }

      private void restore(Object options) throws Exception {
         if (!this.overridden) {
            return;
         }

         Object option = getOption(this.ownerClass, this.fieldName);
         if (this.original != null) {
            setValue(options, option, this.original);
         }
         this.original = null;
         this.hadDirectOriginal = false;
         this.overridden = false;
      }
   }
}
