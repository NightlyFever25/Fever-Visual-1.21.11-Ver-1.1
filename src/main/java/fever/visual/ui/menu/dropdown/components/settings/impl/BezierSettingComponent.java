package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.setting.settings.BezierSetting;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import net.minecraft.util.math.Vec2f;

public class BezierSettingComponent extends MenuSettingComponent<BezierSetting> {
   private final Animation startX;
   private final Animation startY;
   private final Animation endX;
   private final Animation endY;
   private boolean dragStart;
   private boolean dragEnd;

   public BezierSettingComponent(BezierSetting setting, CustomComponent parent) {
      super(setting, parent);
      this.startX = new Animation(500L, Easing.BAKEK_PAGES);
      this.startY = new Animation(500L, Easing.BAKEK_PAGES);
      this.endX = new Animation(500L, Easing.BAKEK_PAGES);
      this.endY = new Animation(500L, Easing.BAKEK_PAGES);
   }

   protected void renderComponent(UIContext context) {
      float x = this.x + 9.0F;
      float y = this.y + 2.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float nameHeight = Fonts.REGULAR.getFont(7.0F).height();
      this.hoverAnimation.update(this.isHovered((float)context.getMouseX(), (float)context.getMouseY()));
      float offset = 3.0F;
      float boxX = x - 1.0F + offset;
      float boxY = y + 17.0F + offset;
      float boxWidth = width + 2.0F - offset * 2.0F;
      float boxHeight = this.height - 10.0F - 17.0F - offset * 2.0F;
      context.drawRoundedRect(boxX - offset, boxY - offset, boxWidth + offset * 2.0F, boxHeight + offset * 2.0F, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(76.5F));
      context.drawRoundedRect(boxX + this.startX.getValue() * boxWidth - 3.0F, boxY + this.startY.getValue() * boxHeight - 3.0F, 6.0F, 6.0F, BorderRadius.all(6.0F), Colors.WHITE.withAlpha(255.0F));
      context.drawRoundedRect(boxX + this.endX.getValue() * boxWidth - 3.0F, boxY + this.endY.getValue() * boxHeight - 3.0F, 6.0F, 6.0F, BorderRadius.all(6.0F), Colors.WHITE.withAlpha(255.0F));
      Vec2f anchorStart = new Vec2f(boxX, boxY + boxHeight);
      Vec2f controlStart = new Vec2f(boxX + this.startX.getValue() * boxWidth, boxY + this.startY.getValue() * boxHeight);
      Vec2f controlEnd = new Vec2f(boxX + this.endX.getValue() * boxWidth, boxY + this.endY.getValue() * boxHeight);
      Vec2f anchorEnd = new Vec2f(boxX + boxWidth, boxY);
      context.drawBezier(anchorStart, controlStart, controlEnd, anchorEnd, ColorRGBA.WHITE, 50);
      context.drawLine(anchorStart, controlStart, Colors.WHITE.mulAlpha(0.5F));
      context.drawLine(anchorEnd, controlEnd, Colors.WHITE.mulAlpha(0.5F));
      context.drawFadeoutText(nameFont, Localizator.translate(((BezierSetting)this.setting).getName()), this.x + leftPadding, y + 11.0F - nameFont.height(), Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())), 0.8F, 1.0F, this.getParent().getWidth() - leftPadding - 10.0F);
      if (this.isHovered((float)context.getMouseX(), (float)context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      if (this.dragStart) {
         float xValue = GuiUtility.getSliderValue(0.0F, 1.0F, boxX, boxWidth, (double)context.getMouseX());
         float yValue = GuiUtility.getSliderValueWithoutClamp(0.0F, 1.0F, boxY, boxHeight, (double)context.getMouseY());
         ((BezierSetting)this.setting).start(new Vec2f(xValue, Math.clamp(yValue, -0.5F, 1.5F)));
         CursorUtility.set(CursorType.CROSSHAIR);
      } else if (this.dragEnd) {
         float xValue = GuiUtility.getSliderValue(0.0F, 1.0F, boxX, boxWidth, (double)context.getMouseX());
         float yValue = GuiUtility.getSliderValueWithoutClamp(0.0F, 1.0F, boxY, boxHeight, (double)context.getMouseY());
         ((BezierSetting)this.setting).end(new Vec2f(xValue, Math.clamp(yValue, -0.5F, 1.5F)));
         CursorUtility.set(CursorType.CROSSHAIR);
      }

      this.startX.setValue(((BezierSetting)this.setting).start().x);
      this.startY.setValue(((BezierSetting)this.setting).start().y);
      this.endX.setValue(((BezierSetting)this.setting).end().x);
      this.endY.setValue(((BezierSetting)this.setting).end().y);
   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      float x = this.x + 9.0F;
      float y = this.y + 2.0F;
      float width = this.width - 18.0F;
      if (this.isHovered(mouseX, mouseY)) {
         float boxX = x - 1.0F;
         float boxY = y + 17.0F;
         float boxWidth = width + 2.0F;
         float boxHeight = this.height - 10.0F - 17.0F;
         Vec2f mouse = new Vec2f(GuiUtility.getPercent((float)mouseX, boxX, boxX + boxWidth), GuiUtility.getPercent((float)mouseY, boxY, boxY + boxHeight));
         float startDist = this.distance(((BezierSetting)this.setting).start(), mouse);
         float endDist = this.distance(((BezierSetting)this.setting).end(), mouse);
         if (startDist < endDist) {
            this.dragStart = true;
         } else {
            this.dragEnd = true;
         }
      }

      super.onVisibleMouseClicked(mouseX, mouseY, button);
   }

   public float distance(Vec2f vec, Vec2f vec2) {
      float f = vec.x - vec2.x;
      float g = vec.y - vec2.y;
      return (float)Math.sqrt((double)(f * f + g * g));
   }

   protected void onVisibleMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.dragStart = false;
      this.dragEnd = false;
      super.onVisibleMouseReleased(mouseX, mouseY, button);
   }

   public float getHeight() {
      return this.height = this.width - 14.0F;
   }
}
