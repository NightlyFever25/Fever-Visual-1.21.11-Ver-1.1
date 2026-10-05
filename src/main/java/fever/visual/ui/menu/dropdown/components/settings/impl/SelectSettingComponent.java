package fever.visual.ui.menu.dropdown.components.settings.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.ui.components.animated.AnimatedNumber;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.penis.PenisPlayer;
import fever.visual.utility.time.Timer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SelectSettingComponent extends MenuSettingComponent<SelectSetting> {
   private AnimatedNumber numberAnim;
   private SelectSetting.Value dragging;
   private final Timer sortTimer = new Timer();
   private boolean initialized;

   public SelectSettingComponent(SelectSetting setting, CustomComponent parent) {
      super(setting, parent);
      List<SelectSetting.Value> enabled = new ArrayList();
      setting.getValues().forEach((sel) -> {
         if (sel.isSelected()) {
            enabled.add(sel);
         }

      });
      setting.getSelectedValues().clear();
      setting.getSelectedValues().addAll(enabled);
   }

   protected void renderComponent(UIContext context) {
      if (!this.initialized) {
         for(SelectSetting.Value value : (this.setting).getValues()) {
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

      float x = this.x + 9.0F;
      float y = this.y + 1.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float nameHeight = Fonts.REGULAR.getFont(7.0F).height();
      float headerHeight = 19.0F;
      this.hoverAnimation.update(this.isHovered((float)context.getMouseX(), (float)context.getMouseY()));
      int visibleValues = this.fevervisual$getVisibleValueCount();
      Object[] var10001 = new Object[1];
      String var10004 = Localizator.translate("setting_of");
      var10001[0] = var10004 + " " + visibleValues;
      String rightText = String.format(" %s", var10001);
      if (this.numberAnim == null) {
         this.numberAnim = new AnimatedNumber(Fonts.MEDIUM.getFont(7.0F), 5.0F, 500L, Easing.BAKEK);
      }

      context.drawFadeoutText(nameFont, Localizator.translate(((SelectSetting)this.setting).getName()), this.x + leftPadding, y - 1.0F + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight), Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())), 0.8F, 1.0F, this.getParent().getWidth() - leftPadding - Fonts.REGULAR.getFont(7.0F).width(rightText) - this.numberAnim.getWidth() - 10.0F);
      this.numberAnim.settings(false, Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())));
      this.numberAnim.update(((SelectSetting)this.setting).getSelectedValues().size());
      this.numberAnim.pos(x + width - Fonts.REGULAR.getFont(7.0F).width(rightText) - this.numberAnim.getWidth(), y - 1.0F + GuiUtility.getMiddleOfBox(nameHeight, headerHeight));
      this.numberAnim.render(context);
      context.drawRightText(Fonts.REGULAR.getFont(7.0F), rightText, x + width, y - 1.0F + GuiUtility.getMiddleOfBox(nameHeight, headerHeight), Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())));
      context.drawRoundedRect(x - 1.0F, y + 17.0F, width + 2.0F, (float)(8 + visibleValues * 12), BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(76.5F));
      float offset = 0.0F;

      for(SelectSetting.Value valuex : (this.setting).getValues()) {
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
            float elmtY = this.dragging == valuex ? Math.clamp((float)(context.getMouseY() - 2), y + 18.0F, y + 20.0F + (float)(visibleValues * 12)) : y + 24.0F + offset;
            boolean hover = GuiUtility.isHovered((double)(x - 1.0F), (double)(elmtY - 4.0F), (double)(width + 2.0F), (double)12.0F, context.getMouseX(), context.getMouseY());
            valuex.getYAnim().setEasing(Easing.BAKEK_SMALLER);
            valuex.getYAnim().update(elmtY - y);
            valuex.setYFactor(elmtY);
            if (hover && this.dragging != valuex && !valuex.isAlwaysEnabled()) {
               CursorUtility.set(CursorType.HAND);
            }

            valuex.getHoverAnimation().update(hover);
            valuex.getActiveAnimation().update(valuex.isSelected());
            if (((SelectSetting)this.setting).isDraggable()) {
               context.drawTexture(FeverVisual.id("icons/hud/drag.png"), x + 7.0F, y + valuex.getYAnim().getValue(), 6.0F, 6.0F, Colors.getTextColor());
            }

            if (GuiUtility.isHovered((double)x, (double)(elmtY - 2.0F), (double)17.0F, (double)10.0F, context) || valuex == this.dragging) {
               CursorUtility.set(CursorType.ARROW_VERTICAL);
            }

            context.drawFadeoutText(Fonts.REGULAR.getFont(7.0F), Localizator.translate(valuex.getName()), x + (float)(((SelectSetting)this.setting).isDraggable() ? 18 : 7), y + valuex.getYAnim().getValue() + 0.5F, Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * valuex.getHoverAnimation().getValue() + 0.25F * valuex.getActiveAnimation().getValue())), 0.8F, 1.0F, width - 12.0F - valuex.getActiveAnimation().getValue() * 10.0F);
            float active = valuex.getActiveAnimation().getValue();
            if (active > 0.01F) {
               context.drawTexture(FeverVisual.id("icons/check.png"), x + width - 11.0F - active * 2.0F, y + valuex.getYAnim().getValue(), 6.0F, 6.0F, Colors.getTextColor().mulAlpha(active));
            }

            offset += 12.0F;
         }
      }

      if (this.sortTimer.finished(100L)) {
         ((SelectSetting)this.setting).getValues().sort(Comparator.comparingDouble(SelectSetting.Value::getYFactor));
         this.sortTimer.reset();
      }

   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (button == MouseButton.LEFT) {
         float x = this.x + 9.0F;
         float y = this.y + 1.0F;
         float offset = 0.0F;

         for(SelectSetting.Value value : (this.setting).getValues()) {
            if (!value.isHidden()) {
               boolean hover = GuiUtility.isHovered((double)(x - 1.0F), (double)(y + 20.0F + offset), (double)(this.width - 2.0F), (double)12.0F, mouseX, mouseY);
               if (GuiUtility.isHovered((double)x, (double)(y + 22.0F + offset), (double)17.0F, (double)10.0F, mouseX, mouseY) && ((SelectSetting)this.setting).isDraggable()) {
                  this.dragging = value;
               } else if (hover) {
                  value.toggle();
               }

               offset += 12.0F;
            }
         }

         super.onVisibleMouseClicked(mouseX, mouseY, button);
      }

   }

   protected void onVisibleMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.dragging = null;
      super.onVisibleMouseReleased(mouseX, mouseY, button);
   }

   public float getHeight() {
      return this.height = (float)(31 + this.fevervisual$getVisibleValueCount() * 12);
   }

   private int fevervisual$getVisibleValueCount() {
      int count = 0;

      for(SelectSetting.Value value : (this.setting).getValues()) {
         if (!value.isHidden()) {
            ++count;
         }
      }

      return count;
   }
}
