package fever.visual.utility.render.shader;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import fever.visual.utility.render.pipeline.HandShaderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.Perspective;

public class HandShaderRenderer {
   private static HandShaderRenderer instance;

   private final MinecraftClient client = MinecraftClient.getInstance();
   private HandShaderPipeline pipeline;
   private GpuTexture sceneBeforeTexture;
   private GpuTextureView sceneBeforeTextureView;
   private GpuTexture sceneAfterTexture;
   private GpuTextureView sceneAfterTextureView;
   private GpuTexture depthBeforeTexture;
   private GpuTextureView depthBeforeTextureView;
   private GpuTexture depthAfterTexture;
   private GpuTextureView depthAfterTextureView;
   private GpuTexture trailTextureA;
   private GpuTextureView trailTextureViewA;
   private GpuTexture trailTextureB;
   private GpuTextureView trailTextureViewB;
   private boolean trailAIsHistory = true;
   private int lastWidth;
   private int lastHeight;
   private boolean enabled;
   private boolean initialized;
   private boolean capturing;
   private float blurRadius = 12.0F;
   private float threshold = 0.01F;
   private float opacity = 0.6F;
   private int color = 0xFF46AAFF;
   private int mode;
   private float flameStrength = 0.85F;
   private float flameRiseSpeed = 0.8F;
   private float flameWobble = 0.65F;
   private float flameLength = 0.95F;
   private float flameBrightness = 0.9F;
   private long trailClockNs;
   private float trailAccumulator;

   public static HandShaderRenderer getInstance() {
      if (instance == null) {
         instance = new HandShaderRenderer();
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

   public void setBlurRadius(float blurRadius) {
      this.blurRadius = blurRadius;
   }

   public void setThreshold(float threshold) {
      this.threshold = threshold;
   }

   public void setOpacity(float opacity) {
      this.opacity = opacity;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public void setMode(int mode) {
      this.mode = mode;
   }

   public void setFlameSettings(float strength, float riseSpeed, float wobble, float length, float brightness) {
      this.flameStrength = strength;
      this.flameRiseSpeed = riseSpeed;
      this.flameWobble = wobble;
      this.flameLength = length;
      this.flameBrightness = brightness;
   }

   public void captureSceneBeforeHands() {
      if (!canCapture()) {
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
      if (!canCapture() || !this.capturing) {
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

   public void renderEffect() {
      if (!canCapture() || !this.capturing || this.pipeline == null) {
         this.capturing = false;
         return;
      }

      Framebuffer framebuffer = this.client.getFramebuffer();
      if (framebuffer == null || framebuffer.getColorAttachmentView() == null) {
         this.capturing = false;
         return;
      }

      float time = (System.currentTimeMillis() % 100000L) / 1000.0F;
      GpuTextureView history = this.trailAIsHistory ? this.trailTextureViewA : this.trailTextureViewB;
      GpuTextureView next = this.trailAIsHistory ? this.trailTextureViewB : this.trailTextureViewA;
      float trailStep = this.mode == 6 ? consumeTrailStep() : 0.0F;
      this.pipeline.render(framebuffer.getColorAttachmentView(), this.sceneAfterTextureView, this.sceneBeforeTextureView,
              this.depthAfterTextureView, this.depthBeforeTextureView, history, next,
              this.lastWidth, this.lastHeight, this.blurRadius, this.threshold, this.color, this.opacity, this.mode, time,
              this.flameStrength, this.flameRiseSpeed, this.flameWobble, this.flameLength, this.flameBrightness, trailStep);
      if (this.mode == 6 && trailStep > 0.0F) this.trailAIsHistory = !this.trailAIsHistory;
      this.capturing = false;
   }

   public void invalidate() {
      cleanupTextures();
      if (this.pipeline != null) {
         this.pipeline.close();
         this.pipeline = null;
      }
      this.lastWidth = 0;
      this.lastHeight = 0;
      this.initialized = false;
      this.capturing = false;
      this.trailClockNs = 0L;
      this.trailAccumulator = 0.0F;
   }

   private boolean canCapture() {
      return this.enabled && this.client.player != null && this.client.world != null && this.client.options.getPerspective() == Perspective.FIRST_PERSON;
   }

   private void ensureInitialized() {
      if (this.initialized) {
         return;
      }
      if (this.pipeline != null) {
         this.pipeline.close();
      }
      this.pipeline = new HandShaderPipeline();
      this.lastWidth = 0;
      this.lastHeight = 0;
      this.initialized = true;
   }

   private void ensureTextures(int width, int height) {
      if (width == this.lastWidth && height == this.lastHeight && this.sceneBeforeTexture != null
              && this.sceneAfterTexture != null && this.depthBeforeTexture != null && this.depthAfterTexture != null
              && this.trailTextureA != null && this.trailTextureB != null) {
         return;
      }

      cleanupTextures();
      this.sceneBeforeTexture = RenderSystem.getDevice().createTexture(() -> "fevervisual:hand_shader_scene_before", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, width, height, 1, 1);
      this.sceneBeforeTextureView = RenderSystem.getDevice().createTextureView(this.sceneBeforeTexture);
      this.sceneAfterTexture = RenderSystem.getDevice().createTexture(() -> "fevervisual:hand_shader_scene_after", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, width, height, 1, 1);
      this.sceneAfterTextureView = RenderSystem.getDevice().createTextureView(this.sceneAfterTexture);
      this.depthBeforeTexture = RenderSystem.getDevice().createTexture(() -> "fevervisual:hand_shader_depth_before", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32, width, height, 1, 1);
      this.depthBeforeTextureView = RenderSystem.getDevice().createTextureView(this.depthBeforeTexture);
      this.depthAfterTexture = RenderSystem.getDevice().createTexture(() -> "fevervisual:hand_shader_depth_after", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32, width, height, 1, 1);
      this.depthAfterTextureView = RenderSystem.getDevice().createTextureView(this.depthAfterTexture);
      int trailUsage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
      int trailWidth = Math.max(1, width / 3);
      int trailHeight = Math.max(1, height / 3);
      this.trailTextureA = RenderSystem.getDevice().createTexture(() -> "fevervisual:hand_fire_trail_a", trailUsage, TextureFormat.RGBA8, trailWidth, trailHeight, 1, 1);
      this.trailTextureViewA = RenderSystem.getDevice().createTextureView(this.trailTextureA);
      this.trailTextureB = RenderSystem.getDevice().createTexture(() -> "fevervisual:hand_fire_trail_b", trailUsage, TextureFormat.RGBA8, trailWidth, trailHeight, 1, 1);
      this.trailTextureViewB = RenderSystem.getDevice().createTextureView(this.trailTextureB);
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.clearColorTexture(this.trailTextureA, 0);
      encoder.clearColorTexture(this.trailTextureB, 0);
      this.trailAIsHistory = true;
      this.trailClockNs = 0L;
      this.trailAccumulator = 0.0F;
      this.lastWidth = width;
      this.lastHeight = height;
   }

   private float consumeTrailStep() {
      long now = System.nanoTime();
      if (this.trailClockNs != 0L) {
         this.trailAccumulator += Math.min(0.1F, (now - this.trailClockNs) / 1_000_000_000.0F);
      }
      this.trailClockNs = now;
      if (this.trailAccumulator < 1.0F / 120.0F) {
         return 0.0F;
      }
      float step = Math.min(this.trailAccumulator, 0.05F);
      this.trailAccumulator = 0.0F;
      return step;
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
      close(this.trailTextureViewA);
      close(this.trailTextureA);
      close(this.trailTextureViewB);
      close(this.trailTextureB);
      this.sceneBeforeTextureView = null;
      this.sceneBeforeTexture = null;
      this.sceneAfterTextureView = null;
      this.sceneAfterTexture = null;
      this.depthBeforeTextureView = null;
      this.depthBeforeTexture = null;
      this.depthAfterTextureView = null;
      this.depthAfterTexture = null;
      this.trailTextureViewA = null;
      this.trailTextureA = null;
      this.trailTextureViewB = null;
      this.trailTextureB = null;
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
