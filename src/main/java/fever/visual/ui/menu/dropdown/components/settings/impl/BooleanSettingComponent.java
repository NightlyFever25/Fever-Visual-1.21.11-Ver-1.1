package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.animation.types.ColorAnimation;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;

public class BooleanSettingComponent extends MenuSettingComponent<BooleanSetting> {
   private Animation circleOpacityAnimation;
   private Animation enableAnimation;
   private ColorAnimation backgroundColorAnimation;

   public BooleanSettingComponent(BooleanSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   public void onInit() {
      this.circleOpacityAnimation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
      this.enableAnimation = new Animation(300L, Easing.BAKEK);
      this.backgroundColorAnimation = new ColorAnimation(300L, new ColorRGBA(24.0F, 24.0F, 27.0F), Easing.FIGMA_EASE_IN_OUT);
      this.width = 13.0F;
      this.height = 8.0F;
      super.onInit();
   }

   public void update(UIContext context) {
      super.update(context);
   }

   protected void renderComponent(UIContext context) {
      this.circleOpacityAnimation.update(((BooleanSetting)this.setting).isEnabled() ? 1.0F : 0.75F);
      this.enableAnimation.update(((BooleanSetting)this.setting).isEnabled() ? 1.0F : 0.0F);
      this.backgroundColorAnimation.update(((BooleanSetting)this.setting).isEnabled() ? Colors.getAccentColor() : FeverVisual.getInstance().getThemeManager().getCurrentTheme().getAdditionalColor());
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
      context.drawFadeoutText(nameFont, Localizator.translate(((BooleanSetting)this.setting).getName()), this.x + leftPadding, this.y + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight) - 0.5F, Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.enableAnimation.getValue() + 0.25F * this.hoverAnimation.getValue())), 0.7F, 0.99F, this.width - checkWidth - 20.0F);
      context.drawLegacyRoundedRect(this.x + this.width - checkWidth - 9.0F, this.y + 5.0F, checkWidth, checkHeight, BorderRadius.all(3.0F), this.backgroundColorAnimation.getColor().withAlpha(!((BooleanSetting)this.setting).isEnabled() ? 255.0F - 100.0F * Interface.glass() : 255.0F));
      context.drawLegacyRoundedRect(this.x + this.width - checkWidth - 8.0F + 5.0F * this.enableAnimation.getValue(), this.y + 6.0F, 6.0F, 6.0F, BorderRadius.all(4.0F), (new ColorRGBA(255.0F, 255.0F, 255.0F)).withAlpha(this.circleOpacityAnimation.getValue() * 255.0F));
   }

   public void drawRegular8(UIContext context) {
   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         ((BooleanSetting)this.setting).toggle();
      }

      super.onVisibleMouseReleased(mouseX, mouseY, button);
   }

   public float getHeight() {
      return this.height = 18.0F;
   }
}
