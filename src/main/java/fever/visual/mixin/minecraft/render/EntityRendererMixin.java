package fever.visual.mixin.minecraft.render;

import fever.visual.utility.mixins.EntityRenderStateAddition;
import fever.visual.systems.modules.modules.visuals.GlowEsp;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
   @Inject(method = "updateRenderState", at = @At("HEAD"))
   private void updateRenderingEntity(T entity, S state, float tickDelta, CallbackInfo ci) {
      ((EntityRenderStateAddition)state).fevervisual$setEntity(entity);
   }

   @Inject(method = "updateRenderState", at = @At("RETURN"))
   private void fevervisual$applyGlowEspColor(T entity, S state, float tickDelta, CallbackInfo ci) {
      Integer color = GlowEsp.outlineColorFor(entity);
      if (color != null) {
         state.outlineColor = color;
      }
   }
}
