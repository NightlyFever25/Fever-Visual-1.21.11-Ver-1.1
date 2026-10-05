package fever.visual.mixin.minecraft.entity;

import fever.visual.systems.modules.modules.player.Freelook;
import fever.visual.utility.game.PlatformUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
   @Inject(method = "changeLookDirection(DD)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$preventLunarFreelookBodyRotation(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      if (PlatformUtility.isLunarClient() && Freelook.isActive && (Object)this == MinecraftClient.getInstance().player) {
         ci.cancel();
      }
   }
}
