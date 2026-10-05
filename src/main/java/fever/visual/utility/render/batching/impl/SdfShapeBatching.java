package fever.visual.utility.render.batching.impl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import fever.visual.FeverVisual;
import fever.visual.utility.render.compat.RenderSystem;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/** Batches exact rounded rectangles and squircles with per-vertex SDF parameters. */
public final class SdfShapeBatching {
   private static final int STRIDE = 56;
   private static final int VERTICES_PER_SHAPE = 6;
   private static final int INITIAL_CAPACITY = STRIDE * VERTICES_PER_SHAPE * 128;
   private static final float SMOOTHNESS = 0.5F;
   private static final SdfShapeBatching INSTANCE = new SdfShapeBatching();
   private static final VertexFormatElement SHAPE_DATA_0 = registerShapeData(3);
   private static final VertexFormatElement SHAPE_DATA_1 = registerShapeData(4);
   private static final VertexFormat FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV0)
      .add("ShapeData0", SHAPE_DATA_0)
      .add("ShapeData1", SHAPE_DATA_1)
      .build();
   private static final RenderPipeline PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(FeverVisual.id("pipeline/hud_sdf_shape"))
         .withVertexShader(FeverVisual.id("core/hud_sdf_shape"))
         .withFragmentShader(FeverVisual.id("core/hud_sdf_shape"))
         .withVertexFormat(FORMAT, VertexFormat.DrawMode.TRIANGLES)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static final Vector4f COLOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f OFFSET = new Vector3f();
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

   private ByteBuffer data = MemoryUtil.memAlloc(INITIAL_CAPACITY);
   private GpuBuffer vertexBuffer;
   private int vertexCount;

   private SdfShapeBatching() {
   }

   public static SdfShapeBatching begin() {
      INSTANCE.data.clear();
      INSTANCE.vertexCount = 0;
      return INSTANCE;
   }

   public void addRect(Matrix4f matrix, float x, float y, float width, float height, int color) {
      this.add(matrix, x, y, width, height, 0.0F, 0.0F, 0.0F, 0.0F, 2.0F, color);
   }

   public void add(
      Matrix4f matrix, float x, float y, float width, float height,
      float topLeft, float bottomLeft, float topRight, float bottomRight,
      float exponent, int color
   ) {
      if (width <= 0.0F || height <= 0.0F || (color >>> 24) == 0) {
         return;
      }
      this.ensureCapacity(STRIDE * VERTICES_PER_SHAPE);
      float padding = 0.75F;
      float ax = x - padding * 0.5F;
      float ay = y - padding * 0.5F;
      float aw = width + padding;
      float ah = height + padding;
      this.vertex(matrix, ax, ay, 0.0F, 0.0F, width, height, exponent, topLeft, bottomLeft, topRight, bottomRight, color);
      this.vertex(matrix, ax, ay + ah, 0.0F, 1.0F, width, height, exponent, topLeft, bottomLeft, topRight, bottomRight, color);
      this.vertex(matrix, ax + aw, ay + ah, 1.0F, 1.0F, width, height, exponent, topLeft, bottomLeft, topRight, bottomRight, color);
      this.vertex(matrix, ax, ay, 0.0F, 0.0F, width, height, exponent, topLeft, bottomLeft, topRight, bottomRight, color);
      this.vertex(matrix, ax + aw, ay + ah, 1.0F, 1.0F, width, height, exponent, topLeft, bottomLeft, topRight, bottomRight, color);
      this.vertex(matrix, ax + aw, ay, 1.0F, 0.0F, width, height, exponent, topLeft, bottomLeft, topRight, bottomRight, color);
   }

   public void draw() {
      if (this.vertexCount == 0) {
         return;
      }
      this.data.flip();
      int bytes = this.data.remaining();
      if (this.vertexBuffer == null || this.vertexBuffer.size() < bytes) {
         if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
         }
         this.vertexBuffer = com.mojang.blaze3d.systems.RenderSystem.getDevice().createBuffer(
            () -> "fevervisual:hud_sdf_shapes",
            GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
            nextPowerOfTwo(bytes)
         );
      }

      Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
      boolean overriddenTarget = com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride != null;
      GpuTextureView colorTarget = overriddenTarget
         ? com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride
         : framebuffer.getColorAttachmentView();
      GpuTextureView depthTarget = overriddenTarget
         ? com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride
         : framebuffer.useDepthAttachment ? framebuffer.getDepthAttachmentView() : null;
      RenderSystem.copyShaderColor(COLOR);
      GpuBufferSlice transforms = com.mojang.blaze3d.systems.RenderSystem.getDynamicUniforms().write(
         com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix(), COLOR, OFFSET, TEXTURE_MATRIX
      );
      CommandEncoder encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(this.vertexBuffer.slice(), this.data);
      try (RenderPass pass = encoder.createRenderPass(
         () -> "fevervisual:hud_sdf_shapes", colorTarget, OptionalInt.empty(), depthTarget, OptionalDouble.empty()
      )) {
         pass.setPipeline(PIPELINE);
         com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("DynamicTransforms", transforms);
         pass.setVertexBuffer(0, this.vertexBuffer);
         ScreenRect scissor = RenderSystem.getScissor();
         if (scissor != null) {
            int scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
            int targetHeight = colorTarget.texture().getHeight(0);
            int left = (int)Math.floor(scissor.getLeft() * (double)scale);
            int right = (int)Math.ceil(scissor.getRight() * (double)scale);
            int top = (int)Math.floor(scissor.getTop() * (double)scale);
            int bottom = (int)Math.ceil(scissor.getBottom() * (double)scale);
            pass.enableScissor(left, targetHeight - bottom, Math.max(0, right - left), Math.max(0, bottom - top));
         }
         pass.draw(0, this.vertexCount);
      }
   }

   private void vertex(
      Matrix4f matrix, float x, float y, float u, float v, float width, float height, float exponent,
      float topLeft, float bottomLeft, float topRight, float bottomRight, int color
   ) {
      float transformedX = matrix.m00() * x + matrix.m10() * y + matrix.m30();
      float transformedY = matrix.m01() * x + matrix.m11() * y + matrix.m31();
      float transformedZ = matrix.m02() * x + matrix.m12() * y + matrix.m32();
      this.data.putFloat(transformedX).putFloat(transformedY).putFloat(transformedZ);
      this.data.put((byte)(color >> 16)).put((byte)(color >> 8)).put((byte)color).put((byte)(color >> 24));
      this.data.putFloat(u).putFloat(v);
      this.data.putFloat(width).putFloat(height).putFloat(SMOOTHNESS).putFloat(exponent);
      this.data.putFloat(topLeft).putFloat(bottomLeft).putFloat(topRight).putFloat(bottomRight);
      this.vertexCount++;
   }

   private void ensureCapacity(int additionalBytes) {
      int required = this.data.position() + additionalBytes;
      if (required <= this.data.capacity()) {
         return;
      }
      this.data = MemoryUtil.memRealloc(this.data, nextPowerOfTwo(required));
   }

   private static VertexFormatElement registerShapeData(int index) {
      for (int id = 31; id >= 7; id--) {
         if (VertexFormatElement.byId(id) == null) {
            return VertexFormatElement.register(
               id, index, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4
            );
         }
      }
      throw new IllegalStateException("No free vertex format element for HUD SDF batching");
   }

   private static int nextPowerOfTwo(int value) {
      int highest = Integer.highestOneBit(Math.max(1, value - 1));
      return highest >= 1 << 30 ? value : highest << 1;
   }
}
