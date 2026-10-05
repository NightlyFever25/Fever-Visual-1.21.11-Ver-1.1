package fever.visual.mixin.minecraft.client.render;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.NoRender;
import fever.visual.systems.modules.modules.other.Optimization;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
   @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
   private void onAddParticle(
      ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir
   ) {
      NoRender NoRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
      if (NoRender.isEnabled() && NoRender.getWeather().isSelected() && parameters.getType() == ParticleTypes.RAIN) {
         cir.cancel();
         return;
      }

      Optimization optimization = Optimization.getInstanceSafe();
      if (optimization != null && optimization.shouldSkipParticle(parameters, x, y, z)) {
         cir.cancel();
      }
   }
}
