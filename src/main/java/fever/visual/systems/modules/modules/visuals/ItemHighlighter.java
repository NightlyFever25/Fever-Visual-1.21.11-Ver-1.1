package fever.visual.systems.modules.modules.visuals;


import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.mixin.accessors.HandledScreenAccessor;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.PostHudRenderEvent;
import fever.visual.systems.event.impl.render.ScreenRenderEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.PotionUtility;
import fever.visual.utility.interfaces.IScaledResolution;
import fever.visual.ui.hud.impl.Hotbar;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

@ModuleInfo(
        name = "Item Highlighter",
        category = ModuleCategory.VISUALS,
        desc = "modules.descriptions.item_highlighter"
)
@Environment(EnvType.CLIENT)
public class ItemHighlighter extends BaseModule {
   private static final float HIGHLIGHT_ALPHA = 0.55F;
   private static final int HOTBAR_SLOT_SIZE = 20;
   private static final int HOTBAR_TOTAL_WIDTH = 182;
   private static final int HOTBAR_TOP_OFFSET = 22;
   private final SelectSetting highlightItems = new SelectSetting(this, "modules.settings.item_highlighter.items").min(0);
   private final SelectSetting.Value hlTotem = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.totem").select();
   private final SelectSetting.Value hlEnchantedApple = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.enchanted_apple").select();
   private final SelectSetting.Value hlGoldenApple = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.golden_apple").select();
   private final SelectSetting.Value hlGoldenCarrot = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.golden_carrot").select();
   private final SelectSetting.Value hlHealingPotion = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.healing_potion").select();
   private final SelectSetting.Value hlEnderEye = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.ender_eye").select();
   private final SelectSetting.Value hlNetheriteScrap = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.netherite_scrap").select();
   private final SelectSetting.Value hlFireCharge = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.fire_charge").select();
   private final SelectSetting.Value hlNetherStar = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.nether_star").select();
   private final SelectSetting.Value hlDriedKelp = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.dried_kelp").select();
   private final SelectSetting.Value hlPhantomMembrane = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.phantom_membrane").select();
   private final SelectSetting.Value hlSugar = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.sugar").select();
   private final SelectSetting.Value hlSnowball = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.snowball").select();
   private final SelectSetting.Value hlPoppedChorusFruit = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.popped_chorus_fruit").select();
   private final SelectSetting.Value hlPrismarineShard = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.prismarine_shard").select();
   private final SelectSetting.Value hlFireworkStar = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.firework_star").select();
   private final SelectSetting.Value hlMagentaShulkerBox = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.magenta_shulker_box").select();
   private final SelectSetting.Value hlCrossbow = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.crossbow").select();
   private final SelectSetting.Value hlPlayerHead = new SelectSetting.Value(this.highlightItems, "modules.settings.item_highlighter.player_head").select();
   private final ColorSetting colorTotem = new ColorSetting(this, "modules.settings.item_highlighter.color_totem", () -> !this.hlTotem.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorEnchantedApple = new ColorSetting(this, "modules.settings.item_highlighter.color_enchanted_apple", () -> !this.hlEnchantedApple.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorGoldenApple = new ColorSetting(this, "modules.settings.item_highlighter.color_golden_apple", () -> !this.hlGoldenApple.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorGoldenCarrot = new ColorSetting(this, "modules.settings.item_highlighter.color_golden_carrot", () -> !this.hlGoldenCarrot.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorHealingPotion = new ColorSetting(this, "modules.settings.item_highlighter.color_healing_potion", () -> !this.hlHealingPotion.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorEnderEye = new ColorSetting(this, "modules.settings.item_highlighter.color_ender_eye", () -> !this.hlEnderEye.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorNetheriteScrap = new ColorSetting(this, "modules.settings.item_highlighter.color_netherite_scrap", () -> !this.hlNetheriteScrap.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorFireCharge = new ColorSetting(this, "modules.settings.item_highlighter.color_fire_charge", () -> !this.hlFireCharge.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorNetherStar = new ColorSetting(this, "modules.settings.item_highlighter.color_nether_star", () -> !this.hlNetherStar.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorDriedKelp = new ColorSetting(this, "modules.settings.item_highlighter.color_dried_kelp", () -> !this.hlDriedKelp.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorPhantomMembrane = new ColorSetting(this, "modules.settings.item_highlighter.color_phantom_membrane", () -> !this.hlPhantomMembrane.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorSugar = new ColorSetting(this, "modules.settings.item_highlighter.color_sugar", () -> !this.hlSugar.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorSnowball = new ColorSetting(this, "modules.settings.item_highlighter.color_snowball", () -> !this.hlSnowball.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorPoppedChorusFruit = new ColorSetting(this, "modules.settings.item_highlighter.color_popped_chorus_fruit", () -> !this.hlPoppedChorusFruit.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorPrismarineShard = new ColorSetting(this, "modules.settings.item_highlighter.color_prismarine_shard", () -> !this.hlPrismarineShard.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorFireworkStar = new ColorSetting(this, "modules.settings.item_highlighter.color_firework_star", () -> !this.hlFireworkStar.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorMagentaShulkerBox = new ColorSetting(this, "modules.settings.item_highlighter.color_magenta_shulker_box", () -> !this.hlMagentaShulkerBox.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorCrossbow = new ColorSetting(this, "modules.settings.item_highlighter.color_crossbow", () -> !this.hlCrossbow.isSelected()).color(Colors.getAccentColor());
   private final ColorSetting colorPlayerHead = new ColorSetting(this, "modules.settings.item_highlighter.color_player_head", () -> !this.hlPlayerHead.isSelected()).color(Colors.getAccentColor());
   private final EventListener<ScreenRenderEvent> onScreenRender = event -> {
      if (mc.player != null && mc.currentScreen != null && mc.currentScreen instanceof HandledScreenAccessor screen) {
         int screenX = screen.getX();
         int screenY = screen.getY();

         for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (slot.hasStack()) {
               ColorRGBA c = this.getColorForStack(slot.getStack());
               if (c != null) {
                  float x = screenX + slot.x;
                  float y = screenY + slot.y;
                  event.getContext().drawRect(x, y, 16.0F, 16.0F, c);
                  ColorRGBA borderColor = new ColorRGBA(
                     Math.min(255.0F, c.getRed() * 1.3F), Math.min(255.0F, c.getGreen() * 1.3F), Math.min(255.0F, c.getBlue() * 1.3F), c.getAlpha()
                  );
                  event.getContext().drawRoundedBorder(x, y, 16.0F, 16.0F, 0.5F, BorderRadius.all(0.0F), borderColor);
               }
            }
         }
      }
   };
   private final EventListener<PostHudRenderEvent> onHudRender = event -> {
      if (mc.player != null) {
         CustomDrawContext ctx = event.getContext();
         Hotbar hotbar = Hotbar.getInstance();
         boolean useCustomHotbar = hotbar != null && hotbar.isShowing();
         int w = (int) IScaledResolution.sr.getScaledWidth();
         int h = (int)IScaledResolution.sr.getScaledHeight();
         int left = (w - HOTBAR_TOTAL_WIDTH) / 2;
         int top = h - HOTBAR_TOP_OFFSET;

         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
               ColorRGBA c = this.getColorForStack(stack);
               if (c != null) {
                  Hotbar.SlotHighlightRect customRect = useCustomHotbar ? hotbar.getSlotHighlightRect(i) : null;
                  float x = customRect != null ? customRect.x() : left + 3 + i * HOTBAR_SLOT_SIZE;
                  float y = customRect != null ? customRect.y() : top + 3;
                  float size = customRect != null ? customRect.size() : 16.0F;
                  ctx.drawRect(x, y, size, size, c);
                  ColorRGBA borderColor = new ColorRGBA(
                     Math.min(255.0F, c.getRed() * 1.3F), Math.min(255.0F, c.getGreen() * 1.3F), Math.min(255.0F, c.getBlue() * 1.3F), c.getAlpha()
                  );
                  ctx.drawRoundedBorder(x, y, size, size, 0.5F, BorderRadius.all(2.0F), borderColor);
               }
            }
         }
      }
   };

   private ColorRGBA getColorForStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Item item = stack.getItem();
         ColorRGBA base;
         if (item == Items.TOTEM_OF_UNDYING && this.hlTotem.isSelected()) {
            base = this.colorTotem.getColor();
         } else if (item == Items.ENCHANTED_GOLDEN_APPLE && this.hlEnchantedApple.isSelected()) {
            base = this.colorEnchantedApple.getColor();
         } else if (item == Items.GOLDEN_APPLE && this.hlGoldenApple.isSelected()) {
            base = this.colorGoldenApple.getColor();
         } else if (item == Items.GOLDEN_CARROT && this.hlGoldenCarrot.isSelected()) {
            base = this.colorGoldenCarrot.getColor();
         } else if (this.hlHealingPotion.isSelected() && PotionUtility.hasEffect(stack, StatusEffects.INSTANT_HEALTH)) {
            base = this.colorHealingPotion.getColor();
         } else if (item == Items.ENDER_EYE && this.hlEnderEye.isSelected()) {
            base = this.colorEnderEye.getColor();
         } else if (item == Items.NETHERITE_SCRAP && this.hlNetheriteScrap.isSelected()) {
            base = this.colorNetheriteScrap.getColor();
         } else if (item == Items.FIRE_CHARGE && this.hlFireCharge.isSelected()) {
            base = this.colorFireCharge.getColor();
         } else if (item == Items.NETHER_STAR && this.hlNetherStar.isSelected()) {
            base = this.colorNetherStar.getColor();
         } else if (item == Items.DRIED_KELP && this.hlDriedKelp.isSelected()) {
            base = this.colorDriedKelp.getColor();
         } else if (item == Items.PHANTOM_MEMBRANE && this.hlPhantomMembrane.isSelected()) {
            base = this.colorPhantomMembrane.getColor();
         } else if (item == Items.SUGAR && this.hlSugar.isSelected()) {
            base = this.colorSugar.getColor();
         } else if (item == Items.SNOWBALL && this.hlSnowball.isSelected()) {
            base = this.colorSnowball.getColor();
         } else if (item == Items.POPPED_CHORUS_FRUIT && this.hlPoppedChorusFruit.isSelected()) {
            base = this.colorPoppedChorusFruit.getColor();
         } else if (item == Items.PRISMARINE_SHARD && this.hlPrismarineShard.isSelected()) {
            base = this.colorPrismarineShard.getColor();
         } else if (item == Items.FIREWORK_STAR && this.hlFireworkStar.isSelected()) {
            base = this.colorFireworkStar.getColor();
         } else if (item == Items.MAGENTA_SHULKER_BOX && this.hlMagentaShulkerBox.isSelected()) {
            base = this.colorMagentaShulkerBox.getColor();
         } else if (item == Items.CROSSBOW && this.hlCrossbow.isSelected()) {
            base = this.colorCrossbow.getColor();
         } else {
            if (item != Items.PLAYER_HEAD || !this.hlPlayerHead.isSelected()) {
               return null;
            }

            base = this.colorPlayerHead.getColor();
         }

         return base.withAlpha(140.25F);
      } else {
         return null;
      }
   }
}
