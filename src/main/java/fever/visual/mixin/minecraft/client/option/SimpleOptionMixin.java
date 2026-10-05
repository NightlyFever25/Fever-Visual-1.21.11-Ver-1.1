package fever.visual.mixin.minecraft.client.option;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.FullBright;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {
   @Shadow
   @Final
   Text text;
   @Shadow
   T value;

   @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
   public void getGammaValue(CallbackInfoReturnable<Double> cir) {
      if (FeverVisual.getInstance().getModuleManager() != null) {
         FullBright fullBright = FeverVisual.getInstance().getModuleManager().getModuleSafe(FullBright.class);
         if (fullBright != null && fullBright.isGammaMode() && this.text.equals(Text.translatable("options.gamma"))) {
            cir.setReturnValue(1337.0);
         }
      }
   }

   @Inject(method = "setValue", at = @At("HEAD"), cancellable = true)
   public void setGammaValue(T value, CallbackInfo ci) {
      if (this.text.equals(Text.translatable("options.gamma"))) {
         this.value = value;
         ci.cancel();
      }
   }
}
