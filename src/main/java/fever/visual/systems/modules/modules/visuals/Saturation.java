package fever.visual.systems.modules.modules.visuals;

import fever.visual.framework.base.CustomDrawContext;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.interfaces.IScaledResolution;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

@ModuleInfo(name = "Saturation", category = ModuleCategory.VISUALS, desc = "modules.descriptions.saturation")
public class Saturation extends BaseModule implements IMinecraft, IScaledResolution {

    public static Saturation INSTANCE;

    private static final Identifier FOOD_EMPTY = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier FOOD_HALF = Identifier.ofVanilla("hud/food_half");
    private static final Identifier FOOD_FULL = Identifier.ofVanilla("hud/food_full");
    private static final Identifier FOOD_EMPTY_HUNGER = Identifier.ofVanilla("hud/food_empty_hunger");
    private static final Identifier FOOD_HALF_HUNGER = Identifier.ofVanilla("hud/food_half_hunger");
    private static final Identifier FOOD_FULL_HUNGER = Identifier.ofVanilla("hud/food_full_hunger");

    private final BooleanSetting showBackground = new BooleanSetting(this, "modules.settings.saturation.background", "Показывать фон")
            .enabled(true);

    private final SliderSetting xOffset = new SliderSetting(this, "modules.settings.saturation_bar.x_offset", "Смещение по X")
            .min(-100.0F).max(100.0F).step(1.0F).currentValue(0.0F);

    private final SliderSetting yOffset = new SliderSetting(this, "modules.settings.saturation_bar.y_offset", "Смещение по Y")
            .min(-50.0F).max(50.0F).step(1.0F).currentValue(-10.0F);


    public Saturation() {
        INSTANCE = this;
    }

    public static void render(CustomDrawContext customContext) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (INSTANCE.mc.player == null || INSTANCE.mc.world == null) return;
        if (INSTANCE.mc.player.isSpectator() || INSTANCE.mc.player.getAbilities().creativeMode) return;
        if (!INSTANCE.shouldRenderFood(INSTANCE.mc.player)) return;

        float saturation = INSTANCE.mc.player.getHungerManager().getSaturationLevel();
        int foodLevel = INSTANCE.mc.player.getHungerManager().getFoodLevel();

        saturation = Math.max(0.0f, Math.min(saturation, (float) foodLevel));

        if (saturation < 1.0f) return;

        INSTANCE.renderVanillaStyle(customContext, saturation, foodLevel);
    }

    private void renderVanillaStyle(CustomDrawContext drawContext, float saturation, int foodLevel) {
        float right = sr.getScaledWidth() / 2.0f + 91;
        float baseY = sr.getScaledHeight() - 39;
        float x = right - 9 + xOffset.getCurrentValue();
        float y = baseY - 10 + yOffset.getCurrentValue();

        boolean hasHunger = mc.player.hasStatusEffect(StatusEffects.HUNGER);

        for (int i = 0; i < 10; i++) {
            float currentX = x - i * 8;
            float fullThreshold = (float) (i + 1) * 2.0f;
            float halfThreshold = fullThreshold - 1.0f;

            boolean drawFull = saturation >= fullThreshold;
            boolean drawHalf = !drawFull && saturation >= halfThreshold;

            if (!drawFull && !drawHalf) continue;

            if (showBackground.isEnabled()) {
                Identifier emptyTexture;
                if (hasHunger) {
                    emptyTexture = FOOD_EMPTY_HUNGER;
                } else {
                    emptyTexture = FOOD_EMPTY;
                }
                drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, emptyTexture,
                        (int) currentX, (int) y, 9, 9, Colors.getTextColor().withAlpha(50).getRGB());
            }

            Identifier fillTexture;
            if (drawFull) {
                fillTexture = hasHunger ? FOOD_FULL_HUNGER : FOOD_FULL;
            } else {
                fillTexture = hasHunger ? FOOD_HALF_HUNGER : FOOD_HALF;
            }
            drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, fillTexture,
                    (int) currentX, (int) y, 9, 9, ColorRGBA.WHITE.getRGB());
        }
    }

    private boolean shouldRenderFood(PlayerEntity player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof LivingEntity)) {
            return true;
        }
        LivingEntity living = (LivingEntity) vehicle;
        int heartCount = (int) ((living.getMaxHealth() + 0.5f) / 2.0f);
        return Math.min(heartCount, 30) == 0;
    }
}
