package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.GlassHandsRenderEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.shader.HandShaderRenderer;

@ModuleInfo(name = "Hand Shader", category = ModuleCategory.VISUALS, desc = "modules.descriptions.hand_shader")
public class HandShader extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.hand_shader.mode", "modules.settings.hand_shader.mode.description");
   private final ModeSetting.Value full = new ModeSetting.Value(this.mode, "modules.settings.hand_shader.mode.full").select();
   private final ModeSetting.Value noFull = new ModeSetting.Value(this.mode, "modules.settings.hand_shader.mode.no_full");
   private final ModeSetting.Value webShader = new ModeSetting.Value(this.mode, "modules.settings.hand_shader.mode.web_shader");
   private final ModeSetting.Value fire = new ModeSetting.Value(this.mode, "modules.settings.hand_shader.mode.fire");
   private final ModeSetting.Value plasma = new ModeSetting.Value(this.mode, "modules.settings.hand_shader.mode.plasma");
   private final ModeSetting.Value glitch = new ModeSetting.Value(this.mode, "modules.settings.hand_shader.mode.glitch");
   private final ModeSetting.Value fireTwo = new ModeSetting.Value(this.mode, "Fire 2");
   private final SliderSetting blurRadius = new SliderSetting(this, "modules.settings.hand_shader.blur_radius", "modules.settings.hand_shader.blur_radius.description")
           .min(1.0F).max(30.0F).step(0.5F).currentValue(12.0F);
   private final SliderSetting opacity = new SliderSetting(this, "modules.settings.hand_shader.opacity", "modules.settings.hand_shader.opacity.description")
           .min(0.0F).max(1.0F).step(0.05F).currentValue(0.6F);
   private final SliderSetting threshold = new SliderSetting(this, "modules.settings.hand_shader.threshold", "modules.settings.hand_shader.threshold.description")
           .min(0.001F).max(0.1F).step(0.001F).currentValue(0.018F);
   private final SliderSetting flameStrength = new SliderSetting(this, "Flame Strength", () -> !this.fireTwo.isSelected())
           .min(0.0F).max(2.0F).step(0.05F).currentValue(0.85F);
   private final SliderSetting flameRiseSpeed = new SliderSetting(this, "Flame Rise Speed", () -> !this.fireTwo.isSelected())
           .min(0.0F).max(2.0F).step(0.05F).currentValue(0.0F);
   private final SliderSetting flameWobble = new SliderSetting(this, "Flame Wobble", () -> !this.fireTwo.isSelected())
           .min(0.0F).max(2.0F).step(0.05F).currentValue(0.65F);
   private final SliderSetting flameLength = new SliderSetting(this, "Flame Length", () -> !this.fireTwo.isSelected())
           .min(0.1F).max(2.5F).step(0.05F).currentValue(0.95F);
   private final SliderSetting flameBrightness = new SliderSetting(this, "Flame Brightness", () -> !this.fireTwo.isSelected())
           .min(0.0F).max(2.0F).step(0.05F).currentValue(0.9F);
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.hand_shader.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.hand_shader.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.hand_shader.color_mode.custom");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.hand_shader.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(70, 170, 255, 255))
           .alpha(true);
   private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.hand_shader.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(151, 71, 255, 255))
           .alpha(true);

   private final EventListener<GlassHandsRenderEvent> onGlassHandsRender = event -> {
      HandShaderRenderer renderer = HandShaderRenderer.getInstance();
      updateRendererSettings(renderer);

      if (event.getPhase() == GlassHandsRenderEvent.Phase.PRE) {
         renderer.captureSceneBeforeHands();
      } else {
         renderer.captureSceneAfterHands();
         renderer.renderEffect();
      }
   };

   @Override
   public void onEnable() {
      HandShaderRenderer renderer = HandShaderRenderer.getInstance();
      renderer.invalidate();
      renderer.setEnabled(true);
      updateRendererSettings(renderer);
   }

   @Override
   public void onDisable() {
      HandShaderRenderer.getInstance().setEnabled(false);
   }

   private void updateRendererSettings(HandShaderRenderer renderer) {
      renderer.setBlurRadius(this.blurRadius.getCurrentValue());
      renderer.setOpacity(this.opacity.getCurrentValue());
      renderer.setThreshold(this.threshold.getCurrentValue());
      renderer.setColor(this.getShaderColor().getRGB());
      renderer.setMode(getModeIndex());
      renderer.setFlameSettings(this.flameStrength.getCurrentValue(), this.flameRiseSpeed.getCurrentValue(),
              this.flameWobble.getCurrentValue(), this.flameLength.getCurrentValue(), this.flameBrightness.getCurrentValue());
   }

   private ColorRGBA getShaderColor() {
      float index = (System.currentTimeMillis() % 2600L) / 2600.0F * 360.0F;
      if (this.colorCustom.isSelected()) {
         float normalized = (index % 360.0F) / 180.0F;
         float mix = normalized > 1.0F ? 2.0F - normalized : normalized;
         return this.color.getColorSafe().mix(this.colorSecond.getColorSafe(), mix);
      }

      return Colors.getAccentColor(index).withAlpha(this.color.getColorSafe().getAlpha());
   }

   private int getModeIndex() {
      if (this.noFull.isSelected()) {
         return 1;
      }
      if (this.webShader.isSelected()) {
         return 2;
      }
      if (this.fire.isSelected()) {
         return 3;
      }
      if (this.plasma.isSelected()) {
         return 4;
      }
      if (this.glitch.isSelected()) {
         return 5;
      }
      if (this.fireTwo.isSelected()) {
         return 6;
      }
      return 0;
   }
}
