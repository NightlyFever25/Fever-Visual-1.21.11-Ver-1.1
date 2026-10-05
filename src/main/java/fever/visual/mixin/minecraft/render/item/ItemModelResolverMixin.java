package fever.visual.mixin.minecraft.render.item;

import fever.visual.systems.modules.modules.visuals.CustomSwords;
import java.util.List;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.HeldItemContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemModelManager.class)
public abstract class ItemModelResolverMixin {
   @ModifyArg(
      method = "clearAndUpdate",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/item/ItemModelManager;update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/world/World;Lnet/minecraft/util/HeldItemContext;I)V"
      ),
      index = 1,
      require = 0
   )
   private ItemStack fevervisual$replaceSwordModel(
      ItemRenderState renderState,
      ItemStack stack,
      ItemDisplayContext displayContext,
      World world,
      HeldItemContext itemOwner,
      int seed
   ) {
      CustomSwords module = CustomSwords.getModule();
      if (module == null || !module.isEnabled() || !this.isSword(stack)) {
         return stack;
      }
      if (itemOwner == null) {
         return stack;
      }
      if (module.isSelfOnly() && itemOwner.getEntity() != net.minecraft.client.MinecraftClient.getInstance().player) {
         return stack;
      }
      ItemStack renderedStack = stack.copy();
      renderedStack.set(
         DataComponentTypes.CUSTOM_MODEL_DATA,
         new CustomModelDataComponent(List.of(), List.of(), List.of(module.getSelectedWeapon()), List.of())
      );
      return renderedStack;
   }

   private boolean isSword(ItemStack stack) {
      return stack.isOf(Items.WOODEN_SWORD)
         || stack.isOf(Items.STONE_SWORD)
         || stack.isOf(Items.COPPER_SWORD)
         || stack.isOf(Items.IRON_SWORD)
         || stack.isOf(Items.GOLDEN_SWORD)
         || stack.isOf(Items.DIAMOND_SWORD)
         || stack.isOf(Items.NETHERITE_SWORD);
   }
}
