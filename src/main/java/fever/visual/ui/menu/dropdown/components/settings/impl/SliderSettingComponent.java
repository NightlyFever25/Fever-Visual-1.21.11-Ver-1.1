package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.time.Timer;

public class SliderSettingComponent extends MenuSettingComponent<SliderSetting> {
   private final Animation animation;
   private final Animation moving;
   private final Timer timer;
   private boolean drag;
   private static SliderSettingComponent current;

   public SliderSettingComponent(SliderSetting setting, CustomComponent parent) {
      super(setting, parent);
      this.animation = new Animation(500L, Easing.BAKEK_PAGES);
      this.moving = new Animation(500L, Easing.FIGMA_EASE_IN_OUT);
      this.timer = new Timer();
   }

   protected void renderComponent(UIContext context) {
      float x = this.x + 9.0F;
      float y = this.y + 2.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float nameHeight = Fonts.REGULAR.getFont(7.0F).height();
      float headerHeight = 19.0F;
      this.animation.update(((SliderSetting)this.setting).getCurrentValue());
      this.hoverAnimation.update(this.isHovered((float)context.getMouseX(), (float)context.getMouseY()));
      context.drawLegacyRoundedRect(x, y + this.height - 12.0F, width, 2.0F, BorderRadius.all(0.25F), Colors.getAdditionalColor().withAlpha((255.0F - 100.0F * Interface.glass()) * 0.7F));
      context.drawLegacyRoundedRect(x, y + this.height - 12.0F, width * GuiUtility.getPercent(this.animation.getValue(), ((SliderSetting)this.setting).getMin(), ((SliderSetting)this.setting).getMax()), 2.0F, BorderRadius.all(0.25F), Colors.getAccentColor());
      if (this.timer.finished(1000L)) {
         context.updateBuffer();
         this.timer.reset();
      }

      if (Interface.showGlass()) {
         context.drawShadow(x + width * GuiUtility.getPercent(this.animation.getValue(), ((SliderSetting)this.setting).getMin(), ((SliderSetting)this.setting).getMax()) - 4.5F - 3.0F * this.moving.getValue(), y + this.height - 11.0F - 3.0F - 2.0F * this.moving.getValue(), 9.0F + 6.0F * this.moving.getValue(), 6.0F + 4.0F * this.moving.getValue(), 10.0F, BorderRadius.all(3.0F + this.moving.getValue() * 2.0F), ColorRGBA.BLACK.withAlpha(255.0F * (0.25F + 0.2F * this.moving.getValue()) * Interface.glass()));
         context.drawLegacySquircle(x + width * GuiUtility.getPercent(this.animation.getValue(), ((SliderSetting)this.setting).getMin(), ((SliderSetting)this.setting).getMax()) - 4.5F - 3.0F * this.moving.getValue(), y + this.height - 11.0F - 3.0F - 2.0F * this.moving.getValue(), 9.0F + 6.0F * this.moving.getValue(), 6.0F + 4.0F * this.moving.getValue(), 7.0F, BorderRadius.all(3.0F + this.moving.getValue()), ColorRGBA.WHITE.withAlpha(255.0F * (1.0F - this.moving.getValue()) * Interface.glass()));
         context.drawLiquidGlass(x + width * GuiUtility.getPercent(this.animation.getValue(), ((SliderSetting)this.setting).getMin(), ((SliderSetting)this.setting).getMax()) - 4.5F - 3.0F * this.moving.getValue(), y + this.height - 11.0F - 3.0F - 2.0F * this.moving.getValue(), 9.0F + 6.0F * this.moving.getValue(), 6.0F + 4.0F * this.moving.getValue(), 7.0F, BorderRadius.all(3.0F + this.moving.getValue()), ColorRGBA.WHITE.withAlpha(255.0F * this.moving.getValue() * Interface.glass()), true);
      }

      if (Interface.showMinimalizm()) {
         context.drawShadow(x + width * GuiUtility.getPercent(this.animation.getValue(), ((SliderSetting)this.setting).getMin(), ((SliderSetting)this.setting).getMax()) - 3.0F, y + this.height - 14.0F + this.moving.getValue(), 6.0F, 6.0F - this.moving.getValue() * 2.0F, 10.0F, BorderRadius.all(3.0F - this.moving.getValue() * 2.0F), ColorRGBA.BLACK.withAlpha(63.75F * Interface.minimalizm()));
         context.drawLegacyRoundedRect(x + width * GuiUtility.getPercent(this.animation.getValue(), ((SliderSetting)this.setting).getMin(), ((SliderSetting)this.setting).getMax()) - 3.0F, y + this.height - 14.0F + this.moving.getValue(), 6.0F, 6.0F - this.moving.getValue() * 2.0F, BorderRadius.all(3.0F - this.moving.getValue() * 2.0F), ColorRGBA.WHITE.withAlpha(255.0F * Interface.minimalizm()));
      }

      String var10000 = TextUtility.formatNumberClean((double)this.animation.getValue());
      String value = var10000 + ((SliderSetting)this.setting).getSuffix();
      context.drawFadeoutText(nameFont, Localizator.translate(((SliderSetting)this.setting).getName()), this.x + leftPadding, y + 11.0F - nameFont.height(), Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())), 0.8F, 1.0F, this.getParent().getWidth() - leftPadding - Fonts.REGULAR.getFont(7.0F).width(value) - 10.0F);
      context.drawRightText(Fonts.REGULAR.getFont(7.0F), value, x + width, y + 11.0F - nameHeight, Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())));
      if (this.isHovered((float)context.getMouseX(), (float)context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      this.moving.setDuration(200L);
      this.moving.update(this.drag ? 1.0F : 0.0F);
      if (this.drag) {
         float xValue = GuiUtility.getSliderValue(((SliderSetting)this.setting).getMin(), ((SliderSetting)this.setting).getMax(), x, width, (double)context.getMouseX());
         ((SliderSetting)this.setting).setCurrentValue(xValue);
         CursorUtility.set(CursorType.ARROW_HORIZONTAL);
         current = this;
      }

   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY)) {
         this.drag = true;
      }

      super.onVisibleMouseClicked(mouseX, mouseY, button);
   }

   protected void onVisibleMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.drag = false;
      super.onVisibleMouseReleased(mouseX, mouseY, button);
   }

   protected void onVisibleKeyPressed(int keyCode, int scanCode, int modifiers) {
      if ((keyCode == 262 || keyCode == 263) && current == this) {
         ((SliderSetting)current.getSetting()).setCurrentValue(((SliderSetting)current.getSetting()).getCurrentValue() + ((SliderSetting)current.getSetting()).getStep() * 0.7F * (float)(keyCode == 262 ? 1 : -1));
      }

   }

   public float getHeight() {
      return this.height = 29.0F;
   }
}
