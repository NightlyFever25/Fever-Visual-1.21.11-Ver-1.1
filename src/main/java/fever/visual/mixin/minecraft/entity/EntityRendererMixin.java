package fever.visual.mixin.minecraft.entity;

import fever.visual.systems.modules.modules.other.Optimization;
import fever.visual.systems.modules.modules.visuals.NameTags;
import fever.visual.systems.event.RenderedEntityTracker;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
   @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
   private void onRenderLabel(T entity, CallbackInfoReturnable<Text> cir) {
      if (NameTags.shouldHideVanilla(entity)) {
         cir.setReturnValue(null);
      }
   }

   @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
   private void fevervisual$skipOptimizedEntities(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      Optimization optimization = Optimization.getInstanceSafe();
      if (optimization != null && optimization.shouldSkipEntity(entity, x, y, z)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "shouldRender", at = @At("RETURN"))
   private void fevervisual$trackRenderedEntity(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      if (cir.getReturnValueZ()) {
         RenderedEntityTracker.mark(entity);
      }
   }
}
