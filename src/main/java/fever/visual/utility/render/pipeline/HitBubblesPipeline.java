package fever.visual.utility.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
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
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryUtil;

public final class HitBubblesPipeline {
   private static final BlendFunction TRANSLUCENT_BLEND = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new RenderPipeline.Snippet[0])
         .withLocation(FeverVisual.id("pipeline/hit_bubbles"))
         .withVertexShader(FeverVisual.id("core/hit_bubbles"))
         .withFragmentShader(FeverVisual.id("core/hit_bubbles"))
         .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
         .withUniform("Ripples", UniformType.UNIFORM_BUFFER)
         .withSampler("Scene")
         .withBlend(TRANSLUCENT_BLEND)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final int FLOAT_COUNT = 156;
   private static final int UNIFORM_SIZE = FLOAT_COUNT * Float.BYTES;

   private GpuBuffer uniformBuffer;
   private GpuBuffer dummyVertexBuffer;
   private GpuTexture sceneTexture;
   private GpuTextureView sceneTextureView;
   private ByteBuffer dataBuffer;
   private int width = -1;
   private int height = -1;
   private boolean disabledAfterError;

   public void apply(Framebuffer framebuffer, float[] uniform) {
      if (this.disabledAfterError
         || framebuffer == null
         || framebuffer.getColorAttachment() == null
         || framebuffer.getColorAttachmentView() == null
         || framebuffer.textureWidth <= 0
         || framebuffer.textureHeight <= 0) {
         return;
      }

      try {
         this.applyUnsafe(framebuffer, uniform);
      } catch (Throwable throwable) {
         FeverVisual.LOGGER.error("Hit Bubbles post-processing failed", throwable);
         this.disabledAfterError = true;
         this.close();
      }
   }

   private void applyUnsafe(Framebuffer framebuffer, float[] uniform) {
      this.ensureInitialized();
      this.ensureTexture(framebuffer.textureWidth, framebuffer.textureHeight);
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.copyTextureToTexture(framebuffer.getColorAttachment(), this.sceneTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);

      this.dataBuffer.clear();
      for (int i = 0; i < FLOAT_COUNT; i++) {
         this.dataBuffer.putFloat(i < uniform.length ? uniform[i] : 0.0F);
      }
      this.dataBuffer.flip();
      encoder.writeToBuffer(this.uniformBuffer.slice(), this.dataBuffer);

      GpuSampler linearSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      try (RenderPass pass = encoder.createRenderPass(() -> "fevervisual:hit_bubbles", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
         pass.setPipeline(PIPELINE);
         pass.setVertexBuffer(0, this.dummyVertexBuffer);
         pass.bindTexture("Scene", this.sceneTextureView, linearSampler);
         pass.setUniform("Ripples", this.uniformBuffer);
         pass.draw(0, 6);
      }
   }

   private void ensureInitialized() {
      if (this.dataBuffer != null) {
         return;
      }

      this.dataBuffer = MemoryUtil.memAlloc(UNIFORM_SIZE);
      this.uniformBuffer = RenderSystem.getDevice().createBuffer(
         () -> "fevervisual:hit_bubbles_uniform",
         GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
         UNIFORM_SIZE
      );
      ByteBuffer dummy = MemoryUtil.memAlloc(4);
      dummy.putInt(0).flip();
      this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:hit_bubbles_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
      MemoryUtil.memFree(dummy);
   }

   private void ensureTexture(int width, int height) {
      if (this.sceneTexture != null && this.width == width && this.height == height) {
         return;
      }

      this.closeTexture();
      this.sceneTexture = RenderSystem.getDevice().createTexture(
         () -> "fevervisual:hit_bubbles_scene",
         GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
         TextureFormat.RGBA8,
         width,
         height,
         1,
         1
      );
      this.sceneTextureView = RenderSystem.getDevice().createTextureView(this.sceneTexture);
      this.width = width;
      this.height = height;
   }

   public void close() {
      this.closeTexture();
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
   }

   public void reset() {
      this.close();
      this.disabledAfterError = false;
   }

   private void closeTexture() {
      if (this.sceneTextureView != null) {
         this.sceneTextureView.close();
         this.sceneTextureView = null;
      }
      if (this.sceneTexture != null) {
         this.sceneTexture.close();
         this.sceneTexture = null;
      }
      this.width = -1;
      this.height = -1;
   }
}
