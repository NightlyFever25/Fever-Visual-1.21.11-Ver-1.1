package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.ui.components.ColorPicker;
import fever.visual.ui.menu.MenuScreen;
import fever.visual.ui.menu.dropdown.DropDownScreen;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;

public class ColorSettingComponent extends MenuSettingComponent<ColorSetting> {
   private static final float COLOR_DOT_SIZE = 5.0F;
   private ColorPicker picker;

   public ColorSettingComponent(ColorSetting setting, CustomComponent parent) {
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

      float checkWidth = 13.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float headerHeight = 19.0F;
      context.drawFadeoutText(nameFont, Localizator.translate(((ColorSetting)this.setting).getName()), this.x + leftPadding, this.y + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight) - 0.5F, Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())), 0.7F, 0.99F, this.width - checkWidth - 20.0F);
      float dotX = this.x + this.width - leftPadding - 5.0F;
      float dotY = this.y + headerHeight / 2.0F;
      context.drawLegacyRoundedRect(
              dotX - COLOR_DOT_SIZE / 2.0F,
              dotY - COLOR_DOT_SIZE / 2.0F,
              COLOR_DOT_SIZE,
              COLOR_DOT_SIZE,
              BorderRadius.all(COLOR_DOT_SIZE / 2.0F),
              ((ColorSetting)this.setting).getColorSafe()
      );
      if (this.picker != null) {
         ((ColorSetting)this.setting).color(this.picker.built());
      }

   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         MenuScreen var7 = FeverVisual.getInstance().getMenuScreen();
         if (var7 instanceof DropDownScreen) {
            DropDownScreen dropDownScreen = (DropDownScreen)var7;
            if (this.picker != null) {
               dropDownScreen.getColorPickers().remove(this.picker);
            }

            dropDownScreen.getColorPickers().add(this.picker = new ColorPicker((float)mouseX, (float)mouseY, 6.0F, ((ColorSetting)this.setting).isAlpha(), ((ColorSetting)this.setting).getColor(), Localizator.translate(((ColorSetting)this.setting).getName())));
         }
      }

      super.onVisibleMouseClicked(mouseX, mouseY, button);
   }

   public float getHeight() {
      return this.height = 18.0F;
   }
}
