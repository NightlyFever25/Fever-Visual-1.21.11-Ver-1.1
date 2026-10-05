package fever.visual.mixin.minecraft.network;

import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin implements IMinecraft {
   @Inject(method = "onEntity", at = @At("TAIL"))
   public void onEntity(EntityS2CPacket packet, CallbackInfo ci) {
      ClientPlayNetworkHandler self = (ClientPlayNetworkHandler) (Object) this;
      ClientWorld world = self.getWorld();
   }
}
