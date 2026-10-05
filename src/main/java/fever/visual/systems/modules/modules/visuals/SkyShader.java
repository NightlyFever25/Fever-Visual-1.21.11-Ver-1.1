package fever.visual.systems.modules.modules.visuals;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.system.MemoryUtil;

@ModuleInfo(name = "Sky Shader", category = ModuleCategory.VISUALS, desc = "modules.descriptions.sky_shader")
public class SkyShader extends BaseModule implements IMinecraft {
   private static final int SKY_UNIFORM_SIZE = 64;
   private static RenderPipeline waterPipeline;
   private static RenderPipeline causticPipeline;
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer uniformDataBuffer;

   private final ModeSetting mode = new ModeSetting(this, "modules.settings.sky_shader.mode", "modules.settings.sky_shader.mode.description");
   private final ModeSetting.Value water = new ModeSetting.Value(this.mode, "modules.settings.sky_shader.mode.water").select();
   private final ModeSetting.Value caustic = new ModeSetting.Value(this.mode, "modules.settings.sky_shader.mode.caustic");
   private final SliderSetting speed = new SliderSetting(this, "modules.settings.sky_shader.speed", "modules.settings.sky_shader.speed.description")
           .min(0.1F).max(5.0F).step(0.1F).currentValue(1.0F);
   private final SliderSetting scale = new SliderSetting(this, "modules.settings.sky_shader.scale", "modules.settings.sky_shader.scale.description")
           .min(1.0F).max(20.0F).step(0.5F).currentValue(5.0F);
   private final SliderSetting intensity = new SliderSetting(this, "modules.settings.sky_shader.intensity", "modules.settings.sky_shader.intensity.description")
           .min(0.001F).max(0.05F).step(0.001F).currentValue(0.03F);
   private final SliderSetting alpha = new SliderSetting(this, "modules.settings.sky_shader.alpha", "modules.settings.sky_shader.alpha.description")
           .min(0.3F).max(1.0F).step(0.05F).currentValue(1.0F);
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.sky_shader.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.sky_shader.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.sky_shader.color_mode.custom");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.sky_shader.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(88, 184, 255, 255));
   private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.sky_shader.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(151, 71, 255, 255));

   private long startMillis = -1L;

   public SkyShader() {
      WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
         if (this.isEnabled()) {
            this.renderSkyShader();
         }
      });
   }

   @Override
   public void onDisable() {
      this.startMillis = -1L;
   }

   private void renderSkyShader() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      ensurePipeline();
      RenderPipeline pipeline = this.caustic.isSelected() ? causticPipeline : waterPipeline;
      if (pipeline == null || uniformBuffer == null) {
         return;
      }

      if (this.startMillis < 0L) {
         this.startMillis = System.currentTimeMillis();
      }

      ByteBuffer buffer = createUniformData();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.writeToBuffer(uniformBuffer.slice(), buffer);

      MinecraftClient client = MinecraftClient.getInstance();
      Framebuffer framebuffer = client.getFramebuffer();
      if (framebuffer == null) {
         return;
      }

      try (RenderPass pass = encoder.createRenderPass(
              () -> "fevervisual:sky_shader",
              framebuffer.getColorAttachmentView(),
              OptionalInt.empty(),
              framebuffer.getDepthAttachmentView(),
              OptionalDouble.empty())) {
         pass.setPipeline(pipeline);
         pass.setUniform("SkyData", uniformBuffer);
         pass.draw(0, 6);
      }
   }

   private ByteBuffer createUniformData() {
      float time = (System.currentTimeMillis() - this.startMillis) / 1000.0F;
      float framebufferWidth = mc.getWindow().getFramebufferWidth();
      float framebufferHeight = mc.getWindow().getFramebufferHeight();
      ColorRGBA shaderColor = this.getShaderColor();
      net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();
      float yawRad = (float)Math.toRadians(-camera.getYaw());
      float pitchRad = (float)Math.toRadians(camera.getPitch());
      float fov = mc.options.getFov().getValue().floatValue();

      ByteBuffer buffer = uniformDataBuffer;
      buffer.clear();
      buffer.putFloat(framebufferWidth).putFloat(framebufferHeight).putFloat(time).putFloat(fov);
      buffer.putFloat(shaderColor.getRed() / 255.0F).putFloat(shaderColor.getGreen() / 255.0F).putFloat(shaderColor.getBlue() / 255.0F).putFloat(this.alpha.getCurrentValue());
      buffer.putFloat(this.speed.getCurrentValue()).putFloat(this.scale.getCurrentValue()).putFloat(this.intensity.getCurrentValue()).putFloat(0.0F);
      buffer.putFloat(yawRad).putFloat(pitchRad).putFloat(0.0F).putFloat(0.0F);
      buffer.flip();
      return buffer;
   }

   private ColorRGBA getShaderColor() {
      float index = (System.currentTimeMillis() % 4200L) / 4200.0F * 360.0F;
      if (this.colorCustom.isSelected()) {
         float normalized = (index % 360.0F) / 180.0F;
         float mix = normalized > 1.0F ? 2.0F - normalized : normalized;
         return this.color.getColorSafe().mix(this.colorSecond.getColorSafe(), mix);
      }

      return Colors.getAccentColor(index);
   }

   private static void ensurePipeline() {
      if (waterPipeline != null && causticPipeline != null && uniformBuffer != null && uniformDataBuffer != null) {
         return;
      }

      if (waterPipeline == null) {
         waterPipeline = createPipeline("water", FeverVisual.id("core/post/sky/water"));
      }
      if (causticPipeline == null) {
         causticPipeline = createPipeline("caustic", FeverVisual.id("core/post/sky/caustic"));
      }
      if (uniformBuffer == null) {
         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "fevervisual:sky_shader_uniform", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, SKY_UNIFORM_SIZE);
      }
      if (uniformDataBuffer == null) {
         uniformDataBuffer = MemoryUtil.memAlloc(SKY_UNIFORM_SIZE);
      }
   }

   private static RenderPipeline createPipeline(String name, net.minecraft.util.Identifier fragmentShader) {
      return RenderPipelines.register(
              RenderPipeline.builder()
                      .withLocation(FeverVisual.id("pipeline/sky_shader_" + name))
                      .withVertexShader(FeverVisual.id("core/post/sky/sky"))
                      .withFragmentShader(fragmentShader)
                      .withVertexFormat(VertexFormat.builder().build(), VertexFormat.DrawMode.TRIANGLES)
                      .withUniform("SkyData", UniformType.UNIFORM_BUFFER)
                      .withBlend(BlendFunction.TRANSLUCENT)
                      .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
                      .withDepthWrite(false)
                      .withCull(false)
                      .build()
      );
   }
}
