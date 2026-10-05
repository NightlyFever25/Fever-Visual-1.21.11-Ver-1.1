package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.modules.modules.other.Sounds;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.ui.menu.MenuScreen;
import fever.visual.ui.menu.api.MenuCloseListener;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.sounds.ClientSounds;

@ModuleInfo(name = "Click Gui", category = ModuleCategory.DISPLAY, key = 344, desc = "modules.descriptions.menu")
public class MenuModule extends BaseModule {
   private static final MenuCloseListener menuCloseListener = new MenuCloseListener();

   private final BooleanSetting showSearch = new BooleanSetting(this, "modules.settings.menu.show_search", "modules.settings.menu.show_search.description")
           .enabled(true);

   private final BooleanSetting rainbowGlass = new BooleanSetting(this, "modules.settings.menu.rainbow_glass", "modules.settings.menu.rainbow_glass.description")
           .enabled(true);
   private final SliderSetting rainbowSpeed = new SliderSetting(this, "modules.settings.menu.rainbow_speed", "modules.settings.menu.rainbow_speed.description",
           () -> !rainbowGlass.isEnabled())
           .min(1.0f)
           .max(20.0f)
           .currentValue(6.0f)
           .step(0.5f);
   private final SliderSetting rainbowSaturation = new SliderSetting(this, "modules.settings.menu.rainbow_saturation", "modules.settings.menu.rainbow_saturation.description",
           () -> !rainbowGlass.isEnabled())
           .min(0.1f)
           .max(1.0f)
           .currentValue(0.35f)
           .step(0.05f);
   private final SliderSetting rainbowAlpha = new SliderSetting(this, "modules.settings.menu.rainbow_alpha", "modules.settings.menu.glass_alpha.description",
           () -> !rainbowGlass.isEnabled())
           .min(0.1f)
           .max(1.0f)
           .currentValue(1.0f)
           .step(0.05f);
   private final ColorSetting glassColorTop = new ColorSetting(this, "modules.settings.menu.glass_top_color",
           () -> rainbowGlass.isEnabled() || !Interface.showGlass())
           .color(new ColorRGBA(255.0F, 255.0F, 255.0F, 255.0F))
           .alpha(true);

   private final ColorSetting glassColorBottom = new ColorSetting(this, "modules.settings.menu.glass_bottom_color",
           () -> rainbowGlass.isEnabled() || !Interface.showGlass())
           .color(new ColorRGBA(180.0F, 210.0F, 255.0F, 255.0F))
           .alpha(true);
   private final SliderSetting manualAlpha = new SliderSetting(this, "modules.settings.menu.manual_alpha", "modules.settings.menu.glass_alpha.description",
           () -> rainbowGlass.isEnabled() || !Interface.showGlass())
           .min(0.1f)
           .max(1.0f)
           .currentValue(1.0f)
           .step(0.05f);

   @Override
   public void onEnable() {
      if (!(mc.currentScreen instanceof MenuScreen)) {
         MenuScreen menuScreen = FeverVisual.getInstance().getMenuScreen();
         mc.setScreen(menuScreen);
         Sounds soundsModule = FeverVisual.getInstance().getModuleManager().getModule(Sounds.class);
         if (soundsModule.isEnabled()) {
            ClientSounds.CLICKGUI_OPEN.play(soundsModule.getVolume().getCurrentValue());
         }
         super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (mc.currentScreen instanceof MenuScreen) {
         mc.setScreen(null);
         FeverVisual.getInstance().getMenuScreen().setClosing(true);
      }
      super.onDisable();
   }

   public boolean shouldShowSearch() {
      return showSearch.isEnabled();
   }

   public ColorRGBA getGlassColorTop(float animAlpha) {
      if (rainbowGlass.isEnabled()) {
         return computeRainbowColor(0.0f, animAlpha);
      }
      ColorRGBA c = glassColorTop.getColorSafe();
      return c.withAlpha(c.getAlpha() * manualAlpha.getCurrentValue() * animAlpha);
   }

   public ColorRGBA getGlassColorBottom(float animAlpha) {
      if (rainbowGlass.isEnabled()) {
         return computeRainbowColor(0.33f, animAlpha);
      }
      ColorRGBA c = glassColorBottom.getColorSafe();
      return c.withAlpha(c.getAlpha() * manualAlpha.getCurrentValue() * animAlpha);
   }

   private ColorRGBA computeRainbowColor(float phaseOffset, float animAlpha) {
      float saturation     = rainbowSaturation.getCurrentValue();
      float alphaMultiplier = rainbowAlpha.getCurrentValue();
      double cyclePeriodMs = rainbowSpeed.getCurrentValue() * 1000.0;

      float hue = (float) ((System.currentTimeMillis() % (long) cyclePeriodMs) / cyclePeriodMs);
      hue = (hue + phaseOffset) % 1.0f;

      ColorRGBA rgb = ColorRGBA.fromHSB(hue, saturation, 1.0f);
      return rgb.withAlpha(255.0f * animAlpha * alphaMultiplier);
   }
}
