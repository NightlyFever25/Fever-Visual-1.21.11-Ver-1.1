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

public class HandShaderPipeline {
   private static final BlendFunction REPLACE_BLEND = new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE, DestFactor.ZERO);
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
           RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                   .withLocation(FeverVisual.id("pipeline/hand_shader"))
                   .withVertexShader(FeverVisual.id("core/hand_shader"))
                   .withFragmentShader(FeverVisual.id("core/hand_shader"))
                   .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                   .withUniform("HandShaderData", UniformType.UNIFORM_BUFFER)
                   .withSampler("AfterSampler")
                   .withSampler("BeforeSampler")
                   .withSampler("TrailSampler")
                   .withSampler("DepthSampler")
                   .withSampler("BeforeDepthSampler")
                   .withBlend(REPLACE_BLEND)
                   .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                   .withDepthWrite(false)
                   .withCull(false)
                   .build()
   );
   private static final RenderPipeline FIRE_TRAIL_PIPELINE = RenderPipelines.register(
           RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                   .withLocation(FeverVisual.id("pipeline/hand_fire_trail"))
                   .withVertexShader(FeverVisual.id("core/hand_shader"))
                   .withFragmentShader(FeverVisual.id("core/hand_fire_trail"))
                   .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                   .withUniform("HandShaderData", UniformType.UNIFORM_BUFFER)
                   .withSampler("AfterSampler")
                   .withSampler("BeforeSampler")
                   .withSampler("PrevTrailSampler")
                   .withSampler("DepthSampler")
                   .withSampler("BeforeDepthSampler")
                   .withBlend(REPLACE_BLEND)
                   .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                   .withDepthWrite(false)
                   .withCull(false)
                   .build()
   );
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f MODEL_OFFSET = new Vector3f();
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static final int BUFFER_SIZE = 64;

   private GpuBuffer uniformBuffer;
   private GpuBuffer dummyVertexBuffer;
   private ByteBuffer dataBuffer;
   private boolean initialized;

   public void render(GpuTextureView targetView, GpuTextureView afterView, GpuTextureView beforeView,
                      GpuTextureView depthView, GpuTextureView beforeDepthView,
                      GpuTextureView previousTrailView, GpuTextureView nextTrailView, int width, int height,
                      float blurRadius, float threshold, int tintColor, float opacity, int mode, float time,
                      float flameStrength, float flameRiseSpeed, float flameWobble, float flameLength, float flameBrightness,
                      float trailStep) {
      if (targetView == null || afterView == null || beforeView == null || depthView == null || beforeDepthView == null
              || width <= 0 || height <= 0) {
         return;
      }

      ensureInitialized();
      prepareUniformData(width, height, blurRadius, threshold, tintColor, opacity, mode, time,
              flameStrength, flameRiseSpeed, flameWobble, flameLength, flameBrightness, trailStep);

      int size = this.dataBuffer.remaining();
      if (this.uniformBuffer == null || this.uniformBuffer.size() < size) {
         if (this.uniformBuffer != null) {
            this.uniformBuffer.close();
         }
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:hand_shader_uniform", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, size);
      }

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(this.uniformBuffer.slice(), this.dataBuffer);
      GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
      GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

      GpuTextureView trailView = previousTrailView == null ? beforeView : previousTrailView;
      if (mode == 6 && trailStep > 0.0F && previousTrailView != null && nextTrailView != null) {
         try (RenderPass pass = encoder.createRenderPass(() -> "fevervisual:hand_fire_trail_pass", nextTrailView, OptionalInt.empty())) {
            pass.setPipeline(FIRE_TRAIL_PIPELINE);
            pass.setVertexBuffer(0, this.dummyVertexBuffer);
            pass.bindTexture("AfterSampler", afterView, sampler);
            pass.bindTexture("BeforeSampler", beforeView, sampler);
            pass.bindTexture("PrevTrailSampler", previousTrailView, sampler);
            pass.bindTexture("DepthSampler", depthView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            pass.bindTexture("BeforeDepthSampler", beforeDepthView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setUniform("HandShaderData", this.uniformBuffer);
            pass.draw(0, 6);
         }
         trailView = nextTrailView;
      }

      try (RenderPass pass = encoder.createRenderPass(() -> "fevervisual:hand_shader_pass", targetView, OptionalInt.empty())) {
         pass.setPipeline(PIPELINE);
         pass.setVertexBuffer(0, this.dummyVertexBuffer);
         pass.bindTexture("AfterSampler", afterView, sampler);
         pass.bindTexture("BeforeSampler", beforeView, sampler);
         pass.bindTexture("TrailSampler", trailView, sampler);
         pass.bindTexture("DepthSampler", depthView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
         pass.bindTexture("BeforeDepthSampler", beforeDepthView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("DynamicTransforms", transforms);
         pass.setUniform("HandShaderData", this.uniformBuffer);
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
      this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:hand_shader_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
      MemoryUtil.memFree(dummy);
      this.initialized = true;
   }

   private void prepareUniformData(int width, int height, float blurRadius, float threshold, int tintColor, float opacity,
                                   int mode, float time, float flameStrength, float flameRiseSpeed,
                                   float flameWobble, float flameLength, float flameBrightness, float trailStep) {
      this.dataBuffer.clear();
      this.dataBuffer.putFloat(width).putFloat(height).putFloat(blurRadius).putFloat(threshold);

      float alpha = ((tintColor >> 24) & 0xFF) / 255.0F;
      float red = ((tintColor >> 16) & 0xFF) / 255.0F;
      float green = ((tintColor >> 8) & 0xFF) / 255.0F;
      float blue = (tintColor & 0xFF) / 255.0F;
      this.dataBuffer.putFloat(red).putFloat(green).putFloat(blue).putFloat(Math.max(0.0F, Math.min(1.0F, opacity * alpha)));
      this.dataBuffer.putFloat(mode).putFloat(time).putFloat(flameStrength).putFloat(flameRiseSpeed);
      this.dataBuffer.putFloat(flameWobble).putFloat(flameLength).putFloat(flameBrightness).putFloat(trailStep);
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
