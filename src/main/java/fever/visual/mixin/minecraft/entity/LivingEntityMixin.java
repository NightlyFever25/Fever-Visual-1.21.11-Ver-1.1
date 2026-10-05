package fever.visual.mixin.minecraft.entity;

import com.holdmylua.source.access.LivingEntityAccessor;
import com.holdmylua.source.global.GlobalsStorage;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fever.visual.FeverVisual;
import fever.visual.systems.event.impl.game.EntityDeathEvent;
import fever.visual.systems.event.impl.game.EntityJumpEvent;
import fever.visual.systems.modules.modules.combat.SwingAnimation;
import fever.visual.systems.modules.modules.visuals.HoldMyItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import com.hmi.HandMyItemsRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements LivingEntityAccessor {

   @Shadow
   public abstract ItemStack getMainHandStack();
   @Shadow
   public abstract ItemStack getOffHandStack();
   @Unique private boolean feverVisual$interactOffhand;
   @Unique private boolean feverVisual$interactMainHand;
   @Unique private boolean feverVisual$blockBreaking;
   @Unique private int feverVisual$offHandSwingTicks;
   @Unique private boolean feverVisual$offHandSwinging;
   @Unique private float feverVisual$offHandSwingProgress;
   @Unique private float feverVisual$lastOffHandSwingProgress;
   @Unique private int feverVisual$mainHandSwingTicks;
   @Unique private boolean feverVisual$mainHandSwinging;
   @Unique private float feverVisual$mainHandSwingProgress;
   @Unique private float feverVisual$lastMainHandSwingProgress;
   @Unique private int feverVisual$mainSwingCount;
   @Unique private boolean feverVisual$swingMHand;
   @Unique private boolean feverVisual$swingOHand;

   @ModifyReturnValue(method = "getHandSwingDuration", at = @At("RETURN"))
   public int modifySwingDuration(int original) {
      var holdMyItems = HoldMyItems.getInstance();
      if (holdMyItems != null && holdMyItems.isEnabled() && (Object)this == MinecraftClient.getInstance().player) {
         ItemStack stack = this.getMainHandStack();
         if (stack.isIn(ItemTags.SWORDS) || GlobalsStorage.itemSwingSpeed.containsKey(stack.getItem().toString())) {
            return this.feverVisual$getHmiSwingDuration(stack);
         }
      }
      SwingAnimation swingAnimationModule = FeverVisual.getInstance().getModuleManager().getModule(SwingAnimation.class);
      if (swingAnimationModule != null && swingAnimationModule.isEnabled() && swingAnimationModule.shouldApplyAnimation(this.getMainHandStack())) {
         return Math.max(1, (int) (original * swingAnimationModule.getDurationMultiplier()));
      }

      return original;
   }

   @Unique
   private int feverVisual$getHmiSwingDuration(ItemStack stack) {
      HoldMyItems holdMyItems = HoldMyItems.getInstance();
      int fallback = holdMyItems != null && holdMyItems.isEnabled() && stack.isIn(ItemTags.SWORDS) ? holdMyItems.getSwingSpeed() : 10;
      return Math.max(1, GlobalsStorage.itemSwingSpeed.getOrDefault(stack.getItem().toString(), fallback));
   }

   @Override
   public int hMI5_0$getSwingCount() {
      return this.feverVisual$mainSwingCount;
   }

   @Override
   public void hMI5_0$resetOffHandSwing(boolean interact) {
      this.feverVisual$offHandSwingTicks = 0;
      this.feverVisual$offHandSwinging = true;
      this.feverVisual$swingOHand = !this.feverVisual$swingOHand;
      this.feverVisual$interactOffhand = interact;
   }

   @Override
   public void hMI5_0$resetMainHandSwing(boolean interact) {
      this.feverVisual$mainHandSwingTicks = 0;
      this.feverVisual$mainHandSwinging = true;
      this.feverVisual$swingMHand = !this.feverVisual$swingMHand;
      ++this.feverVisual$mainSwingCount;
      this.feverVisual$interactMainHand = interact;
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.interactionManager != null && client.interactionManager.isBreakingBlock()) {
         this.feverVisual$blockBreaking = true;
      }
   }

   @Override
   public float hMI5_0$getMainHandSwingProgress(float tickDelta) {
      float f = this.feverVisual$mainHandSwingProgress - this.feverVisual$lastMainHandSwingProgress;
      if (f < 0.0F) {
         f += 1.0F;
      }
      return this.feverVisual$lastMainHandSwingProgress + f * tickDelta;
   }

   @Override
   public float hMI5_0$getOffHandSwingProgress(float tickDelta) {
      float f = this.feverVisual$offHandSwingProgress - this.feverVisual$lastOffHandSwingProgress;
      if (f < 0.0F) {
         f += 1.0F;
      }
      return this.feverVisual$lastOffHandSwingProgress + f * tickDelta;
   }

   @Override
   public boolean hMI5_0$getMInteract() {
      return this.feverVisual$interactMainHand;
   }

   @Override
   public boolean hMI5_0$getOInteract() {
      return this.feverVisual$interactOffhand;
   }

   @Override
   public boolean hMI5_0$getBlockBreak() {
      return this.feverVisual$blockBreaking;
   }

   @Override
   public boolean hMI5_0$getMHandEvent() {
      return this.feverVisual$swingMHand;
   }

   @Override
   public boolean hMI5_0$getOHandEvent() {
      return this.feverVisual$swingOHand;
   }

   @Inject(method = "baseTick", at = @At("HEAD"))
   private void feverVisual$tickHmiSwing(CallbackInfo ci) {
      if (!HandMyItemsRuntime.isActive()) {
         return;
      }

      this.feverVisual$lastOffHandSwingProgress = this.feverVisual$offHandSwingProgress;
      this.feverVisual$lastMainHandSwingProgress = this.feverVisual$mainHandSwingProgress;
      int mainDuration = this.feverVisual$getHmiSwingDuration(this.getMainHandStack());
      if (this.feverVisual$mainHandSwinging) {
         ++this.feverVisual$mainHandSwingTicks;
         if (this.feverVisual$mainHandSwingTicks >= mainDuration) {
            this.feverVisual$mainHandSwingTicks = 0;
            this.feverVisual$mainHandSwinging = false;
         }
      } else {
         this.feverVisual$interactMainHand = false;
         this.feverVisual$blockBreaking = false;
         this.feverVisual$mainHandSwingTicks = 0;
      }
      this.feverVisual$mainHandSwingProgress = (float)this.feverVisual$mainHandSwingTicks / (float)mainDuration;

      int offDuration = this.feverVisual$getHmiSwingDuration(this.getOffHandStack());
      if (this.feverVisual$offHandSwinging) {
         ++this.feverVisual$offHandSwingTicks;
         if (this.feverVisual$offHandSwingTicks >= offDuration) {
            this.feverVisual$offHandSwingTicks = 0;
            this.feverVisual$offHandSwinging = false;
         }
      } else {
         this.feverVisual$interactOffhand = false;
         this.feverVisual$offHandSwingTicks = 0;
      }
      this.feverVisual$offHandSwingProgress = (float)this.feverVisual$offHandSwingTicks / (float)offDuration;
   }

   @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"))
   private void feverVisual$onHmiSwingHand(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
      if (!HandMyItemsRuntime.isActive()) {
         return;
      }

      if (hand == Hand.OFF_HAND) {
         int duration = this.feverVisual$getHmiSwingDuration(this.getOffHandStack());
         if (!this.feverVisual$offHandSwinging || this.feverVisual$offHandSwingTicks >= duration / 2 || this.feverVisual$offHandSwingTicks < 0) {
            this.feverVisual$offHandSwingTicks = -1;
            this.feverVisual$offHandSwinging = true;
         }
      } else {
         int duration = this.feverVisual$getHmiSwingDuration(this.getMainHandStack());
         if (!this.feverVisual$mainHandSwinging || this.feverVisual$mainHandSwingTicks >= duration / 2 || this.feverVisual$mainHandSwingTicks < 0) {
            this.feverVisual$mainHandSwingTicks = -1;
            this.feverVisual$mainHandSwinging = true;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.interactionManager != null && client.interactionManager.isBreakingBlock()) {
               this.feverVisual$blockBreaking = true;
            }
         }
      }
   }

   @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
   public void triggerJumpEvent(CallbackInfo ci) {
      LivingEntity livingEntity = (LivingEntity) (Object) this;
      EntityJumpEvent event = new EntityJumpEvent(livingEntity);
      FeverVisual.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "onDeath", at = @At("TAIL"))
   public void triggerEntityDeathEvent(DamageSource damageSource, CallbackInfo ci) {
      LivingEntity entity = (LivingEntity) (Object) this;
      FeverVisual.getInstance().getEventManager().triggerEvent(new EntityDeathEvent(entity, damageSource));
   }
}
