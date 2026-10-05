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
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin implements IMinecraft {

    @Shadow protected double value;
    @Shadow protected abstract void updateMessage();
    @Shadow protected abstract void applyValue();

    @Unique
    private final Animation hoverProgress = new Animation(250L, 0.0F, Easing.CUBIC_IN_OUT);
    @Unique
    private final Animation pressProgress = new Animation(120L, 0.0F, Easing.CUBIC_IN_OUT);
    @Unique
    private float sliderAnim = 0.0F;
    @Unique
    private boolean drag = false;
    @Unique
    private boolean sliderAnimInit = false;
    @Unique
    private boolean wasDragging = false;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void onRenderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SliderWidget widget = (SliderWidget) (Object) this;

        if (!this.fevervisual$shouldUseCustomSlider(widget)) return;

        ci.cancel();
        renderCustomSlider(CustomDrawContext.of(context), widget, mouseX, mouseY, delta);
    }

    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void onClick(Click click, boolean doubled, CallbackInfo ci) {
        SliderWidget widget = (SliderWidget) (Object) this;
        if (!this.fevervisual$shouldUseCustomSlider(widget)) return;

        drag = true;
        wasDragging = true;
        ci.cancel();
    }

    @Inject(method = "onRelease", at = @At("HEAD"), cancellable = true)
    private void onRelease(Click click, CallbackInfo ci) {
        SliderWidget widget = (SliderWidget) (Object) this;
        if (!this.fevervisual$shouldUseCustomSlider(widget)) return;

        drag = false;
        wasDragging = false;
        ci.cancel();
    }

    @Inject(method = "onDrag", at = @At("HEAD"), cancellable = true)
    private void onDrag(Click click, double deltaX, double deltaY, CallbackInfo ci) {
        SliderWidget widget = (SliderWidget) (Object) this;
        if (!this.fevervisual$shouldUseCustomSlider(widget)) return;

        if (drag) {
            int x     = widget.getX();
            int width = widget.getWidth();
            double newValue = (click.x() - x) / (double)(width - 5);
            newValue = MathHelper.clamp(newValue, 0.0, 1.0);
            value = newValue;
            applyValue();
            updateMessage();
        }
        ci.cancel();
    }

    @Unique
    private void renderCustomSlider(CustomDrawContext context, SliderWidget widget, int mouseX, int mouseY, float delta) {
        int x      = widget.getX();
        int y      = widget.getY();
        int width  = widget.getWidth();
        int height = widget.getHeight();

        boolean isHovered = widget.isHovered();
        hoverProgress.update(isHovered || drag);
        pressProgress.update(drag);

        float hoverFactor = hoverProgress.getValue();
        float pressFactor = pressProgress.getValue();

        boolean dark           = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
        float glassIntensity   = Interface.glass();
        float minimalizm       = Interface.minimalizm();
        boolean showGlass      = Interface.showGlass();
        boolean showMinimalizm = Interface.showMinimalizm();

        ColorRGBA bgColor;
        ColorRGBA textColor;
        ColorRGBA thumbColor;

        if (widget.active) {
            if (drag) {
                bgColor   = Colors.getAccentColor().withAlpha(255);
                textColor = this.fevervisual$getAccentTextColor();
                thumbColor = dark ? ColorRGBA.WHITE : ColorRGBA.BLACK;
            } else if (isHovered) {
                bgColor   = Colors.getAccentColor().mix(Colors.getBackgroundColor(), 0.7F).withAlpha(255);
                textColor = this.fevervisual$getAccentTextColor();
                thumbColor = dark ? ColorRGBA.WHITE : ColorRGBA.BLACK;
            } else {
                bgColor   = Colors.getBackgroundColor().withAlpha(200);
                textColor = Colors.getTextColor();
                thumbColor = dark ? ColorRGBA.WHITE : ColorRGBA.BLACK;
            }
        } else {
            bgColor   = Colors.getBackgroundColor().withAlpha(100);
            textColor = Colors.getTextColor().withAlpha(100);
            thumbColor = dark ? ColorRGBA.WHITE.withAlpha(100) : ColorRGBA.BLACK.withAlpha(100);
        }

        int borderRadius = 6 + (int)(hoverFactor * 3);
        float thumbScale = 1.0F - pressFactor * 0.05F;

        if (showMinimalizm) {
            context.drawShadow(x, y, width, height, 8.0F, BorderRadius.all(borderRadius),
                    Colors.getBackgroundColor().withAlpha(95.0F * minimalizm));
            context.drawSquircle(x, y, width, height,
                    borderRadius, BorderRadius.all(borderRadius),
                    ColorRGBA.WHITE.withAlpha(26.0F * minimalizm));
        }

        if (showGlass && (isHovered || drag) && widget.active) {
            context.drawLiquidGlass(x, y, width, height,
                    7.0F, 0.08F - 0.06F * hoverFactor,
                    BorderRadius.all(borderRadius),
                    ColorRGBA.WHITE.withAlpha(255 * glassIntensity));
        }

        context.drawSquircle(x, y, width, height,
                borderRadius, BorderRadius.all(borderRadius), bgColor);
        float targetSliderX = x + (float)(value * (width - 5));
        targetSliderX = MathHelper.clamp(targetSliderX, x, x + width - 5);

        if (!sliderAnimInit) {
            sliderAnim = targetSliderX;
            sliderAnimInit = true;
        }
        float smoothing = Math.min(1.0F, 0.45F + (1.0F - hoverFactor) * 0.15F);
        sliderAnim += (targetSliderX - sliderAnim) * smoothing;
        context.getMatrices().push();
        context.getMatrices().translate(sliderAnim + 2.5F, y + height / 2F, 0);
        context.getMatrices().scale(thumbScale, thumbScale, 1);

        context.drawSquircle(
                -2.5F, -height / 2F, 5, height,
                2, BorderRadius.all(2),
                thumbColor
        );

        context.getMatrices().pop();
        float fontSize = height > 15 ? 8 : 7;
        context.drawCenteredText(
                Fonts.MEDIUM.getFont(fontSize),
                widget.getMessage().getString(),
                x + width / 2F,
                y + height / 2F - Fonts.MEDIUM.getFont(fontSize).height() / 2F,
                textColor
        );
    }

    @Unique
    private ColorRGBA fevervisual$getAccentTextColor() {
        ColorRGBA textColor = Colors.getTextColor();
        boolean brightThemeText = (textColor.getRed() + textColor.getGreen() + textColor.getBlue()) / 3.0F > 145.0F;
        return brightThemeText ? ColorRGBA.WHITE : ColorRGBA.BLACK.withAlpha(245.0F);
    }

    @Unique
    private boolean fevervisual$shouldUseCustomSlider(SliderWidget widget) {
        if (!this.fevervisual$isCustomButtonsEnabled() || mc.currentScreen == null || widget == null) {
            return false;
        }

        return CustomButtons.shouldStyleCurrentSettingsScreen();
    }

    @Unique
    private boolean fevervisual$isCustomButtonsEnabled() {
        return CustomButtons.isEnabledSafe();
    }
}
