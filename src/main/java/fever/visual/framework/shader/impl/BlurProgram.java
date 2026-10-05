package fever.visual.framework.shader.impl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.Generated;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.interfaces.IWindow;
import fever.visual.utility.render.CustomRenderTarget;
import fever.visual.utility.render.pipeline.HudBlurPipeline;
import fever.visual.utility.time.Timer;
import net.minecraft.client.gl.Framebuffer;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;


public class BlurProgram implements IMinecraft, IWindow {
   private static final Framebuffer MAIN_FBO = mc.getFramebuffer();
   public static final Supplier<CustomRenderTarget> CACHE = Suppliers.memoize(() -> new CustomRenderTarget(false).setLinear());
   public static final Supplier<CustomRenderTarget> BUFFER = Suppliers.memoize(() -> new CustomRenderTarget(false).setLinear());
   private final Timer timer = new Timer();
   private HudBlurPipeline pipeline;
   private float blurOffset = 1.0F;
   private float blurDownscale = 0.5F;


   public void initShaders() {
      if (this.pipeline == null) this.pipeline = new HudBlurPipeline();
   }

   public void draw() {
      this.draw(1.0F);
   }

   public void draw(float blurOffset) {
      if (this.timer.finished(25L)) {
         this.blurOffset = blurOffset;
         CustomRenderTarget cache = (CustomRenderTarget)CACHE.get();
         CustomRenderTarget buffer = (CustomRenderTarget)BUFFER.get();
         GpuTexture source = MAIN_FBO.getColorAttachment();
         cache.setDownscale(this.blurDownscale).setLinear();
         buffer.setDownscale(this.blurDownscale).setLinear();
         cache.prepare();
         buffer.prepare();
         this.pipeline.render(
            cache.getColorAttachmentView(), MAIN_FBO.getColorAttachmentView(),
            source.getWidth(0), source.getHeight(0), this.blurOffset, false
         );
         this.pipeline.render(
            buffer.getColorAttachmentView(), cache.getColorAttachmentView(),
            cache.textureWidth, cache.textureHeight, this.blurOffset, true
         );
         this.timer.reset();
      }
   }

   public static GpuTexture getTexture() {
      return ((CustomRenderTarget)BUFFER.get()).getColorAttachment();
   }

   public static GpuTextureView getTextureView() {
      return ((CustomRenderTarget)BUFFER.get()).getColorAttachmentView();
   }

   @Generated
   public void setBlurOffset(float blurOffset) {
      this.blurOffset = blurOffset;
   }

   @Generated
   public void setBlurDownscale(float blurDownscale) {
      this.blurDownscale = blurDownscale;
   }
}
