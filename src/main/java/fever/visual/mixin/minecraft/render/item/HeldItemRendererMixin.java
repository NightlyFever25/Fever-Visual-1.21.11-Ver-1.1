package fever.visual.mixin.minecraft.render.item;

import fever.visual.FeverVisual;
import fever.visual.systems.event.impl.render.GlassHandsRenderEvent;
import fever.visual.systems.event.impl.render.HandRenderEvent;
import fever.visual.systems.modules.modules.visuals.AspectRatio;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
   @Unique
   private boolean feverVisual$handMatrixPushed;
   @Unique
   private boolean feverVisual$handPassCaptureActive;

   @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
   private void onRenderItemPre(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, ClientPlayerEntity player, int light, CallbackInfo ci) {
      FeverVisual.getInstance().getEventManager().triggerEvent(new GlassHandsRenderEvent(GlassHandsRenderEvent.Phase.PRE, matrices, tickProgress));
   }

   @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("TAIL"))
   private void onRenderItemPost(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, ClientPlayerEntity player, int light, CallbackInfo ci) {
      FeverVisual.getInstance().getEventManager().triggerEvent(new GlassHandsRenderEvent(GlassHandsRenderEvent.Phase.POST, matrices, tickProgress));
   }

   @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
   private void onRenderFirstPersonItem(
           AbstractClientPlayerEntity player,
           float tickDelta,
           float pitch,
           Hand hand,
           float swingProgress,
           ItemStack item,
           float equipProgress,
           MatrixStack matrices,
           OrderedRenderCommandQueue queue,
           int light,
      CallbackInfo ci) {
      AspectRatio.setRenderingHands(true);
      matrices.push();
      this.feverVisual$handMatrixPushed = true;
      boolean isMainHand = hand == Hand.MAIN_HAND;
      Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      boolean isRightArm = arm == Arm.RIGHT;
      HandRenderEvent event = new HandRenderEvent(arm, swingProgress, item, equipProgress, matrices);
      FeverVisual.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
         if (!event.isFullTransform()) {
            float f = -0.4F * MathHelper.sin(MathHelper.sqrt(0.0F) * (float) Math.PI);
            float g = 0.2F * MathHelper.sin(MathHelper.sqrt(0.0F) * (float) (Math.PI * 2));
            float h = -0.2F * MathHelper.sin(0.0F);
            matrices.translate((arm == Arm.RIGHT ? 1 : -1) * f, g, h);
            int i = arm == Arm.RIGHT ? 1 : -1;
            matrices.translate(i * 0.56F, -0.52F, -0.72F);
         }
         if (!item.isEmpty()) {
            HeldItemRenderer rendererInstance = (HeldItemRenderer) (Object) this;
            rendererInstance.renderItem(
                    player,
                    item,
                    isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                            : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    matrices,
                    queue,
                    light);
         }

         matrices.pop();
         this.feverVisual$handMatrixPushed = false;
         AspectRatio.setRenderingHands(false);
      }
   }

   @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
   private void onRenderFirstPersonItemEnd(
           AbstractClientPlayerEntity player,
           float tickDelta,
           float pitch,
           Hand hand,
           float swingProgress,
           ItemStack item,
           float equipProgress,
           MatrixStack matrices,
           OrderedRenderCommandQueue queue,
           int light,
           CallbackInfo ci) {
      if (this.feverVisual$handMatrixPushed) {
         matrices.pop();
         this.feverVisual$handMatrixPushed = false;
      }
      AspectRatio.setRenderingHands(false);
   }
}
