package fever.visual.mixin.minecraft.client.gui.screen;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.systems.modules.modules.visuals.CustomInv;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.GuiPanelStyle;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StatusEffectsDisplay.class)
public abstract class StatusEffectsDisplayMixin implements IMinecraft {
    @Redirect(
            method = "drawStatusEffectBackgrounds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture("
                            + "Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
            ),
            require = 0
    )
    private void fevervisual$replaceEffectBackgroundGui(
            DrawContext context,
            RenderPipeline pipeline,
            Identifier texture,
            int x,
            int y,
            int width,
            int height
    ) {
        if (!fevervisual$isCustomInventoryEnabled()) {
            context.drawGuiTexture(pipeline, texture, x, y, width, height);
            return;
        }
        GuiPanelStyle.drawPanel(CustomDrawContext.of(context), x, y, width, height, 5.0F, 0.96F);
    }
    @Redirect(
            method = "drawStatusEffectBackgrounds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTexture("
                            + "Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"
            ),
            require = 0
    )
    private void fevervisual$replaceEffectBackgroundTextured(
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
        if (!fevervisual$isCustomInventoryEnabled()) {
            context.drawTexture(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
            return;
        }
        GuiPanelStyle.drawPanel(CustomDrawContext.of(context), x, y, width, height, 5.0F, 0.96F);
    }
    @Redirect(
            method = "drawTexts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow("
                            + "Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)V"
            )
    )
    private void fevervisual$redirectCurrentEffectText(
            DrawContext context,
            TextRenderer textRenderer,
            OrderedText text,
            int x,
            int y,
            int color
    ) {
        if (!fevervisual$isCustomInventoryEnabled()) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
            return;
        }

        StringBuilder plain = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            plain.appendCodePoint(codePoint);
            return true;
        });
        fevervisual$drawCustomEffectText(context, plain.toString(), x, y, color);
    }


    @Unique
    private void fevervisual$drawCustomEffectText(DrawContext context, String text, int x, int y, int color) {
        if (text.isEmpty()) {
            return;
        }
        boolean isName = (color & 0xFFFFFF) == 0xFFFFFF;

        CustomDrawContext customContext = CustomDrawContext.of(context);
        Font font;
        if (isName) {
            font = Fonts.MEDIUM.getFont(7.0F);
            customContext.drawText(font, text, (float) x, (float) y, Colors.getTextColor());
        } else {
            font = Fonts.MEDIUM.getFont(6.3F);
            customContext.drawText(font, text, (float) x, (float) y, Colors.getTextColor().withAlpha(165.0F));
        }
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
