package fever.visual.ui.hud.impl;

import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import net.minecraft.client.network.PlayerListEntry;

public class Watermark extends HudElement {
   private final BooleanSetting showPing = new BooleanSetting(this, "hud.watermark.show_ping").enabled(true);
   private final BooleanSetting showFps = new BooleanSetting(this, "hud.watermark.show_fps").enabled(true);
   private final BooleanSetting showBps = new BooleanSetting(this, "hud.watermark.show_bps").enabled(true);
   private final BooleanSetting ySpeed = new BooleanSetting(this, "hud.watermark.speedY").enable();
   private final ModeSetting colorMode = new ModeSetting(this, "hud.watermark.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "hud.watermark.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "hud.watermark.color_mode.custom");
   private final ColorSetting colorFirst = new ColorSetting(this, "hud.watermark.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(151, 71, 255, 255))
           .alpha(true);
   private final ColorSetting colorSecond = new ColorSetting(this, "hud.watermark.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(100, 160, 255, 255))
           .alpha(true);

   public Watermark() {
      super("hud.watermark", "icons/hud/watermark.png");
   }

   @Override
   public void update(UIContext context) {
      float padding = 8.0F;
      float separatorSpacing = 6.0F;
      this.height = 22.0F;
      float calculatedWidth = padding;
      String name = "Fever Visual";
      float nameFont = 8.0F;
      calculatedWidth += Fonts.MEDIUM.getFont(nameFont).width(name);
      boolean hasExtraInfo = this.showPing.isEnabled() || this.showFps.isEnabled() || this.showBps.isEnabled();
      if (hasExtraInfo) {
         calculatedWidth += separatorSpacing;
         calculatedWidth += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
      }

      if (this.showPing.isEnabled()) {
         int ping = this.getPing();
         String pingText = ping + " ms";
         float textFont = 7.5F;
         calculatedWidth += Fonts.REGULAR.getFont(textFont).width(pingText) + separatorSpacing;
         calculatedWidth += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
      }

      if (this.showFps.isEnabled()) {
         int fps = mc.getCurrentFps();
         String fpsText = fps + " FPS";
         float textFont = 7.5F;
         calculatedWidth += Fonts.REGULAR.getFont(textFont).width(fpsText) + separatorSpacing;
      }

      if (this.showBps.isEnabled()) {
         double motion = !this.ySpeed.isEnabled()
                 ? Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ)
                 : Math.hypot(mc.player.getY() - mc.player.lastY, Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ));
         String bpsText = String.format("%.2f", motion * 20.0).replace(",", ".") + " BPS";
         float textFont = 7.5F;
         if (this.showFps.isEnabled()) {
            calculatedWidth += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
         }
         calculatedWidth += Fonts.REGULAR.getFont(textFont).width(bpsText);
      }

      this.width = calculatedWidth + padding;
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      float padding = 8.0F;
      float separatorSpacing = 6.0F;
      this.height = 22.0F;
      float currentX = this.x + padding;
      float centerY = this.y + this.height / 2.0F;

      context.drawClientRect(this.x, this.y, this.width, this.height, this.animation.getValue(), this.dragAnim.getValue(), 6.0F);

      String name = "Fever Visual";
      float nameFont = 8.0F;
      float nameTextHeight = Fonts.REGULAR.getFont(nameFont).height();
      currentX += this.drawWatermarkText(context, name, currentX, centerY - nameTextHeight / 2.0F, nameFont) + separatorSpacing;
      boolean hasExtraInfo = this.showPing.isEnabled() || this.showFps.isEnabled() || this.showBps.isEnabled();
      if (hasExtraInfo) {
         float separatorHeight = Fonts.REGULAR.getFont(8.0F).height();
         context.drawText(Fonts.REGULAR.getFont(8.0F), "/", currentX, centerY - separatorHeight / 2.0F, Colors.getTextColor().withAlpha(80.0F));
         currentX += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
      }

      if (this.showPing.isEnabled()) {
         int ping = this.getPing();
         String pingText = ping + " ms";
         float textFont = 7.5F;
         float textHeight = Fonts.REGULAR.getFont(textFont).height();
         context.drawText(Fonts.REGULAR.getFont(textFont), pingText, currentX, centerY - textHeight / 2.0F, Colors.getTextColor());
         currentX += Fonts.REGULAR.getFont(textFont).width(pingText) + separatorSpacing;
         float separatorHeight2 = Fonts.REGULAR.getFont(8.0F).height();
         context.drawText(Fonts.REGULAR.getFont(8.0F), "/", currentX, centerY - separatorHeight2 / 2.0F, Colors.getTextColor().withAlpha(80.0F));
         currentX += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
      }

      if (this.showFps.isEnabled()) {
         int fps = mc.getCurrentFps();
         String fpsText = fps + " FPS";
         float textFont = 7.5F;
         float textHeight = Fonts.REGULAR.getFont(textFont).height();
         context.drawText(Fonts.REGULAR.getFont(textFont), fpsText, currentX, centerY - textHeight / 2.0F, Colors.getTextColor());
         currentX += Fonts.REGULAR.getFont(textFont).width(fpsText) + separatorSpacing;
      }

      if (this.showBps.isEnabled()) {
         if (this.showFps.isEnabled()) {
            float separatorHeight3 = Fonts.REGULAR.getFont(8.0F).height();
            context.drawText(Fonts.REGULAR.getFont(8.0F), "/", currentX, centerY - separatorHeight3 / 2.0F, Colors.getTextColor().withAlpha(80.0F));
            currentX += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
         }

         double motion = !this.ySpeed.isEnabled()
                 ? Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ)
                 : Math.hypot(mc.player.getY() - mc.player.lastY, Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ));
         String bpsText = String.format("%.2f", motion * 20.0).replace(",", ".") + " BPS";
         float textFont = 7.5F;
         float textHeight = Fonts.REGULAR.getFont(textFont).height();
         context.drawText(Fonts.REGULAR.getFont(textFont), bpsText, currentX, centerY - textHeight / 2.0F, Colors.getTextColor());
      }
   }

   private float drawWatermarkText(UIContext context, String text, float x, float y, float fontSize) {
      Font font = Fonts.MEDIUM.getFont(fontSize);
      float offset = 0.0F;
      float totalWidth = font.width(text);
      for (int i = 0; i < text.length(); i++) {
         String symbol = String.valueOf(text.charAt(i));
         float charWidth = font.width(symbol);
         float progress = (float) i / (text.length() - 1);
         context.drawText(font, symbol, x + offset, y, this.getWatermarkColor(-progress * 180.0F));
         offset += charWidth;
      }

      return offset;
   }

   private ColorRGBA getWatermarkColor(float index) {
      float time = (System.currentTimeMillis() % 2200L) / 2200.0F;
      float wave = (float)(0.5F + 0.5F * Math.sin((time + index / 180.0F) * Math.PI * 2.0));
      if (!this.colorCustom.isSelected()) {
         return Colors.getAccentColor(0.0F).mix(Colors.getAccentColor(180.0F), wave);
      }

      return this.colorFirst.getColorSafe().mix(this.colorSecond.getColorSafe(), wave);
   }

   private int getPing() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         if (playerListEntry != null) {
            return playerListEntry.getLatency();
         }
      }

      return 0;
   }
}
