package fever.visual.mixin.minecraft.client.gui.overlay;

import fever.visual.systems.modules.modules.visuals.CustomChat;
import fever.visual.utility.interfaces.ChatHudBackendContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
   "net.minecraft.client.gui.hud.ChatHud$Hud",
   "net.minecraft.client.gui.hud.ChatHud$Interactable"
})
public class ChatHudBackendMixin {
   @Redirect(method = "fill", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
   private void fevervisual$drawCustomChatBackground(DrawContext context, int x1, int y1, int x2, int y2, int color) {
      if (!CustomChat.renderMessageBackground(context, x1, y1, x2, y2, color)) {
         context.fill(x1, y1, x2, y2, color);
      }
   }

   @Inject(method = "text", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomChatText(int y, float opacity, OrderedText text, CallbackInfoReturnable<Boolean> cir) {
      DrawContext context = ((ChatHudBackendContext)(Object)this).fevervisual$getContext();
      if (CustomChat.renderChatText(context, y, opacity, text)) {
         cir.setReturnValue(false);
      }
   }
}
