package fever.visual.mixin.minecraft.client;

import com.holdmylua.source.access.LivingEntityAccessor;
import fever.visual.FeverVisual;
import fever.visual.minecraft.MinecraftClientMixinProtection;
import fever.visual.systems.event.impl.game.GameTickEvent;
import fever.visual.utility.render.penis.PenisAtlas;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import com.hmi.HandMyItemsRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
   @Shadow
   private int itemUseCooldown;
   @Shadow
   public ClientPlayerEntity player;

   @Inject(method = "tick", at = @At("HEAD"))
   public void tick(CallbackInfo ci) {
      FeverVisual.getInstance().getEventManager().triggerEvent(new GameTickEvent());
   }

   @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;onResolutionChanged()V"))
   public void initializeClient(RunArgs args, CallbackInfo ci) {
      MinecraftClientMixinProtection.init();
   }

   @Inject(method = "<init>", at = @At("RETURN"))
   public void endInitialize(RunArgs args, CallbackInfo ci) {
      PenisAtlas atlas = PenisAtlas.getOrCreateAtlasFor(16, 16);
      atlas.registerAnimationFromPenisFile(FeverVisual.id("penises/combat.penis"));
      atlas.registerAnimationFromPenisFile(FeverVisual.id("penises/movement.penis"));
      atlas.registerAnimationFromPenisFile(FeverVisual.id("penises/visuals.penis"));
      atlas.registerAnimationFromPenisFile(FeverVisual.id("penises/player.penis"));
      atlas.registerAnimationFromPenisFile(FeverVisual.id("penises/other.penis"));
      atlas.registerAnimationFromPenisFile(FeverVisual.id("penises/search.penis"));
      atlas.buildAtlas();
      PenisAtlas atlas12 = PenisAtlas.getOrCreateAtlasFor(12, 12);
      atlas12.registerAnimationFromPenisFile(FeverVisual.id("penises/check_enable.penis"));
      atlas12.registerAnimationFromPenisFile(FeverVisual.id("penises/check_disable.penis"));
      atlas12.buildAtlas();
   }

   @Inject(method = "stop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;close()V", shift = Shift.AFTER))
   public void shutdownClient(CallbackInfo ci) {
      MinecraftClientMixinProtection.shutdown();
   }

   @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
   public void changeWindowTitle(CallbackInfoReturnable<String> cir) {
      MinecraftClientMixinProtection.updateTitle(cir);
   }

   @Inject(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V"))
   private void feverVisual$hmiDoAttack(CallbackInfoReturnable<Boolean> cir) {
      if (HandMyItemsRuntime.isActive() && this.player instanceof LivingEntityAccessor accessor) {
         accessor.hMI5_0$resetMainHandSwing(false);
      }
   }

   @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V"))
   private void feverVisual$hmiDoItemUse(ClientPlayerEntity instance, Hand hand) {
      if (HandMyItemsRuntime.isActive() && instance instanceof LivingEntityAccessor accessor) {
         if (hand == Hand.MAIN_HAND) {
            accessor.hMI5_0$resetMainHandSwing(true);
         } else {
            accessor.hMI5_0$resetOffHandSwing(true);
         }
      }
      instance.swingHand(hand);
   }
}
