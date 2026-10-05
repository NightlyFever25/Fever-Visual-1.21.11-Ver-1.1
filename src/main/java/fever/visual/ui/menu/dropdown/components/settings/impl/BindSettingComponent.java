package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.setting.settings.BindSetting;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.animation.types.ColorAnimation;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.ScissorUtility;
import lombok.Generated;

public class BindSettingComponent extends MenuSettingComponent<BindSetting> {
   private final ColorAnimation bindColorAnimation;
   private final Animation widthAnimation;
   private Animation changeAnimation;
   private int prevKey;
   private boolean bindingMode;

   public BindSettingComponent(BindSetting setting, CustomComponent parent) {
      super(setting, parent);
      this.bindColorAnimation = new ColorAnimation(300L, new ColorRGBA(24.0F, 24.0F, 27.0F), Easing.FIGMA_EASE_IN_OUT);
      this.widthAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
      this.changeAnimation = new Animation(300L, 1.0F, Easing.FIGMA_EASE_IN_OUT);
   }

   protected void renderComponent(UIContext context) {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      Font keyFont = Fonts.REGULAR.getFont(7.0F);
      float leftPadding = 10.0F;
      float headerHeight = 19.0F;
      this.bindColorAnimation.update(this.bindingMode ? Colors.getAccentColor() : Colors.getTextColor());
      this.changeAnimation.setDuration(500L);
      this.changeAnimation.update(1.0F);
      String key = TextUtility.getKeyName(((BindSetting)this.setting).getKey());
      String prev = TextUtility.getKeyName(this.prevKey);
      float keyWidth = keyFont.width(key) + 7.0F;
      this.widthAnimation.update(keyWidth);
      context.drawRoundedRect(this.x + this.width - 9.0F - this.widthAnimation.getValue(), this.y + 4.0F, this.widthAnimation.getValue(), 11.0F, BorderRadius.all(3.0F), Colors.getAdditionalColor());
      ScissorUtility.push(context.getMatrices(), this.x + this.width - 9.0F - this.widthAnimation.getValue(), this.y + 4.0F, this.widthAnimation.getValue(), 11.0F);
      context.drawText(keyFont, prev, this.x + this.width - 9.0F - this.widthAnimation.getValue() + 4.0F + 4.0F * this.changeAnimation.getValue(), this.y + 7.0F, this.bindColorAnimation.getColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()) * (1.0F - this.changeAnimation.getValue())));
      context.drawText(keyFont, key, this.x + this.width - 9.0F - this.widthAnimation.getValue() + 4.0F - 4.0F + 4.0F * this.changeAnimation.getValue(), this.y + 7.0F, this.bindColorAnimation.getColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()) * this.changeAnimation.getValue()));
      ScissorUtility.pop();
      context.drawFadeoutText(nameFont, Localizator.translate(((BindSetting)this.setting).getName()), this.x + leftPadding, this.y + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight), Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())), 0.7F, 0.99F, this.width - this.widthAnimation.getValue() - 20.0F);
      if (this.isHovered(context)) {
         CursorUtility.set(CursorType.HAND);
      }

   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         this.bindingMode = !this.bindingMode;
      }

      if (this.bindingMode && button != MouseButton.LEFT) {
         int buttonIndex = button.getButtonIndex();
         ((BindSetting)this.setting).setKey(buttonIndex);
         this.bindingMode = false;
      }

      super.onVisibleMouseClicked(mouseX, mouseY, button);
   }

   protected void onVisibleKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.bindingMode) {
         super.onVisibleKeyPressed(keyCode, scanCode, modifiers);
      } else {
         this.prevKey = ((BindSetting)this.setting).getKey();
         if (keyCode != 256 && keyCode != 261) {
            ((BindSetting)this.setting).setKey(keyCode);
         } else {
            ((BindSetting)this.setting).setKey(-1);
         }

         this.changeAnimation = new Animation(500L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
         this.bindingMode = false;
      }

   }

   public float getHeight() {
      return this.height = 19.0F;
   }

   @Generated
   public void setBindingMode(boolean bindingMode) {
      this.bindingMode = bindingMode;
   }
}
