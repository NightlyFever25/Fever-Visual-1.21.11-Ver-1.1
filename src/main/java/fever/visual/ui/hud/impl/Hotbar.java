package fever.visual.ui.hud.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.modules.modules.visuals.ArmorDurability;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.PlatformUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Hotbar extends HudElement {
    private static Hotbar instance;

    private static final int SLOTS = 9;
    private static final float SLOT_W = 20.0F;
    private static final float SLOT_H = 18.0F;
    private static final float PAD_X = 6.0F;
    private static final float PAD_Y = 5.0F;
    private static final float SLOT_GAP = 2.0F;

    private static final float LIFT_PX = 3.0F;
    private static final float ITEM_NORM = 14.0F;
    private static final float ITEM_ACT = 16.0F;

    private static final float DOT_W = 4.0F;
    private static final float DOT_H = 4.0F;

    private static final float HAND_SLOT_W = SLOT_W;
    private static final float HAND_SLOT_H = SLOT_H;
    private static final float HAND_SEP_W = 1.0F;
    private static final float HAND_ITEM_SIZE = 16.0F;

    private static final float ICON_W = 9.0F;
    private static final float ICON_H = 9.0F;
    private static final float ICON_GAP = 1.0F;
    private static final int ICON_MAX = 10;
    private static final float STAT_GAP_Y = 4.0F;
    private static final float STAT_PAD_X = 4.0F;
    private static final float STAT_PAD_Y = 4.0F;
    private static final float STATS_SHIFT_X = -4.0F;

    private static final float STAT_ROW_W = ICON_MAX * ICON_W + (ICON_MAX - 1) * ICON_GAP;

    private static final float DUR_BAR_H = 2.0F;
    private static final float DUR_BAR_BG_ALPHA = 85.0F;
    private static final float DUR_BAR_FG_ALPHA = 220.0F;

    private static final float EXPERIENCE_HEIGHT = 4.0F;
    private static final float EXPERIENCE_WIDTH = STAT_ROW_W + 10.0F;
    private static final int MAX_STAT_ROWS = 4;
    private final Animation showAnim = new Animation(300L, Easing.BAKEK);
    private final Animation chatOffset = new Animation(300L, 0.0F, Easing.BAKEK);

    private float handSlotAnim = 0.0F;

    private float visHp = -1.0F;
    private float visFood = -1.0F;
    private float visAir = -1.0F;
    private float visArmor = -1.0F;
    private float visAbsorption = -1.0F;

    private float lastRealHp = -1.0F;
    private float lastRealFood = -1.0F;
    private float lastAir = -1.0F;
    private float lastRealArmor = -1.0F;
    private float lastRealAbsorption = -1.0F;

    private boolean wasCreative = false;
    private float smoothSelectedSlot = -1.0F;

    private static final ColorRGBA DOT_COLOR = ColorRGBA.WHITE;

    private final ColorRGBA heartFull;
    private final ColorRGBA heartHalf;
    private final ColorRGBA heartEmpty;

    private final ColorRGBA foodFull;
    private final ColorRGBA foodHalf;
    private final ColorRGBA foodEmpty;

    private final ColorRGBA oxygenFull;
    private final ColorRGBA oxygenHalf;
    private final ColorRGBA oxygenEmpty;

    private final ColorRGBA armorFull;
    private final ColorRGBA armorHalf;
    private final ColorRGBA armorEmpty;

    private final ColorRGBA absorptionFull;
    private final ColorRGBA absorptionHalf;
    private final ColorRGBA absorptionEmpty;

    private final List<IconBurst> heartBursts = new ArrayList<>();
    private final List<IconBurst> foodBursts = new ArrayList<>();
    private final List<IconBurst> oxygenBursts = new ArrayList<>();
    private final List<IconBurst> armorBursts = new ArrayList<>();
    private final List<IconBurst> absorptionBursts = new ArrayList<>();

    public record SlotHighlightRect(float x, float y, float size) {
    }


    public Hotbar() {
        super("hud.hotbar", "icons/hud/hotbar.png");
        instance = this;

        this.heartFull = Colors.RED;
        this.heartHalf = Colors.RED.withAlpha(160.0F);
        this.heartEmpty = ColorRGBA.WHITE.withAlpha(40.0F);

        this.foodFull = new ColorRGBA(205, 125, 40, 255);
        this.foodHalf = new ColorRGBA(235, 165, 70, 190);
        this.foodEmpty = new ColorRGBA(95, 55, 20, 120);

        this.oxygenFull = new ColorRGBA(80, 160, 255, 255);
        this.oxygenHalf = new ColorRGBA(80, 160, 255, 160);
        this.oxygenEmpty = ColorRGBA.WHITE.withAlpha(40.0F);

        this.armorFull = new ColorRGBA(150, 150, 150, 255);
        this.armorHalf = new ColorRGBA(150, 150, 150, 160);
        this.armorEmpty = new ColorRGBA(80, 80, 80, 120);

        this.absorptionFull = new ColorRGBA(255, 215, 0, 255);
        this.absorptionHalf = new ColorRGBA(255, 215, 0, 160);
        this.absorptionEmpty = new ColorRGBA(180, 150, 0, 120);
    }

    public static boolean shouldSuppressVanilla() {
        return instance != null && instance.isShowing();
    }

    private static final float BOTTOM_OFFSET_Y=-17.0F;
    private static final float CHAT_LIFT_Y= 20.0F;

    @Override
    public void update(UIContext context) {
        float statPanelH = STAT_PAD_Y * 2.0F + ICON_H * MAX_STAT_ROWS + STAT_GAP_Y * (MAX_STAT_ROWS - 1) + EXPERIENCE_HEIGHT + 4.0F;
        float hotbarH = PAD_Y * 2.0F + SLOT_H + DOT_H + 1.0F;
        this.width = PAD_X * 2.0F + SLOTS * SLOT_W + (SLOTS - 1) * SLOT_GAP;
        this.height = statPanelH + STAT_GAP_Y + hotbarH;
        super.update(context);
        boolean isInChat = PlatformUtility.isChatEditorOpen();
        this.chatOffset.update(isInChat);
        this.x = context.getScaledWindowWidth() / 2.0F - this.width / 2.0F;
        this.y = context.getScaledWindowHeight() - this.height - BOTTOM_OFFSET_Y
                - this.chatOffset.getValue() * CHAT_LIFT_Y;
    }
    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        return super.isHovered(mouseX, mouseY);
    }

    public static Hotbar getInstance() {
        return instance;
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
    }

    public SlotHighlightRect getSlotHighlightRect(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOTS) {
            return null;
        }

        int selectedSlot = mc.player != null ? mc.player.getInventory().getSelectedSlot() : slotIndex;
        float selected = this.smoothSelectedSlot < 0.0F ? selectedSlot : this.smoothSelectedSlot;
        float hotbarY = this.getHotbarY();
        float slotsStartX = this.x + PAD_X;
        float slotCenterX = slotsStartX + slotIndex * (SLOT_W + SLOT_GAP) + SLOT_W / 2.0F;
        float focus = getSlotFocus(slotIndex, selected);
        float itemSize = ITEM_NORM + (ITEM_ACT - ITEM_NORM) * focus;
        float itemX = slotCenterX - itemSize / 2.0F;
        float itemY = hotbarY + PAD_Y + (SLOT_H - itemSize) / 2.0F - LIFT_PX * focus + 2.0F;
        return new SlotHighlightRect(itemX, itemY, itemSize);
    }

    private float getHotbarY() {
        float healthAndFoodY = this.y + STAT_PAD_Y + ICON_H * 2 + STAT_GAP_Y * 2;
        float experienceY = healthAndFoodY + ICON_H + STAT_GAP_Y;
        return experienceY + EXPERIENCE_HEIGHT + 4.0F;
    }

    @Override
    protected void renderComponent(UIContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;

        Interface ui = FeverVisual.getInstance()
                .getModuleManager()
                .getModule(Interface.class);

        if (ui == null || player == null || mc.world == null) return;

        boolean creative = player.getAbilities().creativeMode;

        if (creative) {
            wasCreative = true;
            visHp = -1.0F;
            visFood = -1.0F;
            visAir = -1.0F;
            visArmor = -1.0F;
            visAbsorption = -1.0F;
            lastRealHp = -1.0F;
            lastRealFood = -1.0F;
            lastAir = -1.0F;
            lastRealArmor = -1.0F;
            lastRealAbsorption = -1.0F;
            heartBursts.clear();
            foodBursts.clear();
            oxygenBursts.clear();
            armorBursts.clear();
            absorptionBursts.clear();
        } else if (wasCreative) {
            visHp = player.getHealth();
            visFood = player.getHungerManager().getFoodLevel();
            visAir = player.getAir();
            visArmor = player.getArmor() / 2.0F;
            visAbsorption = player.getAbsorptionAmount() / 2.0F;

            lastRealHp = visHp;
            lastRealFood = visFood;
            lastAir = visAir;
            lastRealArmor = visArmor;
            lastRealAbsorption = visAbsorption;

            wasCreative = false;
        }

        showAnim.update(true);
        float show = showAnim.getValue();
        float alpha = 255.0F * show;

        ItemStack offHandStack = player.getOffHandStack();
        boolean hasOffHandItem = !offHandStack.isEmpty();

        float handTarget = hasOffHandItem ? 1.0F : 0.0F;
        handSlotAnim += (handTarget - handSlotAnim) * 0.18F;
        if (Math.abs(handSlotAnim - handTarget) < 0.001F) {
            handSlotAnim = handTarget;
        }

        boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
        float glassIntensity = Interface.glass();
        float minimalizm = Interface.minimalizm();
        boolean showGlass = Interface.showGlass();
        boolean showMinimalizm = Interface.showMinimalizm();

        ColorRGBA bgColor = Colors.getBackgroundColor()
                .withAlpha(255.0F * (dark ? 0.8F - 0.6F * glassIntensity : 0.7F));

        float realArmor = player.getArmor() / 2.0F;
        float realAbsorption = player.getAbsorptionAmount() / 2.0F;
        boolean hasArmor = realArmor > 0.0F;
        boolean hasAbsorption = realAbsorption > 0.0F;
        float healthAndFoodY = this.y + STAT_PAD_Y + ICON_H * 2 + STAT_GAP_Y * 2;
        float armorY = -1;
        float absorptionY = -1;

        if (hasAbsorption && hasArmor) {
            absorptionY = healthAndFoodY - (ICON_H + STAT_GAP_Y);
            armorY = absorptionY - (ICON_H + STAT_GAP_Y);
        } else if (hasAbsorption) {
            absorptionY = healthAndFoodY - (ICON_H + STAT_GAP_Y);
        } else if (hasArmor) {
            armorY = healthAndFoodY - (ICON_H + STAT_GAP_Y);
        }

        float experienceY = healthAndFoodY + ICON_H + STAT_GAP_Y;
        float hotbarY = experienceY + EXPERIENCE_HEIGHT + 4.0F;

        float leftStartX = this.x + STAT_PAD_X - 5.0F + STATS_SHIFT_X;
        float rightStartX = this.x + this.width - STAT_PAD_X - STAT_ROW_W + 5.0F + STATS_SHIFT_X;
        float experienceX = this.x + (this.width - EXPERIENCE_WIDTH) / 2.0F + STATS_SHIFT_X;


        float baseHotbarW = PAD_X * 2.0F + SLOTS * SLOT_W + (SLOTS - 1) * SLOT_GAP;
        float handCellW = HAND_SLOT_W * handSlotAnim;
        float hotbarH = PAD_Y * 2.0F + SLOT_H + DOT_H + 1.0F;

        float hotbarX = this.x;
        float hotbarBgX = hotbarX - handCellW;
        float hotbarBgW = baseHotbarW + handCellW;
        float statBgX = this.x - 4.0F + STATS_SHIFT_X;
        float statBgY = this.y;
        float statBgW = this.width + 8.0F;
        float statBgH = hotbarY - this.y - 1.0F;

        if (!creative) {
            float realHp = player.getHealth();
            float realFood = player.getHungerManager().getFoodLevel();

            if (visHp < 0.0F) visHp = realHp;
            if (visFood < 0.0F) visFood = realFood;
            if (visArmor < 0.0F) visArmor = realArmor;
            if (visAbsorption < 0.0F) visAbsorption = realAbsorption;

            visHp += (realHp - visHp) * 0.12F;
            visFood += (realFood - visFood) * 0.12F;
            visArmor += (realArmor - visArmor) * 0.12F;
            visAbsorption += (realAbsorption - visAbsorption) * 0.12F;

            if (Math.abs(visHp - realHp) < 0.01F) visHp = realHp;
            if (Math.abs(visFood - realFood) < 0.01F) visFood = realFood;
            if (Math.abs(visArmor - realArmor) < 0.01F) visArmor = realArmor;
            if (Math.abs(visAbsorption - realAbsorption) < 0.01F) visAbsorption = realAbsorption;

            float hpIcons = realHp / 2.0F;
            float foodIcons = realFood / 2.0F;
            float armorIcons = realArmor;
            float absorptionIcons = realAbsorption;
            if (lastRealHp >= 0.0F && realHp < lastRealHp) {
                spawnDisappearBursts(lastRealHp / 2.0F, realHp / 2.0F, leftStartX, healthAndFoodY, heartBursts, heartFull);
            }
            if (lastRealFood >= 0.0F && realFood < lastRealFood) {
                spawnDisappearBurstsReverse(lastRealFood / 2.0F, realFood / 2.0F, rightStartX, healthAndFoodY, foodBursts, foodFull);
            }
            if (hasArmor && lastRealArmor >= 0.0F && realArmor < lastRealArmor && armorY >= 0) {
                spawnDisappearBursts(lastRealArmor, realArmor, leftStartX, armorY, armorBursts, armorFull);
            }
            if (hasAbsorption && lastRealAbsorption >= 0.0F && realAbsorption < lastRealAbsorption && absorptionY >= 0) {
                spawnDisappearBursts(lastRealAbsorption, realAbsorption, leftStartX, absorptionY, absorptionBursts, absorptionFull);
            }

            lastRealHp = realHp;
            lastRealFood = realFood;
            lastRealArmor = realArmor;
            lastRealAbsorption = realAbsorption;
            if (hasArmor && armorY >= 0) {
                drawStatRow(context, leftStartX, armorY, armorIcons, armorFull, armorHalf, armorEmpty, false);
                renderBursts(context, armorBursts);
                updateArmorBursts();
            } else {
                armorBursts.clear();
            }
            if (hasAbsorption && absorptionY >= 0) {
                drawStatRow(context, leftStartX, absorptionY, absorptionIcons, absorptionFull, absorptionHalf, absorptionEmpty, false);
                renderBursts(context, absorptionBursts);
                updateAbsorptionBursts();
            } else {
                absorptionBursts.clear();
            }
            drawStatRow(context, leftStartX, healthAndFoodY, hpIcons, heartFull, heartHalf, heartEmpty, false);
            renderBursts(context, heartBursts);
            drawStatRow(context, rightStartX, healthAndFoodY, foodIcons, foodFull, foodHalf, foodEmpty, true);
            renderBursts(context, foodBursts);
            updateBursts();
            boolean inWater = player.isSubmergedInWater() || player.isTouchingWater();
            int currentAir = player.getAir();

            if (inWater) {
                if (visAir < 0.0F) visAir = currentAir;
                visAir += (currentAir - visAir) * 0.08F;

                if (Math.abs(visAir - currentAir) < 0.01F) visAir = currentAir;

                float airIcons = visAir / 30.0F;

                if (lastAir < 0.0F) {
                    lastAir = currentAir;
                }

                float airRowY = healthAndFoodY - (ICON_H + STAT_GAP_Y);

                if (currentAir < lastAir) {
                    spawnDisappearBurstsReverse(lastAir / 30.0F, currentAir / 30.0F, rightStartX, airRowY, oxygenBursts, oxygenFull);
                }
                lastAir = currentAir;
                drawStatRow(context, rightStartX, airRowY, airIcons, oxygenFull, oxygenHalf, oxygenEmpty, true);
                renderBursts(context, oxygenBursts);
                updateOxygenBursts();
            } else {
                lastAir = -1.0F;
                visAir = -1.0F;
                oxygenBursts.clear();
            }
            drawExperienceBar(context, player, experienceX, experienceY, show);
        }
        if (showMinimalizm) {
            context.drawBlurredRect(
                    hotbarBgX, hotbarY, hotbarBgW, hotbarH,
                    45.0F, 7.0F,
                    BorderRadius.all(6.0F),
                    ColorRGBA.WHITE.withAlpha(alpha * minimalizm)
            );
        }

        if (showGlass) {
            context.drawLiquidGlass(
                    hotbarBgX, hotbarY, hotbarBgW, hotbarH,
                    7.0F,
                    0.08F - 0.07F * this.dragAnim.getValue(),
                    BorderRadius.all(6.0F),
                    ColorRGBA.WHITE.withAlpha(alpha * glassIntensity)
            );
        }

        context.drawSquircle(
                hotbarBgX, hotbarY, hotbarBgW, hotbarH,
                7.0F,
                BorderRadius.all(6.0F),
                bgColor.withAlpha(bgColor.getAlpha() * show)
        );
        float slotsStartX = hotbarX + PAD_X;
        if (handSlotAnim > 0.01F && hasOffHandItem) {
            float slotY = hotbarY + PAD_Y;
            float handCellX = hotbarX - handCellW + SLOT_GAP * 0.25F + 4.0F;

            float sepX = hotbarX - 0.5F + 4.0F;
            float sepY = slotY + 4.0F;
            float sepH = HAND_SLOT_H - 6.0F;
            context.drawRoundedRect(sepX, sepY, HAND_SEP_W, sepH, BorderRadius.all(0.5F), new ColorRGBA(255, 255, 255, (int) (95.0F * handSlotAnim * show)));
            float itemScale = 0.90F + 0.10F * handSlotAnim;
            float itemSize = HAND_ITEM_SIZE * itemScale;
            float itemX = handCellX + (handCellW - itemSize) / 2.0F;
            float itemY = slotY + (HAND_SLOT_H - itemSize) / 2.0F - 0.5F * handSlotAnim + 2.0F;
            context.drawItem(offHandStack, (int) itemX, (int) itemY);
        }
        Font font = Fonts.REGULAR.getFont(8.5F);
        int selectedSlot = player.getInventory().getSelectedSlot();
        if (smoothSelectedSlot < 0.0F) {
            smoothSelectedSlot = selectedSlot;
        } else {
            smoothSelectedSlot += (selectedSlot - smoothSelectedSlot) * 0.22F;
        }
        if (Math.abs(smoothSelectedSlot - selectedSlot) < 0.001F) {
            smoothSelectedSlot = selectedSlot;
        }
        float dotCenterX = slotsStartX + smoothSelectedSlot * (SLOT_W + SLOT_GAP) + SLOT_W / 2.0F;
        float dotFocus = getSlotFocus(selectedSlot, smoothSelectedSlot);
        float dotScale = 0.80F + 0.20F * dotFocus;
        float dotW = DOT_W * dotScale;
        float dotH = DOT_H * dotScale;
        float dotY = hotbarY + PAD_Y + SLOT_H - 0.5F + 2.0F;

        for (int i = 0; i < SLOTS; i++) {
            float focus = getSlotFocus(i, smoothSelectedSlot);
            float slotCenterX = slotsStartX + i * (SLOT_W + SLOT_GAP) + SLOT_W / 2.0F;
            float slotTopY = hotbarY + PAD_Y;

            ItemStack stack = player.getInventory().getStack(i);

            if (!stack.isEmpty()) {
                float itemSize = ITEM_NORM + (ITEM_ACT - ITEM_NORM) * focus;
                float itemX = slotCenterX - itemSize / 2.0F;
                float itemY = slotTopY + (SLOT_H - itemSize) / 2.0F - LIFT_PX * focus + 2.0F;
                context.drawItem(stack, (int) itemX, (int) itemY);
            } else {
                String num = String.valueOf(i + 1);
                float numW = font.width(num);
                float numH = font.height();
                float numX = slotCenterX - numW / 2.0F;
                float numY = slotTopY + (SLOT_H - numH) / 2.0F - LIFT_PX * focus + 2.0F;
                float slotAlpha = (110.0F + 150.0F * focus) * show;
                context.drawText(font, num, numX, numY, ColorRGBA.WHITE.withAlpha(slotAlpha));
            }
        }
        context.drawRoundedRect(dotCenterX - dotW / 2.0F, dotY, dotW, dotH, BorderRadius.all(dotH / 2.0F), DOT_COLOR.withAlpha(255.0F * show));
        this.renderItemOverlays(context, player, offHandStack, hasOffHandItem, hotbarX, hotbarY, slotsStartX, show);
    }

    private void renderItemOverlays(UIContext context, PlayerEntity player, ItemStack offHandStack, boolean hasOffHandItem, float hotbarX, float hotbarY, float slotsStartX, float show) {
        if (!hasVisibleItemOverlay(player, offHandStack, hasOffHandItem)) {
            return;
        }

        context.getOriginalContext().createNewRootLayer();
        CustomDrawContext overlayContext = CustomDrawContext.isolated(context, CustomDrawContext.Pass.POST);
        if (handSlotAnim > 0.01F && hasOffHandItem) {
            float slotY = hotbarY + PAD_Y;
            float handCellW = HAND_SLOT_W * handSlotAnim;
            float handCellX = hotbarX - handCellW + SLOT_GAP * 0.25F + 4.0F;
            float itemScale = 0.90F + 0.10F * handSlotAnim;
            float itemSize = HAND_ITEM_SIZE * itemScale;
            float itemX = handCellX + (handCellW - itemSize) / 2.0F;
            float itemY = slotY + (HAND_SLOT_H - itemSize) / 2.0F - 0.5F * handSlotAnim + 2.0F;
            drawDurabilityBar(overlayContext, offHandStack, itemX, itemY, itemSize, show, handSlotAnim);
            if (offHandStack.getCount() > 1) {
                Font smallFont = Fonts.REGULAR.getFont(6.0F);
                String cnt = String.valueOf(offHandStack.getCount());
                float cntX = handCellX + handCellW - smallFont.width(cnt) - 1.0F;
                float cntY = itemY + itemSize - smallFont.height() - 1.0F;
                drawItemCount(overlayContext, smallFont, cnt, cntX, cntY, ColorRGBA.WHITE.withAlpha((145.0F + 110.0F * handSlotAnim) * show));
            }
        }

        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            float focus = getSlotFocus(i, smoothSelectedSlot);
            float slotCenterX = slotsStartX + i * (SLOT_W + SLOT_GAP) + SLOT_W / 2.0F;
            float slotTopY = hotbarY + PAD_Y;
            float itemSize = ITEM_NORM + (ITEM_ACT - ITEM_NORM) * focus;
            float itemX = slotCenterX - itemSize / 2.0F;
            float itemY = slotTopY + (SLOT_H - itemSize) / 2.0F - LIFT_PX * focus + 2.0F;
            drawDurabilityBar(overlayContext, stack, itemX, itemY, itemSize, show, 1.0F);
            if (stack.getCount() > 1) {
                Font smallFont = Fonts.REGULAR.getFont(6.0F);
                String cnt = String.valueOf(stack.getCount());
                float cntX = slotCenterX + itemSize / 2.0F - smallFont.width(cnt);
                float cntY = itemY + itemSize - smallFont.height();
                drawItemCount(overlayContext, smallFont, cnt, cntX, cntY, ColorRGBA.WHITE.withAlpha((145.0F + 110.0F * focus) * show));
            }
        }
    }

    private boolean hasVisibleItemOverlay(PlayerEntity player, ItemStack offHandStack, boolean hasOffHandItem) {
        if (handSlotAnim > 0.01F && hasOffHandItem && needsItemOverlay(offHandStack)) {
            return true;
        }
        for (int i = 0; i < SLOTS; i++) {
            if (needsItemOverlay(player.getInventory().getStack(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean needsItemOverlay(ItemStack stack) {
        return !stack.isEmpty() && (stack.getCount() > 1 || stack.isDamageable() && stack.getMaxDamage() > 0);
    }

    private void drawItemCount(CustomDrawContext context, Font font, String text, float x, float y, ColorRGBA color) {
        context.drawText(font, text, x, y, color);
    }

    private void drawExperienceBar(UIContext context, PlayerEntity player, float x, float y, float show) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        float progress = MathHelper.clamp(player.experienceProgress, 0.0F, 1.0F);
        int level = player.experienceLevel;
        float radius = EXPERIENCE_HEIGHT / 2.0F;
        context.drawRoundedRect(x, y, EXPERIENCE_WIDTH, EXPERIENCE_HEIGHT, BorderRadius.all(radius), new ColorRGBA(255, 255, 255, (int) (32.0F * show)));
        if (progress > 0.0F) {
            float filledWidth = Math.max(EXPERIENCE_HEIGHT, EXPERIENCE_WIDTH * progress);
            ColorRGBA accent = Colors.ACCENT;
            context.drawRoundedRect(x, y, filledWidth, EXPERIENCE_HEIGHT, BorderRadius.all(radius),
                    accent.withAlpha((int) (215.0F * show)));
            context.drawRoundedRect(x + 1.0F, y + 1.0F, Math.max(0.0F, filledWidth - 2.0F), 1.0F,
                    BorderRadius.all(0.5F), ColorRGBA.WHITE.withAlpha((int) (42.0F * show)));
        }
        if (level > 0) {
            Font font = Fonts.SEMIBOLD.getFont(7.0F);
            String text = String.valueOf(level);
            float textX = x + EXPERIENCE_WIDTH / 2.0F - font.width(text) / 2.0F;
            float textY = y - 7.0F;
            context.drawText(font, text, textX + 0.7F, textY + 0.7F, ColorRGBA.BLACK.withAlpha((int) (115.0F * show)));
            context.drawText(font, text, textX, textY, Colors.getTextColor().withAlpha((int) (225.0F * show)));
        }
    }

    private void drawDurabilityBar(CustomDrawContext context,
                                   ItemStack stack,
                                   float itemX,
                                   float itemY,
                                   float itemSize,
                                   float show,
                                   float anim) {
        if (!stack.isDamageable() || stack.getMaxDamage() <= 0) return;

        int maxDamage = stack.getMaxDamage();
        int damage = stack.getDamage();
        float durability = MathHelper.clamp((maxDamage - damage) / (float) maxDamage, 0.0F, 1.0F);

        float barW = Math.max(8.0F, itemSize);
        float barX = itemX;
        float barY = itemY + itemSize + 1.0F;

        context.drawRoundedRect(barX, barY, barW, DUR_BAR_H, BorderRadius.all(1.0F), new ColorRGBA(0, 0, 0, (int) (DUR_BAR_BG_ALPHA * show * anim)));

        float fillW = barW * durability;
        if (fillW > 0.0F) {
            context.drawRoundedRect(barX, barY, fillW, DUR_BAR_H, BorderRadius.all(1.0F), getDurabilityColor(stack, durability).withAlpha(DUR_BAR_FG_ALPHA * show * anim));
        }
    }

    private ColorRGBA getDurabilityColor(ItemStack stack, float durability) {
        durability = MathHelper.clamp(durability, 0.0F, 1.0F);

        ArmorDurability module = ArmorDurability.getModule();
        if (module != null && module.isEnabled() && isArmorLike(stack)) {
            return ColorRGBA.fromInt(module.getColorIntByPercent(durability));
        }

        int r = (int) (255.0F * (1.0F - durability));
        int g = (int) (255.0F * durability);
        int b = 40;

        return new ColorRGBA(r, g, b, 255);
    }

    private boolean isArmorLike(ItemStack stack) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) {
            return false;
        }

        EquipmentSlot slot = equippable.slot();
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }

    private float getSlotFocus(int slotIndex, float selectedSlot) {
        float distance = Math.abs(slotIndex - selectedSlot);
        float focus = 1.0F / (1.0F + distance * distance * 0.85F);
        return MathHelper.clamp(focus, 0.0F, 1.0F);
    }

    private void spawnDisappearBursts(float prevValue,
                                      float currValue,
                                      float startX,
                                      float startY,
                                      List<IconBurst> target,
                                      ColorRGBA color) {
        for (int i = 0; i < ICON_MAX; i++) {
            int prevState = getIconState(prevValue, i);
            int currState = getIconState(currValue, i);

            if (currState < prevState) {
                float iconX = startX + i * (ICON_W + ICON_GAP);
                target.add(new IconBurst(iconX, startY, color));
            }
        }
    }

    private void spawnDisappearBurstsReverse(float prevValue,
                                             float currValue,
                                             float startX,
                                             float startY,
                                             List<IconBurst> target,
                                             ColorRGBA color) {
        for (int i = 0; i < ICON_MAX; i++) {
            int iconIndex = ICON_MAX - 1 - i;
            int prevState = getIconStateReverse(prevValue, iconIndex);
            int currState = getIconStateReverse(currValue, iconIndex);

            if (currState < prevState) {
                float iconX = startX + iconIndex * (ICON_W + ICON_GAP);
                target.add(new IconBurst(iconX, startY, color));
            }
        }
    }

    private int getIconState(float value, int index) {
        float remaining = value - index;
        if (remaining >= 1.0F) return 2;
        if (remaining >= 0.5F) return 1;
        return 0;
    }

    private int getIconStateReverse(float value, int index) {
        float remaining = value - (ICON_MAX - 1 - index);
        if (remaining >= 1.0F) return 2;
        if (remaining >= 0.5F) return 1;
        return 0;
    }

    private void updateBursts() {
        Iterator<IconBurst> it = heartBursts.iterator();
        while (it.hasNext()) {
            if (it.next().isFinished()) {
                it.remove();
            }
        }

        it = foodBursts.iterator();
        while (it.hasNext()) {
            if (it.next().isFinished()) {
                it.remove();
            }
        }
    }

    private void updateOxygenBursts() {
        Iterator<IconBurst> it = oxygenBursts.iterator();
        while (it.hasNext()) {
            if (it.next().isFinished()) {
                it.remove();
            }
        }
    }

    private void updateArmorBursts() {
        Iterator<IconBurst> it = armorBursts.iterator();
        while (it.hasNext()) {
            if (it.next().isFinished()) {
                it.remove();
            }
        }
    }

    private void updateAbsorptionBursts() {
        Iterator<IconBurst> it = absorptionBursts.iterator();
        while (it.hasNext()) {
            if (it.next().isFinished()) {
                it.remove();
            }
        }
    }

    private void renderBursts(UIContext context, List<IconBurst> bursts) {
        for (IconBurst burst : bursts) {
            float t = burst.getProgress();
            float ease = easeOutCubic(t);

            float scale = 1.0F + 0.55F * ease;
            float alpha = 1.0F - ease;
            float rise = 9.0F * ease;

            float size = ICON_W * scale;
            float x = burst.centerX - size / 2.0F;
            float y = burst.centerY - size / 2.0F - rise;

            context.drawRoundedRect(
                    x,
                    y,
                    size,
                    size,
                    BorderRadius.all(2.0F),
                    burst.color.withAlpha(220.0F * alpha)
            );
        }
    }

    private float easeOutCubic(float t) {
        float inv = 1.0F - MathHelper.clamp(t, 0.0F, 1.0F);
        return 1.0F - inv * inv * inv;
    }

    private void drawStatRow(UIContext context,
                             float startX, float startY,
                             float value,
                             ColorRGBA colorFull,
                             ColorRGBA colorHalf,
                             ColorRGBA colorEmpty,
                             boolean reverse) {
        for (int i = 0; i < ICON_MAX; i++) {
            float ix = startX + i * (ICON_W + ICON_GAP);
            float remaining;

            if (reverse) {
                remaining = value - (ICON_MAX - 1 - i);
            } else {
                remaining = value - i;
            }

            if (remaining >= 1.0F) {
                drawPixelRoundedRect(context, ix, startY, ICON_W, ICON_H, colorFull);
            } else if (remaining > 0.0F) {
                drawPixelRoundedRect(context, ix, startY, ICON_W, ICON_H, colorEmpty);

                if (remaining >= 0.5F) {
                    if (reverse) {
                        context.drawRect(ix + ICON_W / 2.0F, startY + 1.0F, ICON_W / 2.0F - 1.0F, ICON_H - 2.0F, colorHalf);
                    } else {
                        context.drawRect(ix + 1.0F, startY + 1.0F, ICON_W / 2.0F - 1.0F, ICON_H - 2.0F, colorHalf);
                    }
                }
            } else {
                drawPixelRoundedRect(context, ix, startY, ICON_W, ICON_H, colorEmpty);
            }
        }
    }

    private void drawPixelRoundedRect(UIContext context, float x, float y, float width, float height, ColorRGBA color) {
        context.drawRect(x + 1.0F, y, width - 2.0F, height, color);
        context.drawRect(x, y + 1.0F, width, height - 2.0F, color);
    }

    private static final class IconBurst {
        private final float centerX;
        private final float centerY;
        private final ColorRGBA color;
        private final long startTime;
        private final long duration = 220L;

        private IconBurst(float startX, float startY, ColorRGBA color) {
            this.centerX = startX + ICON_W / 2.0F;
            this.centerY = startY + ICON_H / 2.0F;
            this.color = color;
            this.startTime = Util.getMeasuringTimeMs();
        }

        private float getProgress() {
            long now = Util.getMeasuringTimeMs();
            return MathHelper.clamp((now - startTime) / (float) duration, 0.0F, 1.0F);
        }

        private boolean isFinished() {
            return getProgress() >= 1.0F;
        }
    }
}
