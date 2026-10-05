package fever.visual.mixin.minecraft.client;

import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.systems.modules.modules.visuals.CustomButtons;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NarratedMultilineTextWidget.class)
public class NarratedMultilineTextWidgetMixin implements IMinecraft {
   @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
   private void fevervisual$renderCustomSettingsMultilineTextWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      NarratedMultilineTextWidget widget = (NarratedMultilineTextWidget) (Object) this;
      if (!CustomButtons.isEnabledSafe() || !CustomButtons.shouldStyleCurrentSettingsScreen() || !widget.visible) {
         return;
      }

      String text = widget.getMessage().getString();
      if (text.isEmpty()) {
         ci.cancel();
         return;
      }

      CustomDrawContext customContext = CustomDrawContext.of(context);
      Font font = Fonts.MEDIUM.getFont(9.0F);
      float x = this.fevervisual$getCenteredTextX(widget, font, text);
      float y = widget.getY() + widget.getHeight() / 2.0F - font.height() / 2.0F;

      customContext.drawText(font, text, x, y, Colors.getTextColor());
      ci.cancel();
   }

   @Unique
   private float fevervisual$getCenteredTextX(NarratedMultilineTextWidget widget, Font font, String text) {
      return widget.getX() + widget.getWidth() / 2.0F - font.width(text) / 2.0F;
   }
}
