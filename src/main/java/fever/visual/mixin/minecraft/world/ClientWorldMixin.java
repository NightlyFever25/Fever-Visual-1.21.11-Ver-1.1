package fever.visual.mixin.minecraft.world;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.NoRender;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
   @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
   private void fevervisual$suppressBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
      NoRender noRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender.isEnabled() && noRender.getBreakParticles().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "spawnBlockBreakingParticle", at = @At("HEAD"), cancellable = true)
   private void fevervisual$suppressBlockBreakingParticle(BlockPos pos, Direction direction, CallbackInfo ci) {
      NoRender noRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender.isEnabled() && noRender.getBreakParticles().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
   private void onHandleBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
   }
}
