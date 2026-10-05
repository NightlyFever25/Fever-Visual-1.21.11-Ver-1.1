package fever.visual.mixin.minecraft.world.explosion;

import java.util.List;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventManager;
import fever.visual.systems.event.impl.game.AncientDebrisEvent;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExplosionImpl.class)
public abstract class ExplosionImplMixin implements IMinecraft {
   @Inject(method = "getBlocksToDestroy", at = @At("RETURN"))
   private void onBlocksToDestroyCalculated(CallbackInfoReturnable<List<BlockPos>> cir) {
      ExplosionImpl self = (ExplosionImpl) (Object) this;
      List<BlockPos> affectedBlocks = cir.getReturnValue();
      if (affectedBlocks == null || affectedBlocks.isEmpty() || self.getWorld().getRegistryKey() != World.NETHER) {
         return;
      }

      List<BlockPos> debris = affectedBlocks.stream()
            .filter(pos -> self.getWorld().getBlockState(pos).isOf(Blocks.ANCIENT_DEBRIS)).toList();
      EventManager eventManager = FeverVisual.getInstance().getEventManager();
      if (!debris.isEmpty() && eventManager != null) {
         eventManager.triggerEvent(new AncientDebrisEvent(debris, self.getPosition()));
      }
   }
}
