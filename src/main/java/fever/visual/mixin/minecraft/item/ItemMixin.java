package fever.visual.mixin.minecraft.item;

import fever.visual.systems.modules.modules.visuals.ArmorDurability;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "getItemBarColor", at = @At("HEAD"), cancellable = true)
    private void fevervisual$applyArmorDurabilityColor(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        ArmorDurability module = ArmorDurability.getModule();
        if (module == null || !module.isEnabled()) {
            return;
        }

        if (stack == null || !stack.isDamageable() || !fevervisual$isArmorLike(stack)) {
            return;
        }

        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }

        float durabilityPercent = 1.0F - (float) stack.getDamage() / (float) maxDamage;
        cir.setReturnValue(module.getColorIntByPercent(durabilityPercent));
    }

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
