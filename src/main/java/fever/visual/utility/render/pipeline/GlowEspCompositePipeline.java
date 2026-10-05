package fever.visual.utility.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public final class GlowEspCompositePipeline implements AutoCloseable {
   private static final BlendFunction ADDITIVE = new BlendFunction(
      SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ONE
   );
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(FeverVisual.id("pipeline/glow_esp_composite"))
         .withVertexShader(FeverVisual.id("core/glow_esp_composite"))
         .withFragmentShader(FeverVisual.id("core/glow_esp_composite"))
         .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
         .withUniform("GlowEspData", UniformType.UNIFORM_BUFFER)
         .withSampler("MaskSampler")
         .withSampler("GlowSampler")
         .withBlend(ADDITIVE)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final Vector4f COLOR = new Vector4f(1.0F);
   private static final Vector3f OFFSET = new Vector3f();
   private static final Matrix4f TEXTURE = new Matrix4f();

   private GpuBuffer uniform;
   private GpuBuffer vertices;
   private ByteBuffer data;

   public void composite(GpuTextureView target, GpuTextureView mask, GpuTextureView glow, float strength,
                         int mode, float divider, boolean chams, boolean outline) {
      if (target == null || mask == null || glow == null) return;
      this.ensureInitialized();
      this.data.clear();
      this.data.putFloat(strength).putFloat(mode).putFloat(divider).putFloat(chams ? 1.0F : 0.0F);
      this.data.putFloat(outline ? 1.0F : 0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).flip();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(this.uniform.slice(), this.data);
      GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), COLOR, OFFSET, TEXTURE);
      GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      try (RenderPass pass = encoder.createRenderPass(() -> "fevervisual:glow_esp_composite", target, OptionalInt.empty())) {
         pass.setPipeline(PIPELINE);
         pass.setVertexBuffer(0, this.vertices);
         pass.bindTexture("MaskSampler", mask, sampler);
         pass.bindTexture("GlowSampler", glow, sampler);
         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("DynamicTransforms", transforms);
         pass.setUniform("GlowEspData", this.uniform);
         pass.draw(0, 6);
      }
   }

   private void ensureInitialized() {
      if (this.data != null) return;
      this.data = MemoryUtil.memAlloc(32);
      this.uniform = RenderSystem.getDevice().createBuffer(
         () -> "fevervisual:glow_esp_uniform", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32
      );
      ByteBuffer dummy = MemoryUtil.memAlloc(4);
      dummy.putInt(0).flip();
      this.vertices = RenderSystem.getDevice().createBuffer(() -> "fevervisual:glow_esp_vertices", GpuBuffer.USAGE_VERTEX, dummy);
      MemoryUtil.memFree(dummy);
   }

   @Override
   public void close() {
      if (this.uniform != null) this.uniform.close();
      if (this.vertices != null) this.vertices.close();
      if (this.data != null) MemoryUtil.memFree(this.data);
      this.uniform = null;
      this.vertices = null;
      this.data = null;
   }
}
