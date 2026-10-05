package fever.visual.mixin.minecraft.client.gui;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.mixin.accessors.HandledScreenAccessor;
import fever.visual.systems.modules.modules.visuals.CustomButtons;
import fever.visual.systems.modules.modules.visuals.CustomChat;
import fever.visual.systems.modules.modules.visuals.CustomInv;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.GuiPanelStyle;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.render.RenderLayer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.function.Function;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin implements IMinecraft {
   @Unique
   private static final float fevervisual$TOOLTIP_FONT_SIZE = 8.7F;

   @Unique
   private static final float fevervisual$TOOLTIP_EMPHASIS_FONT_SIZE = 9.4F;

   @Unique
   private static final int fevervisual$BACKGROUND_PADDING_X = 4;

   @Unique
   private static final int fevervisual$BACKGROUND_PADDING_Y = 4;

   @Unique
   private static final int fevervisual$LINE_STEP = 9;

   @Unique
   private static final int fevervisual$FIRST_LINE_GAP = 1;

   @Unique
   private static final float fevervisual$TOOLTIP_Z = 400.0F;

   @Unique
   private static final Pattern fevervisual$COMPONENTS_LINE_PATTERN =
      Pattern.compile(
         ".*\\b\\d+\\s+(components?|\\u043A\\u043E\\u043C\\u043F\\u043E\\u043D\\u0435\\u043D\\u0442(?:\\u043E\\u0432|\\u0430|\\u044B)?)\\b.*",
         Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
      );

   @Inject(
      method = "drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void fevervisual$replaceHandledScreenBackgroundTexture(
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int textureWidth,
      int textureHeight,
      CallbackInfo ci
   ) {
      if (!this.fevervisual$isCustomInventoryEnabled()) {
         return;
      }

      if (!(mc.currentScreen instanceof HandledScreen<?> handled)) {
         return;
      }

      if (handled instanceof InventoryScreen || handled instanceof CreativeInventoryScreen) {
         return;
      }

      HandledScreenAccessor accessor = (HandledScreenAccessor) handled;
      if (x != accessor.getX() || y != accessor.getY()) {
         return;
      }

      if (width != accessor.getBackgroundWidth() || height != accessor.getBackgroundHeight()) {
         return;
      }

      if (Math.abs(u) > 0.001F || Math.abs(v) > 0.001F) {
         return;
      }

      GuiPanelStyle.drawPanel(CustomDrawContext.of((DrawContext) (Object) this), x, y, width, height, 7.0F);
      ci.cancel();
   }

   @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomButtonsStringText(TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomButtonsText() || text == null || text.isEmpty()) {
         return;
      }

      this.fevervisual$renderCustomButtonsText(text, x, y, color);
      ci.cancel();
   }

   @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomButtonsText(TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomButtonsText() || text == null) {
         return;
      }

      String plain = text.getString();
      if (plain.isEmpty()) {
         return;
      }

      this.fevervisual$renderCustomButtonsText(plain, x, y, color);
      ci.cancel();
   }

   @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomButtonsOrderedText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomButtonsText() || text == null) {
         return;
      }

      String plain = this.fevervisual$orderedToString(text);
      if (plain.isEmpty()) {
         return;
      }

      this.fevervisual$renderCustomButtonsText(plain, x, y, color);
      ci.cancel();
   }

   @Inject(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomButtonsStringTextWithShadow(TextRenderer textRenderer, String text, int x, int y, int color, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomButtonsText() || text == null || text.isEmpty()) {
         return;
      }

      this.fevervisual$renderCustomButtonsText(text, x, y, color);
      ci.cancel();
   }

   @Inject(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomButtonsTextWithShadow(TextRenderer textRenderer, Text text, int x, int y, int color, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomButtonsText() || text == null) {
         return;
      }

      String plain = text.getString();
      if (plain.isEmpty()) {
         return;
      }

      this.fevervisual$renderCustomButtonsText(plain, x, y, color);
      ci.cancel();
   }

   @Inject(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomButtonsOrderedTextWithShadow(TextRenderer textRenderer, OrderedText text, int x, int y, int color, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomButtonsText() || text == null) {
         return;
      }

      String plain = this.fevervisual$orderedToString(text);
      if (plain.isEmpty()) {
         return;
      }

      this.fevervisual$renderCustomButtonsText(plain, x, y, color);
      ci.cancel();
   }

   @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawTooltip(TextRenderer textRenderer, List<Text> text, int x, int y, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomTooltip() || text == null || text.isEmpty()) {
         return;
      }

      this.fevervisual$renderTextTooltip(textRenderer, text, HoveredTooltipPositioner.INSTANCE, x, y);
      ci.cancel();
   }

   @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/util/Identifier;)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawTooltip(TextRenderer textRenderer, List<Text> text, int x, int y, @Nullable Identifier texture, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomTooltip() || text == null || text.isEmpty()) {
         return;
      }

      this.fevervisual$renderTextTooltip(textRenderer, text, HoveredTooltipPositioner.INSTANCE, x, y);
      ci.cancel();
   }

   @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawTooltip(TextRenderer textRenderer, List<Text> text, Optional<?> data, int x, int y, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomTooltip() || text == null || text.isEmpty()) {
         return;
      }

      this.fevervisual$renderTextTooltip(textRenderer, text, HoveredTooltipPositioner.INSTANCE, x, y);
      ci.cancel();
   }

   @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawTooltip(TextRenderer textRenderer, List<Text> text, Optional<?> data, int x, int y, @Nullable Identifier texture, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomTooltip() || text == null || text.isEmpty()) {
         return;
      }

      this.fevervisual$renderTextTooltip(textRenderer, text, HoveredTooltipPositioner.INSTANCE, x, y);
      ci.cancel();
   }

   @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;II)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawTooltip(TextRenderer textRenderer, Text text, int x, int y, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomTooltip() || text == null) {
         return;
      }

      this.fevervisual$renderTextTooltip(textRenderer, List.of(text), HoveredTooltipPositioner.INSTANCE, x, y);
      ci.cancel();
   }

   @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IILnet/minecraft/util/Identifier;)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawTooltip(TextRenderer textRenderer, Text text, int x, int y, @Nullable Identifier texture, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomTooltip() || text == null) {
         return;
      }

      this.fevervisual$renderTextTooltip(textRenderer, List.of(text), HoveredTooltipPositioner.INSTANCE, x, y);
      ci.cancel();
   }

   @Inject(method = "drawOrderedTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawOrderedTooltip(TextRenderer textRenderer, List<? extends OrderedText> text, int x, int y, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomTooltip() || text == null || text.isEmpty()) {
         return;
      }

      this.fevervisual$renderOrderedTooltip(textRenderer, text, HoveredTooltipPositioner.INSTANCE, x, y);
      ci.cancel();
   }

   @Inject(method = "drawOrderedTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/util/Identifier;)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawOrderedTooltip(TextRenderer textRenderer, List<? extends OrderedText> text, int x, int y, @Nullable Identifier texture, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomTooltip() || text == null || text.isEmpty()) {
         return;
      }

      this.fevervisual$renderOrderedTooltip(textRenderer, text, HoveredTooltipPositioner.INSTANCE, x, y);
      ci.cancel();
   }

   @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Lnet/minecraft/client/gui/tooltip/TooltipPositioner;IIZ)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawTooltip(TextRenderer textRenderer, List<OrderedText> text, TooltipPositioner positioner, int x, int y, boolean focused, CallbackInfo ci) {
      if (!this.fevervisual$shouldRenderCustomTooltip() || text == null || text.isEmpty()) {
         return;
      }

      this.fevervisual$renderOrderedTooltip(textRenderer, text, positioner, x, y);
      ci.cancel();
   }

   @Unique
   private boolean fevervisual$shouldRenderCustomTooltip() {
      if (mc.currentScreen == null) {
         return false;
      }

      return this.fevervisual$isCustomInventoryEnabled()
         || (CustomButtons.isEnabledSafe() && CustomButtons.shouldStyleCurrentButtonScreen());
   }

   @Unique
   private boolean fevervisual$shouldRenderCustomButtonsText() {
      return CustomButtons.isEnabledSafe() && CustomButtons.shouldStyleCurrentSettingsScreen();
   }

   @Unique
   private void fevervisual$renderCustomButtonsText(String text, int x, int y, int color) {
      CustomDrawContext customContext = CustomDrawContext.of((DrawContext) (Object) this);
      customContext.drawText(
         Fonts.MEDIUM.getFont(9.0F),
         text,
         x,
         y,
         this.fevervisual$getCustomButtonsTextColor(color)
      );
   }

   @Unique
   private ColorRGBA fevervisual$getCustomButtonsTextColor(int color) {
      int adjusted = color;
      if ((adjusted & 0xFF000000) == 0) {
         adjusted |= 0xFF000000;
      }

      ColorRGBA vanillaColor = ColorRGBA.fromInt(adjusted);
      boolean grayscale = Math.abs(vanillaColor.getRed() - vanillaColor.getGreen()) < 2.0F
         && Math.abs(vanillaColor.getGreen() - vanillaColor.getBlue()) < 2.0F;

      if (!grayscale) {
         return vanillaColor;
      }

      float brightnessAlpha = Math.max(85.0F, vanillaColor.getRed()) / 255.0F;
      return Colors.getTextColor().withAlpha(vanillaColor.getAlpha() * brightnessAlpha);
   }

   @Unique
   private boolean fevervisual$isCustomInventoryEnabled() {
      if (FeverVisual.getInstance().getModuleManager() == null) {
         return false;
      }

      CustomInv customInv = FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomInv.class);
      return customInv != null && customInv.isEnabled();
   }

   @Unique
   private void fevervisual$renderTextTooltip(TextRenderer textRenderer, List<Text> lines, TooltipPositioner positioner, int x, int y) {
      DrawContext context = (DrawContext)(Object)this;
      context.createNewRootLayer();
      CustomDrawContext customContext = CustomDrawContext.isolated(context);
      Font baseFont = Fonts.MEDIUM.getFont(fevervisual$TOOLTIP_FONT_SIZE);
      Font emphasisFont = Fonts.SEMIBOLD.getFont(fevervisual$TOOLTIP_EMPHASIS_FONT_SIZE);

      int tooltipWidth = this.fevervisual$getTooltipWidthForTextLines(textRenderer, baseFont, emphasisFont, lines);
      int tooltipHeight = this.fevervisual$getTooltipHeight(lines.size());
      int backgroundWidth = tooltipWidth + fevervisual$BACKGROUND_PADDING_X * 2;
      int backgroundHeight = tooltipHeight + fevervisual$BACKGROUND_PADDING_Y * 2;

      Vector2ic pos = positioner.getPosition(
         context.getScaledWindowWidth(),
         context.getScaledWindowHeight(),
         x,
         y,
         backgroundWidth,
         backgroundHeight
      );

      int backgroundX = pos.x();
      int backgroundY = pos.y();
      int drawX = backgroundX + fevervisual$BACKGROUND_PADDING_X;
      int drawY = backgroundY + fevervisual$BACKGROUND_PADDING_Y;

      customContext.pushMatrix();
      customContext.getMatrices().translate(0.0F, 0.0F, fevervisual$TOOLTIP_Z);

      GuiPanelStyle.drawPanel(customContext, backgroundX, backgroundY, backgroundWidth, backgroundHeight, 6.0F, 0.98F);

      int lineY = drawY;

      for (int i = 0; i < lines.size(); i++) {
         Text line = lines.get(i);
         String plain = line.getString();
         boolean emphasized = this.fevervisual$isEmphasizedLine(plain);
         Font font = emphasized ? emphasisFont : baseFont;

         customContext.drawText(font, line, drawX, lineY);

         lineY += fevervisual$LINE_STEP;
         if (i == 0) {
            lineY += fevervisual$FIRST_LINE_GAP;
         }
      }

      customContext.popMatrix();
   }

   @Unique
   private void fevervisual$renderOrderedTooltip(TextRenderer textRenderer, List<? extends OrderedText> lines, TooltipPositioner positioner, int x, int y) {
      DrawContext context = (DrawContext)(Object)this;
      context.createNewRootLayer();
      CustomDrawContext customContext = CustomDrawContext.isolated(context);
      Font baseFont = Fonts.MEDIUM.getFont(fevervisual$TOOLTIP_FONT_SIZE);
      Font emphasisFont = Fonts.SEMIBOLD.getFont(fevervisual$TOOLTIP_EMPHASIS_FONT_SIZE);

      int tooltipWidth = this.fevervisual$getTooltipWidthForOrderedLines(textRenderer, baseFont, emphasisFont, lines);
      int tooltipHeight = this.fevervisual$getTooltipHeight(lines.size());
      int backgroundWidth = tooltipWidth + fevervisual$BACKGROUND_PADDING_X * 2;
      int backgroundHeight = tooltipHeight + fevervisual$BACKGROUND_PADDING_Y * 2;

      Vector2ic pos = positioner.getPosition(
         context.getScaledWindowWidth(),
         context.getScaledWindowHeight(),
         x,
         y,
         backgroundWidth,
         backgroundHeight
      );

      int backgroundX = pos.x();
      int backgroundY = pos.y();
      int drawX = backgroundX + fevervisual$BACKGROUND_PADDING_X;
      int drawY = backgroundY + fevervisual$BACKGROUND_PADDING_Y;

      customContext.pushMatrix();
      customContext.getMatrices().translate(0.0F, 0.0F, fevervisual$TOOLTIP_Z);

      GuiPanelStyle.drawPanel(customContext, backgroundX, backgroundY, backgroundWidth, backgroundHeight, 6.0F, 0.98F);

      int lineY = drawY;

      for (int i = 0; i < lines.size(); i++) {
         OrderedText line = lines.get(i);
         customContext.drawText(textRenderer, line, drawX, lineY, 0xFFFFFFFF, false);

         lineY += fevervisual$LINE_STEP;
         if (i == 0) {
            lineY += fevervisual$FIRST_LINE_GAP;
         }
      }

      customContext.popMatrix();
   }

   @Unique
   private int fevervisual$getTooltipWidthForTextLines(TextRenderer textRenderer, Font baseFont, Font emphasisFont, List<Text> lines) {
      int maxWidth = 0;

      for (Text line : lines) {
         String plain = line.getString();
         Font font = this.fevervisual$isEmphasizedLine(plain) ? emphasisFont : baseFont;
         int vanillaWidth = textRenderer.getWidth(line);
         int customWidth = Math.round(font.width(plain));
         int adjustedWidth = Math.min(Math.max(vanillaWidth, customWidth), vanillaWidth + 10);
         maxWidth = Math.max(maxWidth, adjustedWidth);
      }

      return maxWidth + 1;
   }

   @Unique
   private int fevervisual$getTooltipWidthForOrderedLines(TextRenderer textRenderer, Font baseFont, Font emphasisFont, List<? extends OrderedText> lines) {
      int maxWidth = 0;

      for (OrderedText line : lines) {
         String plain = this.fevervisual$orderedToString(line);
         Font font = this.fevervisual$isEmphasizedLine(plain) ? emphasisFont : baseFont;
         int vanillaWidth = textRenderer.getWidth(line);
         int customWidth = Math.round(font.width(plain));
         int adjustedWidth = Math.min(Math.max(vanillaWidth, customWidth), vanillaWidth + 10);
         maxWidth = Math.max(maxWidth, adjustedWidth);
      }

      return maxWidth + 1;
   }

   @Unique
   private int fevervisual$getTooltipHeight(int lineCount) {
      if (lineCount <= 0) {
         return 0;
      }

      return Math.max(8, lineCount * fevervisual$LINE_STEP + (lineCount > 1 ? fevervisual$FIRST_LINE_GAP : -1));
   }

   @Unique
   private boolean fevervisual$isEmphasizedLine(String line) {
      return this.fevervisual$isNamespaceLine(line) || this.fevervisual$isComponentsLine(line);
   }

   @Unique
   private boolean fevervisual$isNamespaceLine(String line) {
      return line != null && line.trim().toLowerCase(Locale.ROOT).startsWith("minecraft:");
   }

   @Unique
   private boolean fevervisual$isComponentsLine(String line) {
      return line != null && fevervisual$COMPONENTS_LINE_PATTERN.matcher(line.trim().toLowerCase(Locale.ROOT)).matches();
   }

   @Unique
   private ColorRGBA fevervisual$getEmphasisColor(String line) {
      ColorRGBA textColor = Colors.getTextColor();
      boolean brightText = this.fevervisual$isBright(textColor);

      if (this.fevervisual$isNamespaceLine(line)) {
         return textColor.mix(Colors.getAccentColor(), brightText ? 0.42F : 0.34F).withAlpha(255.0F);
      }

      return (brightText ? textColor.mix(ColorRGBA.WHITE, 0.2F) : textColor.mix(ColorRGBA.BLACK, 0.2F)).withAlpha(245.0F);
   }

   @Unique
   private boolean fevervisual$isBright(ColorRGBA color) {
      return (color.getRed() + color.getGreen() + color.getBlue()) / 3.0F > 145.0F;
   }

   @Unique
   private String fevervisual$orderedToString(OrderedText orderedText) {
      StringBuilder builder = new StringBuilder();
      orderedText.accept((index, style, codePoint) -> {
         builder.appendCodePoint(codePoint);
         return true;
      });
      return builder.toString();
   }
}
