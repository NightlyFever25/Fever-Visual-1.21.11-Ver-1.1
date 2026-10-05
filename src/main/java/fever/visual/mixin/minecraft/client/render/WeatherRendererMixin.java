package fever.visual.mixin.minecraft.client.render;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.NoRender;
import fever.visual.systems.modules.modules.other.Optimization;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherRendering.class)
public abstract class WeatherRendererMixin {
   @Inject(method = "buildPrecipitationPieces", at = @At("HEAD"), cancellable = true)
   private void fevervisual$suppressWeather(World world, int ticks, float tickDelta, Vec3d pos, WeatherRenderState state, CallbackInfo ci) {
      NoRender NoRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
      if (NoRender.isEnabled() && NoRender.getWeather().isSelected()) {
         ci.cancel();
         return;
      }

      Optimization optimization = Optimization.getInstanceSafe();
      if (optimization != null && optimization.shouldSkipWeatherRendering()) {
         ci.cancel();
      }
   }
}
