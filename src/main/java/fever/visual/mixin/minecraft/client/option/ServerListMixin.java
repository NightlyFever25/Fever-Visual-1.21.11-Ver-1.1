package fever.visual.mixin.minecraft.client.option;

import fever.visual.utility.game.server.SponsorServerUtility;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public class ServerListMixin {
   @Inject(method = "loadFile", at = @At("RETURN"))
   private void feverVisual$ensureSponsorAfterLoad(CallbackInfo ci) {
      ServerList serverList = (ServerList)(Object)this;
      if (SponsorServerUtility.ensureFirst(serverList)) {
         serverList.saveFile();
      }
   }

   @Inject(method = "saveFile", at = @At("HEAD"))
   private void feverVisual$ensureSponsorBeforeSave(CallbackInfo ci) {
      SponsorServerUtility.ensureFirst((ServerList)(Object)this);
   }

   @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
   private void feverVisual$cancelSponsorRemove(ServerInfo serverInfo, CallbackInfo ci) {
      if (SponsorServerUtility.isSponsor(serverInfo)) {
         SponsorServerUtility.ensureFirst((ServerList)(Object)this);
         ci.cancel();
      }
   }
}
