package fever.visual.ui.hud.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.notifications.NotificationType;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.components.animated.AnimatedNumber;
import fever.visual.ui.hud.HudList;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.server.ServerUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.mixins.StatusEffectInstanceAddition;
import fever.visual.utility.render.ScissorUtility;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;

public class Effects extends HudList {
   private static final float SEPARATOR_HEIGHT = 0.25F;
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");
   int lastSize = -1;
   private final Map<String, StatusEffectInstance> effects = new TreeMap<>();
   private final Map<StatusEffect, Boolean> ended = new HashMap<>();
   private final BooleanSetting alert = new BooleanSetting(this, "hud.effects.alert");

   private final ColorSetting beneficialColor = new ColorSetting(this, "hud.effects.beneficial_color")
           .color(new ColorRGBA(100, 255, 100, 255))
           .alpha(false);
   private final ColorSetting harmfulColor = new ColorSetting(this, "hud.effects.harmful_color")
           .color(new ColorRGBA(255, 100, 100, 255))
           .alpha(false);

   public Effects() {
      super("hud.effects", "icons/hud/potion.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = 92.0F;
      this.height = 18.0F;
      Collection<StatusEffectInstance> original = mc.player.getStatusEffects();

      for (StatusEffectInstance eff : original) {
         StatusEffect potion = (StatusEffect)eff.getEffectType().value();
         String realName = potion.getName().getString();
         if (realName != null && !ServerUtility.isCM()) {
            if (this.effects.containsKey(realName)) {
               this.effects.replace(realName, eff);
               Animation anim = ((StatusEffectInstanceAddition)eff).fevervisual$getAnimPotion();
               if (anim.getValue() == 0.0F) {
                  anim.setValue(1.0F);
               }
            } else {
               this.effects.put(realName, eff);
            }
         }
      }

      if (!this.effects.isEmpty()) {
         this.height += 5.0F;
      }

      for (StatusEffectInstance effx : this.effects.values()) {
         Animation anim = ((StatusEffectInstanceAddition)effx).fevervisual$getAnimPotion();
         StatusEffect potion = (StatusEffect)effx.getEffectType().value();
         if (this.alert.isEnabled()) {
            String effectName = potion.getName().getString() + " " + (effx.getAmplifier() > 0 ? effx.getAmplifier() + 1 : "");
            if (!mc.player.hasStatusEffect(effx.getEffectType())) {
               if (!this.ended.getOrDefault(potion, false) && !potion.getCategory().equals(StatusEffectCategory.HARMFUL)) {
                  FeverVisual.getInstance()
                          .getNotificationManager()
                          .addNotificationOther(NotificationType.INFO, "Эффект " + effectName + " закончился", "Действие эффекта завершено");
                  this.ended.put(potion, true);
               }
            } else {
               this.ended.put(potion, false);
            }
         }

         anim.update(original.contains(effx));
         anim.setEasing(Easing.BAKEK);
         this.width = Math.max(Fonts.REGULAR.getFont(7.0F).width(potion.getName().getString()) + 60.0F, this.width);
         this.height = this.height + 18.0F * anim.getValue();
      }

      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (mc.player != null && mc.world != null) {
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
         float headerHeight = 18.0F;
         Font headerFont = Fonts.MEDIUM.getFont(8.0F);
         String headerText = "Potions";
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
         StatusEffectInstance toRemove = null;
         for (StatusEffectInstance eff : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)eff).fevervisual$getAnimPotion();
            if (anim.getValue() == 0.0F) {
               toRemove = eff;
            } else {
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
         for (StatusEffectInstance effx : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)effx).fevervisual$getAnimPotion();
            if (anim.getValue() != 0.0F) {
               float off = -4.5F + 4.5F * anim.getValue();
               context.drawGuiTexture(
                       RenderPipelines.GUI_TEXTURED,
                       InGameHud.getEffectTexture(effx.getEffectType()),
                       Math.round(this.x + 7.0F * anim.getValue()),
                       Math.round(this.y + offset + off + GuiUtility.getMiddleOfBox(8.0F, 18.0F) + 1.0F),
                       8,
                       8,
                       ColorRGBA.WHITE.withAlpha(alpha * anim.getValue()).getRGB()
               );
               offset += 18.0F * anim.getValue();
            }
         }
         offset = 22.0F;

         for (StatusEffectInstance effxx : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)effxx).fevervisual$getAnimPotion();
            AnimatedNumber timeAnimation = ((StatusEffectInstanceAddition)effxx).fevervisual$getTimeAnimation();
            StatusEffect potion = (StatusEffect)effxx.getEffectType().value();
            if (anim.getValue() != 0.0F) {
               float off = -4.5F + 4.5F * anim.getValue();
               String effectName = potion.getName().getString() + " " + (effxx.getAmplifier() > 0 ? effxx.getAmplifier() + 1 : "");
               ColorRGBA effectColor = potion.getCategory() == StatusEffectCategory.BENEFICIAL ? beneficialColor.getColorSafe() : harmfulColor.getColorSafe();
               boolean isFiniteEffect = !effxx.isInfinite()
                       && effxx.getDuration() > 0
                       && effxx.getDuration() < 999999;
               boolean isStaticDuration = effxx.isInfinite() || effxx.getDuration() >= 999999;
               boolean isTurtleHelmetEffect = potion.getTranslationKey().contains("water_breathing") && effxx.getDuration() == 200;

               if (isFiniteEffect && !isStaticDuration && !isTurtleHelmetEffect) {
                  int totalSeconds = effxx.getDuration() / 20;
                  int minutes = totalSeconds / 60;
                  int seconds = totalSeconds % 60;
                  String timeStr = String.format("%02d:%02d", minutes, seconds);
                  String minutesAndSeparator = String.format("%02d:", minutes);
                  float timeX = this.x + this.width - 7.0F * anim.getValue();
                  float timeY = this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F);
                  float minutesWidth = font.width(minutesAndSeparator);
                  float totalWidth = font.width(timeStr);
                  context.drawText(font, minutesAndSeparator, timeX - totalWidth, timeY,
                          effectColor.withAlpha(alpha * anim.getValue()));
                  timeAnimation.settings(true, effectColor.withAlpha(alpha * anim.getValue()));
                  timeAnimation.update(seconds);
                  timeAnimation.pos(timeX - totalWidth + minutesWidth, timeY);
                  timeAnimation.render(context);
               } else {
                  String duration = "**:**";
                  float timeX = this.x + this.width - 7.0F * anim.getValue();
                  float timeY = this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F);
                  context.drawRightText(font, duration, timeX, timeY, effectColor.withAlpha((int)(alpha * anim.getValue())));
               }
               context.drawText(
                       font,
                       effectName,
                       this.x + 19.0F * anim.getValue(),
                       this.y + offset + off + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
                       effectColor.withAlpha(alpha * anim.getValue())
               );
               offset += 18.0F * anim.getValue();
            }
         }

         if (toRemove != null) {
            StatusEffect potion = (StatusEffect)toRemove.getEffectType().value();
            this.effects.remove(potion.getName().getString(), toRemove);
         }

         ScissorUtility.pop();
      }
   }

   @Override
   public boolean show() {
      return mc.player != null && mc.world != null
              ? (!mc.player.getStatusEffects().isEmpty() || mc.currentScreen instanceof ChatScreen || this.alwaysDisplay.isEnabled()) && !ServerUtility.isCM()
              : false;
   }

   public static boolean shouldReplaceVanilla() {
      return Interface.isHudElementEnabled("hud.effects");
   }
}
