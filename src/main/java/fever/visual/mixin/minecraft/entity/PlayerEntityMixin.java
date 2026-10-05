package fever.visual.mixin.minecraft.entity;

import fever.visual.FeverVisual;
import fever.visual.systems.event.impl.game.AttackEvent;
import fever.visual.systems.event.impl.game.PostAttackEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void attackAHook2(Entity target, CallbackInfo ci) {
      AttackEvent event = new AttackEvent(target);
      FeverVisual.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "attack", at = @At("RETURN"), cancellable = true)
   private void attackAHook(Entity target, CallbackInfo ci) {
      PostAttackEvent event = new PostAttackEvent(target);
      FeverVisual.getInstance().getEventManager().triggerEvent(event);
   }

}
