package fever.visual.ui.hud.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.modules.visuals.ArmorDurability;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import net.minecraft.item.ItemStack;

public class ArmorHUD extends HudElement {
    private final Animation[] armorAnimations = new Animation[4];

    private final BooleanSetting alwaysShow = new BooleanSetting(this, "hud.always_display");

    public ArmorHUD() {
        super("hud.armor", "icons/hud/armor.png");
        for (int i = 0; i < armorAnimations.length; i++) {
            armorAnimations[i] = new Animation(300L, 0.0F, Easing.BAKEK);
        }
    }

    @Override
    public void update(UIContext context) {
        int itemSize = 16;
        int spacing = 2;
        int padding = 3;
        this.width = padding * 2 + (itemSize * 4) + (spacing * 3);
        this.height = padding * 2 + itemSize - 2;
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

        float alpha = 255.0F * this.animation.getValue() * this.visible.getValue();
        if (showMinimalizm) {
            context.drawBlurredRect(
                    this.x, this.y, this.width, this.height,
                    45.0F, 7.0F,
                    BorderRadius.all(3.0F),
                    ColorRGBA.WHITE.withAlpha(alpha * minimalizm)
            );
        }

        if (showGlass) {
            context.drawLiquidGlass(
                    this.x, this.y, this.width, this.height,
                    7.0F,
                    0.08F - 0.07F * this.dragAnim.getValue(),
                    BorderRadius.all(3.0F),
                    ColorRGBA.WHITE.withAlpha(alpha * glassIntensity)
            );
        }

        context.drawSquircle(this.x, this.y, this.width, this.height, 7.0F,
                BorderRadius.all(3.0F), bgColor.withAlpha((int)(bgColor.getAlpha() * this.animation.getValue())));

        int itemSize = 16;
        int spacing = 2;
        int padding = 3;
        float startX = this.x + padding;
        float slotHeight = itemSize - 1;
        float slotY = this.y + (this.height - slotHeight) / 2f;

        boolean hasAnyArmor = false;
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(36 + i);
            if (!stack.isEmpty()) {
                hasAnyArmor = true;
                break;
            }
        }
        boolean shouldRenderSlots = alwaysShow.isEnabled() || hasAnyArmor;

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(36 + i);

            armorAnimations[3 - i].update(shouldRenderSlots);
            float anim = armorAnimations[3 - i].getValue();

            if (anim <= 0.01F || stack.isEmpty() || !shouldRenderSlots) {
                continue;
            }

            float itemX = startX + (3 - i) * (itemSize + spacing);
            float itemY = slotY;

            context.drawItem(stack, itemX, itemY, 1.0F);
        }

        boolean hasDurabilityOverlay = false;
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(36 + i);
            float durability = getDurabilityPercent(stack);
            if (!stack.isEmpty() && durability > 0.0F && durability < 1.0F) {
                hasDurabilityOverlay = true;
                break;
            }
        }
        if (!hasDurabilityOverlay) {
            return;
        }

        context.getOriginalContext().createNewRootLayer();
        CustomDrawContext overlayContext = CustomDrawContext.isolated(context, CustomDrawContext.Pass.POST);
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(36 + i);
            float anim = armorAnimations[3 - i].getValue();
            if (anim <= 0.01F || stack.isEmpty() || !shouldRenderSlots) {
                continue;
            }

            float itemX = startX + (3 - i) * (itemSize + spacing);
            float itemY = slotY;
            float durabilityPercent = getDurabilityPercent(stack);
            if (durabilityPercent < 1.0F && durabilityPercent > 0.0F) {
                float barWidth = itemSize - 2.0F;
                float barHeight = 1.5F;
                float barX = itemX + 1.0F;
                float barY = itemY + itemSize - barHeight - 1.0F;
                overlayContext.drawRoundedRect(
                        barX, barY, barWidth, barHeight,
                        BorderRadius.all(0.75F),
                        new ColorRGBA(20, 20, 20, (int)(200 * anim * this.animation.getValue()))
                );
                int barColor = getDurabilityColor(durabilityPercent);
                overlayContext.drawRoundedRect(
                        barX, barY, barWidth * durabilityPercent, barHeight,
                        BorderRadius.all(0.75F),
                        new ColorRGBA(
                                (barColor >> 16) & 0xFF,
                                (barColor >> 8) & 0xFF,
                                barColor & 0xFF,
                                (int)(255 * anim * this.animation.getValue())
                        )
                );
            }
        }
    }

    private float getDurabilityPercent(ItemStack stack) {
        if (!stack.isEmpty() && stack.isDamageable()) {
            int maxDurability = stack.getMaxDamage();
            int currentDamage = stack.getDamage();
            if (currentDamage >= maxDurability) {
                return 0.0F;
            } else {
                return 1.0F - (float) currentDamage / maxDurability;
            }
        }
        return 1.0F;
    }

    private int getDurabilityColor(float percent) {
        ArmorDurability module = ArmorDurability.getModule();
        if (module != null && module.isEnabled()) {
            return module.getColorIntByPercent(percent);
        }

        if (percent <= 0.0f) return new ColorRGBA(255, 85, 85, 255).getRGB();
        if (percent >= 1.0f) return new ColorRGBA(85, 255, 85, 255).getRGB();
        if (percent < 0.5f) {
            int red = 255;
            int green = (int)(510 * percent);
            return new ColorRGBA(red, Math.min(green, 255), 85, 255).getRGB();
        } else {
            int red = (int)(510 * (1 - percent));
            int green = 255;
            return new ColorRGBA(Math.max(red, 85), green, 85, 255).getRGB();
        }
    }

    @Override
    public boolean show() {
        if (mc.player == null || mc.world == null) return false;
        if (alwaysShow.isEnabled()) return true;
        for (int i = 0; i < 4; i++) {
            if (!mc.player.getInventory().getStack(36 + i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
