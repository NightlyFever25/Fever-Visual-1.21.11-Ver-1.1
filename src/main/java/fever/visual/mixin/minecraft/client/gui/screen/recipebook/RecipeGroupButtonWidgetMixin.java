package fever.visual.mixin.minecraft.client.gui.screen.recipebook;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.systems.modules.modules.visuals.CustomInv;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.GuiPanelStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeGroupButtonWidget;
import net.minecraft.client.render.RenderLayer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RecipeGroupButtonWidget.class)
public abstract class RecipeGroupButtonWidgetMixin implements IMinecraft {
   @Redirect(
      method = "drawIcon",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
      )
   )
   private void fevervisual$replaceRecipeTabBackground(
      DrawContext context,
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      int width,
      int height
   ) {
      if (!this.fevervisual$isCustomInventoryEnabled()) {
         context.drawGuiTexture(pipeline, texture, x, y, width, height);
         return;
      }

      GuiPanelStyle.drawPanel(CustomDrawContext.of(context), x, y, width, height, 6.0F, 0.96F);
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
