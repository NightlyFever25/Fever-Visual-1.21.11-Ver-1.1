package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.setting.settings.RangeSetting;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;

public class RangeSettingComponent extends MenuSettingComponent<RangeSetting> {
   private final Animation xAnim;
   private final Animation widthAnim;
   private boolean dragFirst;
   private boolean dragSecond;

   public RangeSettingComponent(RangeSetting setting, CustomComponent parent) {
      super(setting, parent);
      this.xAnim = new Animation(500L, Easing.BAKEK_PAGES);
      this.widthAnim = new Animation(500L, Easing.BAKEK_PAGES);
   }

   protected void renderComponent(UIContext context) {
      float x = this.x + 9.0F;
      float y = this.y + 2.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float nameHeight = Fonts.REGULAR.getFont(7.0F).height();
      float first = ((RangeSetting)this.setting).getFirstValue();
      float second = ((RangeSetting)this.setting).getSecondValue();
      if (first >= second) {
         first = ((RangeSetting)this.setting).getSecondValue();
         second = ((RangeSetting)this.setting).getFirstValue();
      }

      this.xAnim.update(first);
      this.widthAnim.update(second);
      this.hoverAnimation.update(this.isHovered((float)context.getMouseX(), (float)context.getMouseY()));
      context.drawRoundedRect(x, y + this.height - 12.0F, width, 2.0F, BorderRadius.all(0.25F), Colors.getAdditionalColor().withAlpha(178.5F));
      context.drawRoundedRect(x + width * GuiUtility.getPercent(this.xAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()), y + this.height - 12.0F, width * GuiUtility.getPercent(this.widthAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()) - width * GuiUtility.getPercent(this.xAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()), 2.0F, BorderRadius.all(0.25F), Colors.getAccentColor());
      context.drawShadow(x + width * GuiUtility.getPercent(this.xAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()) - 3.0F, y + this.height - 14.0F, 6.0F, 6.0F, 10.0F, BorderRadius.all(3.0F), ColorRGBA.BLACK.withAlpha(63.75F));
      context.drawRoundedRect(x + width * GuiUtility.getPercent(this.xAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()) - 3.0F, y + this.height - 14.0F, 6.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE);
      context.drawShadow(x + width * GuiUtility.getPercent(this.xAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()) + width * GuiUtility.getPercent(this.widthAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()) - width * GuiUtility.getPercent(this.xAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()) - 3.0F, y + this.height - 14.0F, 6.0F, 6.0F, 10.0F, BorderRadius.all(3.0F), ColorRGBA.BLACK.withAlpha(63.75F));
      context.drawRoundedRect(x + width * GuiUtility.getPercent(this.xAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()) + width * GuiUtility.getPercent(this.widthAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()) - width * GuiUtility.getPercent(this.xAnim.getValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax()) - 3.0F, y + this.height - 14.0F, 6.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE);
      String value = String.format("от %s до %s", TextUtility.formatNumber((double)this.xAnim.getValue()), TextUtility.formatNumber((double)this.widthAnim.getValue()));
      context.drawFadeoutText(nameFont, Localizator.translate(((RangeSetting)this.setting).getName()), this.x + leftPadding, y + 11.0F - nameFont.height(), Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())), 0.8F, 1.0F, this.getParent().getWidth() - leftPadding - Fonts.REGULAR.getFont(7.0F).width(value) - 10.0F);
      context.drawRightText(Fonts.REGULAR.getFont(7.0F), value, x + width, y + 11.0F - nameHeight, Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())));
      if (this.isHovered((float)context.getMouseX(), (float)context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      if (this.dragFirst) {
         float xValue = GuiUtility.getSliderValue(((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax(), x, width, (double)context.getMouseX());
         ((RangeSetting)this.setting).setFirstValue(xValue);
         CursorUtility.set(CursorType.ARROW_HORIZONTAL);
      } else if (this.dragSecond) {
         float xValue = GuiUtility.getSliderValue(((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax(), x, width, (double)context.getMouseX());
         ((RangeSetting)this.setting).setSecondValue(xValue);
         CursorUtility.set(CursorType.ARROW_HORIZONTAL);
      }

   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      float x = this.x + 9.0F;
      float width = this.width - 18.0F;
      if (this.isHovered(mouseX, mouseY)) {
         float firstDist = (float)Math.abs(mouseX - (double)(x + width * GuiUtility.getPercent(((RangeSetting)this.setting).getFirstValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax())));
         float secondDist = (float)Math.abs(mouseX - (double)(x + width * GuiUtility.getPercent(((RangeSetting)this.setting).getSecondValue(), ((RangeSetting)this.setting).getMin(), ((RangeSetting)this.setting).getMax())));
         if (firstDist < secondDist) {
            this.dragFirst = true;
         } else {
            this.dragSecond = true;
         }
      }

      super.onVisibleMouseClicked(mouseX, mouseY, button);
   }

   protected void onVisibleMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.dragFirst = false;
      this.dragSecond = false;
      super.onVisibleMouseReleased(mouseX, mouseY, button);
   }

   public float getHeight() {
      return this.height = 29.0F;
   }
}
