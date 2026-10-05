package fever.visual.ui.components.popup;

import fever.visual.utility.render.compat.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.setting.Setting;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.components.popup.list.Button;
import fever.visual.ui.components.popup.list.CheckBox;
import fever.visual.ui.components.popup.list.Separator;
import fever.visual.ui.components.popup.list.Text;
import fever.visual.ui.components.popup.list.Title;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.RenderUtility;

public class Popup extends CustomComponent {
   private final Animation animation = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation blurAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final List<CustomComponent> components = new ArrayList<>();
   private boolean showing;
   private final float offsetFactor;
   private Runnable onClose = () -> {};
   private boolean closed;

   public Popup(float x, float y) {
      this(x, y, 90.0F);
   }

   public Popup(float x, float y, float width) {
      this(x, y, width, 2.0F);
   }

   public Popup(float x, float y, float width, float offsetFactor) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.offsetFactor = offsetFactor;
      this.showing = true;
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.animation.setEasing(this.showing ? Easing.BAKEK : Easing.BAKEK_BACK);
      this.animation.update(this.showing);
      this.blurAnim.update(this.animation.getValue() >= 0.6F);
      this.height = 0.0F;

      for (int componentIndex = 0; componentIndex < this.components.size(); componentIndex++) {
         CustomComponent component = this.components.get(componentIndex);
         if (component instanceof MenuSettingComponent settingComponent) {
            settingComponent.getVisibilityAnimation().update(settingComponent.getSetting().isVisible() ? 1.0F : 0.0F);
            component.set(this.x - 2.0F, this.y + this.height, this.width + 4.0F, 0.0F);
         } else {
            component.set(this.x, this.y + this.height, this.width, 0.0F);
         }

         this.height = this.height
            + (component.getHeight() + 0.5F) * (component instanceof MenuSettingComponent settingComponent ? settingComponent.getOpacity() : 1.0F);
      }

      this.height += 2.0F;
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, this.animation.getValue()));
      RenderUtility.scale(
         context.getMatrices(), this.x + this.width / this.offsetFactor, this.y + this.height / this.offsetFactor, 0.5F + this.animation.getValue() * 0.5F
      );
      context.drawShadow(this.x, this.y, this.width, this.height, 15.0F, BorderRadius.all(6.0F), ColorRGBA.BLACK.withAlpha(127.5F));
      if (Interface.showMinimalizm()) {
         context.drawBlurredRect(
            this.x,
            this.y,
            this.width,
            this.height,
            45.0F,
            7.0F,
            BorderRadius.all(6.0F),
            ColorRGBA.WHITE.withAlpha(255.0F * this.animation.getValue() * Interface.minimalizm())
         );
      }

      if (Interface.showGlass()) {
         context.drawLiquidGlass(
            this.x,
            this.y,
            this.width,
            this.height,
            7.0F,
            0.08F,
            BorderRadius.all(6.0F),
            ColorRGBA.WHITE.withAlpha(255.0F * this.animation.getValue() * Interface.glass())
         );
      }

      boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      context.drawSquircle(
         this.x,
         this.y,
         this.width,
         this.height,
         7.0F,
         BorderRadius.all(6.0F),
         Colors.getBackgroundColor().withAlpha(255.0F * (dark ? 0.8F - 0.6F * Interface.glass() : 0.7F))
      );

      for (int componentIndex = 0; componentIndex < this.components.size(); componentIndex++) {
         CustomComponent component = this.components.get(componentIndex);
         if (component instanceof MenuSettingComponent settingComponent) {
            if (settingComponent.getOpacity() <= 0.001F) {
               continue;
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, settingComponent.getOpacity() * this.animation.getValue());
         }

         CustomComponent previousVisibleComponent = this.getPreviousVisibleComponent(componentIndex);
         if (previousVisibleComponent != null && !(component instanceof Separator) && !(previousVisibleComponent instanceof Separator)) {
            float separatorHeight = 0.5F;
            context.drawRect(this.x, component.getY() - 1.0F, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
         }

         component.render(context);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.animation.getValue());
      }

      for (CustomComponent component : this.components) {
         if (component instanceof MenuSettingComponent comp) {
            comp.drawRegular8(context);
         }
      }

      RenderUtility.end(context.getMatrices());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private CustomComponent getPreviousVisibleComponent(int componentIndex) {
      for (int i = componentIndex - 1; i >= 0; i--) {
         CustomComponent component = this.components.get(i);
         if (!(component instanceof MenuSettingComponent settingComponent) || settingComponent.getOpacity() > 0.001F) {
            return component;
         }
      }

      return null;
   }

   public Popup setting(Setting setting) {
      MenuSettingComponent settingComponent = GuiUtility.settinge(setting, this);
      if (settingComponent != null) {
         this.components.add(settingComponent);
      }

      return this;
   }

   public Popup add(CustomComponent component) {
      this.components.add(component);
      return this;
   }

   public Popup text(String text) {
      this.components.add(new Text(text));
      return this;
   }

   public Popup title(String text) {
      this.components.add(new Title(text));
      return this;
   }

   public Popup separator() {
      this.components.add(new Separator());
      return this;
   }

   public Popup checkbox(String text, boolean enabled) {
      this.components.add(new CheckBox(text).enabled(enabled));
      return this;
   }

   public Popup checkbox(String text, boolean enabled, CheckBoxAction action) {
      this.components.add(new CheckBox(text).enabled(enabled).action(action));
      return this;
   }

   public Popup button(String text, String icon, PopupAction runnable) {
      this.components.add(new Button(this, text, icon, runnable));
      return this;
   }

   public Popup onClose(Runnable onClose) {
      this.onClose = onClose;
      return this;
   }

   public void setShowing(boolean showing) {
      this.showing = showing;
      if (!showing && !this.closed) {
         this.onClose.run();
         this.closed = true;
      }
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.onMouseClicked(mouseX, mouseY, button);
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.onMouseReleased(mouseX, mouseY, button);
         }
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
         }
      }

      super.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.onKeyPressed(keyCode, scanCode, modifiers);
         }
      }

      super.onKeyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.charTyped(chr, modifiers);
         }
      }

      return super.charTyped(chr, modifiers);
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public boolean isShowing() {
      return this.showing;
   }
}
