package fever.visual.ui.hud.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import net.minecraft.item.ItemStack;

public class InventoryHUD extends HudElement {
    private final Animation contentAnimation = new Animation(300L, 0.0F, Easing.BAKEK);

    public InventoryHUD() {
        super("hud.inventory", "icons/hud/inventory.png");
    }

    @Override
    public void update(UIContext context) {
        contentAnimation.update(this.animation.getValue() * this.visible.getValue() >= 1.0F);

        float slotSize = 18.0F;
        float padding = 6.0F;
        float headerHeight = 20.0F;

        this.width = 9 * slotSize + padding * 2;
        this.height = headerHeight + 3 * slotSize + padding;

        super.update(context);
    }

    @Override
    protected void renderComponent(UIContext context) {
        if (mc.player == null) return;

        boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
        float glassIntensity = Interface.glass();
        float minimalizm = Interface.minimalizm();
        boolean showGlass = Interface.showGlass();
        boolean showMinimalizm = Interface.showMinimalizm();

        ColorRGBA bgColor = Colors.getBackgroundColor()
                .withAlpha(255.0F * (dark ? 0.8F - 0.6F * glassIntensity : 0.7F));

        float alpha = 255.0F * this.contentAnimation.getValue();
        float slotSize = 18.0F;
        float padding = 6.0F;
        float headerHeight = 20.0F;

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
                BorderRadius.all(6.0F), bgColor.withAlpha(bgColor.getAlpha() * this.contentAnimation.getValue()));

        Font titleFont = Fonts.SEMIBOLD.getFont(7.0F);
        String title = "Inventory";
        float titleWidth = titleFont.width(title);
        float titleX = this.x + (this.width - titleWidth) / 2.0F;
        float titleY = this.y + (headerHeight - titleFont.height()) / 2.0F;

        context.drawText(titleFont, title,
                titleX, titleY,
                Colors.getTextColor().withAlpha(alpha));

      float slotsStartX = this.x + padding;
      float slotsStartY = this.y + headerHeight;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + (row + 1) * 9;
                ItemStack stack = mc.player.getInventory().getStack(slotIndex);
                if (!stack.isEmpty()) {
                    float slotX = slotsStartX + col * slotSize;
                    float slotY = slotsStartY + row * slotSize;
                    context.drawItem(stack, slotX + 1, slotY + 1, 1.0F);
                }
            }
        }

        boolean hasCountOverlay = false;
        for (int slotIndex = 9; slotIndex < 36; slotIndex++) {
            ItemStack stack = mc.player.getInventory().getStack(slotIndex);
            if (!stack.isEmpty() && stack.getCount() > 1) {
                hasCountOverlay = true;
                break;
            }
        }
        if (!hasCountOverlay) {
            return;
        }

        context.getOriginalContext().createNewRootLayer();
        CustomDrawContext overlayContext = CustomDrawContext.isolated(context, CustomDrawContext.Pass.POST);
        Font countFont = Fonts.REGULAR.getFont(8.0F);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + (row + 1) * 9;
                ItemStack stack = mc.player.getInventory().getStack(slotIndex);
                if (!stack.isEmpty() && stack.getCount() > 1) {
                    float slotX = slotsStartX + col * slotSize;
                    float slotY = slotsStartY + row * slotSize;
                    String countText = String.valueOf(stack.getCount());
                    overlayContext.drawText(
                            countFont,
                            countText,
                            slotX + slotSize - 4.0F - countFont.width(countText),
                            slotY + slotSize - 8.0F,
                            Colors.WHITE.withAlpha(alpha)
                    );
                }
            }
        }
    }

    @Override
    public boolean show() {
        return mc.player != null && mc.world != null &&
                !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen);
    }
}
