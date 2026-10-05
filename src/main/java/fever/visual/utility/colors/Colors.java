package fever.visual.utility.colors;

import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.theme.Theme;
import fever.visual.utility.animation.types.ColorAnimation;
import net.minecraft.util.Util;

public final class Colors {
   public static final ColorRGBA RED = new ColorRGBA(255.0F, 0.0F, 0.0F);
   public static final ColorRGBA GREEN = new ColorRGBA(0.0F, 255.0F, 0.0F);
   public static final ColorRGBA BLUE = new ColorRGBA(0.0F, 0.0F, 255.0F);
   public static final ColorRGBA WHITE = new ColorRGBA(255.0F, 255.0F, 255.0F);
   public static final ColorRGBA BLACK = new ColorRGBA(0.0F, 0.0F, 0.0F);
   public static final ColorRGBA ACCENT = new ColorRGBA(151.0F, 71.0F, 255.0F);
   private static final long ANIMATION_DURATION = 500L;
   private static final ColorAnimation BACKGROUND_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation ADDITIONAL_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation TEXT_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation OUTLINE_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation FLAT_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation ACCENT_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final int CACHE_BUCKET_SHIFT = 2;
   private static long backgroundBucket = Long.MIN_VALUE;
   private static long additionalBucket = Long.MIN_VALUE;
   private static long textBucket = Long.MIN_VALUE;
   private static long outlineBucket = Long.MIN_VALUE;
   private static long flatBucket = Long.MIN_VALUE;
   private static long separatorBucket = Long.MIN_VALUE;
   private static long accentBucket = Long.MIN_VALUE;
   private static ColorRGBA cachedBackground = BLACK;
   private static ColorRGBA cachedAdditional = BLACK;
   private static ColorRGBA cachedText = WHITE;
   private static ColorRGBA cachedOutline = BLACK;
   private static ColorRGBA cachedFlat = BLACK;
   private static ColorRGBA cachedSeparator = BLACK;
   private static ColorRGBA cachedAccent = ACCENT;

   private static long cacheBucket() {
      return Util.getMeasuringTimeMs() >> CACHE_BUCKET_SHIFT;
   }

   private static Theme getTheme() {
      try {
         return FeverVisual.getInstance().getThemeManager().getCurrentTheme();
      } catch (Exception e) {
         return Theme.DARK;
      }
   }

   public static ColorRGBA getBackgroundColor() {
      try {
         long bucket = cacheBucket();
         if (bucket != backgroundBucket) {
            backgroundBucket = bucket;
            cachedBackground = getAnimatedColor(BACKGROUND_COLOR_ANIMATION, getTheme().getBackgroundColor());
         }
         return cachedBackground;
      } catch (Exception e) {
         return BLACK;
      }
   }

   public static ColorRGBA getAdditionalColor() {
      try {
         long bucket = cacheBucket();
         if (bucket != additionalBucket) {
            additionalBucket = bucket;
            cachedAdditional = getAnimatedColor(ADDITIONAL_COLOR_ANIMATION, getTheme().getAdditionalColor());
         }
         return cachedAdditional;
      } catch (Exception e) {
         return BLACK;
      }
   }

   public static ColorRGBA getTextColor() {
      try {
         long bucket = cacheBucket();
         if (bucket != textBucket) {
            textBucket = bucket;
            cachedText = getAnimatedColor(TEXT_COLOR_ANIMATION, getTheme().getTextColor());
         }
         return cachedText;
      } catch (Exception e) {
         return WHITE;
      }
   }

   public static ColorRGBA getOutlineColor() {
      try {
         long bucket = cacheBucket();
         if (bucket != outlineBucket) {
            outlineBucket = bucket;
            cachedOutline = getAnimatedColor(OUTLINE_COLOR_ANIMATION, getTheme().getOutlineColor());
         }
         return cachedOutline;
      } catch (Exception e) {
         return BLACK;
      }
   }

   public static ColorRGBA getFlatColor() {
      try {
         long bucket = cacheBucket();
         if (bucket != flatBucket) {
            flatBucket = bucket;
            cachedFlat = getAnimatedColor(FLAT_COLOR_ANIMATION, getTheme().getFlatColor());
         }
         return cachedFlat;
      } catch (Exception e) {
         return BLACK;
      }
   }

   public static ColorRGBA getSeparatorColor() {
      try {
         long bucket = cacheBucket();
         if (bucket != separatorBucket) {
            separatorBucket = bucket;
            cachedSeparator = ColorRGBA.BLACK.withAlpha(255.0F * (getTheme() == Theme.DARK ? 0.08F : 0.05F));
         }
         return cachedSeparator;
      } catch (Exception e) {
         return BLACK.withAlpha(0.08F);
      }
   }

   public static ColorRGBA getAccentColor() {
      try {
         long bucket = cacheBucket();
         if (bucket != accentBucket) {
            accentBucket = bucket;
            ColorRGBA color = Interface.getAccentColor(0.0F);
            cachedAccent = Interface.isAccentThemeDynamic() ? color : getAnimatedColor(ACCENT_COLOR_ANIMATION, color);
         }
         return cachedAccent;
      } catch (Exception e) {
         return ACCENT;
      }
   }

   public static ColorRGBA getAccentColor(float index) {
      try {
         return Interface.getAccentColor(index);
      } catch (Exception e) {
         return ACCENT;
      }
   }

   public static ColorRGBA getHudTextColor() {
      return getTextColor();
   }

   private static ColorRGBA getAnimatedColor(ColorAnimation animation, ColorRGBA color) {
      if (color == null) {
         return WHITE;
      }
      animation.update(color);
      return animation.getColor();
   }

   @Generated
   private Colors() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
