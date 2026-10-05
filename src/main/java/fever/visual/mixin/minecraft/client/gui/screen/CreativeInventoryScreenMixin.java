package fever.visual.mixin.minecraft.client.gui.screen;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.systems.modules.modules.visuals.CustomInv;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.GuiPanelStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.render.RenderLayer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin implements IMinecraft {
   @Redirect(
      method = "drawBackground",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"
      )
   )
   private void fevervisual$replaceCreativeBackground(
      DrawContext context,
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int textureWidth,
      int textureHeight
   ) {
      if (!this.fevervisual$isCustomInventoryEnabled()) {
         context.drawTexture(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
         return;
      }

      GuiPanelStyle.drawPanel(CustomDrawContext.of(context), x, y, width, height, 7.0F);
   }

   @Redirect(
      method = "renderTabIcon",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
      )
   )
   private void fevervisual$replaceCreativeTabBackground(
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

   @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
   private void fevervisual$drawCustomForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
      if (!this.fevervisual$isCustomInventoryEnabled()) {
         return;
      }

      ci.cancel();
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
