package fever.visual.ui.hud.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.ScissorUtility;
import net.minecraft.client.option.KeyBinding;

public class Keystrokes extends HudElement {
   private static final float KEY_SIZE = 20.0F;
   private static final float GAP = 1.0F;
   private static final float SPACE_WIDTH = KEY_SIZE * 3.0F + GAP * 2.0F;
   private static final float SPACE_HEIGHT = 18.0F;
   private static final float KEY_SQUIRCLE = 4.0F;
   private static final BorderRadius KEY_RADIUS = BorderRadius.all(6.0F);

   private final Animation w = new Animation(120L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation a = new Animation(120L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation s = new Animation(120L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation d = new Animation(120L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation space = new Animation(120L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   public Keystrokes() {
      super("hud.keystrokes", "icons/hud/keybinds.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = SPACE_WIDTH;
      this.height = KEY_SIZE * 2.0F + GAP + SPACE_HEIGHT + GAP;
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.w.update(this.isPressed(mc.options.forwardKey));
      this.a.update(this.isPressed(mc.options.leftKey));
      this.s.update(this.isPressed(mc.options.backKey));
      this.d.update(this.isPressed(mc.options.rightKey));
      this.space.update(this.isPressed(mc.options.jumpKey));

      this.drawKey(context, "W", this.x + KEY_SIZE + GAP, this.y, KEY_SIZE, KEY_SIZE, this.w.getValue());
      this.drawKey(context, "A", this.x, this.y + KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, this.a.getValue());
      this.drawKey(context, "S", this.x + KEY_SIZE + GAP, this.y + KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, this.s.getValue());
      this.drawKey(context, "D", this.x + (KEY_SIZE + GAP) * 2.0F, this.y + KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE, this.d.getValue());
      this.drawKey(context, "SPACE", this.x, this.y + KEY_SIZE * 2.0F + GAP * 2.0F, SPACE_WIDTH, SPACE_HEIGHT, this.space.getValue());
   }

   private void drawKey(UIContext context, String label, float x, float y, float width, float height, float active) {
      float scale = 1.0F - active * 0.15F;
      RenderUtility.scale(context.getMatrices(), x + width / 2.0F, y + height / 2.0F, scale);

      float alpha = 255.0F * this.animation.getValue() * this.visible.getValue();
      context.drawClientRect(x, y, width, height, this.animation.getValue(), this.dragAnim.getValue(), KEY_SQUIRCLE);
      if (active > 0.01F) {
         ScissorUtility.push(context.getMatrices(), x, y, width, height);
         context.drawSquircle(
                 x,
                 y,
                 width,
                 height,
                 KEY_SQUIRCLE,
                 KEY_RADIUS,
                 Colors.getTextColor().withAlpha((55.0F + 75.0F * active) * alpha / 255.0F)
         );
         ScissorUtility.pop();
      }

      Font font = Fonts.MEDIUM.getFont(label.length() > 1 ? 6.0F : 8.0F);
      float textX = x + (width - font.width(label)) / 2.0F;
      float textY = y + GuiUtility.getMiddleOfBox(font.height(), height);
      context.drawText(font, label, textX, textY, Colors.getTextColor().withAlpha(alpha));
      RenderUtility.end(context.getMatrices());
   }

   private boolean isPressed(KeyBinding binding) {
      return binding != null && binding.isPressed();
   }
}
