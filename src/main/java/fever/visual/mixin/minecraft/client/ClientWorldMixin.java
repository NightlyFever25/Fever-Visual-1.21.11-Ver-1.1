package fever.visual.mixin.minecraft.client;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.Ambience;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionType.class)
public class ClientWorldMixin {
   @Inject(method = "skybox", at = @At("HEAD"), cancellable = true)
   private void fevervisual$useEndSkybox(CallbackInfoReturnable<DimensionType.Skybox> info) {
      if (FeverVisual.getInstance().getModuleManager().getModule(Ambience.class).isEnabled()
         && FeverVisual.getInstance().getModuleManager().getModule(Ambience.class).getEndSky().isEnabled()) {
         info.setReturnValue(DimensionType.Skybox.END);
      }
   }
}
