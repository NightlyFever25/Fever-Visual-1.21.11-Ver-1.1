package fever.visual.ui.menu.dropdown.components.settings;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.setting.Setting;
import fever.visual.ui.components.popup.Popup;
import fever.visual.ui.menu.dropdown.DropDownScreen;
import fever.visual.ui.menu.dropdown.components.module.ModuleComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import lombok.Generated;

public abstract class MenuSettingComponent<T extends Setting> extends CustomComponent {
   private final CustomComponent parent;
   protected final T setting;
   private final Animation visibilityAnimation;
   protected final Animation hoverAnimation;

   public MenuSettingComponent(T setting, CustomComponent parent) {
      this.visibilityAnimation = new Animation(300L, Easing.BAKEK_PAGES);
      this.hoverAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
      this.parent = parent;
      this.setting = setting;
   }

   public void update(UIContext context) {
      if (!this.setting.isVisible()) {
         super.update(context);
      } else {
         String translatedDescription = Localizator.translateOrEmpty(this.setting.getDescription());
         if (this.parent instanceof ModuleComponent
                 && this.isHovered(context)
                 && FeverVisual.getInstance().getMenuScreen() instanceof DropDownScreen screen) {
            screen.setDesc(translatedDescription);
         }

         if (this.parent instanceof Popup && this.isHovered(context)) {
            FeverVisual.getInstance().getHud().setDesc(Localizator.translate(translatedDescription));
         }

         super.update(context);
      }
   }

   public void onInit() {
      super.onInit();
   }

   public float getOpacity() {
      return this.visibilityAnimation.getValue();
   }

   public void drawRegular8(UIContext context) {
   }

   public void drawSplit(UIContext context) {
   }

   public final void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.setting.isVisible()) {
         this.onVisibleMouseClicked(mouseX, mouseY, button);
      }

   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      super.onMouseClicked(mouseX, mouseY, button);
   }

   public final void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (this.setting.isVisible()) {
         this.onVisibleMouseReleased(mouseX, mouseY, button);
      }

   }

   protected void onVisibleMouseReleased(double mouseX, double mouseY, MouseButton button) {
      super.onMouseReleased(mouseX, mouseY, button);
   }

   public final void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.setting.isVisible()) {
         this.onVisibleScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
      }

   }

   protected void onVisibleScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      super.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public final void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.setting.isVisible()) {
         this.onVisibleKeyPressed(keyCode, scanCode, modifiers);
      }

   }

   protected void onVisibleKeyPressed(int keyCode, int scanCode, int modifiers) {
      super.onKeyPressed(keyCode, scanCode, modifiers);
   }

   public final boolean charTyped(char chr, int modifiers) {
      return this.setting.isVisible() && this.onVisibleCharTyped(chr, modifiers);
   }

   protected boolean onVisibleCharTyped(char chr, int modifiers) {
      return super.charTyped(chr, modifiers);
   }

   @Generated
   public CustomComponent getParent() {
      return this.parent;
   }

   @Generated
   public T getSetting() {
      return this.setting;
   }

   @Generated
   public Animation getVisibilityAnimation() {
      return this.visibilityAnimation;
   }

   @Generated
   public Animation getHoverAnimation() {
      return this.hoverAnimation;
   }
}
