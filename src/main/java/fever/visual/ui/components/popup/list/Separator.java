package fever.visual.ui.components.popup.list;

import fever.visual.framework.base.UIContext;
import fever.visual.ui.components.popup.PopupComponent;
import fever.visual.utility.colors.Colors;

public class Separator extends PopupComponent {
   @Override
   protected void renderComponent(UIContext context) {
      context.drawRect(this.x, this.y, this.width, this.height, Colors.getSeparatorColor());
   }

   @Override
   public float getHeight() {
      return this.height = 4.0F;
   }
}
