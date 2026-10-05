package fever.visual.framework.objects.gradient.impl;

import fever.visual.framework.objects.gradient.Gradient;
import fever.visual.utility.colors.ColorRGBA;

public class VerticalGradient extends Gradient {
   public VerticalGradient(ColorRGBA startColor, ColorRGBA endColor) {
      super(startColor, endColor, startColor, endColor);
   }

   public VerticalGradient rotate() {
      return new VerticalGradient(this.bottomRightColor, this.topLeftColor);
   }
}
