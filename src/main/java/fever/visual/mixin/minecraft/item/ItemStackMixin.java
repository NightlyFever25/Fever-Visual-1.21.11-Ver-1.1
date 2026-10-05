package fever.visual.mixin.minecraft.item;

import com.holdmylua.source.access.ItemStackAccessor;
import fever.visual.FeverVisual;
import fever.visual.systems.event.impl.game.FinishEatEvent;
import fever.visual.systems.modules.modules.visuals.ArmorDurability;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackAccessor {
   @Unique private int feverVisual$hmiTransform = -1;
   @Unique private int feverVisual$hmiSwingSpeed = 10;
   @Unique private int feverVisual$hmiShouldRenderAsBlock = -1;
   @Inject(method = "finishUsing", at = @At("TAIL"))
   private void onFinishUsing(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
      if (user instanceof PlayerEntity player) {
         FeverVisual.getInstance().getEventManager().triggerEvent(new FinishEatEvent(player, (ItemStack) (Object) this));
      }
   }

   @Inject(method = "getItemBarColor", at = @At("HEAD"), cancellable = true)
   private void fevervisual$applyArmorDurabilityColor(CallbackInfoReturnable<Integer> cir) {
      ArmorDurability module = ArmorDurability.getModule();
      if (module == null || !module.isEnabled()) {
         return;
      }

      ItemStack stack = (ItemStack) (Object) this;
      if (!stack.isDamageable() || !stack.isItemBarVisible() || !fevervisual$isArmorLike(stack)) {
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

   @Override
   public void hMI5_0$setTransform(boolean value) {
      this.feverVisual$hmiTransform = value ? 1 : 0;
   }

   @Override
   public void hMI5_0$setTransform(int value) {
      this.feverVisual$hmiTransform = value;
   }

   @Override
   public int hMI5_0$getTransform() {
      return this.feverVisual$hmiTransform;
   }

   @Override
   public void hMI5_0$setSwingSpeed(int value) {
      this.feverVisual$hmiSwingSpeed = value;
   }

   @Override
   public int hMI5_0$getSwingSpeed() {
      return this.feverVisual$hmiSwingSpeed;
   }

   @Override
   public void hMI5_0$setRenderAsBlock(boolean value) {
      this.feverVisual$hmiShouldRenderAsBlock = value ? 1 : 0;
   }

   @Override
   public int hMI5_0$getRenderAsBlock() {
      return this.feverVisual$hmiShouldRenderAsBlock;
   }
}
