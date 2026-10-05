package fever.visual.utility.render.batching.impl;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import fever.visual.utility.render.batching.Batching;
import fever.visual.utility.render.compat.BufferRenderer;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/** Batches solid GUI primitives without per-rectangle shader uniforms. */
public final class GuiShapeBatching extends Batching {
   private static final int CORNER_SEGMENTS = 6;
   private static final float AA_FRINGE = 0.75F;
   private static final int MAX_POINTS = 4 * (CORNER_SEGMENTS + 1);
   private static final Int2ObjectOpenHashMap<ShapeTemplate> TEMPLATES = new Int2ObjectOpenHashMap<>();
   private static final ThreadLocal<ShapeScratch> SCRATCH = ThreadLocal.withInitial(ShapeScratch::new);

   static {
      cacheTemplate(2.0F);
      cacheTemplate(7.0F);
   }

   private final float[] pointsX;
   private final float[] pointsY;
   private final float[] normalsX;
   private final float[] normalsY;
   private int pointCount;

   public GuiShapeBatching() {
      super(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      ShapeScratch scratch = SCRATCH.get();
      this.pointsX = scratch.pointsX;
      this.pointsY = scratch.pointsY;
      this.normalsX = scratch.normalsX;
      this.normalsY = scratch.normalsY;
   }

   public void addRect(Matrix4f matrix, float x, float y, float width, float height, int color) {
      if (width <= 0.0F || height <= 0.0F || (color >>> 24) == 0) {
         return;
      }
      this.vertex(matrix, x, y, color);
      this.vertex(matrix, x, y + height, color);
      this.vertex(matrix, x + width, y + height, color);
      this.vertex(matrix, x, y, color);
      this.vertex(matrix, x + width, y + height, color);
      this.vertex(matrix, x + width, y, color);
   }

   public void addRounded(
      Matrix4f matrix, float x, float y, float width, float height,
      float topLeft, float topRight, float bottomRight, float bottomLeft,
      float exponent, int color
   ) {
      if (width <= 0.0F || height <= 0.0F || (color >>> 24) == 0) {
         return;
      }

      float maxRadius = Math.min(width, height) * 0.5F;
      topLeft = Math.min(Math.max(0.0F, topLeft), maxRadius);
      topRight = Math.min(Math.max(0.0F, topRight), maxRadius);
      bottomRight = Math.min(Math.max(0.0F, bottomRight), maxRadius);
      bottomLeft = Math.min(Math.max(0.0F, bottomLeft), maxRadius);
      exponent = Math.min(16.0F, Math.max(1.0F, exponent));

      this.pointCount = 0;
      ShapeTemplate template = getTemplate(exponent);
      this.addCorner(x + topLeft, y + topLeft, topLeft, template, 0);
      this.addCorner(x + width - topRight, y + topRight, topRight, template, 1);
      this.addCorner(x + width - bottomRight, y + height - bottomRight, bottomRight, template, 2);
      this.addCorner(x + bottomLeft, y + height - bottomLeft, bottomLeft, template, 3);

      float centerX = x + width * 0.5F;
      float centerY = y + height * 0.5F;
      int transparent = color & 0x00FFFFFF;
      for (int i = 0; i < this.pointCount; i++) {
         int next = (i + 1) % this.pointCount;
         float x1 = this.pointsX[i];
         float y1 = this.pointsY[i];
         float x2 = this.pointsX[next];
         float y2 = this.pointsY[next];

         this.vertex(matrix, centerX, centerY, color);
         this.vertex(matrix, x1, y1, color);
         this.vertex(matrix, x2, y2, color);

         float ox1 = x1 + this.normalsX[i] * AA_FRINGE;
         float oy1 = y1 + this.normalsY[i] * AA_FRINGE;
         float ox2 = x2 + this.normalsX[next] * AA_FRINGE;
         float oy2 = y2 + this.normalsY[next] * AA_FRINGE;
         this.vertex(matrix, x1, y1, color);
         this.vertex(matrix, ox1, oy1, transparent);
         this.vertex(matrix, ox2, oy2, transparent);
         this.vertex(matrix, x1, y1, color);
         this.vertex(matrix, ox2, oy2, transparent);
         this.vertex(matrix, x2, y2, color);
      }
   }

   private void addCorner(float centerX, float centerY, float radius, ShapeTemplate template, int corner) {
      int offset = corner * (CORNER_SEGMENTS + 1);
      if (radius <= 0.001F) {
         int middle = offset + CORNER_SEGMENTS / 2;
         this.addPoint(centerX, centerY, template.normalsX[middle], template.normalsY[middle]);
         return;
      }

      for (int segment = 0; segment <= CORNER_SEGMENTS; segment++) {
         int index = offset + segment;
         this.addPoint(
            centerX + template.pointsX[index] * radius,
            centerY + template.pointsY[index] * radius,
            template.normalsX[index],
            template.normalsY[index]
         );
      }
   }

   private static ShapeTemplate getTemplate(float exponent) {
      int key = Float.floatToIntBits(exponent);
      ShapeTemplate template = TEMPLATES.get(key);
      if (template == null) {
         template = createTemplate(exponent);
         TEMPLATES.put(key, template);
      }
      return template;
   }

   private static void cacheTemplate(float exponent) {
      TEMPLATES.put(Float.floatToIntBits(exponent), createTemplate(exponent));
   }

   private static ShapeTemplate createTemplate(float exponent) {
      float[] pointsX = new float[MAX_POINTS];
      float[] pointsY = new float[MAX_POINTS];
      float[] normalsX = new float[MAX_POINTS];
      float[] normalsY = new float[MAX_POINTS];
      double[] starts = {Math.PI, Math.PI * 1.5, 0.0, Math.PI * 0.5};
      double shapePower = 2.0 / exponent;
      int index = 0;
      for (double start : starts) {
         double end = start + Math.PI * 0.5;
         for (int segment = 0; segment <= CORNER_SEGMENTS; segment++) {
            double angle = start + (end - start) * segment / CORNER_SEGMENTS;
            double cosine = Math.cos(angle);
            double sine = Math.sin(angle);
            double unitX = Math.copySign(Math.pow(Math.abs(cosine), shapePower), cosine);
            double unitY = Math.copySign(Math.pow(Math.abs(sine), shapePower), sine);
            double gradientX = Math.copySign(Math.pow(Math.abs(unitX), exponent - 1.0), unitX);
            double gradientY = Math.copySign(Math.pow(Math.abs(unitY), exponent - 1.0), unitY);
            double length = Math.sqrt(gradientX * gradientX + gradientY * gradientY);
            pointsX[index] = (float)unitX;
            pointsY[index] = (float)unitY;
            normalsX[index] = length > 1.0E-6 ? (float)(gradientX / length) : (float)cosine;
            normalsY[index] = length > 1.0E-6 ? (float)(gradientY / length) : (float)sine;
            index++;
         }
      }
      return new ShapeTemplate(pointsX, pointsY, normalsX, normalsY);
   }

   private void addPoint(float x, float y, float normalX, float normalY) {
      this.pointsX[this.pointCount] = x;
      this.pointsY[this.pointCount] = y;
      this.normalsX[this.pointCount] = normalX;
      this.normalsY[this.pointCount] = normalY;
      this.pointCount++;
   }

   private void vertex(Matrix4f matrix, float x, float y, int color) {
      this.builder.vertex(matrix, x, y, 0.0F).color(color);
   }

   @Override
   public void draw() {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      BuiltBuffer built = this.builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      if (active == this) {
         active = null;
      }
   }

   private record ShapeTemplate(float[] pointsX, float[] pointsY, float[] normalsX, float[] normalsY) {
   }

   private static final class ShapeScratch {
      private final float[] pointsX = new float[MAX_POINTS];
      private final float[] pointsY = new float[MAX_POINTS];
      private final float[] normalsX = new float[MAX_POINTS];
      private final float[] normalsY = new float[MAX_POINTS];
   }
}
