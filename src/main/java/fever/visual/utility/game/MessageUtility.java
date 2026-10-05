package fever.visual.utility.game;

import lombok.Generated;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class MessageUtility implements IMinecraft {
   public static void overlay(MessageUtility.LogLevel logLevel, Text message) {
      log(logLevel, message, true);
   }

   public static void info(Text message) {
      if (mc.player != null) {
         log(MessageUtility.LogLevel.INFO, message, false);
      }
   }

   public static void warn(Text message) {
      log(MessageUtility.LogLevel.WARN, message, false);
   }

   public static void error(Text message) {
      log(MessageUtility.LogLevel.ERROR, message, false);
   }

   private static void log(MessageUtility.LogLevel level, Text message, boolean overlay) {
      if (mc.player != null) {
         Text styledMessage = (Text)message.copy().getWithStyle(Style.EMPTY.withColor(level.getColor().getRGB())).getFirst();
         mc.player.sendMessage(prefix().copy().append(" ").append(styledMessage), overlay);
      }
   }

   private static Text prefix() {
      return Text.of("[%s]".formatted("Fever Visual"))
         .copy()
         .getWithStyle(Style.EMPTY.withColor(Colors.getAccentColor().getRGB()))
         .getFirst();
   }

   @Generated
   private MessageUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static enum LogLevel {
      WARN("Warning", new ColorRGBA(247.0F, 206.0F, 59.0F)),
      ERROR("Error", new ColorRGBA(242.0F, 79.0F, 68.0F)),
      INFO("Info", new ColorRGBA(87.0F, 126.0F, 255.0F));

      private final String level;
      private final ColorRGBA color;

      @Generated
      public String getLevel() {
         return this.level;
      }

      @Generated
      public ColorRGBA getColor() {
         return this.color;
      }

      @Generated
      private LogLevel(final String level, final ColorRGBA color) {
         this.level = level;
         this.color = color;
      }
   }
}
