package fever.visual.framework.base;

import fever.visual.FeverVisual;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.MsdfFont;
import fever.visual.framework.msdf.MsdfRenderer;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.gradient.Gradient;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.theme.Theme;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.ScissorUtility;
import fever.visual.utility.render.obj.CustomSprite;
import fever.visual.utility.render.obj.Rect;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import fever.visual.utility.render.batching.impl.SdfShapeBatching;
public class CustomDrawContext implements IMinecraft {
   public enum Pass {
      PRE,
      MAIN,
      POST
   }

   private static final Map<DrawContext, EnumMap<Pass, FeverGuiElementRenderState>> STATES =
      Collections.synchronizedMap(new WeakHashMap<>());
   private static DrawContext cachedContext;
   private static EnumMap<Pass, FeverGuiElementRenderState> cachedStates;

   private final DrawContext originalContext;
   private final FeverGuiElementRenderState renderState;
   private final MatrixStack matrices = new MatrixStack();

   protected CustomDrawContext(DrawContext originalContext) {
      this(originalContext, Pass.MAIN);
   }

   protected CustomDrawContext(DrawContext originalContext, Pass pass) {
      this(originalContext, pass, false);
   }

   protected CustomDrawContext(DrawContext originalContext, Pass pass, boolean isolated) {
      this.originalContext = originalContext;
      this.copyMatrix(originalContext.getMatrices());
      if (isolated) {
         this.renderState = this.createRenderState(pass, originalContext.getScaledWindowWidth(), originalContext.getScaledWindowHeight());
         originalContext.state.addSpecialElement(this.renderState);
         return;
      }

      EnumMap<Pass, FeverGuiElementRenderState> states = statesFor(originalContext);
      this.renderState = states.computeIfAbsent(pass, ignored -> {
         int width = originalContext.getScaledWindowWidth();
         int height = originalContext.getScaledWindowHeight();
         FeverGuiElementRenderState state = this.createRenderState(pass, width, height);
         originalContext.state.addSpecialElement(state);
         return state;
      });
   }

   protected CustomDrawContext(CustomDrawContext context) {
      this.originalContext = context.originalContext;
      this.renderState = context.renderState;
      this.matrices.multiplyPositionMatrix(context.matrices.peek().getPositionMatrix());
   }

   protected CustomDrawContext(CustomDrawContext context, Pass pass, boolean isolated) {
      this.originalContext = context.originalContext;
      this.matrices.multiplyPositionMatrix(context.matrices.peek().getPositionMatrix());
      if (isolated) {
         this.renderState = this.createRenderState(pass, this.originalContext.getScaledWindowWidth(), this.originalContext.getScaledWindowHeight());
         this.originalContext.state.addSpecialElement(this.renderState);
      } else {
         this.renderState = context.renderState;
      }
   }

   public static CustomDrawContext of(DrawContext originalContext) {
      return new CustomDrawContext(originalContext);
   }

   public static CustomDrawContext of(DrawContext originalContext, Pass pass) {
      return new CustomDrawContext(originalContext, pass);
   }

   public static CustomDrawContext isolated(DrawContext originalContext) {
      return new CustomDrawContext(originalContext, Pass.MAIN, true);
   }

   public static CustomDrawContext isolated(DrawContext originalContext, Pass pass) {
      return new CustomDrawContext(originalContext, pass, true);
   }

   public static CustomDrawContext of(CustomDrawContext context) {
      return context;
   }

   public static CustomDrawContext isolated(CustomDrawContext context, Pass pass) {
      return new CustomDrawContext(context, pass, true);
   }

   public DrawContext getOriginalContext() {
      return this.originalContext;
   }

   private static EnumMap<Pass, FeverGuiElementRenderState> statesFor(DrawContext context) {
      if (context == cachedContext && cachedStates != null) {
         return cachedStates;
      }
      synchronized (STATES) {
         EnumMap<Pass, FeverGuiElementRenderState> states = STATES.computeIfAbsent(
            context,
            ignored -> new EnumMap<>(Pass.class)
         );
         cachedContext = context;
         cachedStates = states;
         return states;
      }
   }

   private FeverGuiElementRenderState createRenderState(Pass pass, int width, int height) {
      return switch (pass) {
         case PRE -> new FeverGuiElementRenderState.Pre(width, height);
         case MAIN -> new FeverGuiElementRenderState.Main(width, height);
         case POST -> new FeverGuiElementRenderState.Post(width, height);
      };
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public int getScaledWindowWidth() {
      return this.originalContext.getScaledWindowWidth();
   }

   public int getScaledWindowHeight() {
      return this.originalContext.getScaledWindowHeight();
   }

   public void enableScissor(int x1, int y1, int x2, int y2) {
      ScissorUtility.push(this.matrices, x1, y1, x2 - x1, y2 - y1);
   }

   public void disableScissor() {
      ScissorUtility.pop();
   }

   public void drawGuiTexture(RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height, int color) {
      this.withVanillaMatrix(ignored -> this.originalContext.drawGuiTexture(pipeline, texture, x, y, width, height, color));
   }

   private void copyMatrix(Matrix3x2fc source) {
      Matrix4f transform = this.matrices.peek().getPositionMatrix();
      transform.identity()
         .m00(source.m00())
         .m01(source.m01())
         .m10(source.m10())
         .m11(source.m11())
         .m30(source.m20())
         .m31(source.m21());
   }

   private void enqueue(Consumer<MatrixStack> draw) {
      ScreenRect customScissor = ScissorUtility.currentScreenRect();
      ScreenRect vanillaScissor = this.originalContext.scissorStack.peekLast();
      ScreenRect scissor = customScissor == null
         ? vanillaScissor
         : vanillaScissor == null ? customScissor : customScissor.intersection(vanillaScissor);
      this.renderState.add(this.matrices.peek().getPositionMatrix(), scissor, draw);
   }

   private void withVanillaMatrix(Consumer<Matrix3x2fStack> action) {
      Matrix4f matrix = this.matrices.peek().getPositionMatrix();
      Matrix3x2fStack vanilla = this.originalContext.getMatrices();
      vanilla.pushMatrix();
      vanilla.set(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m30(), matrix.m31());
      try {
         action.accept(vanilla);
      } finally {
         vanilla.popMatrix();
      }
   }

   public void drawClientRect(float x, float y, float width, float height, float alpha, float dragAnim, float squircle) {
      if (Interface.showMinimalizm()) {
         this.drawBlurredRect(x, y, width, height, 45.0F, squircle, BorderRadius.all(6.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha * Interface.minimalizm()));
      }
      if (Interface.showGlass()) {
         this.drawLiquidGlass(
            x, y, width, height, squircle, 0.08F - 0.07F * dragAnim, BorderRadius.all(6.0F),
            ColorRGBA.WHITE.withAlpha(255.0F * alpha * Interface.glass())
         );
      }

      boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      this.drawSquircle(
         x, y, width, height, squircle, BorderRadius.all(6.0F),
         Colors.getBackgroundColor().withAlpha(255.0F * (dark ? 0.8F - 0.6F * Interface.glass() : 0.7F))
      );
   }

   public void updateBlur() {
      this.enqueue(ignored -> DrawUtility.blurProgram.draw());
   }

   public void updateBuffer() {
      this.enqueue(ignored -> DrawUtility.updateBuffer());
   }

   public void pushMatrix() {
      this.matrices.push();
   }

   public void popMatrix() {
      this.matrices.pop();
   }

   public void drawRect(float x, float y, float width, float height, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F || color.getAlpha() <= 0.0F) return;
      this.enqueue(new RectDraw(x, y, width, height, color));
   }

   public void drawLine(Vec2f from, Vec2f to, ColorRGBA color) {
      this.enqueue(matrices -> DrawUtility.drawLine(matrices, from, to, color));
   }

   public void drawLineStrip(List<Vec2f> points, List<ColorRGBA> colors) {
      this.enqueue(matrices -> DrawUtility.drawLineStrip(matrices, points, colors));
   }

   public void drawLineStrip(float[] xs, float[] ys, int[] colors, int count) {
      this.enqueue(matrices -> DrawUtility.drawLineStrip(matrices, xs, ys, colors, count));
   }

   public void drawBezier(Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      this.enqueue(matrices -> DrawUtility.drawBezier(matrices, p0, p1, p2, p3, color, resolution));
   }

   public void drawSquircle(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F || color.getAlpha() <= 0.0F) return;
      this.enqueue(new SquircleDraw(x, y, width, height, squirt, borderRadius, color));
   }

   /** Uses the original per-shape shader for small GUI controls that require exact legacy rasterization. */
   public void drawLegacySquircle(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F || color.getAlpha() <= 0.0F) return;
      this.enqueue(matrices -> DrawUtility.drawSquircle(matrices, x, y, width, height, squirt, borderRadius, color));
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F || color.getAlpha() <= 0.0F) return;
      this.enqueue(new RoundedRectDraw(x, y, width, height, borderRadius, color));
   }

   /** Uses the original per-shape shader for tiny circles, switches and slider handles. */
   public void drawLegacyRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F || color.getAlpha() <= 0.0F) return;
      this.enqueue(matrices -> DrawUtility.drawRoundedRect(matrices, x, y, width, height, borderRadius, color));
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      this.enqueue(matrices -> DrawUtility.drawRoundedRect(matrices, x, y, width, height, borderRadius, gradient));
   }

   public void drawLiquidGlass(float x, float y, float width, float height, float squirt, float power, BorderRadius borderRadius, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F || color.getAlpha() <= 0.0F || Interface.glass() <= 0.001F) return;
      BorderRadius scaledRadius = scaleRadius(borderRadius, squirt);
      this.enqueue(matrices -> DrawUtility.drawLiquidGlass(
         matrices,
         x - 5.0F * Interface.minimalizm(),
         y - 5.0F * Interface.minimalizm(),
         width + 10.0F * Interface.minimalizm(),
         height + 10.0F * Interface.minimalizm(),
         scaledRadius,
         color,
         color.getAlpha() / 255.0F * Interface.glass(),
         (height == 240.0F ? 100 : 50) * Interface.glass(),
         color.withAlpha(255.0F),
         1.0F,
         true,
         0.0F,
         power * Interface.glass(),
         squirt,
         false
      ));
   }

   public void drawLiquidGlass(
      float x, float y, float width, float height, float squirt, float power,
      BorderRadius borderRadius, ColorRGBA colorTop, ColorRGBA colorBottom
   ) {
      if (width <= 0.0F || height <= 0.0F
         || (colorTop.getAlpha() <= 0.0F && colorBottom.getAlpha() <= 0.0F)
         || Interface.glass() <= 0.001F) return;
      BorderRadius scaledRadius = scaleRadius(borderRadius, squirt);
      this.enqueue(matrices -> DrawUtility.drawLiquidGlass(
         matrices,
         x - 5.0F * Interface.minimalizm(),
         y - 5.0F * Interface.minimalizm(),
         width + 10.0F * Interface.minimalizm(),
         height + 10.0F * Interface.minimalizm(),
         scaledRadius,
         colorTop,
         colorBottom,
         colorTop.getAlpha() / 255.0F * Interface.glass(),
         (height == 240.0F ? 100 : 50) * Interface.glass(),
         colorTop.withAlpha(255.0F),
         1.0F,
         true,
         0.0F,
         power * Interface.glass(),
         squirt,
         false
      ));
   }

   public void drawLiquidGlass(
      float x, float y, float width, float height, float squirt,
      BorderRadius borderRadius, ColorRGBA color, boolean clean
   ) {
      if (width <= 0.0F || height <= 0.0F || color.getAlpha() <= 0.0F) return;
      BorderRadius scaledRadius = scaleRadius(borderRadius, squirt);
      this.enqueue(matrices -> DrawUtility.drawLiquidGlass(
         matrices, x, y, width, height, scaledRadius, color, color.getAlpha() / 255.0F,
         height == 240.0F ? 100.0F : 50.0F, color.withAlpha(255.0F), 1.0F,
         true, 0.0F, 0.08F, squirt, clean
      ));
   }

   private static BorderRadius scaleRadius(BorderRadius radius, float squirt) {
      return new BorderRadius(
         radius.topLeftRadius() * squirt / 2.0F,
         radius.topRightRadius() * squirt / 2.0F,
         radius.bottomLeftRadius() * squirt / 2.0F,
         radius.bottomRightRadius() * squirt / 2.0F
      );
   }

   public void drawLoadingRect(float x, float y, float width, float height, float progress, BorderRadius radius, ColorRGBA color) {
      this.enqueue(matrices -> DrawUtility.drawLoadingRect(matrices, x, y, width, height, progress, radius, color));
   }

   public void drawRoundedBorder(float x, float y, float width, float height, float thickness, BorderRadius radius, ColorRGBA color) {
      this.enqueue(matrices -> DrawUtility.drawRoundedBorder(matrices, x, y, width, height, thickness, radius, color));
   }

   public void drawTexture(Identifier identifier, Rect rect) {
      this.drawTexture(identifier, rect, ColorRGBA.WHITE);
   }

   public void drawTexture(Identifier identifier, Rect rect, ColorRGBA color) {
      this.drawTexture(identifier, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), color);
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height) {
      this.drawTexture(identifier, x, y, width, height, ColorRGBA.WHITE);
   }

   public void drawTexture(
      Identifier identifier, float x, float y, float width, float height,
      float u1, float u2, float v1, float v2, ColorRGBA color
   ) {
      this.enqueue(new TextureDraw(identifier, x, y, width, height, u1, u2, v1, v2, color));
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F || color.getAlpha() <= 0.0F) return;
      this.enqueue(new TextureDraw(identifier, x, y, width, height, 0.0F, 1.0F, 0.0F, 1.0F, color));
   }

   public void drawSprite(CustomSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      this.enqueue(matrices -> DrawUtility.drawSprite(matrices, sprite, x, y, width, height, color));
   }

   public void drawRoundedTexture(Identifier identifier, float x, float y, float width, float height, BorderRadius radius) {
      this.enqueue(matrices -> DrawUtility.drawRoundedTexture(matrices, identifier, x, y, width, height, radius));
   }

   public void drawRoundedTexture(Identifier identifier, float x, float y, float width, float height, BorderRadius radius, ColorRGBA color) {
      this.enqueue(matrices -> DrawUtility.drawRoundedTexture(matrices, identifier, x, y, width, height, radius, color));
   }

   public void drawShadow(float x, float y, float width, float height, float softness, BorderRadius radius, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F || softness <= 0.0F || color.getAlpha() <= 0.0F) return;
      this.enqueue(matrices -> DrawUtility.drawShadow(matrices, x, y, width, height, softness, radius, color));
   }

   public void drawBlurredRect(float x, float y, float width, float height, float blurRadius, BorderRadius radius, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F || blurRadius <= 0.0F || color.getAlpha() <= 0.0F) return;
      this.enqueue(matrices -> DrawUtility.drawBlur(matrices, x, y, width, height, blurRadius, radius, color));
   }

   public void drawBlurredRect(
      float x, float y, float width, float height, float blurRadius, float squirt,
      BorderRadius radius, ColorRGBA color
   ) {
      if (width <= 0.0F || height <= 0.0F || blurRadius <= 0.0F || color.getAlpha() <= 0.0F) return;
      this.enqueue(matrices -> DrawUtility.drawBlur(matrices, x, y, width, height, blurRadius, squirt, radius, color));
   }

   public void drawText(Font font, String text, float x, float y, ColorRGBA color) {
      if (text == null || text.isEmpty() || color.getAlpha() <= 0.0F) return;
      this.enqueue(new MsdfStringDraw(font.getFont(), text, font.getSize(), color.getRGB(), x, y));
   }

   public void drawText(Font font, Text text, float x, float y) {
      this.enqueue(matrices -> MsdfRenderer.renderText(
         font.getFont(), text, font.getSize(), matrices.peek().getPositionMatrix(), x, y, 0.0F
      ));
   }

   public void drawFadeoutText(Font font, String text, float x, float y, ColorRGBA color, float start, float end) {
      this.enqueue(matrices -> MsdfRenderer.renderText(
         font.getFont(), text, font.getSize(), color.getRGB(), matrices.peek().getPositionMatrix(),
         x, y, 0.0F, true, start, end
      ));
   }

   public void drawFadeoutText(
      Font font, String text, float x, float y, ColorRGBA color, float start, float end, float maxWidth
   ) {
      this.enqueue(matrices -> MsdfRenderer.renderText(
         font.getFont(), text, font.getSize(), color.getRGB(), matrices.peek().getPositionMatrix(),
         x, y, 0.0F, true, start, end, maxWidth
      ));
   }

   public void drawCenteredText(Font font, String text, float x, float y, ColorRGBA color) {
      this.drawText(font, text, x - font.getFont().getWidth(text, font.getSize()) / 2.0F, y, color);
   }

   public void drawRightText(Font font, String text, float x, float y, ColorRGBA color) {
      this.drawText(font, text, x - font.getFont().getWidth(text, font.getSize()), y, color);
   }

   public void drawText(TextRenderer renderer, OrderedText text, int x, int y, int color, boolean shadow) {
      this.withVanillaMatrix(matrices -> this.originalContext.drawText(renderer, text, x, y, color, shadow));
   }

   public void drawText(TextRenderer renderer, Text text, int x, int y, int color, boolean shadow) {
      this.withVanillaMatrix(matrices -> this.originalContext.drawText(renderer, text, x, y, color, shadow));
   }

   public void drawText(TextRenderer renderer, String text, int x, int y, int color, boolean shadow) {
      this.withVanillaMatrix(matrices -> this.originalContext.drawText(renderer, text, x, y, color, shadow));
   }

   public void drawItem(Item item, float x, float y, float size) {
      this.drawItem(item.getDefaultStack(), x, y, size);
   }

   public void drawItem(ItemStack item, float x, float y, float size) {
      this.withVanillaMatrix(matrices -> {
         matrices.translate(x, y);
         matrices.scale(size, size);
         this.originalContext.drawItem(item, 0, 0);
      });
   }

   public void drawQueuedItem(ItemStack item, float x, float y, float size) {
      this.enqueue(matrices -> {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         Matrix3x2fStack vanilla = this.originalContext.getMatrices();
         vanilla.pushMatrix();
         vanilla.set(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m30(), matrix.m31());
         try {
            vanilla.translate(x, y);
            vanilla.scale(size, size);
            this.originalContext.drawItem(item, 0, 0);
         } finally {
            vanilla.popMatrix();
         }
      });
   }

   public void drawItem(ItemStack item, int x, int y) {
      this.withVanillaMatrix(matrices -> this.originalContext.drawItem(item, x, y));
   }

   public void drawHead(AbstractClientPlayerEntity player, float x, float y, float size, BorderRadius radius, ColorRGBA color) {
      this.enqueue(matrices -> DrawUtility.drawPlayerHeadWithHat(matrices, player, x, y, size, radius, color));
   }

   public void drawHead(LivingEntity entity, float x, float y, float size, BorderRadius radius, ColorRGBA color) {
      this.enqueue(matrices -> DrawUtility.drawEntityHeadWithHat(matrices, entity, x, y, size, radius, color));
   }

   public void drawBatchItem(ItemStack item, int x, int y) {
      this.drawItem(item, x, y);
   }

   interface SolidShapeDraw extends Consumer<MatrixStack> {
      void add(SdfShapeBatching batching, Matrix4f matrix);
   }

   static final class RectDraw implements SolidShapeDraw {
      private final float x;
      private final float y;
      private final float width;
      private final float height;
      private final ColorRGBA color;

      RectDraw(float x, float y, float width, float height, ColorRGBA color) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.color = color;
      }

      @Override
      public void accept(MatrixStack matrices) {
         DrawUtility.drawRect(matrices, this.x, this.y, this.width, this.height, this.color);
      }

      @Override
      public void add(SdfShapeBatching batching, Matrix4f matrix) {
         batching.addRect(matrix, this.x, this.y, this.width, this.height, this.color.getRGB());
      }
   }

   static final class RoundedRectDraw implements SolidShapeDraw {
      private final float x;
      private final float y;
      private final float width;
      private final float height;
      private final BorderRadius radius;
      private final ColorRGBA color;

      RoundedRectDraw(float x, float y, float width, float height, BorderRadius radius, ColorRGBA color) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.radius = radius;
         this.color = color;
      }

      @Override
      public void accept(MatrixStack matrices) {
         DrawUtility.drawRoundedRect(matrices, this.x, this.y, this.width, this.height, this.radius, this.color);
      }

      @Override
      public void add(SdfShapeBatching batching, Matrix4f matrix) {
         batching.add(
            matrix, this.x, this.y, this.width, this.height,
            this.radius.topLeftRadius(), this.radius.bottomLeftRadius(),
            this.radius.topRightRadius(), this.radius.bottomRightRadius(),
            2.0F, this.color.getRGB()
         );
      }
   }

   static final class SquircleDraw implements SolidShapeDraw {
      private final float x;
      private final float y;
      private final float width;
      private final float height;
      private final float squirt;
      private final BorderRadius radius;
      private final ColorRGBA color;

      SquircleDraw(float x, float y, float width, float height, float squirt, BorderRadius radius, ColorRGBA color) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.squirt = squirt;
         this.radius = radius;
         this.color = color;
      }

      @Override
      public void accept(MatrixStack matrices) {
         DrawUtility.drawSquircle(matrices, this.x, this.y, this.width, this.height, this.squirt, this.radius, this.color);
      }

      @Override
      public void add(SdfShapeBatching batching, Matrix4f matrix) {
         float radiusScale = this.squirt * 0.5F;
         batching.add(
            matrix, this.x, this.y, this.width, this.height,
            this.radius.topLeftRadius() * radiusScale, this.radius.bottomLeftRadius() * radiusScale,
            this.radius.topRightRadius() * radiusScale, this.radius.bottomRightRadius() * radiusScale,
            this.squirt, this.color.getRGB()
         );
      }
   }

   static final class MsdfStringDraw implements Consumer<MatrixStack> {
      private final MsdfFont font;
      private final String text;
      private final float size;
      private final int color;
      private final float x;
      private final float y;

      MsdfStringDraw(MsdfFont font, String text, float size, int color, float x, float y) {
         this.font = font;
         this.text = text;
         this.size = size;
         this.color = color;
         this.x = x;
         this.y = y;
      }

      MsdfFont font() {
         return this.font;
      }

      @Override
      public void accept(MatrixStack matrices) {
         MsdfRenderer.renderText(
            this.font, this.text, this.size, this.color,
            matrices.peek().getPositionMatrix(), this.x, this.y, 0.0F
         );
      }
   }

   static final class TextureDraw implements Consumer<MatrixStack> {
      private final Identifier identifier;
      private final float x;
      private final float y;
      private final float width;
      private final float height;
      private final float u1;
      private final float u2;
      private final float v1;
      private final float v2;
      private final ColorRGBA color;

      TextureDraw(Identifier identifier, float x, float y, float width, float height,
                  float u1, float u2, float v1, float v2, ColorRGBA color) {
         this.identifier = identifier;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.u1 = u1;
         this.u2 = u2;
         this.v1 = v1;
         this.v2 = v2;
         this.color = color;
      }

      Identifier identifier() {
         return this.identifier;
      }

      @Override
      public void accept(MatrixStack matrices) {
         DrawUtility.drawTexture(
            matrices, this.identifier, this.x, this.y, this.width, this.height,
            this.u1, this.u2, this.v1, this.v2, this.color
         );
      }
   }
}
