package fever.visual.mixin.minecraft.render;

import com.llamalad7.mixinextras.sugar.Local;
import fever.visual.systems.modules.modules.visuals.ArmorDurability;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(net.minecraft.client.render.entity.equipment.EquipmentRenderer.class)
public abstract class ArmorDurabilityRenderMixin {
    @ModifyArg(
            method = "render("
                    + "Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;"
                    + "Lnet/minecraft/registry/RegistryKey;"
                    + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                    + "Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;"
                    + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I"
                    + "Lnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel("
                            + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                            + "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;"
                            + "IIILnet/minecraft/client/texture/Sprite;I"
                            + "Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
            ),
            index = 6
    )
    private int fevervisual$tintArmorByDurability(int color, @Local(argsOnly = true) ItemStack stack) {
        ArmorDurability module = ArmorDurability.getModule();
        if (module == null || !module.isEnabled()) {
            return color;
        }
        if (stack == null || stack.isEmpty() || !stack.isDamageable() || !fevervisual$isArmorLike(stack)) {
            return color;
        }

        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return color;
        }

        float durabilityPercent = 1.0F - (float) stack.getDamage() / (float) maxDamage;
        int tint = module.getColorIntByPercent(durabilityPercent); // 0xRRGGBB

        float tr = ((tint >> 16) & 0xFF) / 255.0F;
        float tg = ((tint >> 8) & 0xFF) / 255.0F;
        float tb = (tint & 0xFF) / 255.0F;

        int a = (color >>> 24) & 0xFF;
        int r = Math.round(((color >> 16) & 0xFF) * tr);
        int g = Math.round(((color >> 8) & 0xFF) * tg);
        int b = Math.round((color & 0xFF) * tb);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Unique
    private static boolean fevervisual$isArmorLike(ItemStack stack) {
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
}
