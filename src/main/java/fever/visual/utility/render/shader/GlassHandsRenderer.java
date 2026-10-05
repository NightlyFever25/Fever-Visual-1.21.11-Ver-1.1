package fever.visual.utility.render.shader;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import fever.visual.utility.render.pipeline.GlassCompositePipeline;
import fever.visual.utility.render.pipeline.KawaseBlurPipeline;
import fever.visual.utility.render.pipeline.MaskDiffPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

public class GlassHandsRenderer {
   private static GlassHandsRenderer instance;

   private final MinecraftClient client = MinecraftClient.getInstance();
   private KawaseBlurPipeline kawaseBlur;
   private GlassCompositePipeline glassComposite;
   private MaskDiffPipeline maskDiff;
   private GpuTexture sceneBeforeTexture;
   private GpuTextureView sceneBeforeTextureView;
   private GpuTexture sceneAfterTexture;
   private GpuTextureView sceneAfterTextureView;
   private GpuTexture depthBeforeTexture;
   private GpuTextureView depthBeforeTextureView;
   private GpuTexture depthAfterTexture;
   private GpuTextureView depthAfterTextureView;
   private GpuTexture maskTexture;
   private GpuTextureView maskTextureView;
   private int lastWidth;
   private int lastHeight;
   private boolean capturing;
   private boolean enabled;
   private boolean initialized;
   private float blurRadius = 2.5F;
   private int blurIterations = 3;
   private float saturation = 0.0F;
   private boolean reflect = true;
   private int tintColor = 0x00000000;
   private float tintIntensity = 0.0F;
   private float edgeGlowIntensity = 0.2F;

   public static GlassHandsRenderer getInstance() {
      if (instance == null) {
         instance = new GlassHandsRenderer();
      }
      return instance;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      if (enabled) {
         ensureInitialized();
      } else {
         this.capturing = false;
      }
   }

   public void setBlurRadius(float radius) {
      this.blurRadius = radius;
   }

   public void setBlurIterations(int iterations) {
      this.blurIterations = Math.max(1, Math.min(8, iterations));
   }

   public void setSaturation(float saturation) {
      this.saturation = saturation;
   }

   public void setReflect(boolean reflect) {
      this.reflect = reflect;
   }

   public void setTintColor(int tintColor) {
      this.tintColor = tintColor;
   }

   public void setTintIntensity(float tintIntensity) {
      this.tintIntensity = tintIntensity;
   }

   public void setEdgeGlowIntensity(float edgeGlowIntensity) {
      this.edgeGlowIntensity = edgeGlowIntensity;
   }

   public void captureSceneBeforeHands() {
      if (!this.enabled || !canCapture()) {
         return;
      }

      ensureInitialized();
      Framebuffer framebuffer = this.client.getFramebuffer();
      if (framebuffer == null || framebuffer.getColorAttachment() == null) {
         return;
      }

      int width = framebuffer.textureWidth;
      int height = framebuffer.textureHeight;
      ensureTextures(width, height);
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.copyTextureToTexture(framebuffer.getColorAttachment(), this.sceneBeforeTexture, 0, 0, 0, 0, 0, width, height);
      if (framebuffer.getDepthAttachment() != null) {
         encoder.copyTextureToTexture(framebuffer.getDepthAttachment(), this.depthBeforeTexture, 0, 0, 0, 0, 0, width, height);
      }
      this.capturing = true;
   }

   public void captureSceneAfterHands() {
      if (!this.enabled || !this.capturing || !canCapture()) {
         return;
      }

      Framebuffer framebuffer = this.client.getFramebuffer();
      if (framebuffer == null || framebuffer.getColorAttachment() == null) {
         this.capturing = false;
         return;
      }

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.copyTextureToTexture(framebuffer.getColorAttachment(), this.sceneAfterTexture, 0, 0, 0, 0, 0, this.lastWidth, this.lastHeight);
      if (framebuffer.getDepthAttachment() != null) {
         encoder.copyTextureToTexture(framebuffer.getDepthAttachment(), this.depthAfterTexture, 0, 0, 0, 0, 0, this.lastWidth, this.lastHeight);
      }
   }

   public void renderGlassEffect() {
      if (!this.enabled || !this.capturing || this.maskDiff == null || this.kawaseBlur == null || this.glassComposite == null) {
         this.capturing = false;
         return;
      }

      Framebuffer framebuffer = this.client.getFramebuffer();
      if (framebuffer == null || framebuffer.getColorAttachment() == null) {
         this.capturing = false;
         return;
      }

      this.maskDiff.createMask(this.maskTextureView, this.sceneBeforeTextureView, this.sceneAfterTextureView, this.depthBeforeTextureView, this.depthAfterTextureView, this.lastWidth, this.lastHeight);
      GpuTextureView blurredView = this.kawaseBlur.blur(this.sceneBeforeTexture, this.sceneBeforeTextureView, this.lastWidth, this.lastHeight, this.blurIterations, this.blurRadius);
      if (blurredView != null) {
         this.glassComposite.composite(framebuffer.getColorAttachmentView(), this.sceneBeforeTextureView, blurredView, this.maskTextureView, this.lastWidth, this.lastHeight, this.saturation, this.reflect, this.tintColor, this.tintIntensity, this.edgeGlowIntensity);
      }
      this.capturing = false;
   }

   public void invalidate() {
      cleanupTextures();
      if (this.kawaseBlur != null) {
         this.kawaseBlur.close();
         this.kawaseBlur = null;
      }
      if (this.glassComposite != null) {
         this.glassComposite.close();
         this.glassComposite = null;
      }
      if (this.maskDiff != null) {
         this.maskDiff.close();
         this.maskDiff = null;
      }
      this.lastWidth = 0;
      this.lastHeight = 0;
      this.initialized = false;
      this.capturing = false;
   }

   private boolean canCapture() {
      return this.client.player != null && this.client.world != null;
   }

   private void ensureInitialized() {
      if (this.initialized) {
         return;
      }
      if (this.kawaseBlur != null) {
         this.kawaseBlur.close();
      }
      if (this.glassComposite != null) {
         this.glassComposite.close();
      }
      if (this.maskDiff != null) {
         this.maskDiff.close();
      }
      this.kawaseBlur = new KawaseBlurPipeline();
      this.glassComposite = new GlassCompositePipeline();
      this.maskDiff = new MaskDiffPipeline();
      this.lastWidth = 0;
      this.lastHeight = 0;
      this.initialized = true;
   }

   private void ensureTextures(int width, int height) {
      if (width == this.lastWidth && height == this.lastHeight && this.sceneBeforeTexture != null) {
         return;
      }

      cleanupTextures();
      this.sceneBeforeTexture = createColorTexture("glass_scene_before", width, height);
      this.sceneBeforeTextureView = RenderSystem.getDevice().createTextureView(this.sceneBeforeTexture);
      this.sceneAfterTexture = createColorTexture("glass_scene_after", width, height);
      this.sceneAfterTextureView = RenderSystem.getDevice().createTextureView(this.sceneAfterTexture);
      this.depthBeforeTexture = createDepthTexture("glass_depth_before", width, height);
      this.depthBeforeTextureView = RenderSystem.getDevice().createTextureView(this.depthBeforeTexture);
      this.depthAfterTexture = createDepthTexture("glass_depth_after", width, height);
      this.depthAfterTextureView = RenderSystem.getDevice().createTextureView(this.depthAfterTexture);
      this.maskTexture = createColorTexture("glass_mask", width, height);
      this.maskTextureView = RenderSystem.getDevice().createTextureView(this.maskTexture);
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
      close(this.sceneBeforeTextureView);
      close(this.sceneBeforeTexture);
      close(this.sceneAfterTextureView);
      close(this.sceneAfterTexture);
      close(this.depthBeforeTextureView);
      close(this.depthBeforeTexture);
      close(this.depthAfterTextureView);
      close(this.depthAfterTexture);
      close(this.maskTextureView);
      close(this.maskTexture);
      this.sceneBeforeTextureView = null;
      this.sceneBeforeTexture = null;
      this.sceneAfterTextureView = null;
      this.sceneAfterTexture = null;
      this.depthBeforeTextureView = null;
      this.depthBeforeTexture = null;
      this.depthAfterTextureView = null;
      this.depthAfterTexture = null;
      this.maskTextureView = null;
      this.maskTexture = null;
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
