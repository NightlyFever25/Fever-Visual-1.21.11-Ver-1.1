package fever.visual.utility.render.batching.impl;

import fever.visual.utility.render.compat.RenderSystem;
import lombok.Generated;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.batching.Batching;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;

public class RectBatching extends Batching {
   private final MatrixStack matrices;

   public RectBatching(VertexFormat vertexFormat, MatrixStack matrices) {
      super(vertexFormat);
      this.matrices = matrices;
   }

   @Override
   public void draw() {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      DrawUtility.drawSetup();
      this.build();
      DrawUtility.drawEnd();
      if (active == this) {
         active = null;
      }
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
   }
}
