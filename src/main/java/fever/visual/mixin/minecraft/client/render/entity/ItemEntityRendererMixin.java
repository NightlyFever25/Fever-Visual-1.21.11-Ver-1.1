package fever.visual.mixin.minecraft.client.render.entity;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.ItemPhysics;
import fever.visual.utility.mixins.ItemEntityRenderStateAddition;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
   @Shadow
   @Final
   private Random random;

   @Inject(method = "updateRenderState", at = @At("TAIL"))
   private void fevervisual$captureEntity(ItemEntity entity, ItemEntityRenderState state, float tickProgress, CallbackInfo ci) {
      ((ItemEntityRenderStateAddition)state).fevervisual$setOnGround(entity.isOnGround());
   }

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void fevervisual$renderPhysics(
      ItemEntityRenderState state,
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      CameraRenderState cameraState,
      CallbackInfo ci
   ) {
      ItemPhysics module = FeverVisual.getInstance().getModuleManager().getModuleSafe(ItemPhysics.class);
      if (module == null || !module.isEnabled() || state.itemRenderState.isEmpty()) {
         return;
      }

      matrices.push();
      Box bounds = state.itemRenderState.getModelBoundingBox();
      matrices.translate(0.0F, -((float)bounds.minY) + 0.0625F, 0.0F);
      float rotation = ItemEntity.getRotation(state.age, state.uniqueOffset);
      if (((ItemEntityRenderStateAddition)state).fevervisual$isOnGround()) {
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
      } else {
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation * 300.0F));
      }

      ItemEntityRenderer.render(matrices, queue, state.light, state, this.random, bounds);
      matrices.pop();
      ci.cancel();
   }
}
