package fever.visual.utility.render;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.render.compat.BufferRenderer;
import fever.visual.utility.render.compat.GlStateManager.DstFactor;
import fever.visual.utility.render.compat.GlStateManager.SrcFactor;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class TestikRender3D {
   private TestikRender3D() {
   }

   public static void setup(boolean throughWalls) {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ONE, DstFactor.ZERO);
      RenderSystem.disableCull();
      if (throughWalls) {
         RenderSystem.disableDepthTest();
      } else {
         RenderSystem.enableDepthTest();
      }
      RenderSystem.depthMask(false);
   }

   public static void reset() {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void drawBox(MatrixStack matrices, Box box, ColorRGBA fill, ColorRGBA outline) {
      if (fill.getAlpha() > 0.0F) {
         BufferBuilder quads = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         Draw3DUtility.renderFilledBox(matrices, quads, box, fill);
         BufferRenderer.drawWithGlobalProgram(quads.end());
      }

      if (outline.getAlpha() > 0.0F) {
         BufferBuilder lines = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);
         Draw3DUtility.renderOutlinedBox(matrices, lines, box, outline);
         BufferRenderer.drawWithGlobalProgram(lines.end());
      }
   }

   public static void drawTriangle(BufferBuilder buffer, Matrix4f matrix, Vec3d a, Vec3d b, Vec3d c, ColorRGBA ca, ColorRGBA cb, ColorRGBA cc) {
      vertex(buffer, matrix, a, ca);
      vertex(buffer, matrix, b, cb);
      vertex(buffer, matrix, c, cc);
   }

   public static void drawGradientLine(BufferBuilder buffer, Matrix4f matrix, Vec3d start, Vec3d end, ColorRGBA startColor, ColorRGBA endColor) {
      vertex(buffer, matrix, start, startColor);
      vertex(buffer, matrix, end, endColor);
   }

   private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3d pos, ColorRGBA color) {
      buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
              .color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
   }
}
