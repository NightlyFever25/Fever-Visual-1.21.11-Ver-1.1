package fever.visual.mixin.minecraft.client.gui.screen.multiplayer;

import fever.visual.utility.game.server.SponsorServerUtility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.FeverVisualServerCategoryEntry;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerServerListWidget.class)
public abstract class MultiplayerServerListWidgetMixin extends AlwaysSelectedEntryListWidget<MultiplayerServerListWidget.Entry> {
   @Shadow
   @Final
   private List<MultiplayerServerListWidget.ServerEntry> servers;
   @Shadow
   @Final
   private MultiplayerServerListWidget.Entry scanningEntry;
   @Shadow
   @Final
   private List<MultiplayerServerListWidget.LanServerEntry> lanServers;

   public MultiplayerServerListWidgetMixin(MinecraftClient client, int width, int height, int y, int itemHeight) {
      super(client, width, height, y, itemHeight);
   }

   @Inject(method = "setServers", at = @At("HEAD"))
   private void feverVisual$ensureSponsorBeforeEntries(ServerList serverList, CallbackInfo ci) {
      if (SponsorServerUtility.ensureFirst(serverList)) {
         serverList.saveFile();
      }
   }

   @Inject(method = "updateEntries", at = @At("RETURN"))
   private void feverVisual$addServerCategories(CallbackInfo ci) {
      MultiplayerServerListWidget.Entry selected = this.getSelectedOrNull();
      List<MultiplayerServerListWidget.Entry> entries = new ArrayList<>();
      int serverStart = 0;

      if (!this.servers.isEmpty() && SponsorServerUtility.isSponsor(this.servers.get(0).getServer())) {
         entries.add(new FeverVisualServerCategoryEntry(SponsorServerUtility.SPONSOR_CATEGORY));
         entries.add(this.servers.get(0));
         serverStart = 1;
      }

      if (this.servers.size() > serverStart) {
         entries.add(new FeverVisualServerCategoryEntry(SponsorServerUtility.MULTIPLAYER_CATEGORY));

         for(int i = serverStart; i < this.servers.size(); ++i) {
            entries.add(this.servers.get(i));
         }
      }

      entries.add(this.scanningEntry);
      entries.addAll(this.lanServers);
      this.replaceEntries(entries);

      if (selected != null && entries.contains(selected)) {
         this.setSelected(selected);
      }
   }
}
