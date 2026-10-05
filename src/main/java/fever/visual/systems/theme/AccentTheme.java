package fever.visual.systems.theme;

import fever.visual.utility.colors.ColorRGBA;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum AccentTheme {
   CUSTOM("custom", 10, rgb(255, 223, 255), rgb(255, 255, 94)),
   RAINBOW("rainbow", 10, true, false, rgb(119, 102, 253), rgb(209, 205, 253)),
   ASTOLFO("astolfo", 10, false, true, rgb(119, 102, 253), rgb(209, 205, 253)),
   DEFAULT("default", 5, rgb(119, 102, 253), rgb(209, 205, 253)),
   WHALE("whale", 5, rgb(155, 162, 255), rgb(78, 85, 163)),
   NIGHTLY("nightly", 5, rgb(38, 18, 111), rgb(55, 143, 255)),
   GRAPE("grape", 5, rgb(52, 253, 95), rgb(151, 57, 253)),
   LIGHT_WEIGHT("light_weight", 5, rgb(1, 202, 150), rgb(2, 60, 251)),
   CHERRY("cherry", 5, rgb(251, 135, 141), rgb(255, 31, 31)),
   CANDY("candy", 5, rgb(253, 40, 225), rgb(0, 216, 253)),
   GRASS("grass", 5, rgb(10, 93, 31), rgb(85, 175, 97)),
   RED("red", 5, rgb(127, 0, 0), rgb(255, 31, 87)),
   SUN_SET("sun_set", 5, rgb(255, 197, 63), rgb(255, 31, 87)),
   BANANA_MILK("banana_milk", 5, rgb(253, 203, 0), rgb(253, 239, 193)),
   SEA_MELODY("sea_melody", 5, rgb(12, 107, 253), rgb(175, 191, 253)),
   FADE("fade", 5, rgb(253, 114, 0), rgb(83, 14, 123)),
   DUSK("dusk", 5, rgb(44, 62, 80), rgb(249, 115, 108)),
   AMETHYST("amethyst", 5, rgb(143, 99, 204), rgb(99, 67, 139)),
   PEAR("pear", 5, rgb(63, 188, 0), rgb(195, 253, 131)),
   MOONADA("moonada", 5, rgb(98, 101, 251), rgb(164, 225, 212)),
   CINNAMINT("cinnamint", 5, rgb(170, 243, 226), rgb(67, 190, 151)),
   RASPBERRY("raspberry", 5, rgb(253, 214, 184), rgb(253, 58, 151)),
   SILVER("silver", 5, rgb(253, 253, 253), rgb(139, 139, 139)),
   CLASSIC("classic", 5, rgb(0, 0, 0), rgb(253, 253, 253)),
   PEACH("peach", 5, rgb(253, 68, 0), rgb(253, 149, 149)),
   PEACH_LIME("peach_lime", 5, rgb(251, 118, 125), rgb(193, 221, 71)),
   DUBAI_CHOCOLATE("dubai_chocolate", 5, rgb(88, 77, 122), rgb(179, 221, 71)),
   AURORA("aurora", 5, rgb(138, 147, 243), rgb(180, 165, 255)),
   BLUE_PINK("blue_pink", 5, rgb(72, 226, 228), rgb(214, 81, 239)),
   BLUE_GREEN("blue_green", 5, rgb(13, 231, 88), rgb(2, 104, 224)),
   DARK_BLUE("dark_blue", 5, rgb(61, 101, 222), rgb(33, 15, 89)),
   POLARIZE("polarize", 5, rgb(200, 200, 200), rgb(123, 123, 123)),
   WATER("water", 5, rgb(93, 154, 236), rgb(11, 64, 200)),
   VIOLET("violet", 5, rgb(144, 25, 152), rgb(67, 19, 112)),
   LILAC("lilac", 5, rgb(192, 137, 218), rgb(105, 45, 134)),
   SUNRISE("sunrise", 5, rgb(245, 120, 54), rgb(214, 57, 160)),
   SOLAR_FLARE("solar_flare", 5, rgb(255, 122, 0), rgb(142, 45, 226)),
   SPRING("spring", 5, rgb(168, 224, 99), rgb(86, 171, 47)),
   SUMMER("summer", 5, rgb(255, 226, 89), rgb(255, 167, 81)),
   WINTER("winter", 5, rgb(224, 234, 252), rgb(207, 222, 243)),
   MIDNIGHT("midnight", 5, rgb(15, 32, 39), rgb(44, 83, 100)),
   HALLOWEEN("halloween", 5, rgb(255, 117, 24), rgb(26, 26, 26)),
   NEW_YEAR("new_year", 5, rgb(30, 60, 114), rgb(192, 57, 43)),
   VALENTINE("valentine", 5, rgb(255, 107, 157), rgb(196, 69, 105)),
   FIRE("fire", 5, rgb(255, 210, 0), rgb(255, 0, 0)),
   EARTH("earth", 5, rgb(139, 90, 43), rgb(62, 39, 35)),
   ICE("ice", 5, rgb(161, 255, 206), rgb(0, 180, 219)),
   FOREST("forest", 5, rgb(19, 78, 19), rgb(46, 125, 50)),
   GALAXY("galaxy", 5, rgb(15, 12, 41), rgb(138, 43, 226)),
   DESERT("desert", 5, rgb(232, 195, 158), rgb(184, 134, 11)),
   GOLD("gold", 5, rgb(255, 246, 183), rgb(184, 134, 11)),
   EMERALD("emerald", 5, rgb(80, 200, 120), rgb(3, 102, 48)),
   CORAL("coral", 5, rgb(255, 127, 80), rgb(233, 68, 106)),
   MINT("mint", 5, rgb(182, 255, 219), rgb(31, 166, 122)),
   PASTEL("pastel", 5, rgb(255, 209, 220), rgb(193, 240, 240)),
   TEAL("teal", 5, rgb(15, 155, 142), rgb(0, 92, 87)),
   BLOODY("bloody", 5, rgb(185, 54, 98), rgb(103, 28, 27)),
   NEON("neon", 5, rgb(255, 33, 0), rgb(0, 233, 255)),
   AUTUMN("autumn", 5, rgb(0, 233, 255), rgb(192, 74, 0)),
   CHRISTMAS("christmas", 5, rgb(223, 11, 11), rgb(220, 203, 203)),
   REVOLUT("revolut", 5, rgb(76, 208, 94), rgb(120, 68, 164)),
   WATERMELON("watermelon", 5, rgb(205, 71, 51), rgb(164, 190, 114));

   private static final float DEFAULT_THEME_CYCLE_SECONDS = 6.5F;
   private static final Map<String, AccentTheme> BY_TRANSLATION_KEY = createLookup();

   private final String id;
   private final int speed;
   private final boolean rainbow;
   private final boolean astolfo;
   private final int[] colors;

   AccentTheme(String id, int speed, int... colors) {
      this(id, speed, false, false, colors);
   }

   AccentTheme(String id, int speed, boolean rainbow, boolean astolfo, int... colors) {
      this.id = id;
      this.speed = speed;
      this.rainbow = rainbow;
      this.astolfo = astolfo;
      this.colors = colors;
   }

   public String getTranslationKey() {
      return "modules.settings.interface.accent_theme." + this.id;
   }

   public ColorRGBA getColor(float index) {
      return this.getColor(index, DEFAULT_THEME_CYCLE_SECONDS);
   }

   public ColorRGBA getColor(float index, float cycleSeconds) {
      if (this.rainbow) {
         return rainbow(index, cycleSeconds);
      }

      if (this.astolfo) {
         return astolfo(index, cycleSeconds);
      }

      return fromArgb(gradientSeconds(cycleSeconds, index, this.colors));
   }

   public ColorRGBA getPreviewStart() {
      if (this.rainbow || this.astolfo) {
         return this.getColor(0.0F);
      }

      return fromArgb(this.colors[0]);
   }

   public ColorRGBA getPreviewEnd() {
      if (this.rainbow || this.astolfo) {
         return this.getColor(90.0F);
      }

      return fromArgb(this.colors[Math.min(1, this.colors.length - 1)]);
   }

   public static ColorRGBA customColor(float index, ColorRGBA first, ColorRGBA second) {
      return customColor(index, first, second, DEFAULT_THEME_CYCLE_SECONDS);
   }

   public static ColorRGBA customColor(float index, ColorRGBA first, ColorRGBA second, float cycleSeconds) {
      return fromArgb(gradientSeconds(cycleSeconds, index, first.getRGB(), second.getRGB()));
   }

   public static ColorRGBA fastRainbow(float index) {
      return rainbow(index, 1.8F);
   }

   public static ColorRGBA rainbow(float index, float cycleSeconds) {
      float hue = normalizedCycle(cycleSeconds, index);
      int rgb = java.awt.Color.HSBtoRGB(hue, 0.55F, 1.0F);
      return new ColorRGBA(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255);
   }

   public static ColorRGBA astolfo(float index, float cycleSeconds) {
      float hue = normalizedCycle(cycleSeconds, index);
      return fromArgb(fromHsb(hue < 0.5F ? -hue : hue, 0.5F, 1.0F, 1.0F));
   }

   public static AccentTheme byTranslationKey(String key) {
      if (key == null) {
         return RAINBOW;
      }
      return BY_TRANSLATION_KEY.getOrDefault(key.toLowerCase(Locale.ROOT), RAINBOW);
   }

   private static Map<String, AccentTheme> createLookup() {
      Map<String, AccentTheme> lookup = new HashMap<>();
      for (AccentTheme theme : values()) {
         lookup.put(theme.getTranslationKey().toLowerCase(Locale.ROOT), theme);
      }
      return Map.copyOf(lookup);
   }

   private int gradient(float index) {
      return gradientSeconds(DEFAULT_THEME_CYCLE_SECONDS, index, this.colors);
   }

   private static int gradient(int speed, float index, int... colors) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360.0F);
      angle = (angle > 180 ? 360 - angle : angle) + 180;
      int colorIndex = (int)(angle / 360.0F * colors.length);
      if (colorIndex == colors.length) {
         colorIndex--;
      }

      int color1 = colors[colorIndex];
      int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
      return interpolateColor(color1, color2, angle / 360.0F * colors.length - colorIndex);
   }

   private static int gradientSeconds(float cycleSeconds, float index, int... colors) {
      if (colors.length == 0) {
         return rgb(255, 255, 255);
      }

      if (colors.length == 1) {
         return colors[0];
      }

      float phase = normalizedCycle(cycleSeconds, index);
      float scaled = phase * colors.length;
      int colorIndex = (int)Math.floor(scaled);
      if (colorIndex >= colors.length) {
         colorIndex = colors.length - 1;
      }

      int color1 = colors[colorIndex];
      int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
      float amount = smoothStep(scaled - colorIndex);
      return interpolateColor(color1, color2, amount);
   }

   private static float normalizedCycle(float cycleSeconds, float index) {
      float safeCycleMs = Math.max(0.1F, cycleSeconds) * 1000.0F;
      return ((System.currentTimeMillis() % (long)safeCycleMs) / safeCycleMs + (index % 360.0F) / 360.0F) % 1.0F;
   }

   private static float smoothStep(float value) {
      value = Math.min(1.0F, Math.max(0.0F, value));
      return value * value * (3.0F - 2.0F * value);
   }

   private int astolfo(float index, float saturation, float brightness, float opacity) {
      float hue = (float)((System.currentTimeMillis() / this.speed + (long)index) % 360L);
      hue %= 360.0F;
      float normalized = hue / 360.0F;
      return fromHsb(normalized < 0.5F ? -normalized : normalized, saturation, brightness, opacity);
   }

   private static int fromHsb(float hue, float saturation, float brightness, float opacity) {
      ColorRGBA color = ColorRGBA.fromHSB(hue, saturation, brightness);
      return argb((int)(opacity * 255.0F), Math.round(color.getRed()), Math.round(color.getGreen()), Math.round(color.getBlue()));
   }

   private static int interpolateColor(int color1, int color2, float amount) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      int a = interpolateInt(alpha(color1), alpha(color2), amount);
      int r = interpolateInt(red(color1), red(color2), amount);
      int g = interpolateInt(green(color1), green(color2), amount);
      int b = interpolateInt(blue(color1), blue(color2), amount);
      return argb(a, r, g, b);
   }

   private static int interpolateInt(int first, int second, float amount) {
      return (int)(first + (second - first) * amount);
   }

   private static ColorRGBA fromArgb(int color) {
      return new ColorRGBA(red(color), green(color), blue(color), alpha(color));
   }

   private static int rgb(int red, int green, int blue) {
      return argb(255, red, green, blue);
   }

   private static int argb(int alpha, int red, int green, int blue) {
      return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
   }

   private static int red(int color) {
      return color >> 16 & 255;
   }

   private static int green(int color) {
      return color >> 8 & 255;
   }

   private static int blue(int color) {
      return color & 255;
   }

   private static int alpha(int color) {
      return color >> 24 & 255;
   }
}
