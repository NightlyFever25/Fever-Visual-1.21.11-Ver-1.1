package fever.visual.mixin.minecraft.client.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import fever.visual.systems.modules.modules.visuals.CustomButtons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
   @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
   private void fevervisual$disableVanillaBlurForCustomButtons(DrawContext context, CallbackInfo ci) {
      if (CustomButtons.isEnabledSafe() && CustomButtons.shouldStyleCurrentButtonScreen()) {
         ci.cancel();
      }
   }
}
