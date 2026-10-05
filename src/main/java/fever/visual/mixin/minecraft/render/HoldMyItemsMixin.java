package fever.visual.mixin.minecraft.render;

import com.holdmylua.source.LuaTestHMI;
import com.holdmylua.source.access.ItemStackAccessor;
import com.holdmylua.source.access.LivingEntityAccessor;
import com.holdmylua.source.global.DispatcherStorage;
import com.holdmylua.source.global.GlobalsStorage;
import com.holdmylua.source.global.item_model.ItemModelContext;
import com.holdmylua.source.global.item_model.ItemModelStorage;
import com.holdmylua.source.lua_runtime.LuaScriptCache;
import com.holdmylua.source.lua_runtime.ScriptHolder;
import com.holdmylua.source.patricles.Particle;
import com.holdmylua.source.patricles.ParticleRenderManager;
import fever.visual.systems.modules.modules.visuals.HoldMyItems;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.hmi.HandMyItemsRuntime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin({HeldItemRenderer.class})
public abstract class HoldMyItemsMixin {
   @Unique private boolean repPower = false;
   @Unique private float prevAge = 0.0F;
   @Unique private double previousRotation = 0.0D;
   @Unique private float swingAngleY = 0.0F;
   @Unique private float swingAngleX = 0.0F;
   @Unique private float swingVelocityY = 0.0F;
   @Unique private float swingVelocityX = 0.0F;
   @Unique private float swingVelocityZ = 0.0F;
   @Unique private static final float GRAVITY = 0.1F;
   @Unique private static final float DAMPING = 0.88F;
   @Unique private static final float SENSITIVITY = 0.015F;
   @Unique private float vertAngleY = 0.0F;
   @Unique private float vertVelocityY = 0.0F;
   @Unique private float vertVelocityYSlime = 0.0F;
   @Unique private float vertAngleYSlime = 0.0F;
   @Unique private float riptideCounter = 0.0F;
   @Unique private float netherCounter = 0.0F;
   @Shadow private ItemStack offHand;
   @Shadow @Final private MinecraftClient client;
   @Unique private float fallCounter = 0.0F;
   @Unique private float inWaterCounter = 0.0F;
   @Unique private float inspect = 0.0F;
   @Unique private float tilt = 0.0F;
   @Unique private float freezeCounter = 0.0F;
   @Unique private float clCount = 0.0F;
   @Unique private float crawlCount = 0.0F;
   @Unique private float directionalCrawlCount = 0.0F;
   @Unique private float climbCount = 0.0F;
   @Unique private float mouseHolding = 1.0F;
   @Unique private boolean isSwinging = false;
   @Unique private float swingProgress = 0.0F;
   @Unique private boolean isForward = false;
   @Unique private boolean isAttacking = false;
   @Unique private boolean left = false;
   @Unique private AbstractClientPlayerEntity feverVisual$lastPlayer;
   @Unique private World feverVisual$lastWorld;
   @Unique private Item feverVisual$prevMainHand = Items.AIR;
   @Unique private Item feverVisual$prevOffHand = Items.AIR;
   @Unique private boolean feverVisual$mainHandSwitchEvent;
   @Unique private boolean feverVisual$offHandSwitchEvent;
   @Unique private boolean feverVisual$swingMHand;
   @Unique private boolean feverVisual$swingOHand;
   @Unique private float feverVisual$mainHandSwingProgress;
   @Unique private float feverVisual$offHandSwingProgress;
   @Unique private final ArrayList<Particle> feverVisual$hmiParticles = new ArrayList<>();

   @Unique
   private float easeInOutBack(float x) {
      float c1 = 1.70158F;
      float c2 = c1 * 1.525F;
      return (float)((double)x < 0.5D ? Math.pow((double)(2.0F * x), 2.0D) * (double)((c2 + 1.0F) * 2.0F * x - c2) / 2.0D : (Math.pow((double)(2.0F * x - 2.0F), 2.0D) * (double)((c2 + 1.0F) * (x * 2.0F - 2.0F) + c2) + 2.0D) / 2.0D);
   }

   @Unique
   private float getAttackDamage(ItemStack stack) {
      AttributeModifiersComponent modifiers = (AttributeModifiersComponent)stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (modifiers == null) {
         return 0.0F;
      } else {
         float totalDamage = 0.0F;
         for(var entry : modifiers.modifiers()) {
            if (entry.attribute().value() == EntityAttributes.ATTACK_DAMAGE) {
               totalDamage += (float)entry.modifier().value();
            }
         }
         return totalDamage;
      }
   }

   @Unique
   private void altSwing(MatrixStack matrices, Arm arm, float swingProgress, ItemStack item) {
      MinecraftClient client = MinecraftClient.getInstance();
      int i = arm == Arm.RIGHT ? 1 : -1;
      float f = MathHelper.sin(swingProgress * 3.14F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * (45.0F + f * 0.0F)));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * -45.0F));
   }

   @Shadow public abstract void renderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, MatrixStack var4, OrderedRenderCommandQueue var5, int var6);
   @Shadow protected abstract void swingArm(float var1, MatrixStack var2, int var3, Arm var4);
   @Shadow protected abstract void applyEquipOffset(MatrixStack var1, Arm var2, float var3);
   @Shadow protected abstract void renderMapInBothHands(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, float var6);
   @Shadow protected abstract void renderMapInOneHand(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, Arm var5, float var6, ItemStack var7);
   @Shadow protected abstract void renderArmHoldingItem(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, Arm var6);
   @Shadow protected abstract void applySwingOffset(MatrixStack var1, Arm var2, float var3);
   @Invoker("renderFirstPersonItem")
   protected abstract void invokeRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light);

   @Unique
   private HoldMyItems getConfig() {
      return HoldMyItems.getInstance();
   }

   @Unique
   private void feverVisual$resetAnimationState() {
      this.repPower = false;
      this.prevAge = 0.0F;
      this.previousRotation = 0.0D;
      this.swingAngleY = 0.0F;
      this.swingAngleX = 0.0F;
      this.swingVelocityY = 0.0F;
      this.swingVelocityX = 0.0F;
      this.swingVelocityZ = 0.0F;
      this.vertAngleY = 0.0F;
      this.vertVelocityY = 0.0F;
      this.vertVelocityYSlime = 0.0F;
      this.vertAngleYSlime = 0.0F;
      this.riptideCounter = 0.0F;
      this.netherCounter = 0.0F;
      this.fallCounter = 0.0F;
      this.inWaterCounter = 0.0F;
      this.inspect = 0.0F;
      this.tilt = 0.0F;
      this.freezeCounter = 0.0F;
      this.clCount = 0.0F;
      this.crawlCount = 0.0F;
      this.directionalCrawlCount = 0.0F;
      this.climbCount = 0.0F;
      this.mouseHolding = 1.0F;
      this.isSwinging = false;
      this.swingProgress = 0.0F;
      this.isForward = false;
      this.isAttacking = false;
      this.left = false;
   }

   @Unique
   private void feverVisual$resetIfContextChanged(AbstractClientPlayerEntity player) {
      World world = player.getEntityWorld();
      if (this.feverVisual$lastPlayer != player || this.feverVisual$lastWorld != world || player.age < this.prevAge) {
         this.feverVisual$resetAnimationState();
         this.feverVisual$lastPlayer = player;
         this.feverVisual$lastWorld = world;
      }
   }

   @Unique
   private boolean shouldApplyAnimation() {
      var config = getConfig();
      return config != null && config.isEnabled();
   }

   @Unique
   private boolean isWeaponLikeItem(ItemStack item) {
      return item.isIn(ItemTags.SWORDS)
              || item.isIn(ItemTags.AXES)
              || item.isIn(ItemTags.HOES)
              || item.isIn(ItemTags.PICKAXES)
              || item.isIn(ItemTags.SHOVELS)
              || item.isOf(Items.MACE)
              || item.getUseAction() == UseAction.BLOCK
              || item.getUseAction() == UseAction.SPEAR
              || item.getUseAction() == UseAction.BOW
              || item.getUseAction() == UseAction.CROSSBOW
              || this.getAttackDamage(item) != 0.0F;
   }

   @Redirect(
           method = {"swingArm"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"
           )
   )
   private void applySwing(HeldItemRenderer instance, MatrixStack matrices, Arm arm, float swingProgress) {
      if (!shouldApplyAnimation()) {
         this.applySwingOffset(matrices, arm, swingProgress);
      }
   }

   @Redirect(
           method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
           )
   )
   private void renderOverhaul(HeldItemRenderer instance, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light) {
      this.feverVisual$resetIfContextChanged(player);

      if (!shouldApplyAnimation()) {
         this.feverVisual$resetAnimationState();
         this.invokeRenderFirstPersonItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
         return;
      }

      if (player.isUsingSpyglass()) {
         this.feverVisual$resetAnimationState();
         this.invokeRenderFirstPersonItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
         return;
      }

      var config = getConfig();
      if (config == null) {
         this.feverVisual$resetAnimationState();
         this.invokeRenderFirstPersonItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
         return;
      }

      try {
      if (HandMyItemsRuntime.isActive()) {
         try {
            this.feverVisual$renderLuaHmi(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
         } catch (ScriptException | NoSuchMethodException ignored) {
            this.invokeRenderFirstPersonItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
         }
         return;
      }
      float yaw = player.getYaw();
      double radians = Math.toRadians((double)yaw);
      double forwardX = -Math.sin(radians);
      double forwardZ = Math.cos(radians);
      Vec3d horizontalVelocity = player.getVelocity();
      double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
      double crossProduct = player.getVelocity().getHorizontal().x * forwardZ - horizontalVelocity.z * forwardX;
      float al;
      if (player.getPitch() != 0.0F) {
         al = 90.0F / player.getPitch() / 10.0F;
      } else {
         al = 1.0F;
      }

      if (al > 1.0F) {
         al = 1.0F;
      }

      if (al < 0.0F) {
         al = 1.0F;
      }

      boolean bl = hand == Hand.MAIN_HAND;
      Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
      float kj = bl ? 1.0F : -1.0F;
      matrices.push();
      matrices.push();
      matrices.translate((double)(config.getViewmodelXOffset() * kj), (double)config.getViewmodelYOffset(), (double)config.getViewmodelZOffset());
      double tt = com.holdmyitems.HoldMyItems.deltaTime * 30.0D;
      float swing_rot = (double)swingProgress < 0.6D ? MathHelper.sin(MathHelper.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F) : MathHelper.sin(MathHelper.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
      float swing = MathHelper.sin(swingProgress * 3.14F);
      swing = this.easeInOutBack(swing);

      if ((item.isOf(Items.EXPERIENCE_BOTTLE) || item.isOf(Items.WIND_CHARGE) || item.isOf(Items.EGG) || item.isOf(Items.ENDER_EYE) || item.isOf(Items.SNOWBALL) || item.isOf(Items.ENDER_PEARL) || item.getItem() instanceof SplashPotionItem || item.getItem() instanceof LingeringPotionItem) && player.getOffHandStack().isEmpty() && item.getUseAction() != UseAction.SPEAR && !item.isOf(Items.FIRE_CHARGE) && !player.isSwimming() && !player.isCrawling() && !player.isClimbing()) {
         if (player.getMainArm() == Arm.LEFT) {
            bl = !bl;
         }
         float jj = bl ? 1.0F : -1.0F;
         matrices.push();
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25.0F * jj));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * jj * swing));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
         matrices.translate((double)(-0.15F * jj), 0.1D, 0.1D);
         matrices.translate(0.0D, (double)(-0.55F * swing), (double)(0.4F * swing) * 3.141592653589793D);
         this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, 0.0F, arm.getOpposite());
         matrices.pop();
      }

      if (this.client.options.attackKey.isPressed() && !this.isAttacking && (double)swingProgress == 0.0D) {
         this.left = !this.left;
      }

      if (!item.isEmpty()) {
         if (player.getMainArm() == Arm.LEFT) {
            bl = !bl;
         }
         float ll = bl ? 1.0F : -1.0F;
         if ((this.left || item.isIn(ItemTags.AXES) || item.getUseAction() == UseAction.SPEAR || item.getUseAction() == UseAction.BLOCK) && !item.isIn(ItemTags.SHOVELS)) {
            if (!item.isIn(ItemTags.SWORDS) && !item.isIn(ItemTags.AXES)) {
               if (item.getUseAction() == UseAction.SPEAR) {
                  matrices.translate(0.0D, 0.0D, (double)(0.45F * swing_rot));
                  matrices.translate((double)(-0.25F * kj) * (double)swing, (double)(-0.35F * swing_rot), (double)(-0.6F * swing));
                  matrices.translate(0.0D, (double)(0.1F * swing), 0.0D);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * ll));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swing_rot * ll));
               } else if (item.isIn(ConventionalItemTags.TOOLS) && item.getUseAction() != UseAction.BLOCK && !item.isIn(ItemTags.SHOVELS)) {
                  matrices.translate((double)(0.1F * ll) * (double)swing_rot, (double)(0.1F * swing_rot), (double)(-0.5F * swing));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * ll));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
               } else if (item.getUseAction() != UseAction.BLOCK) {
                  matrices.translate((double)(0.1F * ll) * (double)swing_rot, (double)(0.1F * swing_rot), (double)(-0.1F * swing));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * ll));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * ll));
               } else {
                  matrices.translate((double)(0.1F * ll) * (double)swing_rot, (double)(0.1F * swing_rot), (double)(-0.2F * swing));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * ll));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * swing));
               }
            } else {
               matrices.translate((double)(0.8F * ll) * (double)swing_rot, (double)(0.3F * swing_rot), (double)(-0.5F * swing));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * ll));
               matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0F * swing_rot));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * swing_rot * ll));
               if (item.isIn(ItemTags.SWORDS)) {
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
               } else {
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(30.0F * swing));
               }
            }
         } else if (!item.isIn(ItemTags.SHOVELS)) {
            if (item.isIn(ItemTags.SWORDS)) {
               matrices.translate((double)(-0.55F * ll) * (double)swing_rot, (double)(-0.8F * swing_rot), (double)(-0.77F * swing));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5.0F * swing_rot * ll));
               matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(70.0F * swing_rot * ll));
               matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(50.0F * swing));
            } else if (item.isIn(ConventionalItemTags.TOOLS) && !item.isIn(ItemTags.SHOVELS)) {
               matrices.translate((double)(0.1F * ll) * (double)swing_rot, (double)(0.1F * swing_rot), (double)(-0.5F * swing));
               matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * ll));
               matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
            } else {
               matrices.translate((double)(0.1F * ll) * (double)swing_rot, (double)(0.1F * swing_rot), (double)(-0.1F * swing));
               matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * ll));
               matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * ll));
            }
         } else if (item.isIn(ItemTags.SHOVELS)) {
            matrices.translate(0.0D, (double)(0.15F * swing_rot), (double)(-0.25F * swing_rot));
            matrices.translate(0.0D, 0.0D, (double)(-0.2F * swing));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * swing_rot));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
         }
      } else if (Block.getBlockFromItem(item.getItem()) != Blocks.AIR && (!item.isIn(ConventionalItemTags.TOOLS) || item.isIn(ItemTags.TRIMMABLE_ARMOR) || item.isIn(ItemTags.BOOKSHELF_BOOKS) || item.getUseAction() == UseAction.EAT || !item.isEnchantable()) && item.getUseAction() != UseAction.BOW && item.getUseAction() != UseAction.SPYGLASS && this.getAttackDamage(item) == 0.0F && item.getUseAction() != UseAction.BLOCK && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) && !item.isOf(Items.CARROT_ON_A_STICK) && !item.isOf(Items.FISHING_ROD) && !item.isOf(Items.SHEARS)) {
         swingProgress = (float)((double)swingProgress * 1.2D);
         if (swingProgress > 1.0F) {
            swingProgress = 0.0F;
         }
      } else if (!item.isIn(ItemTags.SHOVELS)) {
         swingProgress = (float)((double)swingProgress * 1.5D);
         if (swingProgress > 1.0F) {
            swingProgress = 0.0F;
         }
      }

      if (player.getVelocity().length() >= 0.08D) {
         this.crawlCount = (float)((double)this.crawlCount + 0.1D * player.getVelocity().length() * 2.0D * tt);
         this.directionalCrawlCount = (float)((double)this.directionalCrawlCount + 0.1D * dotProduct * 4.0D * tt);
         this.directionalCrawlCount = (float)((double)this.directionalCrawlCount + (dotProduct > 0.0D ? 0.1D * Math.abs(crossProduct) * 4.0D * tt : 0.1D * Math.abs(crossProduct) * -1.0D * 4.0D * tt));
      }

      if (player.getVelocity().getY() > 0.0D) {
         this.climbCount = (float)((double)this.climbCount + 0.1D * tt);
      }
      if (player.getVelocity().getY() < 0.0D) {
         this.climbCount = (float)((double)this.climbCount - 0.1D * tt);
      }

      if ((player.isCrawling() && config.isClimbAndCrawl() || player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().getY()) > 0.0D && config.isClimbAndCrawl()) && !player.isUsingItem() && swingProgress == 0.0F) {
         this.clCount = (float)((double)this.clCount + 0.1D * tt);
         if (this.clCount > 1.0F) {
            this.clCount = 1.0F;
         }
         if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * this.clCount));
         }
      } else {
         this.clCount = (float)((double)this.clCount * Math.pow(0.88D, tt));
      }

      if (swingProgress == 0.0F) {
         matrices.translate(bl ? (double)(player.getPitch() / 650.0F * this.clCount * -1.0F) : (double)(player.getPitch() / 650.0F * this.clCount), 0.0D, 0.0D);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.getPitch() * this.clCount));
      }

      if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
         matrices.translate(0.0F, 0.0F, (double)(player.getPitch() / 120.0F * this.clCount));
      } else if (swingProgress == 0.0F) {
         matrices.translate(0.0F, 0.0F, (double)(player.getPitch() / 80.0F * this.clCount));
      }

      if (player.isClimbing() && config.isClimbAndCrawl() && !player.isOnGround() && !item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !player.isUsingItem()) {
         matrices.translate(0.0D, 0.1D, -0.2D);
      }

      if ((player.isInFluid() || player.inPowderSnow) && !player.isSwimming() && !player.isSubmergedInWater()) {
         this.inWaterCounter = (float)((double)this.inWaterCounter + 0.1D * tt);
         if (this.inWaterCounter >= 1.0F) {
            this.inWaterCounter = 1.0F;
         }
      } else {
         this.inWaterCounter = (float)((double)this.inWaterCounter * Math.pow(0.88D, tt));
      }

      if (player.inPowderSnow && (double)player.getFreezingScale() > 0.1D) {
         this.freezeCounter = (float)((double)this.freezeCounter + 0.1D * tt);
      } else {
         this.freezeCounter = (float)((double)this.freezeCounter * Math.pow(0.88D, tt));
      }

      matrices.translate(0.0D, (double)(0.02F * this.inWaterCounter), 0.0D);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F * kj * this.inWaterCounter));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.3F * MathHelper.sin(this.freezeCounter * 5.0F)));

      if (player.getVelocity().getY() < -0.85D && item.isOf(Items.MACE) && player.getMainHandStack() == item) {
         this.fallCounter = (float)((double)this.fallCounter + 0.1D * tt);
         if (this.fallCounter >= 1.0F) {
            this.fallCounter = 1.0F;
         }
      } else {
         this.fallCounter = (float)((double)this.fallCounter * Math.pow(0.88D, tt));
      }

      if (bl) {
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F * this.fallCounter));
         matrices.translate(0.0D, (double)(-0.2F * this.fallCounter), 0.0D);
      }

      this.vertAngleY = (float)((double)this.vertAngleY + player.getVelocity().getY() * 0.015D * tt);
      this.vertAngleY = (float)((double)this.vertAngleY - (double)(0.1F * this.vertAngleY) * tt);
      this.vertAngleY = (float)((double)this.vertAngleY * Math.pow(0.88D, tt));
      this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + player.getVelocity().getY() * 0.015D * tt);
      this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime - (double)(0.1F * this.vertAngleYSlime) * tt);
      this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime * Math.pow(0.88D, tt));
      this.vertAngleYSlime = (float)((double)this.vertAngleYSlime + (double)this.vertVelocityYSlime * tt);
      matrices.translate(0.0F, this.vertAngleY * -1.0F, 0.0F);
      matrices.translate(0.0D, Math.sin((double)player.age * 0.1D) * 0.007D * (double)kj, 0.0D);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15F * MathHelper.sin((float)player.age * 0.15F) * kj));

      if (!item.isEmpty() || player.isCrawling() || player.isClimbing() && !player.isOnGround() || player.isSwimming()) {
         if (player.getMainArm() == Arm.LEFT) {
            bl = !bl;
         }
         if (item.getUseAction() == UseAction.BLOCK) {
            matrices.translate(0.0F, 0.0F, 0.0F);
         } else {
            matrices.translate(0.0D, -0.1D, 0.1D);
         }
      }

      if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN) || item.isIn(ItemTags.HANGING_SIGNS)) {
         matrices.translate(0.0D, 0.1D, 0.0D);
         if (player.isSwimming()) {
            matrices.translate(0.0D, -0.1D, 0.1D);
         }
      }

      if (player.isSwimming() && swingProgress == 0.0F && config.isSwimmingAnimation()) {
         double distance = (double)this.crawlCount;
         double swingAmplitude = 1.5D;
         double frequency = 2.0D;
         double s = distance * frequency;
         double handRotation = Math.sin(s) * swingAmplitude;
         double smoothRotation = handRotation * 0.8D + this.previousRotation * 0.2D;
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(bl ? smoothRotation : -smoothRotation)));
         matrices.translate(0.0D, 0.0D, smoothRotation * 0.2D);
         double k = (double)(this.crawlCount * 2.0F);
         double a = Math.cos(k);
         double b = a;
         if (a <= 0.0D) {
            b = a * 0.5D;
         }
         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(bl ? b * 30.0D : b * 30.0D * -1.0D)));
         matrices.translate(0.0D, 0.0D, a * 0.2D);
         if (item.isEmpty() && !bl && !player.isInvisible()) {
            float l = bl ? 1.0F : -1.0F;
            matrices.translate((double)(1.0F * l), 0.0D - (double)equipProgress * 0.3D, 0.3D);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * l));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
            this.altSwing(matrices, arm, swingProgress, item);
            matrices.scale(0.9F, 0.9F, 0.9F);
            this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
         }
         this.previousRotation = smoothRotation;
      }

      if ((player.isClimbing() && !player.isOnGround() || player.isCrawling() && swingProgress == 0.0F) && !player.isUsingItem()) {
         double s = (double)this.climbCount;
         float a = MathHelper.cos((float)s * 2.0F);
         float leftVal = bl ? 1.0F : -1.0F;
         if (player.isClimbing()) {
            if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * a * leftVal));
            } else {
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(1.0F * a * leftVal));
            }
         }
         if (player.isCrawling() && !player.isUsingItem() && swingProgress == 0.0F) {
            float crawlProgress = MathHelper.sin(this.directionalCrawlCount * 4.0F * this.mouseHolding);
            float upAndDown = MathHelper.cos(this.directionalCrawlCount * 4.0F * this.mouseHolding);
            if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN)) {
               crawlProgress *= 0.14F;
               upAndDown *= 0.14F;
            }
            matrices.translate((double)(0.2F * crawlProgress), (double)(0.3F * crawlProgress) * (double)leftVal, (double)(-0.2F * crawlProgress) * (double)leftVal * (double)al);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * crawlProgress));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(20.0F * upAndDown * leftVal, 0.0F, 20.0F)));
         }
         if (item.isEmpty() && !bl && !player.isInvisible() && (!player.isOnGround() && player.isClimbing() || player.isCrawling())) {
            float l = bl ? 1.0F : -1.0F;
            matrices.translate((double)(1.0F * l), 0.0D - (double)equipProgress * 0.3D, 0.3D);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * l));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
            this.altSwing(matrices, arm, swingProgress, item);
            matrices.scale(0.9F, 0.9F, 0.9F);
            this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
         }
      }

      if (item.isEmpty()) {
         if (bl && !player.isInvisible()) {
            if ((player.isOnGround() || !player.isClimbing()) && !player.isSwimming() && !player.isCrawling()) {
               if (player.getMainArm() == Arm.LEFT) {
                  bl = !bl;
               }
               float ll = bl ? 1.0F : -1.0F;
               matrices.translate(0.0D, (double)(0.2F * swing_rot), (double)(0.15F * swing_rot));
               matrices.translate((double)(0.1F * ll) * (double)swing, (double)(0.15F * swing), (double)(-0.45F * swing));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * swing * ll));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swing));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-10 * swing_rot) * ll));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F * swing_rot));
               this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            } else {
               float l = bl ? 1.0F : -1.0F;
               matrices.translate((double)(1.0F * l), 0.0D - (double)equipProgress * 0.3D, 0.3D);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * l));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * l));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
               this.altSwing(matrices, arm, swingProgress, item);
               matrices.scale(0.9F, 0.9F, 0.9F);
               this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            }
         }
      } else if (item.contains(DataComponentTypes.MAP_ID)) {
         if (bl && this.offHand.isEmpty()) {
            matrices.translate(0.0D, 0.1D, 0.0D);
            this.renderMapInBothHands(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
         } else {
            matrices.translate(bl ? -0.1D : 0.1D, 0.1D, 0.0D);
            this.renderMapInOneHand(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, item);
         }
      } else if (item.getUseAction() == UseAction.CROSSBOW) {
         matrices.push();
         boolean bl2 = CrossbowItem.isCharged(item);
         boolean bl3 = arm == Arm.RIGHT;
         int i = bl3 ? 1 : -1;
         if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
            this.applyEquipOffset(matrices, arm, equipProgress);
            matrices.translate((double)((float)i * -0.4785682F), -0.24387D, 0.05731531D);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * 65.3F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i * 9.785F));
            float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float g = f / (float)CrossbowItem.getPullTime(item, player);
            if (g > 1.0F) { g = 1.0F; }
            if (g > 0.1F) {
               float h = MathHelper.sin((f - 0.1F) * 1.3F);
               float j = g - 0.1F;
               float k = h * j;
               matrices.translate((double)(k * 0.0F), (double)(k * 0.004F), (double)(k * 0.0F));
            }
            matrices.translate((double)(g * 0.0F), (double)(g * 0.0F), (double)(g * 0.04F));
            matrices.scale(1.0F, 1.0F, 1.0F);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)i * 45.0F));
         } else {
            this.swingArm(swingProgress, matrices, i, arm);
            if (bl2 && swingProgress < 0.001F && bl) {
               matrices.translate((double)((float)i * -0.341864F), 0.0D, 0.0D);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * 10.0F));
            }
         }
         float l = bl ? 1.0F : -1.0F;
         matrices.translate(0.0F, 0.0F, -1.0F);
         matrices.translate((double)(-0.45F * (float)i), 0.45D, 1.7D);
         matrices.translate((double)(1.0F * l), 0.0D - (double)equipProgress * 0.3D, 0.3D);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * l));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * l));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
         this.altSwing(matrices, arm, swingProgress, item);
         matrices.scale(0.9F, 0.9F, 0.9F);
         this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
         matrices.translate((double)(-0.25F * (float)i), 1.25D, 0.05D);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-90 * i)));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(77.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(85 * i)));
         matrices.scale(1.2F, 1.2F, 1.2F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
         matrices.translate(0.0D, -0.15D, 0.15D);
         this.renderItem(player, item, bl3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, matrices, vertexConsumers, light);
         matrices.pop();
         if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
            float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float g = f / (float)CrossbowItem.getPullTime(item, player);
            if (g > 1.0F) { g = 1.0F; }
            if (g > 0.1F) {
               float h = MathHelper.sin((f - 0.1F) * 1.3F);
               float j = g - 0.1F;
               float k = h * j;
               matrices.translate((double)(k * 0.0F), (double)(k * 0.004F), (double)(k * 0.0F));
            }
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((double)g <= 0.2D ? 75.0F * g * 5.0F * (float)i : (float)(75 * i)));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(10.0F * g * 1.5F));
            matrices.translate((double)(-0.37F * (float)i), 0.0D, 0.6D);
            matrices.translate((double)(0.15F * g) * (double)i, 0.0D, 0.0D);
            this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
         }
      } else {
         boolean bl2 = arm == Arm.RIGHT;
         int l = bl2 ? 1 : -1;
         if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
            switch (item.getUseAction()) {
               case NONE:
                  this.applyEquipOffset(matrices, arm, equipProgress);
                  break;
               case EAT:
               case DRINK:
                  float u = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                  float y = u / 5.0F;
                  if (y > 1.0F) { y = 1.0F; }
                  float q = MathHelper.sin(u / 2.0F * 3.14F);
                  q /= 10.0F;
                  matrices.translate((double)(1 * l), 0.1D, 0.3D);
                  matrices.translate((double)(0.2F * (float)l) * (double)y, (double)(-0.7F * y), (double)(-0.2F * y));
                  matrices.translate(0.0D, (double)(-0.2F * q), (double)(-0.2F * q));
                  matrices.translate(0.0D, (double)(0.1F * this.easeInOutBack(MathHelper.sin(y * 3.14F))), 0.0D);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                  this.altSwing(matrices, arm, swingProgress, item);
                  matrices.scale(0.9F, 0.9F, 0.9F);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * y * (float)l));
                  this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                  break;
               case BLOCK:
                  float k = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                  float s = k / 4.0F;
                  float s2 = k / 6.0F;
                  if (s > 1.0F) { s = 1.0F; }
                  if (s2 > 1.0F) { s2 = 1.0F; }
                  matrices.translate(0.0D, -0.2D, 0.0D);
                  matrices.translate((double)(1 * l), 0.0D, 0.3D);
                  matrices.translate((double)(0.7F * s) * (double)l, 0.0D, (double)(-1.3F * s));
                  matrices.translate((double)(-0.2F * (float)l) * (double)s2, 0.0D, 0.0D);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(10.0F * Math.sin((double)s2 * Math.PI))));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F * s * (float)l));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(5 * l) * s));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * s));
                  matrices.translate(0.0D, 0.0D, (double)(-0.2F * s));
                  this.altSwing(matrices, arm, swingProgress, item);
                  matrices.scale(0.9F, 0.9F, 0.9F);
                  this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                  matrices.translate((double)(0.35F * (float)l), -0.13D, -0.12D);
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10.0F * (float)l));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * (float)l));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));
                  matrices.translate((double)(-0.2F * (float)l), -0.04D, 0.15D);
                  matrices.scale(1.0F, 1.0F, 1.0F);
                  break;
               case BOW:
                  matrices.push();
                  if (player.getMainArm() == Arm.LEFT) { bl = !bl; }
                  float m1 = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                  float f1 = m1 / 20.0F;
                  float f = (f1 * f1 + f1 * 2.0F) / 3.0F;
                  if (f1 > 1.0F) { f1 = 1.0F; }
                  if (f1 > 0.1F) {
                     float g1 = MathHelper.sin((m1 - 0.1F) * 1.3F);
                     float j1 = g1 * f1;
                     matrices.translate((double)(j1 * 0.0F), (double)(j1 * 0.004F), (double)(j1 * 0.0F));
                  }
                  matrices.translate(bl ? -0.1D : 0.1D, 0.0D, (double)f1 * 0.15D);
                  this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                  matrices.pop();
                  matrices.translate(bl ? -0.5D : 0.5D, -0.45D, 0.1D);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.3F));
                  if (bl) {
                     matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(-0.3F));
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(1.0F));
                  } else {
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-0.3F));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotation(1.0F));
                  }
                  this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                  if (bl) {
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(2.5F));
                  } else {
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotation(2.5F));
                  }
                  matrices.translate(bl ? -0.65D : 0.65D, -0.35D, 0.27D);
                  matrices.pop();
                  if (config.isMb3DCompat()) {
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(10 * l)));
                  }
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(75.0F));
                  matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees((float)(-15 * l)));
                  matrices.translate((double)(0.8F * (float)l), (double)(0.0F - equipProgress * 0.3F), -0.1D);
                  matrices.push();
                  break;
            }
         } else if (player.isUsingRiptide() && item.getUseAction() == UseAction.SPEAR) {
            this.riptideCounter = (float)((double)this.riptideCounter + 0.15D * tt);
            float m = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float f = m / 10.0F;
            if (f > 1.0F) { f = 1.0F; }
            if (f > 0.1F) {
               float g = MathHelper.sin((m - 0.1F) * 1.3F);
               float h = f - 0.1F;
               float j = g * h;
               matrices.translate((double)(j * 0.0F), (double)(j * 0.004F), (double)(j * 0.0F));
            }
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F - this.riptideCounter * 2.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l)));
            matrices.translate((double)(0.2F * (float)l), 0.0D, 0.75D);
            matrices.translate(0.0D, 0.0D, (double)(0.01F * MathHelper.sin(this.riptideCounter * 6.28F)));
            this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-65 * l)));
            matrices.translate((double)(0.65F * (float)l), -1.0D, -0.6D);
         } else {
            this.riptideCounter = 0.0F;
            if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
               if (item.getUseAction() == UseAction.BLOCK) {
                  matrices.translate(0.0D, -0.2D, 0.0D);
               }
            } else {
               matrices.translate((double)(0.1F * (float)l), 0.0D, -0.1D);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
            }
            matrices.translate((double)(1 * l), 0.0D - (double)equipProgress * 0.3D, 0.3D);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
            this.altSwing(matrices, arm, swingProgress, item);
            matrices.scale(0.9F, 0.9F, 0.9F);
            this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
         }
         matrices.translate((double)(-0.3F * (float)l), 0.65D, -0.1D);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-65 * l)));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
         if (item.isIn(ItemTags.WOOL_CARPETS)) {
            matrices.translate((double)(0.2F * (float)l), -0.1D, 0.0D);
         }
         if (Block.getBlockFromItem(item.getItem()) != Blocks.AIR && item.getUseAction() != UseAction.EAT && !item.isIn(ConventionalItemTags.BUCKETS)) {
            if (item.getName().toString().toLowerCase().contains("torch")) {
               matrices.scale(1.5F, 1.5F, 1.5F);
               matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(25 * l)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
               matrices.translate((double)(0.2F * (float)l), 0.2D, 0.05D);
            } else if ((item.isOf(Items.STRING) || item.isOf(Items.REDSTONE) || item.isOf(Items.LEVER) || item.isOf(Items.TRIPWIRE_HOOK) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.RAILS) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE) || item.isIn(ItemTags.DOORS)) && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES) && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
               matrices.translate(0.0D, 0.0D, -0.1D);
               matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(5 * l)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
            } else if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
               matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(25 * l)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
               matrices.translate((double)(0.2F * (float)l), 0.2D, 0.05D);
               if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
                  matrices.translate((double)(-0.2F * (float)l), 0.0D, 0.0D);
                  matrices.scale(1.1F, 1.1F, 1.1F);
               }
            } else {
               float dt = (float)(com.holdmyitems.HoldMyItems.deltaTime * 30.0D);
               float yawDelta = player.lastHeadYaw - player.getHeadYaw();
               float pitchDelta = player.lastPitch - player.getPitch();
               this.swingVelocityY += yawDelta * 0.015F * dt;
               this.swingVelocityY += swingProgress * 2.0F * dt;
               this.swingVelocityX += pitchDelta * 0.015F * dt;
               this.swingVelocityY -= 0.1F * this.swingAngleY * dt;
               this.swingVelocityX -= 0.1F * this.swingAngleX * dt;
               this.swingVelocityY = (float)((double)this.swingVelocityY * Math.pow(0.88D, dt));
               this.swingVelocityX = (float)((double)this.swingVelocityX * Math.pow(0.88D, dt));
               this.swingAngleY += this.swingVelocityY * dt;
               this.swingAngleX += this.swingVelocityX * dt;
               double currentSpeed = player.getVelocity().length();
               this.swingVelocityZ = (float)((double)this.swingVelocityZ + (bl ? (currentSpeed * -1.0D * 15.0D - (double)this.swingVelocityZ) * 0.1D * dt : (currentSpeed * 15.0D - (double)this.swingVelocityZ) * 0.1D * dt));
               if ((currentSpeed > 0.09D && player.isOnGround() || player.isSwimming() || player.isClimbing() && !player.isOnGround()) && this.client.options.getBobView().getValue()) {
                  Random random = new Random();
                  boolean randomBoolean = random.nextBoolean();
                  this.swingVelocityY += (float)(randomBoolean ? -5.5D * currentSpeed * dt : 5.5D * currentSpeed * dt);
               }
               matrices.translate(0.0D, 0.0D, -0.1D);
               matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(35 * l) + this.swingAngleY));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F + this.swingAngleX));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l) + this.swingVelocityZ));
               if (item.isIn(ItemTags.HANGING_SIGNS)) {
                  matrices.translate(0.0D, -0.1D, 0.0D);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-45 * l)));
               }
               matrices.translate((double)(0.3F * (float)l), -0.35D, 0.0D);
               matrices.translate(0.0D, 0.0D, 0.1D);
               matrices.scale(1.5F, 1.5F, 1.5F);
            }
         } else {
            if ((!item.isIn(ConventionalItemTags.TOOLS) || item.isIn(ItemTags.TRIMMABLE_ARMOR) || item.isIn(ItemTags.BOOKSHELF_BOOKS) || item.getUseAction() == UseAction.EAT || !item.isEnchantable()) && item.getUseAction() != UseAction.BOW && item.getUseAction() != UseAction.SPYGLASS && !this.isWeaponLikeItem(item) && item.getUseAction() != UseAction.BLOCK && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) && !item.isOf(Items.CARROT_ON_A_STICK) && !item.isOf(Items.FISHING_ROD) && !item.isOf(Items.SHEARS) && !item.isIn(ItemTags.HOES) && !config.isMb3DCompat()) {
               if (item.getUseAction() == UseAction.BRUSH) {
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(25.0F));
                  matrices.translate(bl ? 0.0D : 0.35D, bl ? 0.0D : 0.25D, bl ? 0.0D : 0.37D);
                  if (!bl) { matrices.scale(0.75F, 0.75F, 0.75F); }
                  matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees((float)(-75 * l)));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(35.0F));
                  matrices.translate(bl ? -0.05D : 0.85D, bl ? 0.0D : 0.05D, bl ? 0.08D : -0.2D);
               } else {
                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(5 * l)));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
                  matrices.translate(0.0D, -0.05D, -0.1D);
                  matrices.scale(0.7F, 0.7F, 0.7F);
               }
               if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH)) {
                  this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + (double)swingProgress * 0.03D * com.holdmyitems.HoldMyItems.deltaTime * 30.0D);
                  if ((player.getVelocity().length() > 0.09D && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && this.client.options.getBobView().getValue()) {
                     Random random = new Random();
                     boolean randomBoolean = random.nextBoolean();
                     this.vertVelocityYSlime += (float)(-0.05D * player.getVelocity().length() * com.holdmyitems.HoldMyItems.deltaTime * 30.0D);
                  }
                  matrices.scale(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
               }
            } else if (item.getUseAction() == UseAction.BLOCK && item.getUseAction() != UseAction.SPEAR) {
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(160 * l)));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-60 * l)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
               matrices.scale(0.75F, 0.75F, 0.75F);
               matrices.translate((double)(0.15F * (float)l), bl ? 0.35D : 0.45D, bl ? -0.15D : -0.1D);
               matrices.translate((double)(0.17F * (float)l), 0.0D, 0.3D);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-90 * l)));
            } else if (item.getUseAction() == UseAction.SPEAR) {
               matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(75 * l)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(45 * l)));
               matrices.translate((double)(-0.3F * (float)l), 0.0D, 0.0D);
            } else if (item.getUseAction() != UseAction.SPEAR) {
               matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(75 * l)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(45 * l)));
            }
            if (item.getUseAction() != UseAction.BLOCK) {
               matrices.scale(1.2F, 1.2F, 1.2F);
            }
            if (item.getUseAction() == UseAction.BOW && !player.isUsingItem()) {
               matrices.translate((double)(-0.1F * (float)l), -0.2D, 0.0D);
            }
            if (item.isOf(Items.MACE)) {
               if (config.isMb3DCompat()) {
                  matrices.translate(-0.08D, 0.17D, 0.0D);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F));
               }
               matrices.translate((double)(0.1F * (float)l), 0.0D, 0.0D);
               matrices.scale(0.9F, 0.9F, 0.9F);
            }
         }
         if (item.getItem() instanceof BlockItem && (!item.isIn(ConventionalItemTags.BUCKETS) && item.getUseAction() != UseAction.EAT && !item.isIn(ItemTags.BANNERS) && !item.isOf(Items.STRING) && !item.isOf(Items.REDSTONE) && !item.isOf(Items.LEVER) && !item.isOf(Items.TRIPWIRE_HOOK) && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES) && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.RAILS) && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE) && !item.isIn(ItemTags.DOORS) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)) && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
            BlockItem blockItem = (BlockItem)item.getItem();
            BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
            matrices.push();
            if (!bl2) { matrices.translate(-0.4F, 0.0F, 0.0F); }
            matrices.scale(0.4F, 0.4F, 0.4F);
            matrices.translate((double)(-0.9F * (float)l), -0.45D, -0.5D);
            if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BUTTONS)) {
               matrices.translate((double)(0.2F * (float)l), -0.15D, -0.2D);
            }
            if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.PRESSURE_PLATES)) {
               matrices.translate(0.0D, 0.1D, 0.0D);
            }
            if (item.isOf(Items.SLIME_BLOCK) || item.isOf(Items.HONEY_BLOCK) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.FLOWERS) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SAPLINGS) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SWORD_EFFICIENT)) {
               this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + (double)swingProgress * 0.03D * com.holdmyitems.HoldMyItems.deltaTime * 30.0D);
               if ((player.getVelocity().length() > 0.09D && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && this.client.options.getBobView().getValue()) {
                  Random random = new Random();
                  boolean randomBoolean = random.nextBoolean();
                  this.vertVelocityYSlime += (float)(-0.05D * player.getVelocity().length() * com.holdmyitems.HoldMyItems.deltaTime * 30.0D);
               }
               matrices.scale(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
            }
            BlockState blockState = blockItem.getBlock().getDefaultState();
            if ((float)player.age - this.prevAge >= 100.0F) {
               this.repPower = !this.repPower;
               this.prevAge = (float)player.age;
            }
            if (blockItem.getBlock() == Blocks.REPEATER && this.repPower) {
               blockState = blockState.with(RepeaterBlock.POWERED, true);
            }
            if (blockItem.getBlock() == Blocks.COMPARATOR && this.repPower) {
               blockState = blockState.with(ComparatorBlock.POWERED, true);
            }
            if (blockItem.getBlock() == Blocks.REDSTONE_TORCH && player.isSubmergedInWater()) {
               blockState = blockState.with(RedstoneTorchBlock.LIT, false);
            }
            if ((blockItem.getBlock() == Blocks.CAMPFIRE || blockItem.getBlock() == Blocks.SOUL_CAMPFIRE) && player.isSubmergedInWater()) {
               blockState = blockState.with(CampfireBlock.LIT, false);
            }
            if (item.isIn(ItemTags.BEDS)) {
               if (bl) { matrices.translate(0.9D, 0.0D, 0.8D); }
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * l)));
            }
             BlockState capturedState = blockState;
             RenderLayer blockLayer = BlockRenderLayers.getEntityBlockLayer(capturedState);
             vertexConsumers.submitCustom(matrices, blockLayer, (entry, consumer) -> {
                MatrixStack renderMatrices = new MatrixStack();
                renderMatrices.multiplyPositionMatrix(entry.getPositionMatrix());
                VertexConsumerProvider provider = ignoredLayer -> consumer;
                blockRenderManager.renderBlockAsEntity(
                   capturedState,
                   renderMatrices,
                   provider,
                   light,
                   OverlayTexture.DEFAULT_UV
                );
             });
             matrices.pop();
         } else {
            if (item.isIn(ConventionalItemTags.TOOLS) && !item.isIn(ItemTags.TRIMMABLE_ARMOR) && !item.isIn(ItemTags.BOOKSHELF_BOOKS) && item.getUseAction() != UseAction.EAT && item.isEnchantable() || item.getUseAction() == UseAction.BOW || item.getUseAction() == UseAction.SPYGLASS || this.isWeaponLikeItem(item) || item.getUseAction() == UseAction.BLOCK || item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) || item.isOf(Items.CARROT_ON_A_STICK) || item.isOf(Items.FISHING_ROD) || item.isOf(Items.SHEARS)) {
               if (item.isIn(ItemTags.SWORDS)) {
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F * swing));
                  matrices.translate(0.0D, (double)(0.1F * swing), (double)(-0.1F * swing));
               }
               if (item.isIn(ItemTags.SHOVELS)) {
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
               } else if (item.getUseAction() == UseAction.SPEAR) {
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F * swing_rot));
                  matrices.translate(0.0D, (double)(0.1F * swing_rot), (double)(-0.1F * swing_rot));
               } else if (item.getUseAction() != UseAction.BLOCK) {
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * swing));
                  matrices.translate(0.0D, (double)(0.05F * swing), (double)(-0.05F * swing));
               }
            }
            if (!item.isOf(Items.NETHER_STAR) && (!item.isOf(Items.END_CRYSTAL) || !config.isMb3DCompat())) {
               this.netherCounter = 0.0F;
            } else {
               this.netherCounter = (float)((double)this.netherCounter + 0.9D * tt);
               matrices.translate(0.0D, (double)(0.25F + 0.02F * MathHelper.sin(this.netherCounter * 0.1F)), 0.0D);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(3.0F * MathHelper.sin(this.netherCounter * 0.2F)));
               matrices.scale(1.0F + 0.01F * MathHelper.sin(this.netherCounter), 1.0F + 0.01F * MathHelper.sin(this.netherCounter), 1.0F + 0.01F * MathHelper.sin(this.netherCounter));
            }
            if (config.isMb3DCompat()) {
               if (item.isIn(ItemTags.SWORDS)) {
                  matrices.translate(0.0D, 0.2D, 0.0D);
               }
               if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH)) {
                  this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + (double)swingProgress * 0.03D * com.holdmyitems.HoldMyItems.deltaTime * 30.0D);
                  if ((player.getVelocity().length() > 0.09D && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && this.client.options.getBobView().getValue()) {
                     Random random = new Random();
                     boolean randomBoolean = random.nextBoolean();
                     this.vertVelocityYSlime += (float)(-0.05D * player.getVelocity().length() * com.holdmyitems.HoldMyItems.deltaTime * 30.0D);
                  }
                  matrices.scale(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
               }
            }
            if (item.isIn(ItemTags.SHOVELS)) {
               matrices.translate((double)(0.07F * (float)l), 0.0D, 0.05D);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * l)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0F));
            }
            if (item.isOf(Items.TORCH)) {
               player.getEntityWorld().addParticleClient(ParticleTypes.ITEM_SLIME, player.getEntityPos().getX(), player.getEntityPos().getY(), player.getEntityPos().getZ(), 0.1D, 0.1D, 0.1D);
            }
            this.renderItem(player, item, bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, matrices, vertexConsumers, light);
         }
      }
      matrices.pop();
      matrices.pop();
      this.isAttacking = this.client.options.attackKey.isPressed();
      } finally {
      }
   }

   @Unique
   private void feverVisual$renderLuaHmi(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress,
                                         ItemStack item, float equipProgress, MatrixStack matrices,
                                         OrderedRenderCommandQueue queue, int light) throws ScriptException, NoSuchMethodException {
      if (player.isUsingSpyglass()) {
         this.invokeRenderFirstPersonItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, queue, light);
         return;
      }

      LuaTestHMI.deltaTime = (float) com.holdmyitems.HoldMyItems.deltaTime;
      LuaTestHMI.tickProgress = tickDelta;
      ((ItemStackAccessor)(Object)item).hMI5_0$setTransform(-1);

      boolean mainHand = hand == Hand.MAIN_HAND;
      if (mainHand) {
         this.feverVisual$mainHandSwitchEvent = item.getItem() != this.feverVisual$prevMainHand;
      } else {
         this.feverVisual$offHandSwitchEvent = item.getItem() != this.feverVisual$prevOffHand;
      }

      boolean interact = false;
      boolean blockBreaking = false;
      if (player instanceof LivingEntityAccessor accessor) {
         this.feverVisual$mainHandSwingProgress = accessor.hMI5_0$getMainHandSwingProgress(tickDelta);
         this.feverVisual$offHandSwingProgress = accessor.hMI5_0$getOffHandSwingProgress(tickDelta);
         swingProgress = mainHand ? this.feverVisual$mainHandSwingProgress : this.feverVisual$offHandSwingProgress;
         this.feverVisual$swingMHand = accessor.hMI5_0$getMHandEvent();
         this.feverVisual$swingOHand = accessor.hMI5_0$getOHandEvent();
         interact = mainHand ? accessor.hMI5_0$getMInteract() : accessor.hMI5_0$getOInteract();
         blockBreaking = accessor.hMI5_0$getBlockBreak();
      }

      Arm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      boolean rightArm = arm == Arm.RIGHT;
      matrices.push();
      var config = this.getConfig();
      if (config != null) {
         float offsetSide = mainHand ? 1.0F : -1.0F;
         matrices.translate(config.getViewmodelXOffset() * offsetSide, config.getViewmodelYOffset(), config.getViewmodelZOffset());
         LuaScriptCache.swingSpeed = Math.max(1, config.getSwingSpeed());
      }

      this.feverVisual$scenePose(matrices, item, rightArm, swingProgress, equipProgress, player, mainHand, hand,
              this.feverVisual$mainHandSwingProgress, this.feverVisual$offHandSwingProgress,
              this.feverVisual$mainHandSwitchEvent, this.feverVisual$offHandSwitchEvent,
              this.feverVisual$swingMHand, this.feverVisual$swingOHand, interact, blockBreaking, this.feverVisual$hmiParticles);

      matrices.push();
      this.feverVisual$handRelativePose(matrices, item, rightArm, swingProgress, equipProgress, player, mainHand, hand,
              this.feverVisual$mainHandSwingProgress, this.feverVisual$offHandSwingProgress,
              this.feverVisual$mainHandSwitchEvent, this.feverVisual$offHandSwitchEvent,
              this.feverVisual$swingMHand, this.feverVisual$swingOHand, interact, blockBreaking, this.feverVisual$hmiParticles);
      int combinedLight = LightmapTextureManager.applyEmission(light, Block.getBlockFromItem(item.getItem()).getDefaultState().getLuminance());
       if (!player.isInvisible() && !item.contains(DataComponentTypes.MAP_ID)) {
          this.renderArmHoldingItem(matrices, queue, combinedLight, 0.0F, 0.0F, arm);
       }
      matrices.pop();

      matrices.push();
      this.feverVisual$itemPose(matrices, item, rightArm, swingProgress, player, mainHand, hand, equipProgress,
              this.feverVisual$mainHandSwingProgress, this.feverVisual$offHandSwingProgress,
              this.feverVisual$mainHandSwitchEvent, this.feverVisual$offHandSwitchEvent,
              this.feverVisual$swingMHand, this.feverVisual$swingOHand, interact, blockBreaking, this.feverVisual$hmiParticles);

      if (item.contains(DataComponentTypes.MAP_ID)) {
         matrices.push();
         matrices.translate(rightArm ? -0.05D : 0.05D, 0.2D, 0.1D);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rightArm ? -12.0F : 12.0F));
         this.renderMapInOneHand(matrices, queue, light, equipProgress, arm, swingProgress, item);
         matrices.pop();
      } else if (!item.isEmpty()) {
         ItemStack renderStack = mainHand ? GlobalsStorage.mainHandItem : GlobalsStorage.offHandItem;
         if (renderStack.isOf(Items.AIR)) {
            renderStack = item;
         }
         DispatcherStorage.setItem(item);
         ItemModelStorage.addData(new ItemModelContext(rightArm, swingProgress, player, hand, mainHand, LuaTestHMI.deltaTime, equipProgress,
                 this.feverVisual$mainHandSwingProgress, this.feverVisual$offHandSwingProgress,
                 this.feverVisual$mainHandSwitchEvent, this.feverVisual$offHandSwitchEvent,
                 this.feverVisual$swingMHand, this.feverVisual$swingOHand, interact, blockBreaking, item), item);
         this.renderItem(player, renderStack, rightArm ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, matrices, queue, light);
      }

      if (rightArm) {
         LuaTestHMI.matricesMain.set(matrices.peek().getPositionMatrix());
      } else {
         LuaTestHMI.matricesOff.set(matrices.peek().getPositionMatrix());
      }
      matrices.push();
      ParticleRenderManager.draw(this.feverVisual$hmiParticles, matrices, queue, "ITEM", hand, light, player, tickDelta);
      matrices.pop();
      matrices.pop();

      matrices.push();
      ParticleRenderManager.draw(this.feverVisual$hmiParticles, matrices, queue, "SCREEN", hand, light, player, tickDelta);
      matrices.pop();

      matrices.pop();
      if (mainHand) {
         this.feverVisual$prevMainHand = item.getItem();
      } else {
         this.feverVisual$prevOffHand = item.getItem();
      }
      GlobalsStorage.offHandItem = Items.AIR.getDefaultStack();
      GlobalsStorage.mainHandItem = Items.AIR.getDefaultStack();
   }

   @Unique
   private void feverVisual$itemPose(MatrixStack matrices, ItemStack item, boolean rightArm, float swingProgress,
                                     AbstractClientPlayerEntity player, boolean mainHand, Hand hand, float equipProgress,
                                     float mainHandSwingProgress, float offHandSwingProgress, boolean mainHandSwitchEvent,
                                     boolean offHandSwitchEvent, boolean swingMHand, boolean swingOHand, boolean interact,
                                     boolean blockBreaking, List<Particle> particles) throws ScriptException, NoSuchMethodException {
      int side = rightArm ? 1 : -1;
      matrices.translate(0.5D * side, -0.15D, -0.85D);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F), 0.5F, 0.5F, 0.5F);
      matrices.scale(0.9F, 0.9F, 0.9F);
      ScriptHolder.itemScriptCache.execute(matrices, rightArm, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand,
              LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent,
              offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
      for (LuaScriptCache cache : ScriptHolder.itemAddonsCache) {
         cache.execute(matrices, rightArm, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand,
                 LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent,
                 offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
      }
   }

   @Unique
   private void feverVisual$handRelativePose(MatrixStack matrices, ItemStack item, boolean rightArm, float swingProgress,
                                             float equipProgress, AbstractClientPlayerEntity player, boolean mainHand, Hand hand,
                                             float mainHandSwingProgress, float offHandSwingProgress, boolean mainHandSwitchEvent,
                                             boolean offHandSwitchEvent, boolean swingMHand, boolean swingOHand, boolean interact,
                                             boolean blockBreaking, List<Particle> particles) throws ScriptException, NoSuchMethodException {
      int side = rightArm ? 1 : -1;
      ScriptHolder.handRelativeScriptCache.execute(matrices, rightArm, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand,
              LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent,
              offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
      for (LuaScriptCache cache : ScriptHolder.handRelativeAddonsCache) {
         cache.execute(matrices, rightArm, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand,
                 LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent,
                 offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
      }
      if (!item.isEmpty()) {
         matrices.translate(1.5D * side, -0.3D, -0.6D);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F), 0.5F * side, 0.5F, 0.5F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * side), 0.5F * side, 0.5F, 0.5F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-65.0F * side), 0.5F * side, 0.5F, 0.5F);
         matrices.scale(0.9F, 0.9F, 0.9F);
      }
   }

   @Unique
   private void feverVisual$scenePose(MatrixStack matrices, ItemStack item, boolean rightArm, float swingProgress,
                                      float equipProgress, AbstractClientPlayerEntity player, boolean mainHand, Hand hand,
                                      float mainHandSwingProgress, float offHandSwingProgress, boolean mainHandSwitchEvent,
                                      boolean offHandSwitchEvent, boolean swingMHand, boolean swingOHand, boolean interact,
                                      boolean blockBreaking, List<Particle> particles) throws ScriptException, NoSuchMethodException {
      ScriptHolder.handScriptCache.execute(matrices, rightArm, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand,
              LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent,
              offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
      for (LuaScriptCache cache : ScriptHolder.handAddonsCache) {
         cache.execute(matrices, rightArm, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand,
                 LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent,
                 offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
      }
      if (!item.isEmpty()) {
         matrices.translate(0.0D, -0.35D, 0.2D);
      }
   }
}
