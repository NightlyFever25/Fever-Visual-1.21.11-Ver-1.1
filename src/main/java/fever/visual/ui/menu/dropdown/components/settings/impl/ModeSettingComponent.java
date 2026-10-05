package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.framework.objects.gradient.Gradient;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.theme.AccentTheme;
import fever.visual.ui.components.ColorPicker;
import fever.visual.ui.menu.dropdown.DropDownScreen;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.penis.PenisPlayer;

public class ModeSettingComponent extends MenuSettingComponent<ModeSetting> {
   private boolean initialized;
   private boolean themeExpanded;
   private final Animation themeOpenAnimation = new Animation(250L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private ColorPicker customFirstPicker;
   private ColorPicker customSecondPicker;

   public ModeSettingComponent(ModeSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (!this.initialized) {
         for (ModeSetting.Value value : this.setting.getValues()) {
            value.setEnablePenis(new PenisPlayer(FeverVisual.id("penises/check_enable.penis")));
            value.setDisablePenis(new PenisPlayer(FeverVisual.id("penises/check_disable.penis")));
            value.setLastState(value.isSelected());
            value.setCurrentPenis(value.isLastState() ? value.getEnablePenis() : value.getDisablePenis());
            if (value.isLastState()) {
               value.getEnablePenis().playOnce();
            } else {
               value.getDisablePenis().setFrame(0);
               value.getDisablePenis().stop();
            }
         }

         this.initialized = true;
      }

      if (this.fevervisual$isAccentThemeSetting()) {
         this.fevervisual$renderAccentTheme(context);
         return;
      }

      float x = this.x + 9.0F;
      float y = this.y + 1.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float headerHeight = 19.0F;
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      int visibleValues = this.fevervisual$getVisibleValueCount();
      context.drawFadeoutText(
         nameFont,
         Localizator.translate(this.getSetting().getName()),
         this.x + leftPadding,
         y - 1.0F + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight),
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())),
         0.8F,
         1.0F,
         this.getParent().getWidth() - leftPadding
      );
      context.drawRoundedRect(
         x - 1.0F, y + 17.0F, width + 2.0F, 8 + visibleValues * 12, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(76.5F)
      );
      float offset = 0.0F;

      for (ModeSetting.Value valuex : this.setting.getValues()) {
         if (!valuex.isHidden()) {
            boolean currentState = valuex.isSelected();
            if (currentState != valuex.isLastState()) {
               if (currentState) {
                  valuex.setCurrentPenis(valuex.getEnablePenis());
               } else {
                  valuex.setCurrentPenis(valuex.getDisablePenis());
               }

               valuex.getCurrentPenis().playOnce();
               valuex.setLastState(currentState);
            }

            valuex.getCurrentPenis().update();
            boolean hover = GuiUtility.isHovered(
               (double)(x - 1.0F), (double)(y + 20.0F + offset), (double)(width + 2.0F), 12.0, context.getMouseX(), context.getMouseY()
            );
            if (hover) {
               CursorUtility.set(CursorType.HAND);
            }

            valuex.getHoverAnimation().update(hover);
            valuex.getActiveAnimation().update(valuex.isSelected());
            context.drawFadeoutText(
               Fonts.REGULAR.getFont(7.0F),
               Localizator.translate(valuex.getName()),
               x + 7.0F,
               y + 24.5F + offset,
               Colors.getTextColor()
                  .withAlpha(255.0F * (0.75F + 0.25F * valuex.getHoverAnimation().getValue() + 0.25F * valuex.getActiveAnimation().getValue())),
               0.8F,
               1.0F,
               width - 12.0F - valuex.getActiveAnimation().getValue() * 10.0F
            );
            float active = valuex.getActiveAnimation().getValue();
            if (active > 0.01F) {
               context.drawTexture(
                  FeverVisual.id("icons/check.png"),
                  x + width - 11.0F - active * 2.0F,
                  y + 24.0F + offset,
                  6.0F,
                  6.0F,
                  Colors.getTextColor().mulAlpha(active)
               );
            }

            offset += 12.0F;
         }
      }
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.fevervisual$isAccentThemeSetting()) {
         this.fevervisual$onAccentThemeClicked(mouseX, mouseY, button);
         super.onVisibleMouseClicked(mouseX, mouseY, button);
         return;
      }

      if (button == MouseButton.LEFT) {
         float offset = 0.0F;

         for (ModeSetting.Value value : this.setting.getValues()) {
            if (!value.isHidden()) {
               boolean hover = GuiUtility.isHovered(
                  (double)(this.x - 1.0F), (double)(this.y + 20.0F + offset), (double)(this.width - 2.0F), 12.0, mouseX, mouseY
               );
               if (hover) {
                  value.select();
               }

               offset += 12.0F;
            }
         }

         super.onVisibleMouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public float getHeight() {
      if (this.fevervisual$isAccentThemeSetting()) {
         return this.height = 32.0F + this.fevervisual$getVisibleValueCount() * 18.0F * this.themeOpenAnimation.getValue();
      }

      return this.height = 31 + this.fevervisual$getVisibleValueCount() * 12;
   }

   private int fevervisual$getVisibleValueCount() {
      int count = 0;
      for (ModeSetting.Value value : this.setting.getValues()) {
         if (!value.isHidden()) {
            count++;
         }
      }
      return count;
   }

   private boolean fevervisual$isAccentThemeSetting() {
      return this.setting != null && this.setting.getName().equalsIgnoreCase("modules.settings.interface.accent_theme");
   }

   private void fevervisual$renderAccentTheme(UIContext context) {
      float x = this.x + 9.0F;
      float y = this.y + 1.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float headerHeight = 19.0F;
      ModeSetting.Value currentValue = this.setting.getValue();
      AccentTheme currentTheme = currentValue == null ? AccentTheme.RAINBOW : AccentTheme.byTranslationKey(currentValue.getName());
      this.themeOpenAnimation.update(this.themeExpanded);
      float open = this.themeOpenAnimation.getValue();

      boolean headerHovered = GuiUtility.isHovered(x - 1.0F, y + 17.0F, width + 2.0F, 13.0F, context);
      this.hoverAnimation.update(headerHovered);
      if (headerHovered) {
         CursorUtility.set(CursorType.HAND);
      }

      context.drawFadeoutText(
         nameFont,
         Localizator.translate(this.getSetting().getName()),
         this.x + leftPadding,
         y - 1.0F + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight),
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())),
         0.8F,
         1.0F,
         this.getParent().getWidth() - leftPadding
      );

      context.drawRoundedRect(
         x - 1.0F,
         y + 17.0F,
         width + 2.0F,
         13.0F + this.fevervisual$getVisibleValueCount() * 18.0F * open,
         BorderRadius.all(6.0F),
         Colors.getBackgroundColor().withAlpha(76.5F)
      );

      context.drawFadeoutText(
         Fonts.REGULAR.getFont(7.0F),
         Localizator.translate(currentValue == null ? AccentTheme.RAINBOW.getTranslationKey() : currentValue.getName()),
         x + 7.0F,
         y + 21.5F,
         Colors.getTextColor().withAlpha(220.0F),
         0.8F,
         1.0F,
         width - 40.0F
      );
      this.fevervisual$drawThemePreview(context, currentTheme, x + width - 27.0F, y + 20.0F, 20.0F, 10.0F, 1.0F);

      if (open <= 0.01F) {
         return;
      }

      float offset = 0.0F;
      for (ModeSetting.Value value : this.setting.getValues()) {
         if (value.isHidden()) {
            continue;
         }

         AccentTheme theme = AccentTheme.byTranslationKey(value.getName());
         float rowY = y + 33.0F + offset - 4.0F * (1.0F - open);
         boolean hover = this.themeExpanded && open > 0.75F && GuiUtility.isHovered(x - 1.0F, rowY, width + 2.0F, 16.0F, context);
         if (hover) {
            CursorUtility.set(CursorType.HAND);
         }

         value.getHoverAnimation().update(hover);
         value.getActiveAnimation().update(value.isSelected());
         float active = value.getActiveAnimation().getValue();
         context.drawRoundedRect(
            x + 3.0F,
            rowY + 1.0F,
            width - 6.0F,
            14.0F,
            BorderRadius.all(5.0F),
            Colors.getAdditionalColor().withAlpha(open * (30.0F * value.getHoverAnimation().getValue() + 45.0F * active))
         );
         context.drawFadeoutText(
            Fonts.REGULAR.getFont(7.0F),
            Localizator.translate(value.getName()),
            x + 7.0F,
            rowY + 5.5F,
            Colors.getTextColor().withAlpha(open * (190.0F + 65.0F * active)),
            0.8F,
            1.0F,
            width - 41.0F
         );
         this.fevervisual$drawThemePreview(context, theme, x + width - 28.0F, rowY + 3.0F, 20.0F, 10.0F, open * (0.7F + 0.3F * active));

         offset += 18.0F;
      }
   }

   private void fevervisual$drawThemePreview(UIContext context, AccentTheme theme, float x, float y, float width, float height, float alpha) {
      ColorRGBA first = this.fevervisual$getPreviewColor(theme, true).withAlpha(255.0F * alpha);
      ColorRGBA second = this.fevervisual$getPreviewColor(theme, false).withAlpha(255.0F * alpha);
      context.drawRoundedRect(
         x,
         y,
         width,
         height,
         BorderRadius.all(3.0F),
         Gradient.of(first, first, second, second)
      );
   }

   private ColorRGBA fevervisual$getPreviewColor(AccentTheme theme, boolean first) {
      Interface interfaceModule = FeverVisual.getInstance().getModuleManager().getModule(Interface.class);
      if (theme == AccentTheme.CUSTOM && interfaceModule != null) {
         return first ? interfaceModule.getCustomAccentFirst().getColorSafe() : interfaceModule.getCustomAccentSecond().getColorSafe();
      }

      return first ? theme.getPreviewStart() : theme.getPreviewEnd();
   }

   private void fevervisual$onAccentThemeClicked(double mouseX, double mouseY, MouseButton button) {
      if (button != MouseButton.LEFT && button != MouseButton.RIGHT) {
         return;
      }

      float x = this.x + 9.0F;
      float y = this.y + 1.0F;
      float width = this.width - 18.0F;
      boolean headerHovered = GuiUtility.isHovered(x - 1.0F, y + 17.0F, width + 2.0F, 13.0F, mouseX, mouseY);
      if (headerHovered) {
         this.themeExpanded = !this.themeExpanded;
         return;
      }

      if (!this.themeExpanded || this.themeOpenAnimation.getValue() < 0.75F) {
         return;
      }

      float offset = 0.0F;
      for (ModeSetting.Value value : this.setting.getValues()) {
         if (value.isHidden()) {
            continue;
         }

         boolean hover = GuiUtility.isHovered(x - 1.0F, y + 33.0F + offset, width + 2.0F, 16.0F, mouseX, mouseY);
         if (hover) {
            value.select();
            if (AccentTheme.byTranslationKey(value.getName()) == AccentTheme.CUSTOM) {
               this.fevervisual$openCustomPickers((float)mouseX, (float)mouseY);
            }
            return;
         }

         offset += 18.0F;
      }
   }

   private void fevervisual$openCustomPickers(float mouseX, float mouseY) {
      if (!(FeverVisual.getInstance().getMenuScreen() instanceof DropDownScreen dropDownScreen)) {
         return;
      }

      Interface interfaceModule = FeverVisual.getInstance().getModuleManager().getModule(Interface.class);
      if (interfaceModule == null) {
         return;
      }

      dropDownScreen.getColorPickers().remove(this.customFirstPicker);
      dropDownScreen.getColorPickers().remove(this.customSecondPicker);
      this.customFirstPicker = new ColorPicker(
         mouseX + 10.0F,
         mouseY,
         6.0F,
         true,
         interfaceModule.getCustomAccentFirst().getColorSafe(),
         Localizator.translate("modules.settings.interface.accent_theme.custom_first")
      );
      this.customSecondPicker = new ColorPicker(
         mouseX + 160.0F,
         mouseY,
         6.0F,
         true,
         interfaceModule.getCustomAccentSecond().getColorSafe(),
         Localizator.translate("modules.settings.interface.accent_theme.custom_second")
      );
      this.customFirstPicker.setOnColorChange(color -> interfaceModule.getCustomAccentFirst().setColor(color));
      this.customSecondPicker.setOnColorChange(color -> interfaceModule.getCustomAccentSecond().setColor(color));
      this.customFirstPicker.setOnClose(() -> FeverVisual.getInstance().getFileManager().writeFile("client"));
      this.customSecondPicker.setOnClose(() -> FeverVisual.getInstance().getFileManager().writeFile("client"));
      dropDownScreen.getColorPickers().add(this.customFirstPicker);
      dropDownScreen.getColorPickers().add(this.customSecondPicker);
   }
}
