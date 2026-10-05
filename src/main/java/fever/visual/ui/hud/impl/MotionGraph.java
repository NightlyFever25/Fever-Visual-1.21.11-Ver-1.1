package fever.visual.ui.hud.impl;

import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.math.MathUtility;
import fever.visual.utility.time.Timer;
import net.minecraft.client.gui.screen.ChatScreen;
public class MotionGraph extends HudElement {
   private static final int MAX_POINTS = 100;
   private static final float PANEL_WIDTH = 100.0F;
   private static final float PANEL_HEIGHT = 30.0F;
   private static final float HEADER_HEIGHT = 14.0F;

   private final float[] points = new float[MAX_POINTS];
   private final float[] graphX = new float[MAX_POINTS];
   private final float[] graphY = new float[MAX_POINTS];
   private final int[] graphColors = new int[MAX_POINTS];
   private int pointCount;
   private int pointStart;
   private final Timer sampleTimer = new Timer();
   private float animatedBps;

   public MotionGraph() {
      super("hud.motion_graph", "icons/hud/world.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = PANEL_WIDTH;
      this.height = PANEL_HEIGHT;
      this.updateSpeed();
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      float alpha = 255.0F * this.animation.getValue() * this.visible.getValue();
      context.drawClientRect(this.x, this.y, this.width, this.height, this.animation.getValue(), this.dragAnim.getValue(), 5.0F);

      Font font = Fonts.MEDIUM.getFont(7.0F);
      String bps = TextUtility.formatNumber(Math.round(this.animatedBps * 100.0F) / 100.0F).replace(",", ".") + " BPS";
      context.drawText(font, bps, this.x + 5.0F, this.y + GuiUtility.getMiddleOfBox(font.height(), HEADER_HEIGHT), Colors.getTextColor().withAlpha(alpha));

      float graphX = this.x + 4.0F;
      float graphY = this.y + HEADER_HEIGHT + 1.0F;
      float graphWidth = this.width - 8.0F;
      float graphHeight = this.height - HEADER_HEIGHT - 5.0F;

      if (this.pointCount < 2) {
         return;
      }

      for (int i = 0; i < this.pointCount; i++) {
         float current = this.getPoint(i);
         float pointX = graphX + i * graphWidth / (MAX_POINTS - 1.0F);
         float pointY = graphY + graphHeight - Math.clamp(current, 0.0F, graphHeight);
         this.graphX[i] = pointX;
         this.graphY[i] = pointY;
         float pointAlpha = i == 0
               ? alpha * 0.15F
               : alpha * Math.clamp(i / (float)this.pointCount, 0.15F, 1.0F);
         ColorRGBA accent = Colors.getAccentColor(i * 10.0F);
         this.graphColors[i] = (Math.clamp(Math.round(pointAlpha), 0, 255) << 24) | (accent.getRGB() & 0x00FFFFFF);
      }

      context.drawLineStrip(this.graphX, this.graphY, this.graphColors, this.pointCount);
   }

   private float getPoint(int index) {
      return this.points[(this.pointStart + index) % MAX_POINTS];
   }

   private void addPoint(float value) {
      if (this.pointCount < MAX_POINTS) {
         this.points[(this.pointStart + this.pointCount) % MAX_POINTS] = value;
         this.pointCount++;
      } else {
         this.points[this.pointStart] = value;
         this.pointStart = (this.pointStart + 1) % MAX_POINTS;
      }
   }

   private void updateSpeed() {
      if (mc.player == null || mc.world == null) {
         this.pointCount = 0;
         this.pointStart = 0;
         this.animatedBps = 0.0F;
         return;
      }

      float bps = (float)(Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ) * 20.0D);
      this.animatedBps = MathUtility.interpolate(this.animatedBps, bps, 0.12F);
      if (this.sampleTimer.finished(50L)) {
         this.addPoint(Math.clamp(this.animatedBps, 0.0F, PANEL_HEIGHT - 15.0F));
         this.sampleTimer.reset();
      }
   }

   @Override
   public boolean show() {
      return mc.player != null && mc.world != null || mc.currentScreen instanceof ChatScreen;
   }
}
