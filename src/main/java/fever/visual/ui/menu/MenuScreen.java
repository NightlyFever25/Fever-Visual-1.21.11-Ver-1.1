package fever.visual.ui.menu;

import fever.visual.framework.base.CustomScreen;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import lombok.Generated;

public abstract class MenuScreen extends CustomScreen {
   protected final Animation menuAnimation;
   protected boolean closing;

   public MenuScreen() {
      this.menuAnimation = new Animation(500L, Easing.LINEAR);
      this.closing = true;
   }

   @Generated
   public Animation getMenuAnimation() {
      return this.menuAnimation;
   }

   @Generated
   public boolean isClosing() {
      return this.closing;
   }

   @Generated
   public void setClosing(boolean closing) {
      this.closing = closing;
   }
}
