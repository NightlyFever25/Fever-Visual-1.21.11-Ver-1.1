package fever.visual.framework.shader.impl;

import fever.visual.framework.shader.GlProgram;
import fever.visual.utility.interfaces.IWindow;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;


public class KawaseBlurProgram extends GlProgram implements IWindow {
   private GlProgram.Uniform resolutionUniform;
   private GlProgram.Uniform offsetUniform;
   private GlProgram.Uniform saturationUniform;
   private GlProgram.Uniform tintIntensityUniform;
   private GlProgram.Uniform tintColorUniform;

   public KawaseBlurProgram(Identifier identifier) {
      super(identifier, VertexFormats.POSITION_TEXTURE_COLOR);
   }


   public void updateUniforms(float offset) {
      this.offsetUniform.set(offset);
      this.resolutionUniform.set(1.0F / mw.getScaledWidth(), 1.0F / mw.getScaledHeight());
      this.saturationUniform.set(1.0F);
      this.tintIntensityUniform.set(0.0F);
      this.tintColorUniform.set(1.0F, 1.0F, 1.0F);
   }

   public void updateUniforms(float offset, int textureWidth, int textureHeight) {
      this.offsetUniform.set(offset);
      float invW = textureWidth > 0 ? 1.0F / textureWidth : 0.0F;
      float invH = textureHeight > 0 ? 1.0F / textureHeight : 0.0F;
      this.resolutionUniform.set(invW, invH);
      this.saturationUniform.set(1.0F);
      this.tintIntensityUniform.set(0.0F);
      this.tintColorUniform.set(1.0F, 1.0F, 1.0F);
   }

   @Override
   protected void setup() {
      this.resolutionUniform = this.findUniform("Resolution");
      this.offsetUniform = this.findUniform("Offset");
      this.saturationUniform = this.findUniform("Saturation");
      this.tintIntensityUniform = this.findUniform("TintIntensity");
      this.tintColorUniform = this.findUniform("TintColor");
      super.setup();
   }
}
