package fever.visual.mixin.minecraft.client.gui.screen.multiplayer;

import fever.visual.mixin.accessors.MultiplayerScreenAccessor;
import fever.visual.utility.game.server.SponsorServerUtility;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.SquareWidgetEntry;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class MultiplayerServerEntryMixin implements SquareWidgetEntry {
   @Shadow
   @Final
   private MultiplayerScreen screen;
   @Shadow
   @Final
   private ServerInfo server;

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void feverVisual$fixMoveWithCategories(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      MultiplayerServerListWidget.ServerEntry self = (MultiplayerServerListWidget.ServerEntry)(Object)this;
      int x = (int)click.x() - self.getContentX();
      int y = (int)click.y() - self.getContentY();
      if (this.isBottomLeft(x, y, 32)) {
         cir.setReturnValue(this.feverVisual$move(-1));
      } else if (this.isTopLeft(x, y, 32)) {
         cir.setReturnValue(this.feverVisual$move(1));
      }
   }

   @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
   private void feverVisual$fixKeyboardMoveWithCategories(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
      if (input.hasShift() && (input.isUp() || input.isDown())) {
         cir.setReturnValue(this.feverVisual$move(input.isUp() ? -1 : 1));
      }
   }

   private boolean feverVisual$move(int direction) {
      ServerList serverList = this.screen.getServerList();
      SponsorServerUtility.ensureFirst(serverList);
      int index = SponsorServerUtility.indexOf(serverList, this.server);
      int target = index + direction;
      if (index < 0 || target < 0 || target >= serverList.size() || SponsorServerUtility.isSponsor(this.server) || target == 0) {
         return true;
      }

      serverList.swapEntries(index, target);
      SponsorServerUtility.ensureFirst(serverList);
      serverList.saveFile();
      ((MultiplayerScreenAccessor)this.screen).feverVisual$getServerListWidget().setServers(serverList);
      return true;
   }
}
