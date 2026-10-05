package fever.visual.utility.render.shader;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import fever.visual.systems.modules.modules.visuals.CustomFog;
import fever.visual.utility.render.pipeline.FogCompatCompositePipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

public final class CustomFogCompatRenderer {
   private static final CustomFogCompatRenderer INSTANCE = new CustomFogCompatRenderer();

   private final MinecraftClient client = MinecraftClient.getInstance();
   private final FogCompatCompositePipeline pipeline = new FogCompatCompositePipeline();
   private GpuTexture sceneTexture;
   private GpuTextureView sceneTextureView;
   private GpuTexture depthTexture;
   private GpuTextureView depthTextureView;
   private int lastWidth;
   private int lastHeight;

   private CustomFogCompatRenderer() {
   }

   public static CustomFogCompatRenderer getInstance() {
      return INSTANCE;
   }

   public void render(CustomFog customFog, float farPlaneDistance) {
      if (customFog == null
         || this.client.world == null
         || this.client.player == null
         || !customFog.shouldUseCompatWorldFog(this.client.gameRenderer.getCamera())) {
         return;
      }

      Framebuffer framebuffer = this.client.getFramebuffer();
      if (framebuffer == null || framebuffer.getColorAttachment() == null || framebuffer.getDepthAttachment() == null) {
         return;
      }

      int width = framebuffer.textureWidth;
      int height = framebuffer.textureHeight;
      if (width <= 0 || height <= 0) {
         return;
      }
      ensureTextures(width, height);

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.copyTextureToTexture(framebuffer.getColorAttachment(), this.sceneTexture, 0, 0, 0, 0, 0, width, height);
      encoder.copyTextureToTexture(framebuffer.getDepthAttachment(), this.depthTexture, 0, 0, 0, 0, 0, width, height);

      float farPlane = Math.max(1.0F, farPlaneDistance);
      this.pipeline.composite(
         framebuffer.getColorAttachmentView(),
         this.sceneTextureView,
         this.depthTextureView,
         width,
         height,
         0.05F,
         farPlane,
         customFog.getFogStart(farPlane),
         customFog.getFogEnd(farPlane),
         customFog.getColorBlend(),
         customFog.getFogColorValue()
      );
   }

   private void ensureTextures(int width, int height) {
      if (width == this.lastWidth && height == this.lastHeight && this.sceneTexture != null && this.depthTexture != null) {
         return;
      }

      cleanupTextures();
      this.sceneTexture = RenderSystem.getDevice().createTexture(
         () -> "fevervisual:custom_fog_compat_scene",
         GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
         TextureFormat.RGBA8,
         width,
         height,
         1,
         1
      );
      this.sceneTextureView = RenderSystem.getDevice().createTextureView(this.sceneTexture);
      this.depthTexture = RenderSystem.getDevice().createTexture(
         () -> "fevervisual:custom_fog_compat_depth",
         GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
         TextureFormat.DEPTH32,
         width,
         height,
         1,
         1
      );
      this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
      this.lastWidth = width;
      this.lastHeight = height;
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
