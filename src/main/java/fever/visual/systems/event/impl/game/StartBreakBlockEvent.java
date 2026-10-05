package fever.visual.systems.event.impl.game;

import lombok.Generated;
import fever.visual.systems.event.EventCancellable;
import net.minecraft.util.math.BlockPos;

public class StartBreakBlockEvent extends EventCancellable {
   private final BlockPos blockPos;

   public StartBreakBlockEvent(BlockPos blockPos) {
      this.blockPos = blockPos;
   }

   @Generated
   public BlockPos getBlockPos() {
      return this.blockPos;
   }
}
