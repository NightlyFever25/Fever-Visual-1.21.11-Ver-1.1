package fever.visual.mixin.minecraft.client.gui.screen;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.systems.modules.modules.visuals.CustomInv;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.GuiPanelStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> implements IMinecraft {
   protected GenericContainerScreenMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
      super(handler, inventory, title);
   }

   @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawChestAndBarrelBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
      if (!this.fevervisual$isCustomInventoryEnabled()) {
         return;
      }

      ci.cancel();
      GuiPanelStyle.drawPanel(CustomDrawContext.of(context), this.x, this.y, this.backgroundWidth, this.backgroundHeight, 7.0F);
   }

   @Unique
   private boolean fevervisual$isCustomInventoryEnabled() {
      if (FeverVisual.getInstance().getModuleManager() == null) {
         return false;
      }

      CustomInv module = FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomInv.class);
      return module != null && module.isEnabled();
   }
}
