package fever.visual.systems.modules.modules.visuals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.systems.localization.Language;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.systems.theme.AccentTheme;
import fever.visual.systems.theme.Theme;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;

@ModuleInfo(name = "Interface", enabledByDefault = true, category = ModuleCategory.DISPLAY, desc = "modules.descriptions.interface")
public class Interface extends BaseModule {
   private static volatile Interface instance;
   private final SelectSetting hudElements = new SelectSetting(this, "modules.settings.interface.hud_elements");
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.interface.mode");
   private final ModeSetting.Value liquidGlass = new ModeSetting.Value(this.mode, "modules.settings.interface.liquidGlass");
   private final ModeSetting.Value minimalism = new ModeSetting.Value(this.mode, "modules.settings.interface.minimalism");
   private final ModeSetting themeMode = new ModeSetting(this, "modules.settings.interface.theme", this.liquidGlass::isSelected);
   public final ModeSetting.Value dark = new ModeSetting.Value(this.themeMode, "modules.settings.interface.dark");
   public final ModeSetting.Value light = new ModeSetting.Value(this.themeMode, "modules.settings.interface.light");
   private final SliderSetting hudBlurUpdateFrames = new SliderSetting(
      this,
      "modules.settings.interface.hud_blur_update_frames",
      "modules.settings.interface.hud_blur_update_frames.description",
      () -> !this.dark.isSelected() || this.liquidGlass.isSelected()
   ).min(1.0F).max(8.0F).step(1.0F).currentValue(2.0F).suffix("f");
   private final ModeSetting accentTheme = new ModeSetting(this, "modules.settings.interface.accent_theme");
   private final ColorSetting customAccentFirst = new ColorSetting(this, "modules.settings.interface.accent_theme.custom_first", () -> true)
           .color(new ColorRGBA(255, 223, 255, 255));
   private final ColorSetting customAccentSecond = new ColorSetting(this, "modules.settings.interface.accent_theme.custom_second", () -> true)
           .color(new ColorRGBA(255, 255, 94, 255));
   private final SliderSetting rainbowSpeed = new SliderSetting(
      this,
      "modules.settings.interface.accent_theme.rainbow_speed",
      "modules.settings.interface.accent_theme.rainbow_speed.description",
      () -> !this.isAccentThemeSelected(AccentTheme.RAINBOW)
   ).min(0.5F).max(10.0F).step(0.1F).currentValue(1.8F).suffix("s");
   private final SliderSetting astolfoSpeed = new SliderSetting(
      this,
      "modules.settings.interface.accent_theme.astolfo_speed",
      "modules.settings.interface.accent_theme.astolfo_speed.description",
      () -> !this.isAccentThemeSelected(AccentTheme.ASTOLFO)
   ).min(0.5F).max(10.0F).step(0.1F).currentValue(3.6F).suffix("s");
   private final ModeSetting language = new ModeSetting(this, "modules.settings.interface.language");
   private final Animation liquidGlassAnim = new Animation(500L, Easing.BOTH_CUBIC);
   private final ExecutorService executor = Executors.newSingleThreadExecutor();
   private boolean languageAutoDetected;
   private int lastLang = 0;
   private int hudBlurFrameCounter;
   private boolean hudBlurInitialized;
   private int hudSelectionMask = -1;
   private Theme appliedTheme;
   private final Set<String> enabledHudElements = new HashSet<>();
   private ModeSetting.Value cachedAccentValue;
   private AccentTheme cachedAccentTheme = AccentTheme.RAINBOW;
   private final EventListener<HudRenderEvent> onHudRenderEvent = event -> {
      if (!this.isEnabled()) return;

      this.liquidGlassAnim.setEasing(Easing.FIGMA_EASE_IN_OUT);
      this.liquidGlassAnim.update(this.liquidGlass.isSelected());
      int lang = this.language.getValues().indexOf(this.language.getValue());
      if (lang != this.lastLang) {
         Localizator.setLanguage(lang == 0 ? Language.RU_RU : (lang == 1 ? Language.EN_US : (lang == 2 ? Language.UK_UA : Language.PL_PL)));
         this.languageAutoDetected = false;
      }

      this.lastLang = lang;
      Theme selectedTheme = this.dark.isSelected() ? Theme.DARK : Theme.LIGHT;
      if (selectedTheme != this.appliedTheme) {
         FeverVisual.getInstance().getThemeManager().setCurrentTheme(selectedTheme);
         this.appliedTheme = selectedTheme;
      }
      this.refreshEnabledHudElements();
   };

   public Interface() {
      instance = this;
      this.initializeAccentThemes();
      new ModeSetting.Value(this.language, "modules.settings.interface.language.russian");
      new ModeSetting.Value(this.language, "modules.settings.interface.language.english");
      initializeHudElements();
      if (this.hudElements.getSelectedValues().isEmpty()) {
         for (SelectSetting.Value value : this.hudElements.getValues()) {
            value.select();
         }
      }
      this.refreshEnabledHudElements();

      FeverVisual.getInstance().getEventManager().subscribe(this);
   }

   private void initializeAccentThemes() {
      for (AccentTheme theme : AccentTheme.values()) {
         ModeSetting.Value value = new ModeSetting.Value(this.accentTheme, theme.getTranslationKey());
         if (theme == AccentTheme.RAINBOW) {
            value.select();
         }
      }
   }

   private void initializeHudElements() {
      new SelectSetting.Value(this.hudElements, "hud.hotbar");
      new SelectSetting.Value(this.hudElements, "hud.effects");
      new SelectSetting.Value(this.hudElements, "hud.keybinds");
      new SelectSetting.Value(this.hudElements, "hud.targethud");
      new SelectSetting.Value(this.hudElements, "hud.dynamic_island").select();
      new SelectSetting.Value(this.hudElements, "hud.world");
      new SelectSetting.Value(this.hudElements, "hud.watermark");
      new SelectSetting.Value(this.hudElements, "hud.armor");
      new SelectSetting.Value(this.hudElements, "hud.arraylist");
      new SelectSetting.Value(this.hudElements, "hud.inventory");
      new SelectSetting.Value(this.hudElements, "hud.custom_scoreboard");
      new SelectSetting.Value(this.hudElements, "hud.custom_tab");
      new SelectSetting.Value(this.hudElements, "hud.keystrokes");
      new SelectSetting.Value(this.hudElements, "hud.motion_graph");
   }

   public static boolean isHudElementEnabled(String elementName) {
      Interface interfaceModule = instance;
      if (interfaceModule == null || !interfaceModule.isEnabled()) {
         return false;
      }

      return interfaceModule.enabledHudElements.contains(elementName.toLowerCase(Locale.ROOT));
   }

   private void refreshEnabledHudElements() {
      int selectionMask = 0;
      for (int i = 0; i < this.hudElements.getValues().size() && i < Integer.SIZE; i++) {
         if (this.hudElements.getValues().get(i).isSelected()) {
            selectionMask |= 1 << i;
         }
      }
      if (selectionMask == this.hudSelectionMask) {
         return;
      }
      this.hudSelectionMask = selectionMask;
      this.enabledHudElements.clear();
      for (SelectSetting.Value value : this.hudElements.getSelectedValues()) {
         this.enabledHudElements.add(value.getName().toLowerCase(Locale.ROOT));
      }
   }

   public static boolean isDynamicIslandEnabled() {
      return isHudElementEnabled("hud.dynamic_island");
   }
   private void detectLanguageByIP() {
      this.executor.submit(() -> {
         try {
            String countryCode = this.getCountryCodeByIP();
            if (countryCode != null) {
               String var2 = countryCode.toUpperCase();
               switch (var2) {
                  case "UA":
                     this.language.setValue(this.language.getValues().get(2));
                     Localizator.setLanguage(Language.UK_UA);
                     break;
                  case "PL":
                     this.language.setValue(this.language.getValues().get(3));
                     Localizator.setLanguage(Language.PL_PL);
                     break;
                  default:
                     this.language.setValue(this.language.getValues().getFirst());
                     Localizator.setLanguage(Language.RU_RU);
               }

               this.languageAutoDetected = true;
               this.lastLang = this.language.getValues().indexOf(this.language.getValue());
            }
         } catch (Exception var4) {
            FeverVisual.LOGGER.error("Failed to detect language by IP", var4);
         }
      });
   }

   private String getCountryCodeByIP() throws IOException {
      URL url = new URL("http://ip-api.com/json/?fields=countryCode");
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("GET");

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
         StringBuilder response = new StringBuilder();

         String line;
         while ((line = reader.readLine()) != null) {
            response.append(line);
         }

         String json = response.toString();
         int start = json.indexOf("countryCode\":\"") + 14;
         if (start >= 14) {
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
         }
      }

      return null;
   }

   public static boolean glassSelected() {
      Interface interfaceModule = instance;
      if (interfaceModule == null) return false;
      return interfaceModule.liquidGlass.isSelected();
   }

   public static float glass() {
      Interface interfaceModule = instance;
      if (interfaceModule == null) return 0.0F;
      return interfaceModule.liquidGlassAnim.getValue();
   }

   public static float minimalizm() {
      return 1.0F - glass();
   }

   public static ColorRGBA getAccentColor(float index) {
      Interface interfaceModule = instance;
      if (interfaceModule == null || interfaceModule.accentTheme.getValue() == null) {
         return AccentTheme.RAINBOW.getColor(index);
      }

      AccentTheme theme = interfaceModule.getSelectedAccentTheme();
      if (theme == AccentTheme.RAINBOW) {
         return AccentTheme.rainbow(index, interfaceModule.rainbowSpeed.getCurrentValue());
      }

      if (theme == AccentTheme.ASTOLFO) {
         return AccentTheme.astolfo(index, interfaceModule.astolfoSpeed.getCurrentValue());
      }

      if (theme == AccentTheme.CUSTOM) {
         return AccentTheme.customColor(index, interfaceModule.customAccentFirst.getColorSafe(), interfaceModule.customAccentSecond.getColorSafe());
      }

      return theme.getColor(index);
   }

   public static boolean isAccentThemeDynamic() {
      Interface interfaceModule = instance;
      if (interfaceModule == null || interfaceModule.accentTheme.getValue() == null) {
         return true;
      }

      return true;
   }

   private boolean isAccentThemeSelected(AccentTheme theme) {
      if (this.accentTheme.getValue() == null) {
         return false;
      }

      return this.getSelectedAccentTheme() == theme;
   }

   private AccentTheme getSelectedAccentTheme() {
      ModeSetting.Value value = this.accentTheme.getValue();
      if (value != this.cachedAccentValue) {
         this.cachedAccentValue = value;
         this.cachedAccentTheme = value == null
            ? AccentTheme.RAINBOW
            : AccentTheme.byTranslationKey(value.getName());
      }
      return this.cachedAccentTheme;
   }

   public static boolean showGlass() {
      return glass() > 0.0F;
   }

   public static boolean showMinimalizm() {
      return glass() < 1.0F;
   }

   public static boolean shouldUpdateHudBlur() {
      Interface interfaceModule = instance;
      if (interfaceModule == null || !interfaceModule.isEnabled()) {
         return false;
      }
      if (interfaceModule.enabledHudElements.isEmpty()
         || (!showMinimalizm() && !showGlass())) {
         interfaceModule.hudBlurFrameCounter = 0;
         interfaceModule.hudBlurInitialized = false;
         return false;
      }

      if (!interfaceModule.dark.isSelected() || interfaceModule.liquidGlass.isSelected()) {
         interfaceModule.hudBlurFrameCounter = 0;
         interfaceModule.hudBlurInitialized = false;
         return true;
      }

      int frames = Math.max(1, Math.round(interfaceModule.hudBlurUpdateFrames.getCurrentValue()));
      if (frames <= 1) {
         interfaceModule.hudBlurFrameCounter = 0;
         interfaceModule.hudBlurInitialized = true;
         return true;
      }

      if (!interfaceModule.hudBlurInitialized) {
         interfaceModule.hudBlurInitialized = true;
         interfaceModule.hudBlurFrameCounter = 0;
         return true;
      }

      interfaceModule.hudBlurFrameCounter++;
      if (interfaceModule.hudBlurFrameCounter >= frames) {
         interfaceModule.hudBlurFrameCounter = 0;
         return true;
      }

      return false;
   }

   @Generated
   public ModeSetting getMode() {
      return this.mode;
   }

   @Generated
   public ModeSetting.Value getLiquidGlass() {
      return this.liquidGlass;
   }

   @Generated
   public ModeSetting.Value getMinimalism() {
      return this.minimalism;
   }

   @Generated
   public ModeSetting getThemeMode() {
      return this.themeMode;
   }

   @Generated
   public ModeSetting getAccentTheme() {
      return this.accentTheme;
   }

   @Generated
   public ColorSetting getCustomAccentFirst() {
      return this.customAccentFirst;
   }

   @Generated
   public ColorSetting getCustomAccentSecond() {
      return this.customAccentSecond;
   }

   @Generated
   public SliderSetting getRainbowSpeed() {
      return this.rainbowSpeed;
   }

   @Generated
   public SliderSetting getAstolfoSpeed() {
      return this.astolfoSpeed;
   }

   @Generated
   public ModeSetting.Value getDark() {
      return this.dark;
   }

   @Generated
   public ModeSetting.Value getLight() {
      return this.light;
   }
   @Generated
   public ModeSetting getLanguage() {
      return this.language;
   }

   @Generated
   public Animation getLiquidGlassAnim() {
      return this.liquidGlassAnim;
   }

   @Generated
   public ExecutorService getExecutor() {
      return this.executor;
   }

   @Generated
   public boolean isLanguageAutoDetected() {
      return this.languageAutoDetected;
   }

   @Generated
   public int getLastLang() {
      return this.lastLang;
   }

   @Generated
   public EventListener<HudRenderEvent> getOnHudRenderEvent() {
      return this.onHudRenderEvent;
   }

   @Generated
   public SelectSetting getHudElements() {
      return this.hudElements;
   }
}
