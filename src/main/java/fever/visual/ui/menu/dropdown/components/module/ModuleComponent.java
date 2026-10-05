package fever.visual.ui.menu.dropdown.components.module;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.Module;
import fever.visual.systems.modules.modules.other.Sounds;
import fever.visual.systems.setting.Setting;
import fever.visual.ui.menu.dropdown.DropDownScreen;
import fever.visual.ui.menu.dropdown.components.MenuPanel;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.ScissorUtility;
import fever.visual.utility.sounds.ClientSounds;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;

/** State and controls for one module row in the dashboard. */
public class ModuleComponent extends CustomComponent {
   private final Module module;
   private final MenuPanel parent;
   private final Animation hoverAnimation = new Animation(180L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation enableAnimation = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation expandAnimation = new Animation(260L, Easing.BAKEK_PAGES);
   private final List<MenuSettingComponent<?>> settingComponents = new ArrayList<>();
   private float headerHeight = 22.0F;
   private boolean bindingMode;
   private boolean expanded;

   public ModuleComponent(Module module, MenuPanel parent) {
      this.module = module;
      this.parent = parent;
   }

   @Override
   public void onInit() {
      this.settingComponents.clear();
      for (Setting setting : this.module.getSettings()) {
         MenuSettingComponent<?> component = GuiUtility.settinge(setting, this);
         if (component != null) {
            component.onInit();
            component.getVisibilityAnimation().setValue(component.getSetting().isVisible() ? 1.0F : 0.0F);
            this.settingComponents.add(component);
         }
      }
      this.height = this.headerHeight;
   }

   public void prepare(UIContext context, float x, float y, float width, float headerHeight) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.headerHeight = headerHeight;
      this.enableAnimation.update(this.module.isEnabled() ? 1.0F : 0.0F);
      this.hoverAnimation.update(this.isHeaderHovered(context.getMouseX(), context.getMouseY()) ? 1.0F : 0.0F);
      this.expandAnimation.update(this.expanded ? 1.0F : 0.0F);
      this.height = this.headerHeight + this.getVisibleSettingsHeight() * this.expandAnimation.getValue();

      if (this.isHeaderHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
         if (FeverVisual.getInstance().getMenuScreen() instanceof DropDownScreen screen) {
            screen.setDesc(Localizator.translate(this.module.getDescription()));
         }
      }
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.prepare(context, this.x, this.y, this.width, this.headerHeight);
      this.renderInlineSettings(context);
   }

   public void renderInlineSettings(UIContext context) {
      float shownHeight = this.height - this.headerHeight;
      if (shownHeight <= 0.25F) {
         return;
      }

      ScissorUtility.push(context.getMatrices(), this.x, this.y + this.headerHeight, this.width, shownHeight);
      float settingY = this.y + this.headerHeight;
      for (MenuSettingComponent<?> component : this.settingComponents) {
         boolean visible = component.getSetting().isVisible();
         component.getVisibilityAnimation().update(visible ? 1.0F : 0.0F);
         if (!visible) {
            continue;
         }
         component.set(this.x, settingY, this.width, component.getHeight());
         component.render(context);
         component.drawRegular8(context);
         component.drawSplit(context);
         settingY += component.getHeight();
      }
      ScissorUtility.pop();
   }

   public float getVisibleSettingsHeight() {
      float total = 0.0F;
      for (MenuSettingComponent<?> component : this.settingComponents) {
         if (component.getSetting().isVisible()) {
            total += component.getHeight();
         }
      }
      return total;
   }

   public float getAnimatedHeight() {
      return this.headerHeight + this.getVisibleSettingsHeight() * this.expandAnimation.getValue();
   }

   // Kept for the legacy MenuPanel contract. The dashboard owns row rendering.
   public void drawRegular8(UIContext context) { }
   public void drawIcons(UIContext context) { }
   public void drawSettingsIndicator(UIContext context) { }
   public void drawSplit(UIContext context) { }

   public boolean isHeaderHovered(double mouseX, double mouseY) {
      return GuiUtility.isHovered(this.x, this.y, this.width, this.headerHeight, mouseX, mouseY);
   }

   @Override
   public boolean isHovered(double mouseX, double mouseY) {
      return GuiUtility.isHovered(this.x, this.y, this.width, this.height, mouseX, mouseY);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHeaderHovered(mouseX, mouseY)) {
         if (this.bindingMode && button != MouseButton.LEFT && button != MouseButton.RIGHT) {
            this.module.setKey(button.getButtonIndex());
            this.bindingMode = false;
            return;
         }

         switch (button) {
            case LEFT -> this.module.toggle();
            case MIDDLE -> {
               if (this.parent != null) {
                  this.parent.getModuleComponents().forEach(component -> component.setBindingMode(false));
               }
               this.bindingMode = true;
            }
            case RIGHT -> this.open();
         }
         return;
      }

      if (!this.expanded || mouseY < this.y + this.headerHeight || mouseY > this.y + this.height) {
         return;
      }
      for (int i = this.settingComponents.size() - 1; i >= 0; i--) {
         MenuSettingComponent<?> component = this.settingComponents.get(i);
         if (component.getSetting().isVisible() && component.isHovered(mouseX, mouseY)) {
            component.onMouseClicked(mouseX, mouseY, button);
            return;
         }
      }
   }

   public void open() {
      if (this.settingComponents.isEmpty()) {
         Sounds sounds = FeverVisual.getInstance().getModuleManager().getModule(Sounds.class);
         if (sounds != null && sounds.isEnabled()) {
            ClientSounds.CRITICAL.play(1.0F, 1.0F);
         }
         return;
      }
      this.expanded = !this.expanded;
      Sounds sounds = FeverVisual.getInstance().getModuleManager().getModule(Sounds.class);
      if (sounds != null && sounds.isEnabled()) {
         ClientSounds.CLICKGUI_OPEN.play(0.8F, 1.3F);
      }
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      for (MenuSettingComponent<?> component : this.settingComponents) {
         if (component.getSetting().isVisible()) {
            component.onMouseReleased(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (!this.expanded) {
         return;
      }
      for (MenuSettingComponent<?> component : this.settingComponents) {
         if (component.getSetting().isVisible() && component.isHovered(mouseX, mouseY)) {
            component.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
            return;
         }
      }
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.bindingMode) {
         this.module.setKey(keyCode == 256 || keyCode == 261 ? -1 : keyCode);
         this.bindingMode = false;
         if (FeverVisual.getInstance().getMenuScreen() instanceof DropDownScreen screen) {
            screen.getSearchField().setFocused(false);
         }
         return;
      }
      if (this.expanded) {
         for (MenuSettingComponent<?> component : this.settingComponents) {
            if (component.getSetting().isVisible()) {
               component.onKeyPressed(keyCode, scanCode, modifiers);
            }
         }
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (!this.expanded) {
         return false;
      }
      for (MenuSettingComponent<?> component : this.settingComponents) {
         if (component.getSetting().isVisible() && component.charTyped(chr, modifiers)) {
            return true;
         }
      }
      return false;
   }

   public void collapse() {
      this.expanded = false;
   }

   @Generated public Module getModule() { return this.module; }
   @Generated public MenuPanel getParent() { return this.parent; }
   @Generated public Animation getHoverAnimation() { return this.hoverAnimation; }
   @Generated public Animation getEnableAnimation() { return this.enableAnimation; }
   @Generated public Animation getExpandAnimation() { return this.expandAnimation; }
   @Generated public List<MenuSettingComponent<?>> getSettingComponents() { return this.settingComponents; }
   @Generated public float getHeaderHeight() { return this.headerHeight; }
   @Generated public boolean isBindingMode() { return this.bindingMode; }
   @Generated public void setBindingMode(boolean bindingMode) { this.bindingMode = bindingMode; }
   @Generated public boolean isExpanded() { return this.expanded; }
   @Generated public void setExpanded(boolean expanded) { this.expanded = expanded; }
}
