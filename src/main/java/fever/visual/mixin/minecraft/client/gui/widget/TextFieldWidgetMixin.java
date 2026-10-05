package fever.visual.mixin.minecraft.client.gui.widget;

import fever.visual.systems.modules.modules.visuals.CustomChat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextFieldWidget.class)
public class TextFieldWidgetMixin {
   @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Lnet/minecraft/text/OrderedText;)I"))
   private int fevervisual$getCustomChatOrderedWidth(TextRenderer renderer, OrderedText text) {
      return CustomChat.shouldUseCustomChatInputText() ? CustomChat.getCustomTextWidth(text) : renderer.getWidth(text);
   }

   @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Ljava/lang/String;)I"))
   private int fevervisual$getCustomChatStringWidth(TextRenderer renderer, String text) {
      return CustomChat.shouldUseCustomChatInputText() ? CustomChat.getCustomTextWidth(text) : renderer.getWidth(text);
   }

   @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V"))
   private void fevervisual$drawCustomChatOrderedText(DrawContext context, TextRenderer renderer, OrderedText text, int x, int y, int color, boolean shadow) {
      if (!CustomChat.renderChatInputOrderedText(context, text, x, y, color)) {
         context.drawText(renderer, text, x, y, color, shadow);
      }
   }

   @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V"))
   private void fevervisual$drawCustomChatStringText(DrawContext context, TextRenderer renderer, String text, int x, int y, int color, boolean shadow) {
      if (!CustomChat.renderChatInputStringText(context, text, x, y, color)) {
         context.drawText(renderer, text, x, y, color, shadow);
      }
   }

   @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"))
   private void fevervisual$drawCustomChatPlaceholderText(DrawContext context, TextRenderer renderer, Text text, int x, int y, int color) {
      if (!CustomChat.renderChatInputText(context, text, x, y, color)) {
         context.drawTextWithShadow(renderer, text, x, y, color);
      }
   }
}
