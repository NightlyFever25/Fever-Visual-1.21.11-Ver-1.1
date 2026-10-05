package fever.visual.mixin.minecraft.client.network;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fever.visual.FeverVisual;
import fever.visual.systems.event.impl.game.CloseScreenEvent;
import fever.visual.systems.event.impl.player.ClientPlayerTickEndEvent;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.event.impl.player.SlowDownEvent;
import fever.visual.systems.modules.modules.player.Freelook;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.mixins.ClientPlayerEntityAddition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin implements ClientPlayerEntityAddition, IMinecraft {
   @Unique
   private int groundTicks = 0;

   @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
   private boolean onIsUsingItemRedirect(ClientPlayerEntity player) {
      SlowDownEvent slowDownEvent = new SlowDownEvent();
      FeverVisual.getInstance().getEventManager().triggerEvent(slowDownEvent);
      return player.isUsingItem() && player.getVehicle() == null && !slowDownEvent.isCancelled();
   }


   @WrapWithCondition(
      method = "closeScreen",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V")
   )
   private boolean preventCloseScreen(MinecraftClient instance, Screen screen) {
      FeverVisual.getInstance().getEventManager().triggerEvent(new CloseScreenEvent(screen));
      return true;
   }
   @Inject(
           method = {"tick()V"},
           at = {@At("HEAD")}
   )
   public void triggerTickEvent(CallbackInfo ci) {
      FeverVisual.getInstance().getEventManager().triggerEvent(new ClientPlayerTickEvent());
      Freelook freelook = FeverVisual.getInstance().getModuleManager().getModule(Freelook.class);
      if (freelook != null && freelook.isEnabled() && mc.player != null) {
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   public void triggerTickEndEvent(CallbackInfo ci) {
      FeverVisual.getInstance().getEventManager().triggerEvent(new ClientPlayerTickEndEvent());
   }

   @Inject(method = "tickMovement", at = @At("HEAD"))
   public void updateOnGroundTicks(CallbackInfo ci) {
      if (mc.player != null && mc.player.isOnGround()) {
         this.groundTicks++;
      } else {
         this.groundTicks = 0;
      }
   }

   @Override
   public int fevervisual$getOnGroundTicks() {
      return this.groundTicks;
   }
}
