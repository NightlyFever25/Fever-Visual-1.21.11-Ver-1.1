package fever.visual.framework.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.vertex.VertexFormat;
import fever.visual.FeverVisual;
import fever.visual.utility.render.compat.RenderSystem;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
public class GlProgram {
   private static final List<GlProgram> REGISTERED_PROGRAMS = new ArrayList<>();
   private static final List<String> BUILT_IN_UNIFORMS = List.of("ModelViewMat", "ProjMat", "ColorModulator");

   private final Identifier id;
   private final VertexFormat vertexFormat;
   private final Identifier vertexShader;
   private final Identifier fragmentShader;
   private final List<String> samplers = new ArrayList<>();
   private final Map<String, Uniform> uniforms = new LinkedHashMap<>();
   private final Map<PipelineState, RenderPipeline> pipelines = new LinkedHashMap<>();
   private GpuBuffer activeUniformBuffer;

   public GlProgram(Identifier id, VertexFormat vertexFormat) {
      this.id = id;
      this.vertexFormat = vertexFormat;
      JsonObject definition = this.readDefinition(id);
      this.vertexShader = Identifier.of(definition.get("vertex").getAsString());
      this.fragmentShader = Identifier.of(definition.get("fragment").getAsString());
      JsonArray samplerArray = definition.getAsJsonArray("samplers");
      if (samplerArray != null) {
         for (JsonElement element : samplerArray) {
            this.samplers.add(element.getAsJsonObject().get("name").getAsString());
         }
      }
      JsonArray uniformArray = definition.getAsJsonArray("uniforms");
      if (uniformArray != null) {
         for (JsonElement element : uniformArray) {
            JsonObject uniform = element.getAsJsonObject();
            String name = uniform.get("name").getAsString();
            if (!BUILT_IN_UNIFORMS.contains(name)) {
               String type = uniform.get("type").getAsString();
               int count = uniform.get("count").getAsInt();
               float[] defaults = new float[count];
               JsonArray values = uniform.getAsJsonArray("values");
               for (int i = 0; i < count && i < values.size(); i++) {
                  defaults[i] = values.get(i).getAsFloat();
               }
               this.uniforms.put(name, new Uniform(type, count, defaults));
            }
         }
      }
      REGISTERED_PROGRAMS.add(this);
   }

   private JsonObject readDefinition(Identifier id) {
      Identifier resourceId = Identifier.of(id.getNamespace(), "shaders/core/" + id.getPath() + ".json");
      Resource resource = MinecraftClient.getInstance().getResourceManager().getResource(resourceId).orElse(null);
      if (resource == null) {
         resourceId = Identifier.of(id.getNamespace(), "shaders/" + id.getPath() + ".json");
         resource = MinecraftClient.getInstance().getResourceManager().getResource(resourceId)
            .orElseThrow(() -> new IllegalStateException("Missing shader definition " + id));
      }
      try (Reader reader = resource.getReader()) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      } catch (IOException exception) {
         throw new IllegalStateException("Failed to read shader definition " + resourceId, exception);
      }
   }

   public GlProgram use() {
      RenderSystem.useProgram(this);
      return this;
   }

   public Uniform findUniform(String name) {
      Uniform uniform = this.uniforms.get(name);
      if (uniform == null) {
         throw new IllegalArgumentException("Shader " + this.id + " has no uniform named " + name);
      }
      return uniform;
   }

   public boolean hasSampler() {
      return !this.samplers.isEmpty();
   }

   public String primarySampler() {
      return this.samplers.isEmpty() ? "Sampler0" : this.samplers.getFirst();
   }

   public RenderPipeline pipeline(RenderSystem.State state, BuiltBuffer.DrawParameters parameters) {
      PipelineState key = new PipelineState(parameters.format(), parameters.mode(), state.blend(), state.depthTest(), state.cull(), state.writeDepth());
      return this.pipelines.computeIfAbsent(key, ignored -> {
         RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(FeverVisual.id("pipeline/custom_" + sanitize(this.id.getPath()) + "_" + Integer.toUnsignedString(key.hashCode(), 16)))
            .withVertexShader(this.vertexShader)
            .withFragmentShader(this.fragmentShader)
            .withVertexFormat(parameters.format(), parameters.mode())
            .withDepthTestFunction(state.depthTest())
            .withDepthWrite(state.writeDepth())
            .withCull(state.cull());
         for (String sampler : this.samplers) {
            builder.withSampler(sampler);
         }
         if (!this.uniforms.isEmpty()) {
            builder.withUniform("FeverUniforms", UniformType.UNIFORM_BUFFER);
         }
         if (state.blend() == null) {
            builder.withoutBlend();
         } else {
            builder.withBlend(state.blend());
         }
         return RenderPipelines.register(builder.build());
      });
   }

   public void prepareUniforms(CommandEncoder encoder) {
      if (this.uniforms.isEmpty()) {
         return;
      }
      try (MemoryStack stack = MemoryStack.stackPush()) {
         Std140Builder builder = Std140Builder.onStack(stack, 4096);
         for (Uniform uniform : this.uniforms.values()) {
            uniform.write(builder);
         }
         ByteBuffer data = builder.get();
         if (this.activeUniformBuffer == null || this.activeUniformBuffer.size() < data.remaining()) {
            if (this.activeUniformBuffer != null) {
               this.activeUniformBuffer.close();
            }
            this.activeUniformBuffer = com.mojang.blaze3d.systems.RenderSystem.getDevice().createBuffer(
               () -> "Fever uniforms " + this.id,
               GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
               data.remaining()
            );
         }
         encoder.writeToBuffer(this.activeUniformBuffer.slice(), data);
      }
   }

   public void bindUniforms(RenderPass pass) {
      if (this.activeUniformBuffer == null) {
         return;
      }
      pass.setUniform("FeverUniforms", this.activeUniformBuffer);
   }

   public void finishDraw() {
      // The buffer is deliberately reused. Command encoders preserve write/draw ordering.
   }

   protected void setup() {
   }

   public static void loadAndSetupPrograms() {
      REGISTERED_PROGRAMS.forEach(GlProgram::setup);
   }

   private static String sanitize(String value) {
      return value.replace('/', '_').replace('.', '_');
   }

   private record PipelineState(
      VertexFormat format,
      com.mojang.blaze3d.vertex.VertexFormat.DrawMode mode,
      com.mojang.blaze3d.pipeline.BlendFunction blend,
      com.mojang.blaze3d.platform.DepthTestFunction depthTest,
      boolean cull,
      boolean writeDepth
   ) {
   }

   public static final class Uniform {
      private final String type;
      private final int count;
      private final float[] values;

      private Uniform(String type, int count, float[] values) {
         this.type = type;
         this.count = count;
         this.values = values;
      }

      public void set(float value) {
         this.requireCount(1);
         this.values[0] = value;
      }

      public void set(int value) {
         this.requireCount(1);
         this.values[0] = value;
      }

      public void set(float x, float y) {
         this.requireCount(2);
         this.values[0] = x;
         this.values[1] = y;
      }

      public void set(float x, float y, float z) {
         this.requireCount(3);
         this.values[0] = x;
         this.values[1] = y;
         this.values[2] = z;
      }

      public void set(float x, float y, float z, float w) {
         this.requireCount(4);
         this.values[0] = x;
         this.values[1] = y;
         this.values[2] = z;
         this.values[3] = w;
      }

      public void set(float[] values) {
         if (values.length != this.count) {
            throw new IllegalArgumentException("Expected " + this.count + " uniform values, got " + values.length);
         }
         System.arraycopy(values, 0, this.values, 0, values.length);
      }

      private void requireCount(int expected) {
         if (this.count != expected) {
            throw new IllegalArgumentException("Expected " + this.count + " uniform values, got " + expected);
         }
      }

      private void write(Std140Builder builder) {
         if ("int".equals(this.type) && this.count == 1) {
            builder.putInt((int)this.values[0]);
            return;
         }
         switch (this.count) {
            case 1 -> builder.putFloat(this.values[0]);
            case 2 -> builder.putVec2(this.values[0], this.values[1]);
            case 3 -> builder.putVec3(this.values[0], this.values[1], this.values[2]);
            case 4 -> builder.putVec4(this.values[0], this.values[1], this.values[2], this.values[3]);
            default -> throw new IllegalStateException("Unsupported uniform shape: " + this.type + "[" + this.count + "]");
         }
      }
   }
}
