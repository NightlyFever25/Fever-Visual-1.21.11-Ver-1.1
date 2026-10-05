package fever.visual.mixin.minecraft.client.gui.screen.recipebook;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.modules.visuals.CustomInv;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.GuiPanelStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookWidget.class)
public abstract class RecipeBookWidgetMixin implements IMinecraft {
   @Shadow
   private TextFieldWidget searchField;

   @Shadow
   protected CyclingButtonWidget<Boolean> toggleCraftableButton;

   @Inject(method = "initialize", at = @At("TAIL"))
   private void fevervisual$initCustomControls(int parentWidth, int parentHeight, MinecraftClient client, boolean narrow, CallbackInfo ci) {
      if (this.searchField == null) {
         return;
      }

      if (this.fevervisual$isCustomInventoryEnabled()) {
         this.searchField.setDrawsBackground(false);
         this.searchField.setPlaceholder(Text.empty());
         this.searchField.setEditableColor(Colors.getTextColor().withAlpha(255.0F).getRGB());
      } else {
         this.searchField.setDrawsBackground(true);
         this.searchField.setPlaceholder(Text.translatable("gui.recipebook.search_hint"));
         this.searchField.setEditableColor(16777215);
      }
   }

   @Redirect(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"
      )
   )
   private void fevervisual$replaceRecipeBookBackground(
      DrawContext context,
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int textureWidth,
      int textureHeight
   ) {
      if (!this.fevervisual$isCustomInventoryEnabled()) {
         context.drawTexture(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
         return;
      }

      GuiPanelStyle.drawPanel(CustomDrawContext.of(context), x, y, width, height, 7.0F);
   }

   @Redirect(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"
      )
   )
   private void fevervisual$renderCustomSearchField(TextFieldWidget field, DrawContext context, int mouseX, int mouseY, float delta) {
      if (!this.fevervisual$isCustomInventoryEnabled()) {
         field.render(context, mouseX, mouseY, delta);
         return;
      }

      CustomDrawContext customContext = CustomDrawContext.of(context);
      GuiPanelStyle.drawPanel(customContext, field.getX() - 2.0F, field.getY() - 1.0F, field.getWidth() + 4.0F, field.getHeight() + 2.0F, 5.0F, field.isFocused() ? 1.0F : 0.95F);

      field.setDrawsBackground(false);
      field.setPlaceholder(Text.empty());
      field.setEditableColor(0x00000000);
      field.setUneditableColor(0x00000000);

      Font textFont = Fonts.MEDIUM.getFont(7.3F);
      String inputText = field.getText();
      boolean empty = inputText == null || inputText.isEmpty();
      String displayText = empty ? "\u041D\u0430\u0439\u0442\u0438..." : inputText;
      float textWidth = textFont.width(displayText);
      float textX = field.getX() + field.getWidth() / 2.0F - textWidth / 2.0F;
      float textY = field.getY() + field.getHeight() / 2.0F - textFont.height() / 2.0F + 0.5F;
      context.enableScissor(field.getX() + 2, field.getY() + 1, field.getX() + field.getWidth() - 2, field.getY() + field.getHeight() - 1);
      customContext.drawText(textFont, displayText, textX, textY, empty ? Colors.getTextColor().withAlpha(130.0F) : Colors.getTextColor());

      if (field.isFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
         int cursor = Math.clamp(field.getCursor(), 0, empty ? 0 : inputText.length());
         String left = empty ? "" : inputText.substring(0, cursor);
         float cursorX = field.getX() + field.getWidth() / 2.0F - (empty ? 0.0F : textFont.width(inputText)) / 2.0F + textFont.width(left);
         customContext.drawRect(cursorX + 0.4F, textY + 0.7F, 1.0F, Math.max(6.0F, textFont.height() - 1.4F), Colors.getTextColor().withAlpha(210.0F));
      }

      context.disableScissor();
   }

   @Redirect(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/widget/CyclingButtonWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"
      )
   )
   private void fevervisual$renderCustomCraftableToggle(CyclingButtonWidget<Boolean> button, DrawContext context, int mouseX, int mouseY, float delta) {
      if (!this.fevervisual$isCustomInventoryEnabled()) {
         button.render(context, mouseX, mouseY, delta);
         return;
      }

      CustomDrawContext customContext = CustomDrawContext.of(context);
      GuiPanelStyle.drawPanel(customContext, button.getX(), button.getY(), button.getWidth(), button.getHeight(), 5.0F, 0.98F);

      Font labelFont = Fonts.SEMIBOLD.getFont(6.7F);
      String label = button.getValue() ? "\u041A\u0440" : "\u0412\u0441\u0435";
      float textX = button.getX() + button.getWidth() / 2.0F - labelFont.width(label) / 2.0F;
      float textY = button.getY() + button.getHeight() / 2.0F - labelFont.height() / 2.0F + 0.5F;
      customContext.drawText(labelFont, label, textX, textY, Colors.getTextColor());

      if (button.getValue()) {
         customContext.drawRoundedRect(
            button.getX() + button.getWidth() - 5.0F,
            button.getY() + 2.0F,
            2.5F,
            2.5F,
            BorderRadius.all(1.25F),
            Colors.getAccentColor().withAlpha(230.0F)
         );
      }
   }

   @Unique
   private boolean fevervisual$isCustomInventoryEnabled() {
      if (FeverVisual.getInstance().getModuleManager() == null) {
         return false;
      }

      CustomInv module = FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomInv.class);
      return module != null && module.isEnabled();
   }
}
