package fever.visual.systems.event.impl.render;

import fever.visual.systems.event.EventCancellable;
import net.minecraft.client.util.math.MatrixStack;

public class GlassHandsRenderEvent extends EventCancellable {
   private final Phase phase;
   private final MatrixStack matrices;
   private final float tickDelta;

   public GlassHandsRenderEvent(Phase phase, MatrixStack matrices, float tickDelta) {
      this.phase = phase;
      this.matrices = matrices;
      this.tickDelta = tickDelta;
   }

   public Phase getPhase() {
      return this.phase;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public enum Phase {
      PRE,
      POST
   }
}
