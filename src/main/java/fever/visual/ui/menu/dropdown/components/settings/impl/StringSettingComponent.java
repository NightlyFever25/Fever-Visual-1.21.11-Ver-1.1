package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.setting.settings.StringSetting;
import fever.visual.ui.components.textfield.TextField;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;

public class StringSettingComponent extends MenuSettingComponent<StringSetting> {
   private TextField textField;

   public StringSettingComponent(StringSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   public void onInit() {
      this.width = 13.0F;
      this.height = 8.0F;
      this.textField = new TextField(Fonts.REGULAR.getFont(8.0F));
      this.textField.paste(((StringSetting)this.setting).getText());
      this.textField.setPreview(Localizator.translate("type_text"));
      super.onInit();
   }

   public void update(UIContext context) {
      super.update(context);
   }

   protected void renderComponent(UIContext context) {
      float x = this.x + 8.0F;
      float y = this.y + 15.0F;
      float width = this.width - 16.0F;
      float height = this.height - 20.0F;
      this.hoverAnimation.update(this.isHovered((float)context.getMouseX(), (float)context.getMouseY()));
      if (this.isHovered((float)context.getMouseX(), (float)context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      float checkWidth = 13.0F;
      float checkHeight = 8.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float nameHeight = nameFont.height();
      float headerHeight = 19.0F;
      context.drawFadeoutText(nameFont, Localizator.translate(((StringSetting)this.setting).getName()), this.x + leftPadding, this.y + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight) - 0.5F, Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())), 0.7F, 0.99F, width - checkWidth - 20.0F);
      context.drawRoundedRect(x, y, width, height, BorderRadius.all(4.0F), Colors.getBackgroundColor().withAlpha(76.5F));
      this.textField.set(x, y, width, height);
      this.textField.setAlpha(1.0F);
      this.textField.setTextColor(Colors.getTextColor());
      this.textField.render(context);
      ((StringSetting)this.setting).text(this.textField.getBuiltText());
   }

   public void drawRegular8(UIContext context) {
   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   protected void onVisibleKeyPressed(int keyCode, int scanCode, int modifiers) {
      this.textField.onKeyPressed(keyCode, scanCode, modifiers);
   }

   protected boolean onVisibleCharTyped(char chr, int modifiers) {
      return this.textField.charTyped(chr, modifiers);
   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseClicked(mouseX, mouseY, button);
      super.onVisibleMouseReleased(mouseX, mouseY, button);
   }

   protected void onVisibleMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseReleased(mouseX, mouseY, button);
   }

   public float getHeight() {
      return this.height = 35.0F;
   }
}
