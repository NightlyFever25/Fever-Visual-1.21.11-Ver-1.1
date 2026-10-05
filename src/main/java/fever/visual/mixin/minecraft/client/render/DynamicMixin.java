package fever.visual.mixin.minecraft.client.render;

import fever.visual.utility.game.EntityUtility;
import net.minecraft.client.render.RenderTickCounter.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;

@Mixin(Dynamic.class)
public class DynamicMixin {
   @Shadow
   private float dynamicDeltaTicks;
   @Shadow
   private float tickProgress;
   @Shadow
   private long lastTimeMillis;
   @Final
   @Shadow
   private float tickTime;
   @Final
   @Shadow
   private FloatUnaryOperator targetMillisPerTick;

   @Inject(
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;lastTimeMillis:J", opcode = 181, ordinal = 0),
      method = "beginRenderTick(J)I",
      cancellable = true
   )
   public void onBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      if (EntityUtility.getTimer() != 1.0F) {
         this.dynamicDeltaTicks = (float)(timeMillis - this.lastTimeMillis)
            / this.targetMillisPerTick.apply(this.tickTime)
            * EntityUtility.getTimer();
         this.lastTimeMillis = timeMillis;
         this.tickProgress += this.dynamicDeltaTicks;
         int i = (int)this.tickProgress;
         this.tickProgress -= i;
         cir.setReturnValue(i);
      }
   }
}
