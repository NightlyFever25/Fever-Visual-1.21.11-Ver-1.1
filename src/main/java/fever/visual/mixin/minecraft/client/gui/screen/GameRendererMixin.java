package fever.visual.mixin.minecraft.client.gui.screen;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.AspectRatio;
import fever.visual.systems.modules.modules.visuals.NoRender;
import fever.visual.systems.modules.modules.visuals.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
   @Shadow
   public abstract float getFarPlaneDistance();
   @Shadow
   @Final
   private MinecraftClient client;

   @Unique
   private Matrix4f cachedPerspectiveMatrix;

   @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
   private void tiltViewWhenHurtHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      NoRender NoRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
      if (NoRender != null && NoRender.isEnabled() && NoRender.getHurtCam().isSelected()) {
         ci.cancel();
      }
   }

   @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
   private float renderWorldHook(float delta, float first, float second) {
      NoRender NoRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
      return NoRender != null && NoRender.isEnabled() && NoRender.getNausea().isSelected() ? 0.0F : MathHelper.lerp(delta, first, second);
   }

   @Inject(
           method = {"getBasicProjectionMatrix(F)Lorg/joml/Matrix4f;"},
           at = {@At("TAIL")},
           cancellable = true
   )
   private void onGetBasicProjectionMatrix(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
      if (this.cachedPerspectiveMatrix == null) {
         this.cachedPerspectiveMatrix = new Matrix4f();
      }

      Zoom zoomModule = FeverVisual.getInstance().getModuleManager().getModule(Zoom.class);
      AspectRatio aspectRatio = FeverVisual.getInstance().getModuleManager().getModule(AspectRatio.class);

      if (zoomModule != null && zoomModule.isEnabled()) {
         zoomModule.updateZoom();
         fovDegrees /= zoomModule.getZoomMultiplier();
      }

      if (aspectRatio != null && aspectRatio.isEnabled() && !AspectRatio.isRenderingHands()) {
         float ratio = aspectRatio.getRatio();
         this.cachedPerspectiveMatrix.setPerspective(
                 (float) (fovDegrees * (Math.PI / 180.0)),
                 ratio,
                 0.05F,
                 this.getFarPlaneDistance()
         );
         cir.setReturnValue(new Matrix4f(this.cachedPerspectiveMatrix));
      } else if (zoomModule != null && zoomModule.isEnabled()) {
         this.cachedPerspectiveMatrix.setPerspective(
                 (float) (fovDegrees * (Math.PI / 180.0)),
                 (float) this.client.getWindow().getFramebufferWidth() / this.client.getWindow().getFramebufferHeight(),
                 0.05F,
                 this.getFarPlaneDistance()
         );
         cir.setReturnValue(new Matrix4f(this.cachedPerspectiveMatrix));
      }
   }
}
