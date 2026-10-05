package fever.visual.systems.modules.modules.visuals.emotions;

import fever.visual.framework.base.CustomScreen;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.modules.modules.visuals.Emotions;
import fever.visual.utility.colors.ColorRGBA;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.util.math.Vec2f;

public class EmotionWheelScreen extends CustomScreen {
   private static final float INNER_RADIUS = 54.0F;
   private static final float OUTER_RADIUS = 132.0F;
   private static final float GAP_DEGREES = 2.6F;
   private static final float DEAD_ZONE = 48.0F;
   private static final float CENTER_DIAMETER = 92.0F;
   private static final long STICKY_WINDOW_MS = 190L;

   private final Emotions module;
   private final long openedAt = System.currentTimeMillis();
   private final long startedAt = System.nanoTime();
   private final float[] hoverAnim;
   private long lastNanos = System.nanoTime();
   private int hoveredIndex = -1;
   private boolean sticky;
   private boolean repressed;
   private boolean dismissed;

   public EmotionWheelScreen(Emotions module) {
      this.module = module;
      this.hoverAnim = new float[Emotion.values().length];
   }

   @Override
   public void render(UIContext context) {
      Emotion[] emotions = Emotion.values();
      if (emotions.length == 0 || this.dismissed) {
         this.close();
         return;
      }

      float cx = this.width * 0.5F;
      float cy = this.height * 0.5F;
      float scale = this.fitScale(emotions.length);
      float inner = INNER_RADIUS * scale;
      float outer = OUTER_RADIUS * scale;
      float sweep = 360.0F / emotions.length;
      float dt = this.frameDelta();
      float time = (System.nanoTime() - this.startedAt) / 1_000_000_000.0F;
      this.hoveredIndex = this.resolveHovered(context.getMouseX(), context.getMouseY(), cx, cy, scale, emotions.length);
      this.updateInput();

      context.updateBuffer();
      context.updateBlur();
      context.drawRect(0.0F, 0.0F, this.width, this.height, new ColorRGBA(0.0F, 0.0F, 0.0F, 92.0F));
      this.drawHeader(context, cx, cy, outer);

      for (int i = 0; i < emotions.length; i++) {
         float target = i == this.hoveredIndex ? 1.0F : 0.0F;
         this.hoverAnim[i] += (target - this.hoverAnim[i]) * (1.0F - (float)Math.exp(-dt * 15.0F));
      }

      this.drawPreviews(context, cx, cy, scale, emotions, sweep, time);
      this.drawCenter(context, cx, cy, scale, emotions);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (button == MouseButton.RIGHT) {
         this.dismiss();
         return;
      }
      if (button == MouseButton.LEFT) {
         this.commit();
      }
   }

   @Override
   public void close() {
      this.dismiss();
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private void updateInput() {
      if (this.dismissed) {
         return;
      }
      if (this.module.isWheelKeyDown()) {
         this.repressed = this.sticky;
         return;
      }
      if (this.sticky) {
         if (this.repressed) {
            this.commit();
         }
         return;
      }
      if (System.currentTimeMillis() - this.openedAt <= STICKY_WINDOW_MS) {
         this.sticky = true;
         return;
      }
      this.commit();
   }

   private void commit() {
      if (this.dismissed) {
         return;
      }
      Emotion[] emotions = Emotion.values();
      if (this.hoveredIndex >= 0 && this.hoveredIndex < emotions.length) {
         this.module.playEmotion(emotions[this.hoveredIndex]);
      } else {
         this.module.stopEmotion();
      }
      this.dismiss();
   }

   private void dismiss() {
      if (!this.dismissed) {
         this.dismissed = true;
      }
      if (MinecraftClient.getInstance().currentScreen == this) {
         MinecraftClient.getInstance().setScreen(null);
      }
   }

   private void drawHeader(UIContext context, float cx, float cy, float outer) {
      Font title = Fonts.SEMIBOLD.getFont(10.5F);
      Font hint = Fonts.MEDIUM.getFont(6.0F);
      float top = cy - outer - 34.0F;
      context.drawCenteredText(title, "\u0412\u044b\u0431\u0435\u0440\u0438 \u043d\u0443\u0436\u043d\u0443\u044e \u044d\u043c\u043e\u0446\u0438\u044e", cx, top, ColorRGBA.WHITE.withAlpha(240.0F));
      String hintText = this.sticky
         ? "\u041a\u043b\u0438\u043a\u043d\u0438\u0442\u0435 \u043f\u043e \u044d\u043c\u043e\u0446\u0438\u0438 \u00b7 \u041f\u041a\u041c \u0447\u0442\u043e\u0431\u044b \u0437\u0430\u043a\u0440\u044b\u0442\u044c"
         : "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u044d\u043c\u043e\u0446\u0438\u044e \u0438 \u043e\u0442\u043f\u0443\u0441\u0442\u0438\u0442\u0435 \u043a\u043b\u0430\u0432\u0438\u0448\u0443";
      context.drawCenteredText(hint, hintText, cx, top + title.height() + 4.0F, ColorRGBA.WHITE.withAlpha(130.0F));
   }

   private void drawPreviews(UIContext context, float cx, float cy, float scale, Emotion[] emotions, float sweep, float time) {
      float radius = (INNER_RADIUS + (OUTER_RADIUS - INNER_RADIUS) * 0.52F) * scale;
      for (int i = 0; i < emotions.length; i++) {
         float hover = this.hoverAnim[i];
         double angle = Math.toRadians(sweep * i - 90.0F);
         float x = cx + (float)Math.cos(angle) * radius;
         float y = cy + (float)Math.sin(angle) * radius;
         float size = (42.0F + hover * 4.0F) * scale;
         this.drawPlayerPreview(context, emotions[i], x, y, size, scale, time);
      }
   }

   private void drawCenter(UIContext context, float cx, float cy, float scale, Emotion[] emotions) {
      float diameter = CENTER_DIAMETER * scale;
      float x = cx - diameter * 0.5F;
      float y = cy - diameter * 0.5F;
      Emotion selected = this.hoveredIndex >= 0 && this.hoveredIndex < emotions.length ? emotions[this.hoveredIndex] : null;
      String label = selected != null ? selected.displayName() : (this.module.isPlaying() ? "\u041e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c" : "\u041e\u0442\u043c\u0435\u043d\u0430");
      Font labelFont = Fonts.SEMIBOLD.getFont(7.5F * scale);
      while (labelFont.width(label) > diameter - 16.0F * scale && labelFont.getSize() > 4.5F) {
         labelFont = Fonts.SEMIBOLD.getFont(labelFont.getSize() - 0.5F);
      }

      context.drawRoundedRect(x, y, diameter, diameter, BorderRadius.all(diameter * 0.5F), new ColorRGBA(255.0F, 255.0F, 255.0F, 32.0F));
      context.drawRoundedRect(x + 4.0F * scale, y + 4.0F * scale, diameter - 8.0F * scale, diameter - 8.0F * scale, BorderRadius.all((diameter - 8.0F * scale) * 0.5F), new ColorRGBA(18.0F, 18.0F, 25.0F, 235.0F));
      context.drawCenteredText(labelFont, label, cx, cy - labelFont.height() / 2.0F, ColorRGBA.WHITE.withAlpha(240.0F));
   }

   private void drawPlayerPreview(UIContext context, Emotion emotion, float x, float y, float size, float scale, float time) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null) {
         return;
      }

      float pulse = (float)Math.sin(time * (emotion == Emotion.DANCE ? 6.0F : 3.2F));
      float mouseX = x + switch (emotion) {
         case WAVE -> 26.0F * scale;
         case BOW -> -10.0F * scale;
         case DANCE -> pulse * 22.0F * scale;
         default -> 0.0F;
      };
      float mouseY = y + switch (emotion) {
         case BOW -> 30.0F * scale;
         case SHY, FACEPALM -> 18.0F * scale;
         case CLAP, POINT -> -12.0F * scale;
         default -> pulse * 7.0F * scale;
      };

      float bgW = size * 1.12F;
      float bgH = size * 1.42F;
      float bgX = x - bgW * 0.5F;
      float bgY = y - bgH * 0.58F;
      context.drawRoundedRect(bgX, bgY, bgW, bgH, BorderRadius.all(10.0F * scale), new ColorRGBA(18.0F, 23.0F, 31.0F, 162.0F));
      context.drawRoundedBorder(bgX, bgY, bgW, bgH, 0.9F, BorderRadius.all(10.0F * scale), ColorRGBA.WHITE.withAlpha(42.0F));

      int left = Math.round(bgX + bgW * 0.12F);
      int top = Math.round(bgY + bgH * 0.07F);
      int right = Math.round(bgX + bgW * 0.88F);
      int bottom = Math.round(bgY + bgH * 0.95F);
      int modelSize = Math.round(18.0F * scale + size * 0.26F);
      EmotionPlayback.withPreview(emotion, time % Math.max(0.1F, emotion.duration()), () ->
         InventoryScreen.drawEntity(context.getOriginalContext(), left, top, right, bottom, modelSize, 0.0625F, mouseX, mouseY, client.player)
      );
   }

   private void drawSector(UIContext context, float cx, float cy, float inner, float outer, float midDegrees, float sweepDegrees, ColorRGBA color) {
      float start = midDegrees - sweepDegrees * 0.5F - 90.0F;
      float end = midDegrees + sweepDegrees * 0.5F - 90.0F;
      for (float radius = inner; radius <= outer; radius += 2.2F) {
         this.drawArc(context, cx, cy, radius, start, end, color);
      }
   }

   private void drawSectorBorders(UIContext context, float cx, float cy, float inner, float outer, float midDegrees, float sweepDegrees, ColorRGBA color) {
      float start = midDegrees - sweepDegrees * 0.5F - 90.0F;
      float end = midDegrees + sweepDegrees * 0.5F - 90.0F;
      this.drawArc(context, cx, cy, inner, start, end, color);
      this.drawArc(context, cx, cy, outer, start, end, color);
      this.drawRadial(context, cx, cy, inner, outer, start, color);
      this.drawRadial(context, cx, cy, inner, outer, end, color);
   }

   private void drawArc(UIContext context, float cx, float cy, float radius, float startDegrees, float endDegrees, ColorRGBA color) {
      List<Vec2f> points = new ArrayList<>();
      List<ColorRGBA> colors = new ArrayList<>();
      int steps = Math.max(8, (int)((endDegrees - startDegrees) / 4.0F));
      for (int i = 0; i <= steps; i++) {
         float t = i / (float)steps;
         double angle = Math.toRadians(startDegrees + (endDegrees - startDegrees) * t);
         points.add(new Vec2f(cx + (float)Math.cos(angle) * radius, cy + (float)Math.sin(angle) * radius));
         colors.add(color);
      }
      context.drawLineStrip(points, colors);
   }

   private void drawRadial(UIContext context, float cx, float cy, float inner, float outer, float degrees, ColorRGBA color) {
      double angle = Math.toRadians(degrees);
      this.line(
         context,
         cx + (float)Math.cos(angle) * inner,
         cy + (float)Math.sin(angle) * inner,
         cx + (float)Math.cos(angle) * outer,
         cy + (float)Math.sin(angle) * outer,
         color
      );
   }

   private void line(UIContext context, float x1, float y1, float x2, float y2, ColorRGBA color) {
      context.drawLineStrip(List.of(new Vec2f(x1, y1), new Vec2f(x2, y2)), List.of(color, color));
   }

   private int resolveHovered(float mouseX, float mouseY, float cx, float cy, float scale, int count) {
      float dx = mouseX - cx;
      float dy = mouseY - cy;
      float distSq = dx * dx + dy * dy;
      float dead = DEAD_ZONE * scale;
      float outer = (OUTER_RADIUS + 12.0F) * scale;
      if (distSq < dead * dead || distSq > outer * outer) {
         return -1;
      }
      float sweep = 360.0F / count;
      float angle = (float)Math.toDegrees(Math.atan2(dx, -dy));
      if (angle < 0.0F) {
         angle += 360.0F;
      }
      return Math.round(angle / sweep) % count;
   }

   private float fitScale(int count) {
      float ringMid = 93.0F;
      float arc = ringMid * 2.0F * (float)Math.sin(Math.toRadians(360.0F / count) * 0.5F);
      float wheelScale = arc >= 52.0F ? 1.0F : 52.0F / arc;
      float outer = OUTER_RADIUS * wheelScale;
      float fit = Math.min(this.height * 0.5F / (outer + 48.0F), this.width * 0.5F / (outer + 10.0F));
      return wheelScale * Math.min(1.0F, fit);
   }

   private float frameDelta() {
      long now = System.nanoTime();
      float dt = Math.min(0.1F, (now - this.lastNanos) / 1_000_000_000.0F);
      this.lastNanos = now;
      return dt;
   }
}
