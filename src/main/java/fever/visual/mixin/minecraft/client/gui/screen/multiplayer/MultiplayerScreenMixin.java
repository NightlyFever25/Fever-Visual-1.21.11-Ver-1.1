package fever.visual.mixin.minecraft.client.gui.screen.multiplayer;

import fever.visual.utility.game.server.SponsorServerUtility;
import net.minecraft.client.gui.screen.multiplayer.FeverVisualServerCategoryEntry;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {
   @Shadow
   protected MultiplayerServerListWidget serverListWidget;
   @Shadow
   private ButtonWidget buttonJoin;
   @Shadow
   private ButtonWidget buttonEdit;
   @Shadow
   private ButtonWidget buttonDelete;

   @Inject(method = "updateButtonActivationStates", at = @At("RETURN"))
   private void feverVisual$lockSponsorButtons(CallbackInfo ci) {
      MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
      if (entry instanceof FeverVisualServerCategoryEntry) {
         this.buttonJoin.active = false;
         this.buttonEdit.active = false;
         this.buttonDelete.active = false;
      } else if (entry instanceof MultiplayerServerListWidget.ServerEntry serverEntry && SponsorServerUtility.isSponsor(serverEntry.getServer())) {
         this.buttonEdit.active = false;
         this.buttonDelete.active = false;
      }
   }
}
