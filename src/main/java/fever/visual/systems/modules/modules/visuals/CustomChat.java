package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.mixin.accessors.ChatHudAccessor;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.systems.theme.Theme;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInfo(name = "Custom Chat", category = ModuleCategory.DISPLAY, desc = "modules.descriptions.custom_chat")
public class CustomChat extends BaseModule {
   private static final Pattern PLAYER_MESSAGE_PATTERN = Pattern.compile("^(<[^>]+>|\\[[^]]+]\\s*[^:]+:|[^:]{1,32}:)\\s*(.*)$");
   private static final float MESSAGE_PADDING_X = 7.0F;
   private static final int MESSAGE_PADDING_BOTTOM = 5;
   private static final int COMMAND_SUGGESTION_OFFSET_X = -4;
   private static final float CHAT_TEXT_SIZE = 9.0F;
   private static final int CHAT_LINE_HEIGHT = 13;
   private static boolean draggingScrollbar;
   private static int messageBackgroundFillCount;
   private static int messageBackgroundMinX;
   private static int messageBackgroundMinY;
   private static int messageBackgroundMaxX;
   private static int messageBackgroundMaxY;
   private static float messageBackgroundOpacity;
   private static boolean messageBackgroundFlushed;
   private static boolean renderingChatScreen;

   private final BooleanSetting customDesign = new BooleanSetting(
      this,
      "modules.settings.custom_chat.custom_design",
      "modules.settings.custom_chat.custom_design.description"
   ).enabled(true);
   private final SliderSetting backgroundOpacity = new SliderSetting(
      this,
      "modules.settings.custom_chat.background_opacity",
      "modules.settings.custom_chat.background_opacity.description"
   ).min(0.0F).max(1.0F).step(0.05F).currentValue(0.55F);
   private final SliderSetting inputOpacity = new SliderSetting(
      this,
      "modules.settings.custom_chat.input_opacity",
      "modules.settings.custom_chat.input_opacity.description"
   ).min(0.0F).max(1.0F).step(0.05F).currentValue(0.6F);

   public static boolean renderMessageBackground(DrawContext context, int x1, int y1, int x2, int y2, int vanillaColor) {
      CustomChat module = getActiveModule();
      if (module == null) {
         return false;
      }

      int width = x2 - x1;
      int height = y2 - y1;
      if (width <= 8 || height <= 4) {
         return true;
      }

      if (width > 20 && height >= CHAT_LINE_HEIGHT - 2) {
         float vanillaAlpha = ((vanillaColor >>> 24) & 0xFF) / 255.0F;
         float opacity = Math.clamp(module.backgroundOpacity.getCurrentValue() * Math.max(vanillaAlpha, 0.35F), 0.0F, 1.0F);
         collectMessageBackgroundBounds(x1, y1, x2, y2);
         messageBackgroundOpacity = Math.max(messageBackgroundOpacity, opacity);
      }

      return true;
   }

   public static boolean renderInputBackground(DrawContext context, int x1, int y1, int x2, int y2) {
      CustomChat module = getActiveModule();
      if (module == null) {
         return false;
      }

      int width = x2 - x1;
      int height = y2 - y1;
      if (width <= 8 || height <= 4) {
         return false;
      }

      float opacity = Math.clamp(module.inputOpacity.getCurrentValue(), 0.0F, 1.0F);
      drawPanel(context, x1 - 3, y1 - 3, x2 + 3, y2 + 3, opacity);
      return true;
   }

   public static boolean shouldRenderCustomText() {
      CustomChat module = getActiveModule();
      if (module == null) {
         return false;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if (renderingChatScreen || client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
         return true;
      }

      for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
         String className = element.getClassName();
         if (className.contains("ChatHud") || className.contains("ChatScreen")) {
            return true;
         }
      }

      return false;
   }

   public static void beginChatScreenRender() {
      renderingChatScreen = getActiveModule() != null;
   }

   public static void endChatScreenRender() {
      renderingChatScreen = false;
   }

   public static boolean renderChatText(DrawContext context, int y, float opacity, OrderedText text) {
      CustomChat module = getActiveModule();
      if (module == null || text == null) {
         return false;
      }

      String plain = orderedToString(text);
      if (plain.isEmpty()) {
         return true;
      }

      float alpha = Math.clamp(opacity, 0.0F, 1.0F);
      CustomDrawContext customContext = CustomDrawContext.of(context);
      flushMessageBackground(context);
      ChatParts parts = splitChatMessage(plain);
      float drawX = MESSAGE_PADDING_X - 1.0F;
      float drawY = y + (parts.name().isEmpty() ? 0.8F : 1.8F);

      drawVanillaOrderedText(customContext, text, drawX, drawY, alpha);
      return true;
   }

   public static boolean renderChatFrame(DrawContext context, int currentTick, boolean expanded) {
      CustomChat module = getActiveModule();
      MinecraftClient client = MinecraftClient.getInstance();
      if (module == null || client.inGameHud == null || client.options.getChatVisibility().getValue() == ChatVisibility.HIDDEN) {
         return false;
      }

      ChatHudAccessor accessor = (ChatHudAccessor)client.inGameHud.getChatHud();
      List<?> visibleMessages = accessor.fevervisual$getVisibleMessages();
      if (visibleMessages.isEmpty()) {
         return true;
      }

      beginChatRender();
      context.getMatrices().pushMatrix();
      try {
         float scale = (float)accessor.fevervisual$getChatScale();
         int width = MathHelper.ceil(accessor.fevervisual$getWidth() / scale);
         int bottom = MathHelper.floor((context.getScaledWindowHeight() - 40) / scale);
         float chatOpacity = client.options.getChatOpacity().getValue().floatValue() * 0.9F + 0.1F;
         float backgroundOpacity = client.options.getTextBackgroundOpacity().getValue().floatValue();
         int baseLineHeight = getLineHeight(9);
         double lineSpacing = client.options.getChatLineSpacing().getValue();
         int lineHeight = (int)(baseLineHeight * (lineSpacing + 1.0));
         int textOffset = (int)Math.round((baseLineHeight - 1.0) * (lineSpacing + 1.0) - 4.0 * lineSpacing);
         int linesToRender = Math.min(visibleMessages.size() - accessor.fevervisual$getScrolledLines(), accessor.fevervisual$getVisibleLineCount());

         context.getMatrices().scale(scale, scale);
         context.getMatrices().translate(4.0F, 0.0F);

         for (int k = linesToRender - 1; k >= 0; k--) {
            ChatHudLine.Visible visible = (ChatHudLine.Visible)visibleMessages.get(k + accessor.fevervisual$getScrolledLines());
            float opacity = getLineOpacity(visible, currentTick, expanded);
            if (opacity > 1.0E-5F) {
               int bottomY = bottom - k * lineHeight;
               int topY = bottomY - lineHeight;
               renderMessageBackground(context, -4, topY, width + 8, bottomY, Math.round(255.0F * opacity * backgroundOpacity) << 24);
            }
         }

         for (int k = linesToRender - 1; k >= 0; k--) {
            ChatHudLine.Visible visible = (ChatHudLine.Visible)visibleMessages.get(k + accessor.fevervisual$getScrolledLines());
            float opacity = getLineOpacity(visible, currentTick, expanded);
            if (opacity > 1.0E-5F) {
               int bottomY = bottom - k * lineHeight;
               renderChatText(context, bottomY - textOffset, opacity * chatOpacity, visible.content());
            }
         }
      } finally {
         context.getMatrices().popMatrix();
      }

      return true;
   }

   public static void renderScrollbar(DrawContext context) {
      ScrollbarMetrics metrics = getScrollbarMetrics();
      if (metrics == null) {
         return;
      }

      CustomDrawContext customContext = CustomDrawContext.of(context);
      customContext.drawRoundedRect(
         metrics.trackX(),
         metrics.trackY(),
         metrics.trackWidth(),
         metrics.trackHeight(),
         BorderRadius.all(2.0F),
         Colors.getAdditionalColor().withAlpha(90.0F)
      );
      customContext.drawRoundedRect(
         metrics.trackX(),
         metrics.thumbY(),
         metrics.trackWidth(),
         metrics.thumbHeight(),
         BorderRadius.all(2.0F),
         Colors.getAccentColor().withAlpha(210.0F)
      );
   }

   public static boolean handleScrollbarClick(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      }

      ScrollbarMetrics metrics = getScrollbarMetrics();
      if (metrics == null || !metrics.hovered(mouseX, mouseY)) {
         return false;
      }

      draggingScrollbar = true;
      updateScrollFromMouse(mouseY);
      return true;
   }

   public static boolean handleScrollbarDrag(double mouseY) {
      if (!draggingScrollbar) {
         return false;
      }

      updateScrollFromMouse(mouseY);
      return true;
   }

   public static void handleScrollbarRelease() {
      draggingScrollbar = false;
   }

   public static boolean shouldKeepHistory() {
      return getActiveModule() != null;
   }

   public static void beginChatRender() {
      messageBackgroundFillCount = 0;
      messageBackgroundMinX = Integer.MAX_VALUE;
      messageBackgroundMinY = Integer.MAX_VALUE;
      messageBackgroundMaxX = Integer.MIN_VALUE;
      messageBackgroundMaxY = Integer.MIN_VALUE;
      messageBackgroundOpacity = 0.0F;
      messageBackgroundFlushed = false;
   }

   public static boolean isActive() {
      return getActiveModule() != null;
   }

   public static boolean shouldUseCustomChatInputText() {
      return false;
   }

   public static boolean renderChatInputOrderedText(DrawContext context, OrderedText text, int x, int y, int color) {
      if (!shouldUseCustomChatInputText() || text == null) {
         return false;
      }

      renderPlainChatInputText(context, orderedToString(text), x, y, color);
      return true;
   }

   public static boolean renderChatInputStringText(DrawContext context, String text, int x, int y, int color) {
      if (!shouldUseCustomChatInputText() || text == null) {
         return false;
      }

      renderPlainChatInputText(context, text, x, y, color);
      return true;
   }

   public static boolean renderChatInputText(DrawContext context, net.minecraft.text.Text text, int x, int y, int color) {
      if (!shouldUseCustomChatInputText() || text == null) {
         return false;
      }

      renderPlainChatInputText(context, text.getString(), x, y, color);
      return true;
   }

   public static int getCustomTextWidth(OrderedText text) {
      return Math.round(Fonts.MEDIUM.getFont(CHAT_TEXT_SIZE).width(orderedToString(text)));
   }

   public static int getCustomTextWidth(String text) {
      return Math.round(Fonts.MEDIUM.getFont(CHAT_TEXT_SIZE).width(text));
   }

   public static int getLineHeight(int fallback) {
      return isActive() ? CHAT_LINE_HEIGHT : fallback;
   }

   private static float getLineOpacity(ChatHudLine.Visible line, int currentTick, boolean expanded) {
      if (expanded) {
         return 1.0F;
      }

      double progress = 1.0 - (currentTick - line.addedTime()) / 200.0;
      progress = MathHelper.clamp(progress * 10.0, 0.0, 1.0);
      progress *= progress;
      return (float)progress;
   }

   public static boolean renderCommandSuggestionMessages(
      DrawContext context,
      TextRenderer textRenderer,
      List<OrderedText> messages,
      int x,
      int width,
      boolean chatScreenSized,
      int ownerHeight
   ) {
      CustomChat module = getActiveModule();
      if (module == null) {
         return false;
      }

      if (messages == null || messages.isEmpty()) {
         return true;
      }

      int minY = Integer.MAX_VALUE;
      int maxY = Integer.MIN_VALUE;
      for (int i = 0; i < messages.size(); i++) {
         int y = chatScreenSized ? ownerHeight - 14 - 13 - 12 * i : 72 + 12 * i;
         minY = Math.min(minY, y);
         maxY = Math.max(maxY, y + 12);
      }

      int suggestionX = x + COMMAND_SUGGESTION_OFFSET_X;
      drawPanel(context, suggestionX - 3, minY - 2, suggestionX + width + 3, maxY + 2, Math.max(0.75F, module.inputOpacity.getCurrentValue()));
      CustomDrawContext customContext = CustomDrawContext.of(context);
      for (int i = 0; i < messages.size(); i++) {
         int y = chatScreenSized ? ownerHeight - 14 - 13 - 12 * i : 72 + 12 * i;
         drawVanillaOrderedText(customContext, messages.get(i), suggestionX, y + 1.4F, 1.0F);
      }

      return true;
   }

   public static boolean renderCommandSuggestionWindowBackground(DrawContext context, int x, int y, int width, int height) {
      CustomChat module = getActiveModule();
      if (module == null) {
         return false;
      }

      int suggestionX = x + COMMAND_SUGGESTION_OFFSET_X;
      drawPanel(context, suggestionX - 3, y - 2, suggestionX + width + 3, y + height + 2, Math.max(0.75F, module.inputOpacity.getCurrentValue()));
      return true;
   }

   public static boolean renderCommandSuggestionText(DrawContext context, String text, int x, int y, int color) {
      if (getActiveModule() == null || text == null || text.isEmpty()) {
         return false;
      }

      CustomDrawContext customContext = CustomDrawContext.of(context);
      customContext.drawText(MinecraftClient.getInstance().textRenderer, text, x + COMMAND_SUGGESTION_OFFSET_X, y, normalizeColor(color), true);
      return true;
   }

   public static int getCommandSuggestionOffsetX() {
      return isActive() ? COMMAND_SUGGESTION_OFFSET_X : 0;
   }

   private static CustomChat getActiveModule() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.currentScreen != null && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) {
         return null;
      }
      if (FeverVisual.getInstance().getModuleManager() == null) {
         return null;
      }

      CustomChat module = FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomChat.class);
      return module != null && module.isEnabled() && module.customDesign.isEnabled() ? module : null;
   }

   private static void renderPlainChatInputText(DrawContext context, String text, int x, int y, int color) {
      if (text == null || text.isEmpty()) {
         return;
      }

      CustomDrawContext customContext = CustomDrawContext.of(context);
      customContext.drawText(Fonts.MEDIUM.getFont(CHAT_TEXT_SIZE), text, x, y, ColorRGBA.fromInt(normalizeColor(color)));
   }

   private static ScrollbarMetrics getScrollbarMetrics() {
      CustomChat module = getActiveModule();
      MinecraftClient client = MinecraftClient.getInstance();
      if (module == null || client.inGameHud == null) {
         return null;
      }

      ChatHud chatHud = client.inGameHud.getChatHud();
      ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
      int totalLines = accessor.fevervisual$getVisibleMessages().size();
      int visibleLines = accessor.fevervisual$getVisibleLineCount();
      int maxScroll = Math.max(0, totalLines - visibleLines);
      if (maxScroll <= 0) {
         return null;
      }

      float scale = (float)accessor.fevervisual$getChatScale();
      float trackHeight = Math.max(20.0F, accessor.fevervisual$getHeight() * scale);
      float trackWidth = 3.0F;
      float trackX = 5.0F + accessor.fevervisual$getWidth() * scale + 4.0F;
      float trackY = client.getWindow().getScaledHeight() - 40.0F - trackHeight;
      float thumbHeight = Math.max(18.0F, trackHeight * visibleLines / Math.max(1.0F, totalLines));
      float progress = accessor.fevervisual$getScrolledLines() / (float)maxScroll;
      float thumbY = trackY + (trackHeight - thumbHeight) * (1.0F - Math.clamp(progress, 0.0F, 1.0F));
      return new ScrollbarMetrics(trackX, trackY, trackWidth, trackHeight, thumbY, thumbHeight, maxScroll);
   }

   private static void updateScrollFromMouse(double mouseY) {
      ScrollbarMetrics metrics = getScrollbarMetrics();
      if (metrics == null) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      ChatHudAccessor accessor = (ChatHudAccessor)client.inGameHud.getChatHud();
      float range = Math.max(1.0F, metrics.trackHeight() - metrics.thumbHeight());
      float progress = Math.clamp((float)(mouseY - metrics.trackY() - metrics.thumbHeight() * 0.5F) / range, 0.0F, 1.0F);
      int targetScroll = Math.round((1.0F - progress) * metrics.maxScroll());
      accessor.fevervisual$scroll(targetScroll - accessor.fevervisual$getScrolledLines());
   }

   private static void drawPanel(DrawContext context, int x1, int y1, int x2, int y2, float opacity) {
      float alpha = Math.clamp(opacity, 0.0F, 1.0F);
      float width = x2 - x1;
      float height = y2 - y1;
      CustomDrawContext customContext = CustomDrawContext.of(context);
      boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      float glass = Interface.glass();
      float minimalism = Interface.minimalizm();

      if (Interface.showMinimalizm()) {
         customContext.drawBlurredRect(
            x1,
            y1,
            width,
            height,
            45.0F,
            5.0F,
            BorderRadius.all(5.0F),
            ColorRGBA.WHITE.withAlpha(255.0F * alpha * minimalism)
         );
      }
      if (Interface.showGlass()) {
         customContext.drawLiquidGlass(
            x1,
            y1,
            width,
            height,
            7.0F,
            0.08F,
            BorderRadius.all(6.0F),
            ColorRGBA.WHITE.withAlpha(255.0F * alpha * glass)
         );
      }

      customContext.drawSquircle(
         x1,
         y1,
         width,
         height,
         7.0F,
         BorderRadius.all(6.0F),
         Colors.getBackgroundColor().withAlpha(255.0F * (dark ? 0.8F - 0.6F * glass : 0.7F) * alpha)
      );
   }

   private static void collectMessageBackgroundBounds(int x1, int y1, int x2, int y2) {
      messageBackgroundFillCount++;
      messageBackgroundMinX = Math.min(messageBackgroundMinX, x1);
      messageBackgroundMinY = Math.min(messageBackgroundMinY, y1);
      messageBackgroundMaxX = Math.max(messageBackgroundMaxX, x2);
      messageBackgroundMaxY = Math.max(messageBackgroundMaxY, y2);
   }

   private static void flushMessageBackground(DrawContext context) {
      if (messageBackgroundFlushed || messageBackgroundFillCount <= 0) {
         return;
      }

      drawPanel(
         context,
         messageBackgroundMinX - (int)MESSAGE_PADDING_X,
         messageBackgroundMinY + 1,
         messageBackgroundMaxX + (int)MESSAGE_PADDING_X,
         messageBackgroundMaxY + MESSAGE_PADDING_BOTTOM,
         Math.max(0.92F, messageBackgroundOpacity)
      );
      messageBackgroundFlushed = true;
   }

   private static void drawVanillaOrderedText(CustomDrawContext context, OrderedText text, float x, float y, float alpha) {
      int color = Math.round(255.0F * Math.clamp(alpha, 0.0F, 1.0F)) << 24 | 0xFFFFFF;
      context.drawText(MinecraftClient.getInstance().textRenderer, text, Math.round(x), Math.round(y), color, true);
   }

   private static void drawOrderedText(CustomDrawContext context, OrderedText text, float x, float y, float alpha) {
      float drawX = x;
      for (ChatTextSegment segment : splitOrderedText(text, alpha)) {
         Font font = segment.bold() ? Fonts.SEMIBOLD.getFont(CHAT_TEXT_SIZE) : Fonts.MEDIUM.getFont(CHAT_TEXT_SIZE);
         context.drawText(font, segment.text(), drawX, y, segment.color());
         drawX += font.width(segment.text());
      }
   }

   private static void drawPlayerMessage(CustomDrawContext context, OrderedText text, String plain, ChatParts parts, float x, float y, float alpha) {
      List<ChatTextSegment> segments = splitOrderedText(text, alpha);
      int nameEnd = Math.min(parts.name().length(), plain.length());
      int messageStart = parts.message().isEmpty() ? nameEnd : Math.max(nameEnd, plain.indexOf(parts.message(), nameEnd));
      if (messageStart < nameEnd) {
         messageStart = nameEnd;
      }

      float drawX = drawSegmentRange(context, segments, 0, nameEnd, x, y);
      if (!parts.name().endsWith(":")) {
         ColorRGBA colonColor = getSegmentColorAt(segments, Math.max(0, nameEnd - 1));
         Font colonFont = Fonts.SEMIBOLD.getFont(CHAT_TEXT_SIZE);
         context.drawText(colonFont, ":", drawX, y, colonColor);
         drawX += colonFont.width(":");
      }
      drawX += 3.0F;
      drawSegmentRange(context, segments, messageStart, plain.length(), drawX, y);
   }

   private static float drawSegmentRange(CustomDrawContext context, List<ChatTextSegment> segments, int start, int end, float x, float y) {
      int cursor = 0;
      float drawX = x;
      for (ChatTextSegment segment : segments) {
         int segmentStart = cursor;
         int segmentEnd = cursor + segment.text().length();
         cursor = segmentEnd;
         if (segmentEnd <= start || segmentStart >= end) {
            continue;
         }

         int localStart = Math.max(0, start - segmentStart);
         int localEnd = Math.min(segment.text().length(), end - segmentStart);
         if (localStart >= localEnd) {
            continue;
         }

         String text = segment.text().substring(localStart, localEnd);
         Font font = segment.bold() ? Fonts.SEMIBOLD.getFont(CHAT_TEXT_SIZE) : Fonts.MEDIUM.getFont(CHAT_TEXT_SIZE);
         context.drawText(font, text, drawX, y, segment.color());
         drawX += font.width(text);
      }

      return drawX;
   }

   private static ColorRGBA getSegmentColorAt(List<ChatTextSegment> segments, int index) {
      int cursor = 0;
      for (ChatTextSegment segment : segments) {
         int next = cursor + segment.text().length();
         if (index >= cursor && index < next) {
            return segment.color();
         }
         cursor = next;
      }

      return ColorRGBA.WHITE;
   }

   private static List<ChatTextSegment> splitOrderedText(OrderedText text, float alpha) {
      List<ChatTextSegment> segments = new ArrayList<>();
      StringBuilder builder = new StringBuilder();
      int[] currentColor = {0xFFFFFFFF};
      boolean[] currentBold = {false};
      boolean[] hasSegment = {false};

      text.accept((index, style, codePoint) -> {
         int color = getStyleColor(style, alpha);
         boolean bold = style.isBold();
         if (hasSegment[0] && (color != currentColor[0] || bold != currentBold[0])) {
            segments.add(new ChatTextSegment(builder.toString(), ColorRGBA.fromInt(currentColor[0]), currentBold[0]));
            builder.setLength(0);
         }

         currentColor[0] = color;
         currentBold[0] = bold;
         hasSegment[0] = true;
         builder.appendCodePoint(codePoint);
         return true;
      });

      if (builder.length() > 0) {
         segments.add(new ChatTextSegment(builder.toString(), ColorRGBA.fromInt(currentColor[0]), currentBold[0]));
      }

      return segments;
   }

   private static int getStyleColor(Style style, float alpha) {
      TextColor textColor = style.getColor();
      int rgb = textColor != null ? textColor.getRgb() : 0xFFFFFF;
      int colorAlpha = Math.round(255.0F * Math.clamp(alpha, 0.0F, 1.0F));
      return colorAlpha << 24 | rgb & 0xFFFFFF;
   }

   private static ChatParts splitChatMessage(String text) {
      Matcher matcher = PLAYER_MESSAGE_PATTERN.matcher(text);
      if (!matcher.matches()) {
         return new ChatParts("", text);
      }

      String name = matcher.group(1);
      String message = matcher.group(2);
      return new ChatParts(name, message == null || message.isBlank() ? "" : message);
   }

   private static ColorRGBA getTextColor(int color) {
      int adjusted = color;
      if ((adjusted & 0xFF000000) == 0) {
         adjusted |= 0xFF000000;
      }

      ColorRGBA vanillaColor = ColorRGBA.fromInt(adjusted);
      boolean grayscale = Math.abs(vanillaColor.getRed() - vanillaColor.getGreen()) < 2.0F
         && Math.abs(vanillaColor.getGreen() - vanillaColor.getBlue()) < 2.0F;
      return grayscale ? ColorRGBA.WHITE.withAlpha(vanillaColor.getAlpha()) : vanillaColor;
   }

   private static int normalizeColor(int color) {
      return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
   }

   private static String orderedToString(OrderedText orderedText) {
      StringBuilder builder = new StringBuilder();
      orderedText.accept((index, style, codePoint) -> {
         builder.appendCodePoint(codePoint);
         return true;
      });
      return builder.toString();
   }

   private record ScrollbarMetrics(float trackX, float trackY, float trackWidth, float trackHeight, float thumbY, float thumbHeight, int maxScroll) {
      boolean hovered(double mouseX, double mouseY) {
         return mouseX >= this.trackX - 3.0F
            && mouseX <= this.trackX + this.trackWidth + 3.0F
            && mouseY >= this.trackY
            && mouseY <= this.trackY + this.trackHeight;
      }
   }

   private record ChatParts(String name, String message) {
   }

   private record ChatTextSegment(String text, ColorRGBA color, boolean bold) {
   }
}
