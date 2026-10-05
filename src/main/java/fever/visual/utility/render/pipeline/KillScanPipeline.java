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
import fever.visual.utility.colors.ColorRGBA;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class KillScanPipeline {
   private static final BlendFunction ADDITIVE_BLEND = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ONE);
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new RenderPipeline.Snippet[0])
         .withLocation(FeverVisual.id("pipeline/kill_scan"))
         .withVertexShader(FeverVisual.id("core/kill_scan"))
         .withFragmentShader(FeverVisual.id("core/kill_scan"))
         .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
         .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
         .withSampler("DepthSampler")
         .withBlend(ADDITIVE_BLEND)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final int FLOAT_COUNT = 64;
   private static final int UNIFORM_SIZE = FLOAT_COUNT * Float.BYTES;

   private final float[] uniformScratch = new float[FLOAT_COUNT];
   private GpuBuffer uniformBuffer;
   private GpuBuffer dummyVertexBuffer;
   private GpuTexture depthTexture;
   private GpuTextureView depthTextureView;
   private ByteBuffer dataBuffer;
   private int width = -1;
   private int height = -1;
   private boolean disabledAfterError;

   public void apply(
      Framebuffer framebuffer,
      Matrix4f invProjection,
      Matrix4f invView,
      Vec3d cameraPos,
      Vec3d center,
      float radius,
      float width,
      float sharpness,
      float progress,
      float flick,
      ColorRGBA outerColor,
      ColorRGBA midColor,
      ColorRGBA innerColor,
      ColorRGBA scanlineColor
   ) {
      if (this.disabledAfterError
         || framebuffer == null
         || framebuffer.getColorAttachmentView() == null
         || framebuffer.getDepthAttachment() == null
         || framebuffer.textureWidth <= 0
         || framebuffer.textureHeight <= 0) {
         return;
      }

      try {
         this.pack(invProjection, invView, cameraPos, center, radius, width, sharpness, progress, flick, outerColor, midColor, innerColor, scanlineColor);
         this.applyUnsafe(framebuffer, this.uniformScratch);
      } catch (Throwable throwable) {
         this.disabledAfterError = true;
         this.close();
      }
   }

   private void pack(
      Matrix4f invProjection,
      Matrix4f invView,
      Vec3d cameraPos,
      Vec3d center,
      float radius,
      float width,
      float sharpness,
      float progress,
      float flick,
      ColorRGBA outerColor,
      ColorRGBA midColor,
      ColorRGBA innerColor,
      ColorRGBA scanlineColor
   ) {
      for (int i = 0; i < this.uniformScratch.length; i++) {
         this.uniformScratch[i] = 0.0F;
      }
      this.putMatrix(0, invProjection);
      this.putMatrix(16, invView);
      this.uniformScratch[32] = (float)cameraPos.x;
      this.uniformScratch[33] = (float)cameraPos.y;
      this.uniformScratch[34] = (float)cameraPos.z;
      this.uniformScratch[35] = radius;
      this.uniformScratch[36] = (float)center.x;
      this.uniformScratch[37] = (float)center.y;
      this.uniformScratch[38] = (float)center.z;
      this.uniformScratch[39] = width;
      this.putColor(40, outerColor);
      this.putColor(44, midColor);
      this.putColor(48, innerColor);
      this.putColor(52, scanlineColor);
      this.uniformScratch[56] = sharpness;
      this.uniformScratch[57] = progress;
      this.uniformScratch[58] = flick;
   }

   private void putColor(int offset, ColorRGBA color) {
      this.uniformScratch[offset] = color.getRed() / 255.0F;
      this.uniformScratch[offset + 1] = color.getGreen() / 255.0F;
      this.uniformScratch[offset + 2] = color.getBlue() / 255.0F;
      this.uniformScratch[offset + 3] = color.getAlpha() / 255.0F;
   }

   private void putMatrix(int offset, Matrix4f matrix) {
      this.uniformScratch[offset] = matrix.m00();
      this.uniformScratch[offset + 1] = matrix.m01();
      this.uniformScratch[offset + 2] = matrix.m02();
      this.uniformScratch[offset + 3] = matrix.m03();
      this.uniformScratch[offset + 4] = matrix.m10();
      this.uniformScratch[offset + 5] = matrix.m11();
      this.uniformScratch[offset + 6] = matrix.m12();
      this.uniformScratch[offset + 7] = matrix.m13();
      this.uniformScratch[offset + 8] = matrix.m20();
      this.uniformScratch[offset + 9] = matrix.m21();
      this.uniformScratch[offset + 10] = matrix.m22();
      this.uniformScratch[offset + 11] = matrix.m23();
      this.uniformScratch[offset + 12] = matrix.m30();
      this.uniformScratch[offset + 13] = matrix.m31();
      this.uniformScratch[offset + 14] = matrix.m32();
      this.uniformScratch[offset + 15] = matrix.m33();
   }

   private void applyUnsafe(Framebuffer framebuffer, float[] uniform) {
      this.ensureInitialized();
      this.ensureTexture(framebuffer.textureWidth, framebuffer.textureHeight);
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.copyTextureToTexture(framebuffer.getDepthAttachment(), this.depthTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);

      this.dataBuffer.clear();
      for (int i = 0; i < FLOAT_COUNT; i++) {
         this.dataBuffer.putFloat(i < uniform.length ? uniform[i] : 0.0F);
      }
      this.dataBuffer.flip();
      encoder.writeToBuffer(this.uniformBuffer.slice(), this.dataBuffer);

      GpuSampler nearestSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
      try (RenderPass pass = encoder.createRenderPass(() -> "fevervisual:kill_scan", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
         pass.setPipeline(PIPELINE);
         pass.setVertexBuffer(0, this.dummyVertexBuffer);
         pass.bindTexture("DepthSampler", this.depthTextureView, nearestSampler);
         pass.setUniform("Uniforms", this.uniformBuffer);
         pass.draw(0, 6);
      }
   }

   private void ensureInitialized() {
      if (this.dataBuffer != null) {
         return;
      }

      this.dataBuffer = MemoryUtil.memAlloc(UNIFORM_SIZE);
      this.uniformBuffer = RenderSystem.getDevice().createBuffer(
         () -> "fevervisual:kill_scan_uniform",
         GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
         UNIFORM_SIZE
      );
      ByteBuffer dummy = MemoryUtil.memAlloc(4);
      dummy.putInt(0).flip();
      this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:kill_scan_dummy_vertex", GpuBuffer.USAGE_VERTEX, dummy);
      MemoryUtil.memFree(dummy);
   }

   private void ensureTexture(int width, int height) {
      if (this.depthTexture != null && this.width == width && this.height == height) {
         return;
      }

      this.closeTexture();
      this.depthTexture = RenderSystem.getDevice().createTexture(
         () -> "fevervisual:kill_scan_depth",
         GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
         TextureFormat.DEPTH32,
         width,
         height,
         1,
         1
      );
      this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
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

   private void closeTexture() {
      if (this.depthTextureView != null) {
         this.depthTextureView.close();
         this.depthTextureView = null;
      }
      if (this.depthTexture != null) {
         this.depthTexture.close();
         this.depthTexture = null;
      }
      this.width = -1;
      this.height = -1;
   }
}
