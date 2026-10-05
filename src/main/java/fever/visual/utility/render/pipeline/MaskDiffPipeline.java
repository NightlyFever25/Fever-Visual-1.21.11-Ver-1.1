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
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public class MaskDiffPipeline {
   private static final BlendFunction REPLACE_BLEND = new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE, DestFactor.ZERO);
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
           RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                   .withLocation(FeverVisual.id("pipeline/mask_diff"))
                   .withVertexShader(FeverVisual.id("core/mask_diff"))
                   .withFragmentShader(FeverVisual.id("core/mask_diff"))
                   .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                   .withUniform("MaskData", UniformType.UNIFORM_BUFFER)
                   .withSampler("BeforeSampler")
                   .withSampler("AfterSampler")
                   .withSampler("DepthBeforeSampler")
                   .withSampler("DepthAfterSampler")
                   .withBlend(REPLACE_BLEND)
                   .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                   .withDepthWrite(false)
                   .withCull(false)
                   .build()
   );
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f MODEL_OFFSET = new Vector3f();
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static final int BUFFER_SIZE = 16;

   private GpuBuffer uniformBuffer;
   private GpuBuffer dummyVertexBuffer;
   private ByteBuffer dataBuffer;
   private boolean initialized;

   public void createMask(GpuTextureView targetView, GpuTextureView beforeView, GpuTextureView afterView, GpuTextureView depthBeforeView, GpuTextureView depthAfterView, int width, int height) {
      if (targetView == null || beforeView == null || afterView == null || depthBeforeView == null || depthAfterView == null) {
         return;
      }

      ensureInitialized();
      prepareUniformData(width, height);
      int size = this.dataBuffer.remaining();
      if (this.uniformBuffer == null || this.uniformBuffer.size() < size) {
         if (this.uniformBuffer != null) {
            this.uniformBuffer.close();
         }
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:mask_diff_uniform", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, size);
      }

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(this.uniformBuffer.slice(), this.dataBuffer);
      GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
      GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      GpuSampler nearestSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);

      try (RenderPass pass = encoder.createRenderPass(() -> "fevervisual:mask_diff_pass", targetView, OptionalInt.of(0x00000000))) {
         pass.setPipeline(PIPELINE);
         pass.setVertexBuffer(0, this.dummyVertexBuffer);
         pass.bindTexture("BeforeSampler", beforeView, sampler);
         pass.bindTexture("AfterSampler", afterView, sampler);
         pass.bindTexture("DepthBeforeSampler", depthBeforeView, nearestSampler);
         pass.bindTexture("DepthAfterSampler", depthAfterView, nearestSampler);
         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("DynamicTransforms", transforms);
         pass.setUniform("MaskData", this.uniformBuffer);
         pass.draw(0, 6);
      }
   }

   private void ensureInitialized() {
      if (this.initialized) {
         return;
      }

      this.dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);
      ByteBuffer dummy = MemoryUtil.memAlloc(4);
      dummy.putInt(0);
      dummy.flip();
      this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:mask_diff_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
      MemoryUtil.memFree(dummy);
      this.initialized = true;
   }

   private void prepareUniformData(int width, int height) {
      this.dataBuffer.clear();
      this.dataBuffer.putFloat(width).putFloat(height).putFloat(0.0F).putFloat(0.0F);
      this.dataBuffer.flip();
   }

   public void close() {
      if (this.uniformBuffer != null) {
         this.uniformBuffer.close();
         this.uniformBuffer = null;
      }
      if (this.dummyVertexBuffer != null) {
         this.dummyVertexBuffer.close();
         this.dummyVertexBuffer = null;
      }
      if (this.dataBuffer != null) {
         MemoryUtil.memFree(this.dataBuffer);
         this.dataBuffer = null;
      }
      this.initialized = false;
   }
}
