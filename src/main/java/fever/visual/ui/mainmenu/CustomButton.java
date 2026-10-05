package fever.visual.ui.mainmenu;

import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.render.obj.Rect;

public class CustomButton extends Rect {
   private final String icon;
   private final float iconSize;
   private final Runnable onClick;
   private final ColorRGBA backgroundColor = new ColorRGBA(58.0F, 58.0F, 58.0F);
   private final Animation activeAnim = new Animation(400L, 0.0F, Easing.BAKEK);
   private final Animation hoverAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   public void draw(UIContext context) {
      if (this.hovered(context.getMouseX(), context.getMouseY()) && this.activeAnim.getValue() == 1.0F) {
         CursorUtility.set(CursorType.HAND);
      }

      this.hoverAnim.update(this.hovered(context.getMouseX(), context.getMouseY()) && this.activeAnim.getValue() == 1.0F);
      context.drawRoundedRect(
         this.x,
         this.y,
         this.width,
         this.height,
         BorderRadius.all(Math.min(this.width, this.height) / 2.0F),
         this.backgroundColor.withAlpha(255.0F * (0.33F * this.activeAnim.getValue() + 0.2F * this.hoverAnim.getValue()))
      );
      context.drawTexture(
         FeverVisual.id(this.icon),
         this.x + (this.width - this.iconSize) / 2.0F,
         this.y + (this.height - this.iconSize) / 2.0F,
         this.iconSize,
         this.iconSize,
         ColorRGBA.WHITE.withAlpha(255.0F * this.activeAnim.getValue())
      );
   }

   public void click(double mouseX, double mouseY, int button) {
      if (this.hovered(mouseX, mouseY) && button == 0 && this.activeAnim.getValue() == 1.0F) {
         this.onClick.run();
      }
   }

   @Generated
   public CustomButton(String icon, float iconSize, Runnable onClick) {
      this.icon = icon;
      this.iconSize = iconSize;
      this.onClick = onClick;
   }

   @Generated
   public Animation getActiveAnim() {
      return this.activeAnim;
   }
}
