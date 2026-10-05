package fever.visual.utility.render.batching;

import fever.visual.utility.render.compat.RenderSystem;
import lombok.Generated;
import net.minecraft.client.render.BufferBuilder;
import fever.visual.utility.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

public abstract class Batching {
   protected static Batching active;
   protected BufferBuilder builder;

   public Batching(VertexFormat vertexFormat) {
      this(DrawMode.QUADS, vertexFormat);
   }

   protected Batching(DrawMode drawMode, VertexFormat vertexFormat) {
      this.builder = RenderSystem.renderThreadTesselator().begin(drawMode, vertexFormat);
      active = this;
   }

   protected void build() {
      BuiltBuffer builtBuffer = this.builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }
   }

   public abstract void draw();

   @Generated
   public BufferBuilder getBuilder() {
      return this.builder;
   }

   @Generated
   public static Batching getActive() {
      return active;
   }
}
