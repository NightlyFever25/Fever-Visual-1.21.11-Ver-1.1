package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.setting.settings.ButtonSetting;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;

public class ButtonSettingComponent extends MenuSettingComponent<ButtonSetting> {
   public ButtonSettingComponent(ButtonSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   public void onInit() {
      this.width = 13.0F;
      this.height = 8.0F;
      super.onInit();
   }

   public void update(UIContext context) {
      super.update(context);
   }

   protected void renderComponent(UIContext context) {
      this.hoverAnimation.update(this.isHovered((float)context.getMouseX(), (float)context.getMouseY()));
      if (this.isHovered((float)context.getMouseX(), (float)context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      context.drawRoundedRect(this.x + 7.0F, this.y + 4.0F, this.width - 14.0F, this.height - 7.0F, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(255.0F * (0.3F + 0.2F * this.hoverAnimation.getValue())));
      context.drawCenteredText(nameFont, Localizator.translate(((ButtonSetting)this.setting).getName()), this.x + this.width / 2.0F, this.y + GuiUtility.getMiddleOfBox(nameFont.height(), this.height) - 0.5F, Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())));
   }

   public void drawRegular8(UIContext context) {
   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         ((ButtonSetting)this.setting).getAction().run();
      }

      super.onVisibleMouseReleased(mouseX, mouseY, button);
   }

   public float getHeight() {
      return this.height = 24.0F;
   }
}
