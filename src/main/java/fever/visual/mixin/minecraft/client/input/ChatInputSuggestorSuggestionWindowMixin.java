package fever.visual.mixin.minecraft.client.input;

import fever.visual.systems.modules.modules.visuals.CustomChat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Rect2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screen.ChatInputSuggestor$SuggestionWindow")
public class ChatInputSuggestorSuggestionWindowMixin {
   @Shadow
   @Final
   private Rect2i area;

   @Unique
   private boolean fevervisual$backgroundDrawn;

   @Inject(method = "render", at = @At("HEAD"))
   private void fevervisual$resetCustomSuggestionBackground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
      this.fevervisual$backgroundDrawn = false;
   }

   @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
   private void fevervisual$drawCustomSuggestionBackground(DrawContext context, int x1, int y1, int x2, int y2, int color) {
      if (CustomChat.isActive()) {
         if (!this.fevervisual$backgroundDrawn) {
            CustomChat.renderCommandSuggestionWindowBackground(context, this.area.getX(), this.area.getY(), this.area.getWidth(), this.area.getHeight());
            this.fevervisual$backgroundDrawn = true;
         }
         return;
      }

      context.fill(x1, y1, x2, y2, color);
   }

   @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"))
   private void fevervisual$moveCustomSuggestionText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
      if (!CustomChat.renderCommandSuggestionText(context, text, x, y, color)) {
         context.drawTextWithShadow(textRenderer, text, x, y, color);
      }
   }
}
