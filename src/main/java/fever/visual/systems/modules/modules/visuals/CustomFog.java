package fever.visual.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.mixin.accessors.GameRendererAccessor;
import lombok.Generated;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.RangeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.PlatformUtility;
import fever.visual.utility.render.shader.CustomFogBlurRenderer;
import fever.visual.utility.render.shader.CustomFogCompatRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "Custom Fog", category = ModuleCategory.VISUALS)
public class CustomFog extends BaseModule {
   private static boolean compatWorldFogBufferActive;

   private final RangeSetting distance = new RangeSetting(this, "modules.settings.custom_fog.distance")
      .min(1.0F)
      .max(100.0F)
      .step(1.0F)
      .firstValue(10.0F)
      .secondValue(50.0F);
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.custom_fog.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.custom_fog.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.custom_fog.color_mode.custom");
   private final ColorSetting fogColor = new ColorSetting(this, "modules.settings.custom_fog.color", () -> !this.colorCustom.isSelected()).color(Colors.getAccentColor()).alpha(true);
   private final ColorSetting fogColorSecond = new ColorSetting(this, "modules.settings.custom_fog.color_second", () -> !this.colorCustom.isSelected()).color(new ColorRGBA(100, 160, 255, 255)).alpha(true);
   private final SliderSetting opacity = new SliderSetting(this, "modules.settings.custom_fog.opacity", "modules.settings.custom_fog.opacity.description")
      .min(0.0F)
      .max(1.0F)
      .step(0.05F)
      .currentValue(0.85F);
   private final BooleanSetting fogBlur = new BooleanSetting(this, "modules.settings.custom_fog.fog_blur", "modules.settings.custom_fog.fog_blur.description")
      .enabled(false);
   private final SliderSetting blurRadius = new SliderSetting(
      this,
      "modules.settings.custom_fog.blur_radius",
      "modules.settings.custom_fog.blur_radius.description",
      () -> !this.fogBlur.isEnabled()
   ).min(0.5F).max(8.0F).step(0.1F).currentValue(1.6F);
   private final SliderSetting blurStrength = new SliderSetting(
      this,
      "modules.settings.custom_fog.blur_strength",
      "modules.settings.custom_fog.blur_strength.description",
      () -> !this.fogBlur.isEnabled()
   ).min(0.0F).max(1.0F).step(0.05F).currentValue(0.22F);

   public CustomFog() {
      WorldRenderEvents.START_MAIN.register(context -> {
         if (this.shouldUseCompatWorldFog(context.gameRenderer().getCamera())) {
            compatWorldFogBufferActive = true;
            FogRenderer fogRenderer = ((GameRendererAccessor)context.gameRenderer()).fogRenderer();
            RenderSystem.setShaderFog(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
         } else {
            compatWorldFogBufferActive = false;
         }
      });
      WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
         if (this.shouldModifyFog(context.gameRenderer().getCamera())) {
            FogRenderer fogRenderer = ((GameRendererAccessor)context.gameRenderer()).fogRenderer();
            RenderSystem.setShaderFog(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
            if (this.shouldUseCompatWorldFog(context.gameRenderer().getCamera())) {
               CustomFogCompatRenderer.getInstance().render(this, context.gameRenderer().getFarPlaneDistance());
            }
            CustomFogBlurRenderer.getInstance().render(this, context.gameRenderer().getFarPlaneDistance());
         }
      });
      WorldRenderEvents.AFTER_ENTITIES.register(context -> {
         if (this.shouldModifyFog(context.gameRenderer().getCamera())) {
            FogRenderer fogRenderer = ((GameRendererAccessor)context.gameRenderer()).fogRenderer();
            FogRenderer.FogType fogType = this.shouldUseCompatWorldFog(context.gameRenderer().getCamera())
               ? FogRenderer.FogType.NONE
               : FogRenderer.FogType.WORLD;
            RenderSystem.setShaderFog(fogRenderer.getFogBuffer(fogType));
         }
      });
      WorldRenderEvents.END_MAIN.register(context -> compatWorldFogBufferActive = false);
   }

   public boolean shouldModifyFog(Camera camera) {
      if (this.isEnabled() && mc.world != null && mc.player != null) {
         Entity entity = camera.getFocusedEntity();
         if (camera.getSubmersionType() == CameraSubmersionType.WATER) {
            return false;
         } else if (camera.getSubmersionType() == CameraSubmersionType.LAVA) {
            return false;
         } else if (camera.getSubmersionType() == CameraSubmersionType.POWDER_SNOW) {
            return false;
         } else {
            if (entity instanceof LivingEntity livingEntity) {
               if (livingEntity.hasStatusEffect(StatusEffects.BLINDNESS)) {
                  return false;
               }

               if (livingEntity.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public float getColorBlend() {
      ColorRGBA color = this.getFogColorValue();
      return Math.clamp(this.opacity.getCurrentValue() * (color.getAlpha() / 255.0F), 0.0F, 1.0F);
   }

   public ColorRGBA getFogColorValue() {
      float index = (System.currentTimeMillis() % 4200L) / 4200.0F * 360.0F;
      if (this.colorCustom.isSelected()) {
         float normalized = (index % 360.0F) / 180.0F;
         float mix = normalized > 1.0F ? 2.0F - normalized : normalized;
         return this.fogColor.getColorSafe().mix(this.fogColorSecond.getColorSafe(), mix);
      }

      return Colors.getAccentColor(index).withAlpha(this.fogColor.getColorSafe().getAlpha());
   }

   public float getFogStart(float maximumDistance) {
      return MathHelper.clamp(this.distance.getFirstValue(), -8.0F, maximumDistance);
   }

   public float getFogEnd(float maximumDistance) {
      float start = this.getFogStart(maximumDistance);
      float end = MathHelper.clamp(this.distance.getSecondValue(), 0.0F, maximumDistance);
      return MathHelper.clamp(Math.max(start + 1.0F, end), 1.0F, maximumDistance);
   }

   public boolean shouldUseCompatWorldFog(Camera camera) {
      return this.shouldModifyFog(camera)
         && (PlatformUtility.isLabyMod()
            || PlatformUtility.isLunarClient()
            || !FabricLoader.getInstance().isDevelopmentEnvironment());
   }

   public static void setCompatWorldFogBufferActive(boolean active) {
      compatWorldFogBufferActive = active;
   }

   public static boolean shouldDisableWorldFogBuffer() {
      CustomFog module = FeverVisual.getInstance().getModuleManager() == null ? null : FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomFog.class);
      return compatWorldFogBufferActive
         && module != null
         && module.isEnabled()
         && module.shouldUseCompatWorldFog(mc.gameRenderer.getCamera());
   }

   public boolean shouldApplyBlur(Camera camera) {
      return this.shouldModifyFog(camera) && this.isFogBlurEnabled();
   }

   public float getBlurRadiusValue() {
      return Math.max(0.0F, this.blurRadius.getCurrentValue());
   }

   public float getBlurStrengthValue() {
      return Math.clamp(this.blurStrength.getCurrentValue(), 0.0F, 1.0F);
   }

   private boolean isFogBlurEnabled() {
      return this.fogBlur.isEnabled() && this.blurStrength.getCurrentValue() > 0.001F;
   }

   @Generated
   public RangeSetting getDistance() {
      return this.distance;
   }

   @Generated
   public ColorSetting getFogColor() {
      return this.fogColor;
   }
}
