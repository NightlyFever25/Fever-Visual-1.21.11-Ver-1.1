package fever.visual.ui.hud.inline;

import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.ScissorUtility;
import net.minecraft.util.Identifier;

public class InlineElement extends HudElement {
   private static final Identifier COPY_ICON = FeverVisual.id("icons/hud/copy.png");
   private static final Identifier CHECK_ICON = FeverVisual.id("icons/check.png");
   private static final float SEPARATOR_DOT_SIZE = 3.0F;
   private static final float SEPARATOR_DOT_ALPHA = 0.8F;

   protected final SelectSetting elements = new SelectSetting(this, "elements").draggable().min(1);

   public InlineElement(String name, String icon) {
      super(name, icon);
      this.height = 18.0F;
   }

   @Override
   protected void renderComponent(UIContext context) {
      context.drawClientRect(this.x, this.y, this.width, this.height, this.animation.getValue(), this.dragAnim.getValue(), 3.0F);
      ScissorUtility.push(context.getMatrices(), this.x, this.y, this.width, this.height);
      context.drawTexture(this.iconIdentifier, this.x + 5.0F, this.y + 5.0F, 8.0F, 8.0F, Colors.getTextColor());
      float xOffset = 0.0F;
      for (SelectSetting.Value value : this.elements.getValues()) {
         InlineValue elmt = (InlineValue)value;
         if (elmt.isSelected()) {
            float textWidth = Fonts.MEDIUM.getFont(8.0F).width(elmt.text()) + 8.0F * elmt.copyAnim().getValue();
            boolean hover = GuiUtility.isHovered(this.x + 19.0F + xOffset, this.y + 6.0F, Fonts.MEDIUM.getFont(8.0F).width(elmt.text()), 8.0, context)
               && !elmt.copy().isEmpty();
            if (!hover || elmt.copyTimer().finished(1000L)) {
               elmt.copied(false);
            }

            elmt.copyAnim().update(hover);
            elmt.successAnim().update(elmt.copied());
            context.drawText(
               Fonts.MEDIUM.getFont(8.0F), elmt.text(), this.x + 19.0F + xOffset + 8.0F * elmt.copyAnim().getValue(), this.y + 6.0F, Colors.getTextColor()
            );
            if (!elmt.suffix().isEmpty()) {
               context.drawText(
                  Fonts.MEDIUM.getFont(7.0F), elmt.suffix(), this.x + 19.0F + xOffset + textWidth, this.y + 7.0F, Colors.getTextColor().mulAlpha(0.5F)
               );
            }

            xOffset += textWidth + Fonts.MEDIUM.getFont(7.0F).width(elmt.suffix()) + 10.0F;
         }
      }

      xOffset = 0.0F;

      for (SelectSetting.Value valuex : this.elements.getValues()) {
         InlineValue elmt = (InlineValue)valuex;
         if (elmt.isSelected()) {
            float textWidthx = Fonts.MEDIUM.getFont(8.0F).width(elmt.text()) + 8.0F * elmt.copyAnim().getValue();
            if (xOffset != 0.0F) {
               float separatorCenterX = this.x + 19.0F + xOffset - 6.0F;
               float separatorCenterY = this.y + this.height / 2.0F;
               context.drawLegacyRoundedRect(
                  separatorCenterX - SEPARATOR_DOT_SIZE / 2.0F,
                  separatorCenterY - SEPARATOR_DOT_SIZE / 2.0F,
                  SEPARATOR_DOT_SIZE,
                  SEPARATOR_DOT_SIZE,
                  BorderRadius.all(SEPARATOR_DOT_SIZE / 2.0F),
                  ColorRGBA.WHITE.mulAlpha(SEPARATOR_DOT_ALPHA)
               );
            }

            RenderUtility.rotate(context.getMatrices(), this.x + 19.0F + xOffset + 3.0F, this.y + 6.0F + 3.0F, 90.0F * elmt.successAnim().getValue());
            context.drawTexture(
               COPY_ICON,
               this.x + 19.0F + xOffset,
               this.y + 6.0F,
               6.0F,
               6.0F,
               Colors.getTextColor().mulAlpha(elmt.copyAnim().getValue() * (1.0F - elmt.successAnim().getValue()))
            );
            RenderUtility.end(context.getMatrices());
            RenderUtility.rotate(context.getMatrices(), this.x + 19.0F + xOffset + 3.0F, this.y + 6.0F + 3.0F, -90.0F + 90.0F * elmt.successAnim().getValue());
            context.drawTexture(
               CHECK_ICON,
               this.x + 19.0F + xOffset,
               this.y + 6.0F,
               6.0F,
               6.0F,
               Colors.GREEN.mulAlpha(elmt.copyAnim().getValue() * elmt.successAnim().getValue())
            );
            RenderUtility.end(context.getMatrices());
            xOffset += textWidthx + Fonts.MEDIUM.getFont(7.0F).width(elmt.suffix()) + 10.0F;
         }
      }

      ScissorUtility.pop();
      this.width = 15.0F + xOffset;
      this.getWidthAnim().update(this.width);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      float xOffset = 0.0F;

      for (SelectSetting.Value value : this.elements.getValues()) {
         InlineValue elmt = (InlineValue)value;
         if (elmt.isSelected()) {
            float textWidth = Fonts.MEDIUM.getFont(8.0F).width(elmt.text());
            boolean hover = GuiUtility.isHovered((double)(this.x + 19.0F + xOffset), (double)(this.y + 6.0F), (double)Fonts.MEDIUM.getFont(8.0F).width(elmt.text()), 8.0, mouseX, mouseY) && !elmt.copy().isEmpty();
            if (hover && button == MouseButton.LEFT) {
               TextUtility.copyText(elmt.copy());
               elmt.copyTimer().reset();
               elmt.copied(true);
               return;
            }

            xOffset += textWidth + Fonts.MEDIUM.getFont(7.0F).width(elmt.suffix()) + 10.0F;
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }
}
