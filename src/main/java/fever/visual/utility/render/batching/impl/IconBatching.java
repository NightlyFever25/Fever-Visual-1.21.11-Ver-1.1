package fever.visual.utility.render.batching.impl;

import fever.visual.utility.render.compat.RenderSystem;
import lombok.Generated;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.batching.Batching;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;

public class IconBatching extends Batching {
   private final MatrixStack matrices;

   public IconBatching(VertexFormat vertexFormat, MatrixStack matrices) {
      super(vertexFormat);
      this.matrices = matrices;
   }

   @Override
   public void draw() {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.enableBlend();
      this.build();
      DrawUtility.drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      if (active == this) {
         active = null;
      }
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
   }
}
