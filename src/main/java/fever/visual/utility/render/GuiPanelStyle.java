package fever.visual.utility.render;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.theme.Theme;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;

public final class GuiPanelStyle {
   private GuiPanelStyle() {
      throw new UnsupportedOperationException("Utility class");
   }

   public static void drawPanel(CustomDrawContext context, float x, float y, float width, float height, float radius) {
      drawPanel(context, x, y, width, height, radius, 1.0F);
   }

   public static void drawPanel(CustomDrawContext context, float x, float y, float width, float height, float radius, float alpha) {
      float clampedAlpha = Math.clamp(alpha, 0.0F, 1.0F);
      float glass = Interface.glass();
      float minimalism = Interface.minimalizm();

      context.drawShadow(
         x - 2.0F,
         y - 2.0F,
         width + 4.0F,
         height + 4.0F,
         10.0F,
         BorderRadius.all(Math.max(4.0F, radius - 1.0F)),
         ColorRGBA.BLACK.withAlpha(36.0F * clampedAlpha)
      );

      if (Interface.showMinimalizm()) {
         context.drawBlurredRect(
            x,
            y,
            width,
            height,
            45.0F,
            radius,
            BorderRadius.all(6.0F),
            ColorRGBA.WHITE.withAlpha(255.0F * clampedAlpha * minimalism)
         );
      }

      if (Interface.showGlass()) {
         context.drawLiquidGlass(x, y, width, height, radius, 0.04F, BorderRadius.all(6.0F), ColorRGBA.WHITE.withAlpha(210.0F * clampedAlpha * glass));
      }

      boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      float baseAlpha = dark ? 222.0F : 208.0F;
      float glassPenalty = dark ? 62.0F : 34.0F;
      float backgroundAlpha = Math.max(140.0F, baseAlpha - glassPenalty * glass) * clampedAlpha;
      context.drawSquircle(
         x,
         y,
         width,
         height,
         radius,
         BorderRadius.all(6.0F),
         Colors.getBackgroundColor().withAlpha(backgroundAlpha)
      );

      context.drawRoundedBorder(
         x + 0.5F,
         y + 0.5F,
         Math.max(0.0F, width - 1.0F),
         Math.max(0.0F, height - 1.0F),
         0.8F,
         BorderRadius.all(Math.max(3.0F, radius - 2.0F)),
         Colors.getTextColor().withAlpha(24.0F * clampedAlpha)
      );
   }
}
