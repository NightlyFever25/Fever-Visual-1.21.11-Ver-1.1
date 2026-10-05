package fever.visual.ui.menu.dropdown.components;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.modules.Module;
import fever.visual.systems.modules.modules.other.Sounds;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.modules.modules.visuals.MenuModule;
import fever.visual.systems.setting.Setting;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.RangeSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.components.textfield.TextField;
import fever.visual.ui.menu.MenuScreen;
import fever.visual.ui.menu.api.MenuCategory;
import fever.visual.ui.menu.dropdown.DropDownScreen;
import fever.visual.ui.menu.dropdown.components.module.ModuleComponent;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.gui.ScrollHandler;
import fever.visual.utility.interfaces.IScaledResolution;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.ScissorUtility;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.utility.sounds.ClientSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Generated;

public class MenuPanel extends CustomComponent implements IScaledResolution {
   private enum SettingGroup {
      MODE,
      COLOR,
      TOGGLE,
      VALUE,
      SELECT,
      OTHER
   }

   private final MenuCategory category;
   private final Animation swapping;
   private final Animation sizing;
   private final List<ModuleComponent> moduleComponents;
   private final ScrollHandler modulesScroll;
   private final ScrollHandler settingsScroll;
   private Font titleFont;
   private ModuleComponent lastSelected;
   private ModuleComponent selectedModuleComponent;

   public MenuPanel(MenuCategory category) {
      this.swapping = new Animation(500L, Easing.BAKEK_PAGES);
      this.sizing = new Animation(500L, Easing.BAKEK_SMALLER);
      this.moduleComponents = new ArrayList();
      this.modulesScroll = new ScrollHandler();
      this.settingsScroll = new ScrollHandler();
      this.category = category;
   }

   public void onInit() {
      for(Module module : FeverVisual.getInstance().getModuleManager().getModules().stream().sorted(Comparator.comparing(Module::getName)).filter((modulex) -> modulex.getCategory().equals(this.category.getCategory())).toList()) {
         ModuleComponent component = new ModuleComponent(module, this);
         component.setWidth(this.width);
         component.setHeight(20.0F);
         this.moduleComponents.add(component);
         component.onInit();
      }

      this.titleFont = Fonts.SEMIBOLD.getFont(9.0F);
      this.modulesScroll.reset();
      this.settingsScroll.reset();
      super.onInit();
   }

   public void update(UIContext context) {
      super.update(context);
   }

   private void updateScale(UIContext context) {
      if (Interface.glassSelected()) {
         this.sizing.setEasing(Easing.BAKEK_MANY);
      } else {
         this.sizing.setEasing(Easing.BAKEK_SMALLER);
      }

      this.sizing.setDuration(500L);
      this.sizing.update(FeverVisual.getInstance().getMenuScreen().isClosing() ? 2.0F : (Math.abs(sr.getScaledWidth() / 2.0F - this.x) / 1500.0F * 3.0F < FeverVisual.getInstance().getMenuScreen().getMenuAnimation().getValue() ? 1.0F : 0.0F));
   }

   public void scale(UIContext context) {
      if (Interface.glassSelected()) {
         RenderUtility.scale(context.getMatrices(), sr.getScaledWidth() / 2.0F, this.y + this.height / 2.0F, 2.0F - this.sizing.getValue());
      } else {
         RenderUtility.scale(context.getMatrices(), this.x + this.width / 2.0F, this.y + this.height / 2.0F, 2.0F - this.sizing.getValue());
      }

   }

   public void renderBackground(UIContext context) {
      float alpha = FeverVisual.getInstance().getMenuScreen().isClosing() ? FeverVisual.getInstance().getMenuScreen().getMenuAnimation().getValue() : this.sizing.getValue();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      this.scale(context);
      context.drawRoundedRect(this.x + 1.0F, this.y + 1.0F, this.width - 2.0F, this.height - 2.0F, BorderRadius.all(10.0F), Colors.getBackgroundColor().withAlpha(255.0F * (FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK ? 0.55F : 0.7F)));
      RenderUtility.end(context.getMatrices());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void renderShadow(UIContext context) {
      float alpha = FeverVisual.getInstance().getMenuScreen().isClosing() ? FeverVisual.getInstance().getMenuScreen().getMenuAnimation().getValue() : this.sizing.getValue();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      this.scale(context);
      context.drawShadow(this.x, this.y, this.width, this.height, 25.0F, BorderRadius.all(10.0F), ColorRGBA.BLACK.withAlpha(51.0F));
      RenderUtility.end(context.getMatrices());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void renderBlur(UIContext context) {
      float alpha = FeverVisual.getInstance().getMenuScreen().isClosing() ? 2.0F - this.sizing.getValue() : this.sizing.getValue();
      this.scale(context);
      context.drawBlurredRect(this.x, this.y, this.width, this.height, 45.0F, 10.0F, BorderRadius.all(10.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      if (Interface.showGlass()) {
         MenuModule menuModule = (MenuModule)FeverVisual.getInstance().getModuleManager().getModule(MenuModule.class);
         if (menuModule != null) {
            ColorRGBA glassTop = menuModule.getGlassColorTop(alpha);
            ColorRGBA glassBottom = menuModule.getGlassColorBottom(alpha);
            context.drawLiquidGlass(this.x, this.y, this.width, this.height, 10.0F, 0.08F, BorderRadius.all(10.0F), glassTop, glassBottom);
         } else {
            context.drawLiquidGlass(this.x, this.y, this.width, this.height, 10.0F, 0.08F, BorderRadius.all(10.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
         }
      }

      RenderUtility.end(context.getMatrices());
   }

   public void push(UIContext context) {
      float headerHeight = 24.0F;
      float separatorHeight = 4.0F;
      float offset = Interface.glass() * 2.0F;
      if (this.selectedModuleComponent != null) {
         ScissorUtility.push(context.getMatrices(), this.x + offset, this.y + headerHeight * 2.0F + separatorHeight + offset, this.width - offset * 2.0F, this.height - headerHeight * 2.0F - separatorHeight - 0.5F - offset * 2.0F);
      } else {
         ScissorUtility.push(context.getMatrices(), this.x + offset, this.y + headerHeight + separatorHeight + offset, this.width - offset * 2.0F, this.height - headerHeight - separatorHeight - 0.5F - offset * 2.0F);
      }

   }

   protected void renderComponent(UIContext context) {
      this.modulesScroll.update();
      this.settingsScroll.update();
      float headerHeight = 24.0F;
      this.updateScale(context);
      float alpha = FeverVisual.getInstance().getMenuScreen().isClosing() ? 2.0F - this.sizing.getValue() : this.sizing.getValue();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      this.scale(context);
      boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      context.drawSquircle(this.x, this.y, this.width, this.height, 10.0F, BorderRadius.all(10.0F), Colors.getBackgroundColor().withAlpha(255.0F * (dark ? 0.9F - 0.7F * Interface.glass() : 0.7F)));
      float separatorHeight = 4.0F;
      float titleHeight = this.titleFont.height();
      String categoryKey = "menu.category." + this.category.name().toLowerCase();
      String categoryTitle = fever.visual.systems.localization.Localizator.translate(categoryKey);
      if (categoryTitle.equals(categoryKey)) {
         categoryTitle = this.category.getName();
      }
      float textWidth = this.titleFont.width(categoryTitle);
      context.drawText(this.titleFont, categoryTitle, this.x + this.width / 2.0F - textWidth / 2.0F, this.y + GuiUtility.getMiddleOfBox(titleHeight, headerHeight) + 0.5F, Colors.getTextColor());
      if (Interface.showMinimalizm()) {
         context.drawRect(this.x, this.y + headerHeight, this.width, separatorHeight, Colors.getSeparatorColor().withAlpha(Colors.getSeparatorColor().getAlpha() * Interface.minimalizm()));
      }

      if (this.selectedModuleComponent != null) {
         this.lastSelected = this.selectedModuleComponent;
      }

      this.swapping.update(this.selectedModuleComponent != null ? 1.0F : 0.0F);
      if (this.swapping.getValue() != 1.0F) {
         float x = this.x + -this.width * this.swapping.getValue();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * (1.0F - this.swapping.getValue()));
         ScissorUtility.push(context.getMatrices(), this.x, this.y + headerHeight + separatorHeight, this.width, this.height - headerHeight - separatorHeight - 0.5F);
         float offset = 0.0F;

         for(ModuleComponent moduleComponent : this.moduleComponents) {
            if (!this.searchCheck(moduleComponent)) {
               moduleComponent.setX(x);
               moduleComponent.setY((float)((double)(this.y + offset) - this.modulesScroll.getValue()) + headerHeight + separatorHeight - 1.0F);
               moduleComponent.setWidth(this.width);
               if (this.isVerticallyVisible(moduleComponent, this.y + headerHeight + separatorHeight, this.y + this.height)) {
                  moduleComponent.render(context);
               }
               offset += moduleComponent.getHeight();
            }
         }
         this.modulesScroll.setMax((double)(-offset + this.height - headerHeight - separatorHeight));

         ScissorUtility.pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      }

      if (this.swapping.getValue() != 0.0F) {
         float x = this.x + this.width * (1.0F - this.swapping.getValue());
         float y = this.y + headerHeight + separatorHeight;
         float leftPadding = 6.0F;
         float arrowIconSize = 8.0F;
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * this.swapping.getValue());
         ScissorUtility.push(context.getMatrices(), this.x, this.y, this.width, this.height);
         if (GuiUtility.isHovered((double)x, (double)(this.y + 28.0F), (double)this.width, (double)20.0F, context.getMouseX(), context.getMouseY())) {
            CursorUtility.set(CursorType.HAND);
         }

         context.drawTexture(FeverVisual.id("icons/arrow.png"), x + leftPadding, y + GuiUtility.getMiddleOfBox(arrowIconSize, headerHeight) - 2.0F, arrowIconSize, arrowIconSize, Colors.getTextColor());
         context.drawText(Fonts.REGULAR.getFont(8.0F), this.lastSelected.getModule().getName(), x + arrowIconSize + 8.0F, y + GuiUtility.getMiddleOfBox(arrowIconSize, headerHeight) - 1.0F, Colors.getTextColor().withAlpha(255.0F));
         if (Interface.showMinimalizm()) {
            context.drawRect(x, y + headerHeight - separatorHeight, this.width, separatorHeight, Colors.getSeparatorColor().withAlpha(Colors.getSeparatorColor().getAlpha() * Interface.minimalizm()));
         }

         ScissorUtility.pop();
         ScissorUtility.push(context.getMatrices(), this.x, y + headerHeight, this.width, this.height - headerHeight * 2.0F - separatorHeight - 0.5F - Interface.glass() * 5.0F);
         float settingsY = y + headerHeight;
         float offset = 0.0F;

         for(MenuSettingComponent<?> settingComponent : this.lastSelected.getSettingComponents()) {
            boolean visible = settingComponent.getSetting().isVisible();
            settingComponent.getVisibilityAnimation().update(visible ? 1.0F : 0.0F);
            if (visible) {
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.swapping.getValue());
               settingComponent.setX(x);
               settingComponent.setY((float)((double)(settingsY + offset) - this.settingsScroll.getValue()));
               settingComponent.setWidth(this.width);
               if (this.isVerticallyVisible(settingComponent, settingsY, this.y + this.height)) {
                  settingComponent.render(context);
               }
               offset += settingComponent.getHeight();
            }
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         this.settingsScroll.setMax((double)(-offset + this.height - headerHeight * 2.0F - separatorHeight - (float)(Interface.glassSelected() ? 5 : 0)));
         ScissorUtility.pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      }

      RenderUtility.end(context.getMatrices());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void drawRegular8(UIContext context) {
      float headerHeight = 24.0F;
      float alpha = FeverVisual.getInstance().getMenuScreen().isClosing() ? 2.0F - this.sizing.getValue() : this.sizing.getValue();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      float separatorHeight = 4.0F;
      if (this.selectedModuleComponent != null) {
         this.lastSelected = this.selectedModuleComponent;
      }

      if (this.swapping.getValue() != 1.0F) {
         float x = this.x + -this.width * this.swapping.getValue();

         for(ModuleComponent moduleComponent : this.moduleComponents) {
            if (!this.searchCheck(moduleComponent) && (GuiUtility.isHovered((double)x, (double)this.y, (double)this.width, (double)this.height, (double)moduleComponent.getX(), (double)moduleComponent.getY()) || GuiUtility.isHovered((double)x, (double)this.y, (double)this.width, (double)this.height, (double)moduleComponent.getX(), (double)(moduleComponent.getY() + moduleComponent.getHeight())))) {
               moduleComponent.drawRegular8(context);
            }
         }
      }

      if (this.swapping.getValue() != 0.0F) {
         float x = this.x + this.width * (1.0F - this.swapping.getValue());
         float y = this.y + headerHeight + separatorHeight;

         for(MenuSettingComponent<?> settingComponent : this.lastSelected.getSettingComponents()) {
            if (settingComponent.getSetting().isVisible() && (GuiUtility.isHovered((double)x, (double)y, (double)this.width, (double)this.height, (double)settingComponent.getX(), (double)settingComponent.getY()) || GuiUtility.isHovered((double)x, (double)y, (double)this.width, (double)this.height, (double)settingComponent.getX(), (double)(settingComponent.getY() + settingComponent.getHeight())))) {
               settingComponent.drawRegular8(context);
            }
         }
      }

      if (this.swapping.getValue() != 0.0F) {
         float x = this.x + this.width * (1.0F - this.swapping.getValue());
         float y = this.y + headerHeight + separatorHeight;
         List<MenuSettingComponent<?>> visibleSettings = this.getVisibleSettingComponents();

         for(int i = 0; i < visibleSettings.size() - 1; i++) {
            MenuSettingComponent<?> settingComponent = visibleSettings.get(i);
            MenuSettingComponent<?> nextSettingComponent = visibleSettings.get(i + 1);
            if (this.shouldDrawSettingsSeparator(settingComponent.getSetting(), nextSettingComponent.getSetting())
                    && (GuiUtility.isHovered((double)x, (double)y, (double)this.width, (double)this.height, (double)settingComponent.getX(), (double)settingComponent.getY())
                    || GuiUtility.isHovered((double)x, (double)y, (double)this.width, (double)this.height, (double)settingComponent.getX(), (double)(settingComponent.getY() + settingComponent.getHeight())))) {
               settingComponent.drawSplit(context);
            }
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void drawIcons(UIContext context) {
      float alpha = FeverVisual.getInstance().getMenuScreen().isClosing() ? 2.0F - this.sizing.getValue() : this.sizing.getValue();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      if (this.selectedModuleComponent != null) {
         this.lastSelected = this.selectedModuleComponent;
      }

      if (this.swapping.getValue() != 1.0F) {
         float x = this.x + -this.width * this.swapping.getValue();

         for(ModuleComponent moduleComponent : this.moduleComponents) {
            if (!this.searchCheck(moduleComponent) && this.fevervisual$isComponentInView(x, this.y, moduleComponent)) {
               moduleComponent.drawIcons(context);
               moduleComponent.drawSettingsIndicator(context);
            }
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void drawType(UIContext context) {
      float alpha = FeverVisual.getInstance().getMenuScreen().isClosing() ? 2.0F - this.sizing.getValue() : this.sizing.getValue();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      this.scale(context);
      RenderUtility.end(context.getMatrices());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void drawSplit(UIContext context) {
      float headerHeight = 24.0F;
      float alpha = FeverVisual.getInstance().getMenuScreen().isClosing() ? 2.0F - this.sizing.getValue() : this.sizing.getValue();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      float separatorHeight = 4.0F;
      if (this.selectedModuleComponent != null) {
         this.lastSelected = this.selectedModuleComponent;
      }

      if (this.swapping.getValue() != 1.0F) {
         float x = this.x + -this.width * this.swapping.getValue();

         for(ModuleComponent moduleComponent : this.moduleComponents) {
            if (!this.searchCheck(moduleComponent) && (GuiUtility.isHovered((double)x, (double)this.y, (double)this.width, (double)this.height, (double)moduleComponent.getX(), (double)moduleComponent.getY()) || GuiUtility.isHovered((double)x, (double)this.y, (double)this.width, (double)this.height, (double)moduleComponent.getX(), (double)(moduleComponent.getY() + moduleComponent.getHeight())))) {
               moduleComponent.drawSplit(context);
            }
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.selectedModuleComponent != null) {
         if (GuiUtility.isHovered((double)this.x, (double)(this.y + 52.0F), (double)this.width, (double)(this.height - 52.0F), mouseX, mouseY)) {
            for(MenuSettingComponent<?> settingComponent : this.selectedModuleComponent.getSettingComponents()) {
               if (settingComponent.getSetting().isVisible()) {
                  settingComponent.onMouseClicked(mouseX, mouseY, button);
               }
            }
         }

         float y = this.y + 28.0F;
         if (GuiUtility.isHovered((double)this.x, (double)y, (double)this.width, (double)20.0F, mouseX, mouseY) && button == MouseButton.LEFT) {
            this.selectedModuleComponent = null;
            if (((Sounds)FeverVisual.getInstance().getModuleManager().getModule(Sounds.class)).isEnabled()) {
               ClientSounds.CLICKGUI_OPEN.play(0.8F, 1.2F);
            }
         }
      } else if (GuiUtility.isHovered((double)this.x, (double)(this.y + 28.0F), (double)this.width, (double)(this.height - 28.0F), mouseX, mouseY)) {
         for(ModuleComponent moduleComponent : this.moduleComponents) {
            if (!this.searchCheck(moduleComponent)) {
               moduleComponent.onMouseClicked(mouseX, mouseY, button);
            }
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (this.selectedModuleComponent != null) {
         for(MenuSettingComponent<?> settingComponent : this.selectedModuleComponent.getSettingComponents()) {
            if (settingComponent.getSetting().isVisible()) {
               settingComponent.onMouseReleased(mouseX, mouseY, button);
            }
         }
      } else {
         for(ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.onMouseReleased(mouseX, mouseY, button);
         }
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.isHovered(GuiUtility.getMouse().x(), GuiUtility.getMouse().y())) {
         if (this.selectedModuleComponent != null) {
            this.settingsScroll.onKeyPressed(keyCode);
         } else {
            this.modulesScroll.onKeyPressed(keyCode);
         }
      }

      if (this.selectedModuleComponent != null) {
         for(MenuSettingComponent<?> settingComponent : this.selectedModuleComponent.getSettingComponents()) {
            if (settingComponent.getSetting().isVisible()) {
               settingComponent.onKeyPressed(keyCode, scanCode, modifiers);
            }
         }
      } else {
         for(ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.onKeyPressed(keyCode, scanCode, modifiers);
         }
      }

      super.onKeyPressed(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.selectedModuleComponent != null) {
         for(MenuSettingComponent<?> settingComponent : this.selectedModuleComponent.getSettingComponents()) {
            if (settingComponent.getSetting().isVisible()) {
               settingComponent.charTyped(chr, modifiers);
            }
         }
      } else {
         for (ModuleComponent moduleComponent : this.moduleComponents) {
            moduleComponent.charTyped(chr, modifiers);
         }
      }

      return super.charTyped(chr, modifiers);
   }

   private boolean searchCheck(ModuleComponent component) {
      MenuScreen var3 = FeverVisual.getInstance().getMenuScreen();
      if (!(var3 instanceof DropDownScreen dropDownScreen)) {
         return true;
      } else {
         TextField search = dropDownScreen.getSearchField();
         return search != null && !search.getBuiltText().isBlank() && !component.getModule().getName().toLowerCase().contains(search.getBuiltText().toLowerCase()) && !component.getModule().getName().replace(" ", "").toLowerCase().contains(search.getBuiltText().toLowerCase());
      }
   }

   private boolean fevervisual$isComponentInView(float viewX, float viewY, CustomComponent component) {
      return GuiUtility.isHovered((double)viewX, (double)viewY, (double)this.width, (double)this.height, (double)component.getX(), (double)component.getY()) || GuiUtility.isHovered((double)viewX, (double)viewY, (double)this.width, (double)this.height, (double)component.getX(), (double)(component.getY() + component.getHeight()));
   }

   private List<MenuSettingComponent<?>> getVisibleSettingComponents() {
      List<MenuSettingComponent<?>> visibleSettings = new ArrayList<>();
      if (this.lastSelected == null) {
         return visibleSettings;
      }

      for(MenuSettingComponent<?> settingComponent : this.lastSelected.getSettingComponents()) {
         if (settingComponent.getSetting().isVisible()) {
            visibleSettings.add(settingComponent);
         }
      }

      return visibleSettings;
   }

   private boolean shouldDrawSettingsSeparator(Setting current, Setting next) {
      return this.getSettingGroup(current) != this.getSettingGroup(next);
   }

   private SettingGroup getSettingGroup(Setting setting) {
      String name = setting.getName().toLowerCase();
      if (setting instanceof ColorSetting || name.contains("color") || name.contains("opacity") || name.contains("alpha")) {
         return SettingGroup.COLOR;
      }

      if (setting instanceof ModeSetting) {
         return SettingGroup.MODE;
      }

      if (setting instanceof SelectSetting || name.contains("target") || name.contains("entity")) {
         return SettingGroup.SELECT;
      }

      if (setting instanceof BooleanSetting) {
         return SettingGroup.TOGGLE;
      }

      if (setting instanceof SliderSetting || setting instanceof RangeSetting) {
         return SettingGroup.VALUE;
      }

      return SettingGroup.OTHER;
   }

   private boolean isVerticallyVisible(CustomComponent component, float top, float bottom) {
      return component.getY() + component.getHeight() >= top && component.getY() <= bottom;
   }

   public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.isHovered(mouseX, mouseY)) {
         if (this.selectedModuleComponent != null) {
            this.settingsScroll.scroll(verticalAmount);
         } else {
            this.modulesScroll.scroll(verticalAmount);
            for (ModuleComponent moduleComponent : this.moduleComponents) {
               moduleComponent.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
            }
         }
      }

   }

   @Generated
   public MenuCategory getCategory() {
      return this.category;
   }

   @Generated
   public Animation getSwapping() {
      return this.swapping;
   }

   @Generated
   public Animation getSizing() {
      return this.sizing;
   }

   @Generated
   public List<ModuleComponent> getModuleComponents() {
      return this.moduleComponents;
   }

   @Generated
   public ScrollHandler getModulesScroll() {
      return this.modulesScroll;
   }

   @Generated
   public ScrollHandler getSettingsScroll() {
      return this.settingsScroll;
   }

   @Generated
   public Font getTitleFont() {
      return this.titleFont;
   }

   @Generated
   public ModuleComponent getLastSelected() {
      return this.lastSelected;
   }

   @Generated
   public ModuleComponent getSelectedModuleComponent() {
      return this.selectedModuleComponent;
   }

   @Generated
   public void setTitleFont(Font titleFont) {
      this.titleFont = titleFont;
   }

   @Generated
   public void setLastSelected(ModuleComponent lastSelected) {
      this.lastSelected = lastSelected;
   }

   @Generated
   public void setSelectedModuleComponent(ModuleComponent selectedModuleComponent) {
      this.selectedModuleComponent = selectedModuleComponent;
   }
}
