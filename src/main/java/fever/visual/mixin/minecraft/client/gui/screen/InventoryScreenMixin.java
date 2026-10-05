package fever.visual.mixin.minecraft.client.gui.screen;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.modules.visuals.CustomInv;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.GuiPanelStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends RecipeBookScreen<PlayerScreenHandler> implements IMinecraft {
   @Shadow
   private float mouseX;

   @Shadow
   private float mouseY;

   public InventoryScreenMixin(PlayerScreenHandler handler, RecipeBookWidget<?> recipeBook, PlayerInventory inventory, Text title) {
      super(handler, recipeBook, inventory, title);
   }

   @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
      if (!this.isCustomInventoryEnabled()) {
         return;
      }

      ci.cancel();
      int panelX = this.x;
      int panelY = this.y;

      CustomDrawContext customContext = CustomDrawContext.of(context);
      GuiPanelStyle.drawPanel(customContext, panelX, panelY, this.backgroundWidth, this.backgroundHeight, 7.0F);
      this.fevervisual$drawCraftingSlotGuides(customContext, panelX, panelY);

      if (this.client != null && this.client.player != null) {
         InventoryScreen.drawEntity(context, panelX + 26, panelY + 8, panelX + 75, panelY + 78, 30, 0.0625F, this.mouseX, this.mouseY, this.client.player);
      }
   }

   @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
      if (!this.isCustomInventoryEnabled()) {
         return;
      }

      ci.cancel();
   }

   @Unique
   private void fevervisual$drawCraftingSlotGuides(CustomDrawContext context, int panelX, int panelY) {
      final int slotSize = 18;
      final float borderThickness = 0.8F;
      final float innerInset = 2.5F;
      final float innerSize = slotSize - innerInset * 2.0F;

      for (int row = 0; row < 2; row++) {
         for (int col = 0; col < 2; col++) {
            float slotX = panelX + 98 + col * slotSize;
            float slotY = panelY + 18 + row * slotSize;
            this.fevervisual$drawSlotGuide(context, slotX, slotY, slotSize, borderThickness, innerInset, innerSize);
         }
      }

      this.fevervisual$drawSlotGuide(context, panelX + 154, panelY + 28, slotSize, borderThickness, innerInset, innerSize);
   }

   @Unique
   private void fevervisual$drawSlotGuide(
      CustomDrawContext context,
      float slotX,
      float slotY,
      float slotSize,
      float borderThickness,
      float innerInset,
      float innerSize
   ) {
      context.drawRoundedBorder(
         slotX + 0.5F,
         slotY + 0.5F,
         slotSize - 1.0F,
         slotSize - 1.0F,
         borderThickness,
         BorderRadius.all(3.0F),
         Colors.getTextColor().withAlpha(42.0F)
      );
      context.drawRoundedRect(
         slotX + innerInset,
         slotY + innerInset,
         innerSize,
         innerSize,
         BorderRadius.all(2.0F),
         Colors.getTextColor().withAlpha(9.0F)
      );
   }

   @Unique
   private boolean isCustomInventoryEnabled() {
      if (FeverVisual.getInstance().getModuleManager() == null) {
         return false;
      }

      CustomInv module = FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomInv.class);
      return module != null && module.isEnabled();
   }
}
