package fever.visual.mixin.minecraft.client;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.modules.visuals.CustomButtons;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.theme.Theme;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public class ButtonWidgetMixin implements IMinecraft {

    @Unique
    private final Animation hoverAnim = new Animation(280L, 0.0F, Easing.CUBIC_IN_OUT);

    @Unique
    private final Animation pressAnim = new Animation(150L, 0.0F, Easing.CUBIC_OUT);

    @Unique
    private long lastPressTime = 0;

    @Unique
    private boolean wasPressed = false;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void onRenderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        PressableWidget widget = (PressableWidget) (Object) this;

        if (!CustomButtons.isEnabledSafe()) {
            return;
        }

        if (!CustomButtons.shouldStyleCurrentButtonScreen()) {
            return;
        }

        ci.cancel();
        renderCustomWidget(CustomDrawContext.of(context), widget, mouseX, mouseY, delta);
    }

    @Unique
    private void renderCustomWidget(CustomDrawContext context, PressableWidget widget, int mouseX, int mouseY, float delta) {
        int x = widget.getX();
        int y = widget.getY();
        int width = widget.getWidth();
        int height = widget.getHeight();
        hoverAnim.update(widget.isHovered());

        boolean isPressed = widget.isHovered() && (System.currentTimeMillis() - lastPressTime < 150 && wasPressed);

        if (isPressed && !wasPressed) {
            pressAnim.setValue(1.0F);
            lastPressTime = System.currentTimeMillis();
        } else if (!isPressed && wasPressed) {
            pressAnim.setValue(0.0F);
        }
        pressAnim.update(isPressed ? 1.0F : 0.0F);
        wasPressed = isPressed;

        float hoverFactor = hoverAnim.getValue();
        float pressFactor = pressAnim.getValue();
        float easedPressFactor = Easing.CUBIC_OUT.ease(pressFactor, 0.0F, 1.0F, 1.0F);
        float finalScale = 1.0F - easedPressFactor * 0.025F;

        boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
        float glassIntensity = Interface.glass();
        float minimalizm = Interface.minimalizm();
        boolean showGlass = Interface.showGlass();
        boolean showMinimalizm = Interface.showMinimalizm();

        ColorRGBA bgColor;
        ColorRGBA textColor;

        if (widget.active) {
            if (isPressed) {
                bgColor = Colors.getAccentColor().withAlpha(255);
                textColor = this.fevervisual$getAccentTextColor();
            } else if (widget.isHovered()) {
                float mixFactor = 0.5F + (hoverFactor * 0.3F);
                bgColor = Colors.getAccentColor().mix(Colors.getBackgroundColor(), mixFactor).withAlpha(255);
                textColor = this.fevervisual$getAccentTextColor();
            } else {
                bgColor = Colors.getBackgroundColor().withAlpha(200);
                if (showMinimalizm) {
                    textColor = Colors.getTextColor().mix(dark ? ColorRGBA.WHITE : ColorRGBA.BLACK, minimalizm * 0.45F);
                } else {
                    textColor = Colors.getTextColor();
                }
            }
        } else {
            bgColor = Colors.getBackgroundColor().withAlpha(100);
            if (showMinimalizm) {
                textColor = Colors.getTextColor().mix(dark ? ColorRGBA.WHITE : ColorRGBA.BLACK, minimalizm * 0.35F).withAlpha(150);
            } else {
                textColor = Colors.getTextColor().withAlpha(100);
            }
        }

        context.getMatrices().push();
        context.getMatrices().translate(x + width / 2F, y + height / 2F, 0);
        context.getMatrices().scale(finalScale, finalScale, 1);
        context.getMatrices().translate(-(x + width / 2F), -(y + height / 2F), 0);
        int borderRadius = 6 + (int)(hoverFactor * 4);
        float glassOpacity = 0.08F - 0.06F * hoverFactor;

        if (showMinimalizm) {
            context.drawShadow(x, y, width, height, 8.0F, BorderRadius.all(borderRadius),
                    Colors.getBackgroundColor().withAlpha(95.0F * minimalizm));
            context.drawSquircle(x, y, width, height, borderRadius, BorderRadius.all(borderRadius),
                    ColorRGBA.WHITE.withAlpha(26.0F * minimalizm));
        }

        if (showGlass && widget.isHovered() && widget.active) {
            context.drawLiquidGlass(x, y, width, height, 7.0F, glassOpacity,
                    BorderRadius.all(borderRadius), ColorRGBA.WHITE.withAlpha(255 * glassIntensity));
        }

        context.drawSquircle(x, y, width, height, borderRadius, BorderRadius.all(borderRadius), bgColor);

        float fontSize = height > 15 ? 8 : 7;
        float textAlpha;
        if (showMinimalizm) {
            textAlpha = 0.95F + hoverFactor * 0.05F;
        } else {
            textAlpha = 0.8F + hoverFactor * 0.2F;
        }

        ColorRGBA finalTextColor = textColor.withAlpha((int)(textAlpha * 255));

        context.drawCenteredText(Fonts.MEDIUM.getFont(fontSize), widget.getMessage().getString(),
                x + width / 2F, y + height / 2F - Fonts.MEDIUM.getFont(fontSize).height() / 2F, finalTextColor);

        context.getMatrices().pop();
    }

    @Unique
    private ColorRGBA fevervisual$getAccentTextColor() {
        ColorRGBA textColor = Colors.getTextColor();
        boolean brightThemeText = (textColor.getRed() + textColor.getGreen() + textColor.getBlue()) / 3.0F > 145.0F;
        return brightThemeText ? ColorRGBA.WHITE : ColorRGBA.BLACK.withAlpha(245.0F);
    }
}
