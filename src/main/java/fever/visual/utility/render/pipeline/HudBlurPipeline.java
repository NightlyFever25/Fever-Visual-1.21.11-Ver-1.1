package fever.visual.utility.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import fever.visual.FeverVisual;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

/** Fullscreen Kawase pass used by the HUD blur cache without per-pass vertex uploads. */
public final class HudBlurPipeline implements AutoCloseable {
   private static final RenderPipeline DOWN = createPipeline("hud_kawase_down");
   private static final RenderPipeline UP = createPipeline("hud_kawase_up");
   private GpuBuffer uniformBuffer;
   private GpuBuffer dummyVertexBuffer;
   private ByteBuffer uniformData;

   public void render(GpuTextureView target, GpuTextureView source, int sourceWidth, int sourceHeight, float offset, boolean up) {
      if (target == null || source == null || sourceWidth <= 0 || sourceHeight <= 0) return;
      this.ensureInitialized();

      this.uniformData.clear();
      this.uniformData.putFloat(sourceWidth).putFloat(sourceHeight).putFloat(offset).putFloat(0.0F).flip();

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(this.uniformBuffer.slice(), this.uniformData);
      GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

      try (RenderPass pass = encoder.createRenderPass(() -> "fevervisual:hud_blur", target, OptionalInt.empty())) {
         pass.setPipeline(up ? UP : DOWN);
         pass.setVertexBuffer(0, this.dummyVertexBuffer);
         pass.bindTexture("Sampler0", source, sampler);
         pass.setUniform("KawaseData", this.uniformBuffer);
         pass.draw(0, 6);
      }
   }

   private void ensureInitialized() {
      if (this.uniformBuffer != null) return;
      this.uniformData = MemoryUtil.memAlloc(16);
      this.uniformBuffer = RenderSystem.getDevice().createBuffer(
         () -> "fevervisual:hud_blur_uniform", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16
      );
      ByteBuffer dummy = MemoryUtil.memAlloc(4);
      dummy.putInt(0).flip();
      this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
         () -> "fevervisual:hud_blur_vertex", GpuBuffer.USAGE_VERTEX, dummy
      );
      MemoryUtil.memFree(dummy);
   }

   private static RenderPipeline createPipeline(String fragment) {
      return RenderPipelines.register(
         RenderPipeline.builder()
            .withLocation(FeverVisual.id("pipeline/" + fragment))
            .withVertexShader(FeverVisual.id("core/hud_kawase"))
            .withFragmentShader(FeverVisual.id("core/" + fragment))
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build()
      );
   }

   @Override
   public void close() {
      if (this.uniformBuffer != null) this.uniformBuffer.close();
      if (this.dummyVertexBuffer != null) this.dummyVertexBuffer.close();
      if (this.uniformData != null) MemoryUtil.memFree(this.uniformData);
      this.uniformBuffer = null;
      this.dummyVertexBuffer = null;
      this.uniformData = null;
   }
}
