package fever.visual.mixin.minecraft.client.gui.overlay;

import fever.visual.ui.hud.impl.Hotbar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceBar.class)
public class ExperienceBarMixin {
   @Inject(method = "renderBar", at = @At("HEAD"), cancellable = true)
   private void fevervisual$suppressExperienceBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Hotbar.shouldSuppressVanilla()) {
         ci.cancel();
      }
   }
}
