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

public class FogBlurCompositePipeline {
   private static final BlendFunction REPLACE_BLEND = new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE, DestFactor.ZERO);
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(FeverVisual.id("pipeline/fog_blur_composite"))
         .withVertexShader(FeverVisual.id("core/fog_blur_composite"))
         .withFragmentShader(FeverVisual.id("core/fog_blur_composite"))
         .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
         .withUniform("FogBlurData", UniformType.UNIFORM_BUFFER)
         .withSampler("SceneSampler")
         .withSampler("BlurSampler")
         .withSampler("DepthSampler")
         .withBlend(REPLACE_BLEND)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f MODEL_OFFSET = new Vector3f();
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static final int BUFFER_SIZE = 32;

   private GpuBuffer uniformBuffer;
   private GpuBuffer dummyVertexBuffer;
   private ByteBuffer dataBuffer;
   private boolean initialized;

   public void composite(
      GpuTextureView targetView,
      GpuTextureView sceneView,
      GpuTextureView blurView,
      GpuTextureView depthView,
      int width,
      int height,
      float nearPlane,
      float farPlane,
      float fogStart,
      float fogEnd,
      float strength
   ) {
      if (targetView == null || sceneView == null || blurView == null || depthView == null) {
         return;
      }

      ensureInitialized();
      prepareUniformData(width, height, nearPlane, farPlane, fogStart, fogEnd, strength);
      int size = this.dataBuffer.remaining();
      if (this.uniformBuffer == null || this.uniformBuffer.size() < size) {
         if (this.uniformBuffer != null) {
            this.uniformBuffer.close();
         }

         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:fog_blur_composite_uniform", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, size);
      }

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(this.uniformBuffer.slice(), this.dataBuffer);
      GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
      GpuSampler linearSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      GpuSampler nearestSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);

      try (RenderPass pass = encoder.createRenderPass(() -> "fevervisual:fog_blur_composite_pass", targetView, OptionalInt.empty())) {
         pass.setPipeline(PIPELINE);
         pass.setVertexBuffer(0, this.dummyVertexBuffer);
         pass.bindTexture("SceneSampler", sceneView, linearSampler);
         pass.bindTexture("BlurSampler", blurView, linearSampler);
         pass.bindTexture("DepthSampler", depthView, nearestSampler);
         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("DynamicTransforms", transforms);
         pass.setUniform("FogBlurData", this.uniformBuffer);
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
      this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:fog_blur_composite_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
      MemoryUtil.memFree(dummy);
      this.initialized = true;
   }

   private void prepareUniformData(int width, int height, float nearPlane, float farPlane, float fogStart, float fogEnd, float strength) {
      this.dataBuffer.clear();
      this.dataBuffer.putFloat(width).putFloat(height).putFloat(nearPlane).putFloat(farPlane);
      this.dataBuffer.putFloat(fogStart).putFloat(fogEnd).putFloat(strength).putFloat(0.0F);
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
