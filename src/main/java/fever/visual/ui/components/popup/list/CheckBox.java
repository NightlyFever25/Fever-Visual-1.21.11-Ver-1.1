package fever.visual.ui.components.popup.list;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.MouseButton;
import fever.visual.ui.components.popup.CheckBoxAction;
import fever.visual.ui.components.popup.PopupComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;

public class CheckBox extends PopupComponent {
   private boolean enabled;
   private final String text;
   private final Animation hoverAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation enableAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private CheckBoxAction action;

   public CheckBox(String text) {
      this.text = text;
   }

   @Override
   protected void renderComponent(UIContext context) {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float nameLeftPadding = 8.0F;
      float nameHeight = nameFont.height();
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      this.enableAnimation.update(this.enabled ? 1.0F : 0.0F);
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      context.drawFadeoutText(
         nameFont,
         this.text,
         this.x + nameLeftPadding,
         this.y + GuiUtility.getMiddleOfBox(nameHeight, this.height),
         Colors.getTextColor()
            .withAlpha(RenderSystem.getShaderAlpha() * 255.0F * (0.75F + 0.25F * this.enableAnimation.getValue() + 0.25F * this.hoverAnimation.getValue())),
         0.8F,
         1.0F,
         this.width - 12.0F - 12.0F * this.enableAnimation.getValue()
      );
      float alpha = this.enableAnimation.getValue() * (RenderSystem.getShaderAlpha() * 255.0F);
      if (this.enableAnimation.getValue() >= 0.0F) {
         context.drawTexture(
            FeverVisual.id("icons/check.png"),
            this.x + this.width - 13.0F - this.enableAnimation.getValue() * 2.0F,
            this.y + 7.0F,
            6.0F,
            6.0F,
            Colors.getTextColor().withAlpha(alpha)
         );
      }
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         this.enabled = !this.enabled;
         if (this.action != null) {
            this.action.handleAction(this.enabled);
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      return this.height = 19.0F;
   }

   public CheckBox enabled(boolean value) {
      this.enabled = value;
      return this;
   }

   public CheckBox action(CheckBoxAction action) {
      this.action = action;
      return this;
   }
}
