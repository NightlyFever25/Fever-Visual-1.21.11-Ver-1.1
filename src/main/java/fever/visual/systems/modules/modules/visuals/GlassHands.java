package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.GlassHandsRenderEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.render.shader.GlassHandsRenderer;

@ModuleInfo(name = "Glass Hands", category = ModuleCategory.VISUALS, desc = "modules.descriptions.glass_hands")
public class GlassHands extends BaseModule {
   private final SliderSetting blurRadius = new SliderSetting(this, "modules.settings.glass_hands.blur_radius", "modules.settings.glass_hands.blur_radius.description")
           .min(1.0F).max(5.0F).step(0.1F).currentValue(2.5F);
   private final SliderSetting blurIterations = new SliderSetting(this, "modules.settings.glass_hands.blur_iterations", "modules.settings.glass_hands.blur_iterations.description")
           .min(1.0F).max(5.0F).step(1.0F).currentValue(3.0F);
   private final SliderSetting saturation = new SliderSetting(this, "modules.settings.glass_hands.saturation", "modules.settings.glass_hands.saturation.description")
           .min(0.0F).max(2.0F).step(0.05F).currentValue(0.0F);
   private final BooleanSetting enableTint = new BooleanSetting(this, "modules.settings.glass_hands.tint", "modules.settings.glass_hands.tint.description").enabled(false);
   private final SliderSetting tintIntensity = new SliderSetting(this, "modules.settings.glass_hands.tint_intensity", "modules.settings.glass_hands.tint_intensity.description", () -> !this.enableTint.isEnabled())
           .min(0.0F).max(0.5F).step(0.05F).currentValue(0.2F);
   private final ColorSetting tintColor = new ColorSetting(this, "modules.settings.glass_hands.tint_color", () -> !this.enableTint.isEnabled())
           .color(new ColorRGBA(120, 190, 255, 255));
   private final BooleanSetting enableEdgeGlow = new BooleanSetting(this, "modules.settings.glass_hands.edge_glow", "modules.settings.glass_hands.edge_glow.description").enabled(true);
   private final SliderSetting edgeGlowIntensity = new SliderSetting(this, "modules.settings.glass_hands.edge_glow_intensity", "modules.settings.glass_hands.edge_glow_intensity.description", () -> !this.enableEdgeGlow.isEnabled())
           .min(0.0F).max(1.0F).step(0.05F).currentValue(0.2F);

   private final EventListener<GlassHandsRenderEvent> onGlassHandsRender = event -> {
      GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
      updateRendererSettings(renderer);

      if (event.getPhase() == GlassHandsRenderEvent.Phase.PRE) {
         renderer.captureSceneBeforeHands();
      } else {
         renderer.captureSceneAfterHands();
         renderer.renderGlassEffect();
      }
   };

   @Override
   public void onEnable() {
      GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
      renderer.invalidate();
      renderer.setEnabled(true);
      updateRendererSettings(renderer);
   }

   @Override
   public void onDisable() {
      GlassHandsRenderer.getInstance().setEnabled(false);
   }

   private void updateRendererSettings(GlassHandsRenderer renderer) {
      renderer.setBlurRadius(this.blurRadius.getCurrentValue());
      renderer.setBlurIterations(Math.round(this.blurIterations.getCurrentValue()));
      renderer.setSaturation(this.saturation.getCurrentValue());
      renderer.setReflect(true);
      renderer.setTintColor(this.enableTint.isEnabled() ? this.tintColor.getColorSafe().getRGB() : 0x00000000);
      renderer.setTintIntensity(this.enableTint.isEnabled() ? this.tintIntensity.getCurrentValue() : 0.0F);
      renderer.setEdgeGlowIntensity(this.enableEdgeGlow.isEnabled() ? this.edgeGlowIntensity.getCurrentValue() : 0.0F);
   }
}
