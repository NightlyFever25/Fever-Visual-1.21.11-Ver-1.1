package fever.visual.utility.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
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

public class KawaseBlurPipeline {
   private static final RenderPipeline DOWN_PIPELINE = RenderPipelines.register(
           RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                   .withLocation(FeverVisual.id("pipeline/kawase_down_hands"))
                   .withVertexShader(FeverVisual.id("core/kawase_down"))
                   .withFragmentShader(FeverVisual.id("core/kawase_down"))
                   .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                   .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
                   .withSampler("Sampler0")
                   .withBlend(BlendFunction.TRANSLUCENT)
                   .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                   .withDepthWrite(false)
                   .withCull(false)
                   .build()
   );
   private static final RenderPipeline UP_PIPELINE = RenderPipelines.register(
           RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                   .withLocation(FeverVisual.id("pipeline/kawase_up_hands"))
                   .withVertexShader(FeverVisual.id("core/kawase_up"))
                   .withFragmentShader(FeverVisual.id("core/kawase_up"))
                   .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                   .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
                   .withSampler("Sampler0")
                   .withBlend(BlendFunction.TRANSLUCENT)
                   .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                   .withDepthWrite(false)
                   .withCull(false)
                   .build()
   );
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f MODEL_OFFSET = new Vector3f();
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static final int MAX_ITERATIONS = 8;
   private static final int BUFFER_SIZE = 32;

   private GpuBuffer uniformBuffer;
   private GpuBuffer dummyVertexBuffer;
   private ByteBuffer dataBuffer;
   private GpuTexture[] downTextures;
   private GpuTextureView[] downTextureViews;
   private GpuTexture[] upTextures;
   private GpuTextureView[] upTextureViews;
   private int[] downWidths;
   private int[] downHeights;
   private int[] upWidths;
   private int[] upHeights;
   private GpuTexture finalTexture;
   private GpuTextureView finalTextureView;
   private int lastWidth;
   private int lastHeight;
   private boolean initialized;

   public GpuTextureView blur(GpuTexture sourceTexture, GpuTextureView sourceView, int width, int height, int iterations, float offset) {
      if (sourceTexture == null || sourceView == null) {
         return null;
      }

      ensureInitialized();
      ensureFramebuffers(width, height);
      iterations = Math.max(1, Math.min(MAX_ITERATIONS, iterations));

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      GpuTextureView currentSource = sourceView;
      int currentWidth = width;
      int currentHeight = height;

      for (int i = 0; i < iterations; i++) {
         prepareUniformData(currentWidth, currentHeight, offset);
         ensureUniformBuffer();
         encoder.writeToBuffer(this.uniformBuffer.slice(), this.dataBuffer);
         renderPass(encoder, DOWN_PIPELINE, this.downTextureViews[i], currentSource, sampler, "fevervisual:kawase_down_" + i);
         currentSource = this.downTextureViews[i];
         currentWidth = this.downWidths[i];
         currentHeight = this.downHeights[i];
      }

      for (int i = iterations - 1; i >= 0; i--) {
         prepareUniformData(currentWidth, currentHeight, offset);
         encoder.writeToBuffer(this.uniformBuffer.slice(), this.dataBuffer);
         renderPass(encoder, UP_PIPELINE, this.upTextureViews[i], currentSource, sampler, "fevervisual:kawase_up_" + i);
         currentSource = this.upTextureViews[i];
         currentWidth = this.upWidths[i];
         currentHeight = this.upHeights[i];
      }

      prepareUniformData(currentWidth, currentHeight, offset);
      encoder.writeToBuffer(this.uniformBuffer.slice(), this.dataBuffer);
      renderPass(encoder, UP_PIPELINE, this.finalTextureView, currentSource, sampler, "fevervisual:kawase_final");
      return this.finalTextureView;
   }

   private void renderPass(CommandEncoder encoder, RenderPipeline pipeline, GpuTextureView targetView, GpuTextureView sourceView, GpuSampler sampler, String name) {
      GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
      try (RenderPass pass = encoder.createRenderPass(() -> name, targetView, OptionalInt.empty())) {
         pass.setPipeline(pipeline);
         pass.setVertexBuffer(0, this.dummyVertexBuffer);
         pass.bindTexture("Sampler0", sourceView, sampler);
         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("DynamicTransforms", transforms);
         pass.setUniform("KawaseData", this.uniformBuffer);
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
      this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:kawase_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
      MemoryUtil.memFree(dummy);
      this.downTextures = new GpuTexture[MAX_ITERATIONS];
      this.downTextureViews = new GpuTextureView[MAX_ITERATIONS];
      this.upTextures = new GpuTexture[MAX_ITERATIONS];
      this.upTextureViews = new GpuTextureView[MAX_ITERATIONS];
      this.downWidths = new int[MAX_ITERATIONS];
      this.downHeights = new int[MAX_ITERATIONS];
      this.upWidths = new int[MAX_ITERATIONS];
      this.upHeights = new int[MAX_ITERATIONS];
      this.initialized = true;
   }

   private void ensureFramebuffers(int width, int height) {
      if (width == this.lastWidth && height == this.lastHeight && this.finalTextureView != null) {
         return;
      }

      cleanupFramebuffers();
      this.finalTexture = RenderSystem.getDevice().createTexture(() -> "fevervisual:kawase_final", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, width, height, 1, 1);
      this.finalTextureView = RenderSystem.getDevice().createTextureView(this.finalTexture);

      int currentWidth = width;
      int currentHeight = height;
      for (int i = 0; i < MAX_ITERATIONS; i++) {
         currentWidth = Math.max(1, currentWidth / 2);
         currentHeight = Math.max(1, currentHeight / 2);
         this.downWidths[i] = currentWidth;
         this.downHeights[i] = currentHeight;
         this.upWidths[i] = currentWidth;
         this.upHeights[i] = currentHeight;
         int index = i;
         int textureWidth = currentWidth;
         int textureHeight = currentHeight;
         this.downTextures[i] = RenderSystem.getDevice().createTexture(() -> "fevervisual:kawase_down_" + index, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, textureWidth, textureHeight, 1, 1);
         this.downTextureViews[i] = RenderSystem.getDevice().createTextureView(this.downTextures[i]);
         this.upTextures[i] = RenderSystem.getDevice().createTexture(() -> "fevervisual:kawase_up_" + index, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, textureWidth, textureHeight, 1, 1);
         this.upTextureViews[i] = RenderSystem.getDevice().createTextureView(this.upTextures[i]);
      }

      this.lastWidth = width;
      this.lastHeight = height;
   }

   private void ensureUniformBuffer() {
      int size = this.dataBuffer.remaining();
      if (this.uniformBuffer == null || this.uniformBuffer.size() < size) {
         if (this.uniformBuffer != null) {
            this.uniformBuffer.close();
         }
         this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:kawase_uniform", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, size);
      }
   }

   private void prepareUniformData(int width, int height, float offset) {
      this.dataBuffer.clear();
      this.dataBuffer.putFloat(width).putFloat(height).putFloat(offset).putFloat(0.0F);
      this.dataBuffer.flip();
   }

   private void cleanupFramebuffers() {
      closeTexture(this.finalTextureView);
      closeTexture(this.finalTexture);
      this.finalTextureView = null;
      this.finalTexture = null;
      if (this.downTextureViews != null) {
         for (int i = 0; i < MAX_ITERATIONS; i++) {
            closeTexture(this.downTextureViews[i]);
            closeTexture(this.downTextures[i]);
            closeTexture(this.upTextureViews[i]);
            closeTexture(this.upTextures[i]);
            this.downTextureViews[i] = null;
            this.downTextures[i] = null;
            this.upTextureViews[i] = null;
            this.upTextures[i] = null;
         }
      }
   }

   private static void closeTexture(AutoCloseable texture) {
      if (texture == null) {
         return;
      }
      try {
         texture.close();
      } catch (Exception ignored) {
      }
   }

   public void close() {
      cleanupFramebuffers();
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
      this.lastWidth = 0;
      this.lastHeight = 0;
   }
}
