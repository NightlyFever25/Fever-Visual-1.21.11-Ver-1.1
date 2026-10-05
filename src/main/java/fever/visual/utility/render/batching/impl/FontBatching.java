package fever.visual.utility.render.batching.impl;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.framework.msdf.MsdfFont;
import fever.visual.framework.msdf.MsdfRenderer;
import fever.visual.framework.shader.GlProgram;
import fever.visual.utility.render.batching.Batching;
import com.mojang.blaze3d.vertex.VertexFormat;

public class FontBatching extends Batching {
   protected MsdfFont font;

   public FontBatching(VertexFormat vertexFormat, MsdfFont font) {
      super(vertexFormat);
      this.font = font;
   }

   @Override
   public void draw() {
      float thickness = 0.05F;
      float smoothness = 0.5F;
      float spacing = 0.0F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderTexture(0, this.font.getTextureId());
      GlProgram shader = MsdfRenderer.getProgram().use();
      shader.findUniform("Range").set(this.font.getAtlas().range());
      shader.findUniform("Thickness").set(thickness);
      shader.findUniform("Smoothness").set(smoothness);
      shader.findUniform("EnableFadeout").set(0);
      this.build();
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      if (active == this) {
         active = null;
      }
   }
}
