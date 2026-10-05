package fever.visual.utility.render.compat;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import fever.visual.FeverVisual;
import fever.visual.framework.shader.GlProgram;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
public final class RenderBridge {
   private static final Map<PipelineKey, RenderPipeline> PIPELINES = new HashMap<>();
   private static final ThreadLocal<DrawScratch> DRAW_SCRATCH = ThreadLocal.withInitial(DrawScratch::new);

   private RenderBridge() {
   }

   public static void draw(BuiltBuffer buffer) {
      if (buffer == null) {
         return;
      }

      RenderSystem.State state = RenderSystem.state();
      GlProgram program = state.program;
      BuiltBuffer.DrawParameters parameters = buffer.getDrawParameters();
      RenderPipeline pipeline = program != null ? program.pipeline(state, parameters) : standardPipeline(state, parameters);
      GpuTextureView temporaryView = null;

      try (buffer) {
         GpuBuffer vertexBuffer = parameters.format().uploadImmediateVertexBuffer(buffer.getBuffer());
         GpuBuffer indexBuffer;
         VertexFormat.IndexType indexType;
         if (buffer.getSortedBuffer() == null) {
            com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer sequential =
               com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(parameters.mode());
            indexBuffer = sequential.getIndexBuffer(parameters.indexCount());
            indexType = sequential.getIndexType();
         } else {
            indexBuffer = parameters.format().uploadImmediateIndexBuffer(buffer.getSortedBuffer());
            indexType = parameters.indexType();
         }

         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         boolean overriddenTarget = com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride != null;
         GpuTextureView colorTarget = overriddenTarget
            ? com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride
            : framebuffer.getColorAttachmentView();
         GpuTextureView depthTarget = overriddenTarget
            ? com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride
            : framebuffer.useDepthAttachment ? framebuffer.getDepthAttachmentView() : null;
         DrawScratch scratch = DRAW_SCRATCH.get();
         scratch.color.set(state.shaderColor[0], state.shaderColor[1], state.shaderColor[2], state.shaderColor[3]);
         GpuBufferSlice transforms = com.mojang.blaze3d.systems.RenderSystem.getDynamicUniforms().write(
            com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix(),
            scratch.color,
            scratch.offset,
            scratch.textureMatrix
         );
         Object texture = state.texture;
         boolean expectsTexture = program != null ? program.hasSampler() : state.shader.textured();
         TextureBinding textureBinding = texture != null && expectsTexture ? resolveTexture(texture) : null;
         if (textureBinding != null && textureBinding.temporary()) {
            temporaryView = textureBinding.view();
         }
         CommandEncoder encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
         if (program != null) program.prepareUniforms(encoder);

         try (RenderPass pass = encoder.createRenderPass(
            () -> "Fever Visual immediate draw", colorTarget, OptionalInt.empty(), depthTarget, OptionalDouble.empty()
         )) {
            pass.setPipeline(pipeline);
            com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indexBuffer, indexType);
            if (state.scissor != null) {
               int scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
               int targetHeight = colorTarget.texture().getHeight(0);
               int left = (int)Math.floor(state.scissor.getLeft() * (double)scale);
               int right = (int)Math.ceil(state.scissor.getRight() * (double)scale);
               int top = (int)Math.floor(state.scissor.getTop() * (double)scale);
               int bottom = (int)Math.ceil(state.scissor.getBottom() * (double)scale);
               pass.enableScissor(left, targetHeight - bottom, Math.max(0, right - left), Math.max(0, bottom - top));
            }
            if (textureBinding != null) {
               pass.bindTexture(
                  program != null ? program.primarySampler() : "Sampler0",
                  textureBinding.view(),
                  textureBinding.sampler()
               );
            }
            if (program != null) {
               program.bindUniforms(pass);
            }
            pass.drawIndexed(0, 0, parameters.indexCount(), 1);
         }
      } catch (RuntimeException exception) {
         FeverVisual.LOGGER.error("Failed to submit a 1.21.11 render pass", exception);
         throw exception;
      } finally {
         if (temporaryView != null) {
            temporaryView.close();
         }
         if (program != null) {
            program.finishDraw();
         }
      }
   }

   private static RenderPipeline standardPipeline(RenderSystem.State state, BuiltBuffer.DrawParameters parameters) {
      PipelineKey key = new PipelineKey(
         state.shader,
         parameters.format(),
         parameters.mode(),
         state.blend,
         state.depthTest,
         state.cull,
         state.writeDepth
      );
      return PIPELINES.computeIfAbsent(key, ignored -> {
         RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(FeverVisual.id("pipeline/legacy_" + Integer.toUnsignedString(key.hashCode(), 16)))
            .withVertexShader(key.shader.shader())
            .withFragmentShader(key.shader.shader())
            .withVertexFormat(key.format, key.mode)
            .withDepthTestFunction(key.depthTest)
            .withDepthWrite(key.writeDepth)
            .withCull(key.cull);
         if (key.shader.textured()) {
            builder.withSampler("Sampler0");
         }
         if (key.blend == null) {
            builder.withoutBlend();
         } else {
            builder.withBlend(key.blend);
         }
         return RenderPipelines.register(builder.build());
      });
   }

   private static TextureBinding resolveTexture(Object value) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (value instanceof Identifier id) {
         AbstractTexture texture = client.getTextureManager().getTexture(id);
         return new TextureBinding(texture.getGlTextureView(), texture.getSampler(), false);
      }
      if (value instanceof GpuTextureView view) {
         return new TextureBinding(
            view,
            com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().get(FilterMode.LINEAR),
            false
         );
      }
      if (value instanceof GpuTexture texture) {
         GpuTextureView view = com.mojang.blaze3d.systems.RenderSystem.getDevice().createTextureView(texture);
         return new TextureBinding(
            view,
            com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().get(FilterMode.LINEAR),
            true
         );
      }
      throw new IllegalArgumentException("Unsupported texture binding: " + value);
   }

   private record PipelineKey(
      ShaderProgramKeys.Key shader,
      VertexFormat format,
      com.mojang.blaze3d.vertex.VertexFormat.DrawMode mode,
      com.mojang.blaze3d.pipeline.BlendFunction blend,
      com.mojang.blaze3d.platform.DepthTestFunction depthTest,
      boolean cull,
      boolean writeDepth
   ) {
   }

   private record TextureBinding(GpuTextureView view, GpuSampler sampler, boolean temporary) {
   }

   private static final class DrawScratch {
      private final Vector4f color = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
      private final Vector3f offset = new Vector3f();
      private final Matrix4f textureMatrix = new Matrix4f();
   }
}
