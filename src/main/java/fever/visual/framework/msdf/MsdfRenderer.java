package fever.visual.framework.msdf;

import fever.visual.utility.render.compat.RenderSystem;
import java.util.List;
import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.framework.shader.GlProgram;
import fever.visual.systems.modules.modules.other.NameProtect;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.batching.Batching;
import net.minecraft.client.render.BufferBuilder;
import fever.visual.utility.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public final class MsdfRenderer {
   private static GlProgram msdfFontProgram;
   private static NameProtect nameProtectModule;

   public static GlProgram getProgram() {
      if (msdfFontProgram == null) {
         msdfFontProgram = new GlProgram(FeverVisual.id("msdf_font/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      }
      return msdfFontProgram;
   }

   public static void renderText(MsdfFont font, String text, float size, int color, Matrix4f matrix, float x, float y, float z) {
      renderText(font, text, size, color, matrix, x, y, z, false, 0.0F, 1.0F, 0.0F);
   }

   public static void renderText(
      MsdfFont font,
      String text,
      float size,
      int color,
      Matrix4f matrix,
      float x,
      float y,
      float z,
      boolean enableFadeout,
      float fadeoutStart,
      float fadeoutEnd,
      float maxWidth
   ) {
      text = text.replace("і", "i").replace("І", "I");
      float thickness = 0.05F;
      float smoothness = 0.5F;
      float spacing = 0.0F;
      NameProtect nameProtect = getNameProtectModule();
      if (nameProtect != null && nameProtect.isEnabled()) {
         text = nameProtect.patchName(text);
      }

      if (Batching.getActive() != null) {
         font.applyGlyphs(matrix, Batching.getActive().getBuilder(), text, size, thickness * 0.5F * size, spacing, x - 0.75F, y + size * 0.7F, z, color);
      } else {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.setShaderTexture(0, font.getTextureId());
         GlProgram shader = getProgram().use();
         shader.findUniform("Range").set(font.getAtlas().range());
         shader.findUniform("Thickness").set(thickness);
         shader.findUniform("Smoothness").set(smoothness);
         shader.findUniform("EnableFadeout").set(enableFadeout ? 1 : 0);
         shader.findUniform("FadeoutStart").set(fadeoutStart);
         shader.findUniform("FadeoutEnd").set(fadeoutEnd);
         float textPosX = transformPositionX(matrix, x, y, z);
         float transformedMaxWidth = transformPositionX(matrix, x + maxWidth, y, z) - textPosX;
         shader.findUniform("MaxWidth").set(transformedMaxWidth);
         shader.findUniform("TextPosX").set(textPosX);
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         font.applyGlyphs(matrix, builder, text, size, thickness * 0.5F * size, spacing, x - 0.75F, y + size * 0.7F, z, color);
         BuiltBuffer builtBuffer = builder.endNullable();
         if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
         }

         RenderSystem.setShaderTexture(0, 0);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void renderText(
      MsdfFont font,
      String text,
      float size,
      int color,
      Matrix4f matrix,
      float x,
      float y,
      float z,
      boolean enableFadeout,
      float fadeoutStart,
      float fadeoutEnd
   ) {
      float maxWidth = font.getWidth(text, size) * 2.0F;
      renderText(font, text, size, color, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
   }

   public static void renderText(MsdfFont font, Text text, float size, Matrix4f matrix, float x, float y, float z) {
      renderText(font, text, size, matrix, x, y, z, false, 0.0F, 1.0F, 0.0F);
   }

   public static void renderText(
      MsdfFont font,
      Text text,
      float size,
      Matrix4f matrix,
      float x,
      float y,
      float z,
      boolean enableFadeout,
      float fadeoutStart,
      float fadeoutEnd,
      float maxWidth
   ) {
      float thickness = 0.05F;
      float smoothness = 0.5F;
      float spacing = 0.0F;
      List<FormattedTextProcessor.TextSegment> segments = FormattedTextProcessor.processText(text, Colors.WHITE.getRGB());
      float currentX = x;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderTexture(0, font.getTextureId());
      GlProgram shader = getProgram().use();
      shader.findUniform("Range").set(font.getAtlas().range());
      shader.findUniform("Thickness").set(thickness);
      shader.findUniform("Smoothness").set(smoothness);
      shader.findUniform("EnableFadeout").set(enableFadeout ? 1 : 0);
      shader.findUniform("FadeoutStart").set(fadeoutStart);
      shader.findUniform("FadeoutEnd").set(fadeoutEnd);
      float textPosX = transformPositionX(matrix, x, y, z);
      float transformedMaxWidth = transformPositionX(matrix, x + maxWidth, y, z) - textPosX;
      shader.findUniform("MaxWidth").set(transformedMaxWidth);
      shader.findUniform("TextPosX").set(textPosX);
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (FormattedTextProcessor.TextSegment segment : segments) {
         font.applyGlyphs(matrix, builder, segment.text, size, thickness * 0.5F * size, spacing - 0.3F, currentX - 0.75F, y + size * 0.7F, z, segment.color);
         currentX += font.getWidth(segment.text, size);
      }

      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void renderText(
      MsdfFont font, Text text, float size, Matrix4f matrix, float x, float y, float z, boolean enableFadeout, float fadeoutStart, float fadeoutEnd
   ) {
      float maxWidth = font.getTextWidth(text, size) * 2.0F;
      renderText(font, text, size, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
   }

   private static float transformPositionX(Matrix4f matrix, float x, float y, float z) {
      return matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30();
   }

   private static NameProtect getNameProtectModule() {
      if (nameProtectModule == null
         && FeverVisual.getInstance() != null
         && FeverVisual.getInstance().getModuleManager() != null) {
         nameProtectModule = FeverVisual.getInstance().getModuleManager().getModuleSafe(NameProtect.class);
      }
      return nameProtectModule;
   }

   @Generated
   private MsdfRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
