package fever.visual.mixin.minecraft.client.gui.overlay;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.player.Freelook;
import fever.visual.systems.modules.modules.visuals.ExplosionWave;
import fever.visual.systems.modules.modules.visuals.NoRender;
import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Inject(
           method = {"getSubmersionType()Lnet/minecraft/block/enums/CameraSubmersionType;"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void getSubmergedFluidState(CallbackInfoReturnable<CameraSubmersionType> ci) {
      NoRender NoRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
      if (NoRender.isEnabled() && NoRender.getWater().isSelected()) {
         ci.setReturnValue(CameraSubmersionType.NONE);
      }
   }

   @ModifyArgs(
           method = {"update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
           )
   )
   private void modifyRotation(Args args) {
      float yaw = (Float)args.get(0);
      float pitch = (Float)args.get(1);
      if (Freelook.isActive) {
         yaw = Freelook.x;
         pitch = Freelook.y;
      }
      args.set(0, yaw + ExplosionWave.getShakeYaw());
      args.set(1, pitch + ExplosionWave.getShakePitch());
   }
}
