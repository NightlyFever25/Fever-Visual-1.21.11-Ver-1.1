package fever.visual.utility.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.interfaces.IWindow;
import net.minecraft.client.gl.Framebuffer;

public class CustomRenderTarget extends Framebuffer implements IMinecraft, IWindow {
   private boolean linear;
   private float downscale = 1.0F;
   private GpuTextureView previousColorTarget;
   private GpuTextureView previousDepthTarget;

   public CustomRenderTarget(boolean useDepth) {
      super("Fever Visual render target", useDepth);
   }

   public CustomRenderTarget(int width, int height, boolean useDepth) {
      super("Fever Visual render target", useDepth);
      this.resize(width, height);
   }

   public CustomRenderTarget setLinear() {
      this.linear = true;
      return this;
   }

   public CustomRenderTarget setDownscale(float factor) {
      this.downscale = Math.max(0.1F, Math.min(1.0F, factor));
      return this;
   }

   private void resizeFramebuffer() {
      if (this.needsNewFramebuffer()) {
         int targetWidth = Math.max((int)Math.floor(mw.getFramebufferWidth() * this.downscale), 1);
         int targetHeight = Math.max((int)Math.floor(mw.getFramebufferHeight() * this.downscale), 1);
         this.resize(targetWidth, targetHeight);
      }
   }

   public void prepare() {
      this.resizeFramebuffer();
   }

   public void setup(boolean clear) {
      this.resizeFramebuffer();
      if (clear) {
         if (this.useDepthAttachment) {
            com.mojang.blaze3d.systems.RenderSystem.getDevice()
               .createCommandEncoder()
               .clearColorAndDepthTextures(this.getColorAttachment(), 0, this.getDepthAttachment(), 1.0);
         } else {
            com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.getColorAttachment(), 0);
         }
      }

      this.previousColorTarget = com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride;
      this.previousDepthTarget = com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride;
      com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride = this.getColorAttachmentView();
      com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride = this.getDepthAttachmentView();
   }

   public void setup() {
      this.setup(true);
   }

   public void stop() {
      com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride = this.previousColorTarget;
      com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride = this.previousDepthTarget;
      this.previousColorTarget = null;
      this.previousDepthTarget = null;
   }

   private boolean needsNewFramebuffer() {
      int targetWidth = Math.max((int)Math.floor(mw.getFramebufferWidth() * this.downscale), 1);
      int targetHeight = Math.max((int)Math.floor(mw.getFramebufferHeight() * this.downscale), 1);
      return this.textureWidth != targetWidth || this.textureHeight != targetHeight;
   }
}
