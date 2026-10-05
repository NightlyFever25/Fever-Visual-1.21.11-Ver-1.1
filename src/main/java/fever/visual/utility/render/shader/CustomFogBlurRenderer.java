package fever.visual.utility.render.shader;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import fever.visual.systems.modules.modules.visuals.CustomFog;
import fever.visual.utility.render.pipeline.FogBlurCompositePipeline;
import fever.visual.utility.render.pipeline.KawaseBlurPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;

public class CustomFogBlurRenderer {
   private static CustomFogBlurRenderer instance;

   private final MinecraftClient client = MinecraftClient.getInstance();
   private KawaseBlurPipeline kawaseBlur;
   private FogBlurCompositePipeline compositePipeline;
   private GpuTexture sceneTexture;
   private GpuTextureView sceneTextureView;
   private GpuTexture depthTexture;
   private GpuTextureView depthTextureView;
   private int lastWidth;
   private int lastHeight;
   private boolean initialized;

   public static CustomFogBlurRenderer getInstance() {
      if (instance == null) {
         instance = new CustomFogBlurRenderer();
      }

      return instance;
   }

   public void render(CustomFog customFog, float farPlaneDistance) {
      if (customFog == null || this.client.world == null || this.client.player == null) {
         return;
      }

      Camera camera = this.client.gameRenderer.getCamera();
      if (!customFog.shouldApplyBlur(camera)) {
         return;
      }

      Framebuffer framebuffer = this.client.getFramebuffer();
      if (framebuffer == null || framebuffer.getColorAttachment() == null || framebuffer.getDepthAttachment() == null) {
         return;
      }

      ensureInitialized();
      int width = framebuffer.textureWidth;
      int height = framebuffer.textureHeight;
      ensureTextures(width, height);

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.copyTextureToTexture(framebuffer.getColorAttachment(), this.sceneTexture, 0, 0, 0, 0, 0, width, height);
      encoder.copyTextureToTexture(framebuffer.getDepthAttachment(), this.depthTexture, 0, 0, 0, 0, 0, width, height);

      int iterations = Math.max(1, Math.min(8, Math.round(customFog.getBlurRadiusValue())));
      GpuTextureView blurredView = this.kawaseBlur.blur(this.sceneTexture, this.sceneTextureView, width, height, iterations, customFog.getBlurRadiusValue());
      if (blurredView == null) {
         return;
      }

      float farPlane = Math.max(1.0F, farPlaneDistance);
      float fogStart = customFog.getFogStart(farPlane);
      float fogEnd = customFog.getFogEnd(farPlane);
      this.compositePipeline.composite(
         framebuffer.getColorAttachmentView(),
         this.sceneTextureView,
         blurredView,
         this.depthTextureView,
         width,
         height,
         0.05F,
         farPlane,
         fogStart,
         fogEnd,
         customFog.getBlurStrengthValue()
      );
   }

   public void invalidate() {
      cleanupTextures();
      if (this.kawaseBlur != null) {
         this.kawaseBlur.close();
         this.kawaseBlur = null;
      }
      if (this.compositePipeline != null) {
         this.compositePipeline.close();
         this.compositePipeline = null;
      }
      this.initialized = false;
      this.lastWidth = 0;
      this.lastHeight = 0;
   }

   private void ensureInitialized() {
      if (this.initialized) {
         return;
      }

      this.kawaseBlur = new KawaseBlurPipeline();
      this.compositePipeline = new FogBlurCompositePipeline();
      this.initialized = true;
   }

   private void ensureTextures(int width, int height) {
      if (width == this.lastWidth && height == this.lastHeight && this.sceneTexture != null && this.depthTexture != null) {
         return;
      }

      cleanupTextures();
      this.sceneTexture = createColorTexture("custom_fog_blur_scene", width, height);
      this.sceneTextureView = RenderSystem.getDevice().createTextureView(this.sceneTexture);
      this.depthTexture = createDepthTexture("custom_fog_blur_depth", width, height);
      this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
      this.lastWidth = width;
      this.lastHeight = height;
   }

   private static GpuTexture createColorTexture(String name, int width, int height) {
      return RenderSystem.getDevice().createTexture(() -> "fevervisual:" + name, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, width, height, 1, 1);
   }

   private static GpuTexture createDepthTexture(String name, int width, int height) {
      return RenderSystem.getDevice().createTexture(() -> "fevervisual:" + name, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32, width, height, 1, 1);
   }

   private void cleanupTextures() {
      close(this.sceneTextureView);
      close(this.sceneTexture);
      close(this.depthTextureView);
      close(this.depthTexture);
      this.sceneTextureView = null;
      this.sceneTexture = null;
      this.depthTextureView = null;
      this.depthTexture = null;
   }

   private static void close(AutoCloseable closeable) {
      if (closeable == null) {
         return;
      }

      try {
         closeable.close();
      } catch (Exception ignored) {
      }
   }
}
