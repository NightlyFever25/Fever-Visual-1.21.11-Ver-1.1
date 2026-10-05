package fever.visual.ui.hud.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.Module;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.hud.HudList;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.ScissorUtility;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KeyBinds extends HudList {
   private static final float SEPARATOR_HEIGHT = 0.25F;
   private int lastSize = -1;
   private final List<Module> sortedModules = new ArrayList<>();
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");

   public KeyBinds() {
      super("hud.keybinds", "icons/hud/keybinds.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = 92.0F;
      this.height = 18.0F;
      Font font = Fonts.REGULAR.getFont(7.0F);

      for (Module module : FeverVisual.getInstance().getModuleManager().getModules()) {
         boolean forward = module.isEnabled() && module.getKey() != -1;
         module.getKeybindsAnimation().update(forward);
         module.getKeybindsAnimation().setEasing(Easing.BAKEK);
         if (module.getKeybindsAnimation().getValue() > 0.0F) {
            this.width = Math.max(font.width(module.getName() + TextUtility.getKeyName(module.getKey())) + 20.0F, this.width);
         }

         this.height = this.height + 18.0F * module.getKeybindsAnimation().getValue();
      }

      if (this.height > 18.0F) {
         this.height += 5.0F;
      }

      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      float glassIntensity = Interface.glass();
      float minimalizm = Interface.minimalizm();
      boolean showGlass = Interface.showGlass();
      boolean showMinimalizm = Interface.showMinimalizm();

      ColorRGBA bgColor = Colors.getBackgroundColor()
              .withAlpha(255.0F * (dark ? 0.8F - 0.6F * glassIntensity : 0.7F));

      float alpha = 255.0F * this.animation.getValue() * this.visible.getValue();
      if (showMinimalizm) {
         context.drawBlurredRect(
                 this.x, this.y, this.width, this.height,
                 45.0F, 7.0F,
                 BorderRadius.all(6.0F),
                 ColorRGBA.WHITE.withAlpha(alpha * minimalizm)
         );
      }

      if (showGlass) {
         context.drawLiquidGlass(
                 this.x, this.y, this.width, this.height,
                 7.0F,
                 0.08F - 0.07F * this.dragAnim.getValue(),
                 BorderRadius.all(6.0F),
                 ColorRGBA.WHITE.withAlpha(alpha * glassIntensity)
         );
      }

      context.drawSquircle(this.x, this.y, this.width, this.height, 7.0F,
              BorderRadius.all(6.0F), bgColor.withAlpha(bgColor.getAlpha() * this.animation.getValue()));

      ScissorUtility.push(context.getMatrices(), this.x, this.y, this.width, this.height);

      Font font = Fonts.REGULAR.getFont(7.0F);
      List<Module> modules = this.getSortedModules(font);

      float headerHeight = 18.0F;
      Font headerFont = Fonts.MEDIUM.getFont(8.0F);
      String headerText = "KeyBinds";
      float textWidth = headerFont.width(headerText);
      float textX = this.x + (this.width - textWidth) / 2.0F;
      float textY = this.y + (headerHeight - headerFont.height()) / 2.0F;
      context.drawText(headerFont, headerText, textX, textY, Colors.getTextColor().withAlpha(alpha));

      if (this.height >= 23.0F) {
         context.drawRect(
                 this.x,
                 this.y + headerHeight,
                 this.width,
                 SEPARATOR_HEIGHT,
                 Colors.getTextColor().withAlpha(alpha * 0.10F)
         );
      }

      float offset = 22.0F;

      for (Module module : modules) {
         Animation anim = module.getKeybindsAnimation();
         if (anim.getValue() != 0.0F) {
            float off = -4.5F + 4.5F * anim.getValue();
            if (offset != 22.0F) {
               context.drawRect(
                       this.x,
                       this.y + offset + off,
                       this.width,
                       SEPARATOR_HEIGHT,
                       Colors.getTextColor().withAlpha(alpha * 0.015F * anim.getValue())
               );
            }
            offset += 18.0F * anim.getValue();
         }
      }

      offset = 22.0F;

      for (Module modulex : modules) {
         Animation anim = modulex.getKeybindsAnimation();
         if (anim.getValue() != 0.0F) {
            float off = -4.5F + 4.5F * anim.getValue();
            context.drawText(
                    font,
                    modulex.getName(),
                    this.x + 7.0F * anim.getValue(),
                    this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
                    Colors.getTextColor().withAlpha(alpha * anim.getValue())
            );
            context.drawRightText(
                    font,
                    TextUtility.getKeyName(modulex.getKey()),
                    this.x + this.width - 7.0F * anim.getValue(),
                    this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
                    Colors.getTextColor().withAlpha(alpha * anim.getValue())
            );
            offset += 18.0F * anim.getValue();
         }
      }

      ScissorUtility.pop();
   }

   @Override
   public boolean show() {
      if (mc.currentScreen instanceof ChatScreen || this.alwaysDisplay.isEnabled()) {
         return true;
      }

      for (Module module : FeverVisual.getInstance().getModuleManager().getModules()) {
         if (module.isEnabled() && module.getKey() != -1) {
            return true;
         }
      }

      return false;
   }

   private List<Module> getSortedModules(Font font) {
      List<Module> modules = FeverVisual.getInstance().getModuleManager().getModules();
      if (this.lastSize != modules.size()) {
         this.sortedModules.clear();
         this.sortedModules.addAll(modules);
         this.sortedModules.sort(Comparator.comparingDouble(m -> font.width(m.getName())));
         this.lastSize = modules.size();
      }

      return this.sortedModules;
   }
}
