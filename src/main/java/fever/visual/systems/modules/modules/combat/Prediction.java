package fever.visual.systems.modules.modules.combat;

import fever.visual.utility.render.compat.GlStateManager.DstFactor;
import fever.visual.utility.render.compat.GlStateManager.SrcFactor;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.shared.PredicateValue;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.PotionUtility;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.inventory.EnchantmentUtility;
import fever.visual.utility.render.Draw3DUtility;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.Utils;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Prediction", category = ModuleCategory.COMBAT)
public class Prediction extends BaseModule {
   private static final Identifier BLOOM_TEXTURE = FeverVisual.id("textures/bloom.png");
   private static final Identifier HIT_TEXTURE = FeverVisual.id("textures/hit.png");

   private final List<Predicted> predicted = new ArrayList<>();
   private final List<Landed> landed = new ArrayList<>();
   private final List<ProjectileEntity> projectiles = new ArrayList<>(3);
   private final SelectSetting entities = new SelectSetting(this, "modules.settings.prediction.entities");
   private final ModeSetting renderMode = new ModeSetting(this, "modules.settings.prediction.render_mode");
   private final ModeSetting.Value defaultMode = new ModeSetting.Value(this.renderMode, "modules.settings.prediction.render_mode.default");
   private final ModeSetting.Value glowMode = new ModeSetting.Value(this.renderMode, "modules.settings.prediction.render_mode.glow").select();
   private final BooleanSetting inHand = new BooleanSetting(this, "modules.settings.prediction.hand").enable();
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.prediction.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.prediction.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.prediction.color_mode.custom");
   private final ColorSetting colorFirst = new ColorSetting(this, "modules.settings.prediction.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(160, 115, 255, 255));
   private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.prediction.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(100, 160, 255, 255));
   private final EventListener<HudRenderEvent> onRender2D = event -> {
      CustomDrawContext context = event.getContext();
      MatrixStack ms = context.getMatrices();

      for (Predicted predict : this.predicted) {
         if (predict.entity instanceof ProjectileEntity projectile && projectile.getOwner() != mc.player) {
            continue;
         }

         Vec2f screenPos = Utils.worldToScreen(predict.vectors.getLast());
         if (screenPos != null) {
            float x = screenPos.x;
            float y = screenPos.y;
            Font font = Fonts.MEDIUM.getFont(13.0F);
            float height = font.height() + 6.0F;
            float yOff = -height;
            String name = predict.entity.getName().getString().replace("Брошенный эндер-жемчуг", "Эндер-жемчуг");
            if (predict.entity instanceof PotionEntity potion) {
               name = potion.getStack().getFormattedName().getString();
            }

            name = name.replace("] ", "").replace("[", "") + String.format(" (%s сек)", TextUtility.formatNumber(predict.ticks / 20.0F));

            ItemStack stack = switch (predict.entity) {
               case ThrownItemEntity item -> item.getStack();
               case PersistentProjectileEntity itemx -> itemx.getItemStack();
               case ItemEntity itemxx -> itemxx.getStack();
               default -> Items.ARROW.getDefaultStack();
            };
            float distance = (float)predict.vectors.getLast().distanceTo(mc.player.getEyePos());
            float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
            ms.push();
            ms.translate(x, y, 0.0F);
            ms.scale(scale, scale, 1.0F);
            float firstWidth = font.width(name) + 20.0F;
            context.drawRect(-firstWidth / 2.0F, yOff, firstWidth, height, new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F));
            context.drawItem(stack, -firstWidth / 2.0F, yOff, 1.0F);
            context.drawText(font, name, -firstWidth / 2.0F + 17.0F, yOff + 3.0F, Colors.WHITE);
            yOff += height;
            if (predict.entity instanceof ProjectileEntity projectile && projectile.getOwner() instanceof AbstractClientPlayerEntity player) {
               String owner = "От " + (projectile.getOwner() == mc.player ? "Вас" : projectile.getOwner().getName().getString());
               float secondWidth = font.width(owner) + 22.0F;
               context.drawRect(-secondWidth / 2.0F, yOff, secondWidth, height, new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F));
               context.drawHead(player, -secondWidth / 2.0F, yOff, height, BorderRadius.ZERO, Colors.WHITE);
               context.drawText(font, owner, -secondWidth / 2.0F + 19.0F, yOff + 3.0F, Colors.WHITE);
               yOff += height;
            }

            if (predict.entity instanceof PotionEntity potion) {
               for (StatusEffectInstance effect : PotionUtility.effects(potion.getStack())) {
                  String potionName = ((StatusEffect)effect.getEffectType().value()).getName().getString();
                  int amplifier = effect.getAmplifier();
                  int duration = effect.getDuration();
                  String potionLevel = amplifier > 0 ? " " + (amplifier + 1) : "";
                  String potionTime = this.formatDuration(duration);
                  String fullPotionText = potionName + potionLevel + " (" + potionTime + ")";
                  float potionWidth = font.width(fullPotionText) + 6.0F;
                  context.drawRect(-potionWidth / 2.0F, yOff + 5.0F, potionWidth, height, new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F));
                  context.drawText(
                          font,
                          fullPotionText,
                          -potionWidth / 2.0F + 3.0F,
                          yOff + 8.0F,
                          ColorRGBA.fromInt(((StatusEffect)effect.getEffectType().value()).getColor()).withAlpha(255.0F)
                  );
                  yOff += height;
               }
            }

            ms.pop();
         }
      }
   };
   private final EventListener<Render3DEvent> onRender3D = new EventListener<>() {
      @Override
      public void onEvent(Render3DEvent event) {
         Prediction.this.renderPrediction3D(event);
      }

      @Override
      public int getPriority() {
         return -100;
      }
   };

   private void renderPrediction3D(Render3DEvent event) {
      MatrixStack ms = event.getMatrices();
      ms.push();
      RenderUtility.setupRender3D(true);
      RenderUtility.prepareMatrices(ms);
      RenderSystem.enableDepthTest();

      if (this.defaultMode.isSelected()) {
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

         for (Predicted predict : this.predicted) {
            if (predict.entity instanceof ProjectileEntity projectile && projectile.getOwner() != mc.player) {
               continue;
            }

            Vec3d prevPos = predict.vectors.getFirst();
            Draw3DUtility.drawLine(ms, builder, Utils.getInterpolatedPos(predict.entity, event.getTickDelta()), prevPos, this.getPredictionColor(0.0F));
            int pointIndex = 0;

            for (Vec3d pos : predict.vectors) {
               Draw3DUtility.drawLine(ms, builder, prevPos, pos, this.getPredictionColor(pointIndex++));
               prevPos = pos;
            }
         }

         RenderUtility.buildBuffer(builder);
      } else {
         RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         for (Predicted predict : this.predicted) {
            if (predict.entity instanceof ProjectileEntity projectile && projectile.getOwner() != mc.player) {
               continue;
            }

            Vec3d prevPos = predict.vectors.getFirst();
            Vec3d entityPos = Utils.getInterpolatedPos(predict.entity, event.getTickDelta());
            if (entityPos.distanceTo(mc.player.getEyePos()) > 2.0) {
               for (int i = 0; i < 10; i++) {
                  float t = i / 10.0F;
                  Vec3d interpolatedPos = entityPos.add(prevPos.subtract(entityPos).multiply(t));
                  this.drawGlow(ms, interpolatedPos, buffer, (float)prevPos.distanceTo(entityPos) / 3.0F, 1.0F, i);
                  this.drawGlow(ms, interpolatedPos, buffer, (float)prevPos.distanceTo(entityPos) * 2.0F, 0.05F, i + 20);
               }
            }

            int pointIndex = 0;
            for (Vec3d pos : predict.vectors) {
               if (pos.distanceTo(mc.player.getEyePos()) > 2.0) {
                  for (int i = 0; i < 10; i++) {
                     float t = i / 10.0F;
                     Vec3d interpolatedPos = prevPos.add(pos.subtract(prevPos).multiply(t));
                     this.drawGlow(ms, interpolatedPos, buffer, (float)pos.distanceTo(prevPos) / 3.0F, 1.0F, pointIndex + i);
                     this.drawGlow(ms, interpolatedPos, buffer, (float)pos.distanceTo(prevPos) * 2.0F, 0.05F, pointIndex + i + 20);
                  }
               }

               prevPos = pos;
               pointIndex++;
            }

            float size = 9.0F;
            if (predict.entity instanceof PotionEntity) {
               ms.push();
               ms.translate(predict.vectors.getLast());
               ms.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-90.0F));
               DrawUtility.drawImage(
                       ms, buffer, (double)(-size / 2.0F), (double)(-size / 2.0F), 0.0, (double)size, (double)size, this.getPredictionColor(0.0F).withAlpha(255.0F)
               );
               ms.pop();
            }
         }

         RenderUtility.buildBuffer(buffer);
      }

      float size = 1.0F;
      RenderSystem.setShaderTexture(0, HIT_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (Landed landed : this.landed) {
         if (landed.collidedEntity == null) {
            ms.push();
            ms.translate(landed.hitResult.getPos());
            ms.multiply(landed.hitResult.getSide().getRotationQuaternion());
            ms.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-90.0F));
            DrawUtility.drawImage(ms, buffer, (double)(-size / 2.0F), (double)(-size / 2.0F), 0.0, (double)size, (double)size, this.getPredictionColor(0.0F).withAlpha(255.0F));
            ms.pop();
         }
      }

      RenderUtility.buildBuffer(buffer);
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getCameraPos();
      BufferBuilder quadsBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

      for (Landed landedx : this.landed) {
         if (landedx.collidedEntity != null) {
            Draw3DUtility.renderFilledBox(ms, quadsBuffer, landedx.collidedEntity.getBoundingBox(), this.getPredictionColor(0.0F).mulAlpha(0.5F));
         }
      }

      RenderUtility.buildBuffer(quadsBuffer);
      BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (Landed landedxx : this.landed) {
         if (landedxx.collidedEntity != null) {
            Draw3DUtility.renderOutlinedBox(ms, linesBuffer, landedxx.collidedEntity.getBoundingBox(), this.getPredictionColor(20.0F));
         }
      }

      RenderUtility.buildBuffer(linesBuffer);
      RenderUtility.endRender3D();
      ms.pop();
   }

   public Prediction() {
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.pearls", entity -> entity instanceof EnderPearlEntity).select();
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.tridents", entity -> entity instanceof TridentEntity).select();
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.snowballs", entity -> entity instanceof SnowballEntity).select();
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.arrows", entity -> entity instanceof ArrowEntity).select();
      new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.potions", entity -> entity instanceof PotionEntity).select();
   }

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      this.predicted.clear();
      this.landed.clear();
      this.projectiles.clear();
      if (this.inHand.isEnabled()) {
         ItemStack handStack = mc.player.getMainHandStack();
         Entity inHand = null;
         if (handStack.getItem() instanceof EnderPearlItem) {
            inHand = new EnderPearlEntity(mc.world, mc.player, handStack);
         } else if (handStack.getItem() instanceof TridentItem && mc.player.isUsingItem()) {
            inHand = new TridentEntity(mc.world, mc.player, handStack);
         } else if (handStack.getItem() instanceof BowItem && mc.player.isUsingItem()) {
            ItemStack arrowStack = new ItemStack(Items.ARROW);
            inHand = new ArrowEntity(mc.world, mc.player, arrowStack, handStack);
         } else if (handStack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(handStack)) {
            boolean hasMultishot = EnchantmentUtility.getEnchantmentLevel(handStack, Enchantments.MULTISHOT) > 0;
            ItemStack arrowStack = new ItemStack(Items.ARROW);
            if (hasMultishot) {
               for (int i = 0; i < 3; i++) {
                  ArrowEntity arrow = new ArrowEntity(mc.world, mc.player, arrowStack, handStack);
                  this.projectiles.add(arrow);
               }
            } else {
               inHand = new ArrowEntity(mc.world, mc.player, arrowStack, handStack);
            }
         }

         if (inHand instanceof ProjectileEntity projectile) {
            float speed = 1.5F;
            if (inHand instanceof TridentEntity) {
               speed = 2.5F;
            } else if (inHand instanceof ArrowEntity) {
               speed = 3.0F;
            }

            this.setVelocity(projectile, mc.player, mc.player.getPitch(), mc.player.getYaw(), 0.0F, speed, 1.0F);
            this.predict(projectile, true);
         }
      }

      if (!this.projectiles.isEmpty()) {
         float speed = 3.15F;
         float spreadAngle = 10.0F;

         for (int i = 0; i < this.projectiles.size(); i++) {
            ProjectileEntity projectile = this.projectiles.get(i);
            float yawOffset = 0.0F;
            if (i == 0) {
               yawOffset = -spreadAngle;
            } else if (i == 2) {
               yawOffset = spreadAngle;
            }

            this.setVelocity(projectile, mc.player, mc.player.getPitch(), mc.player.getYaw() + yawOffset, 0.0F, speed, 1.0F);
            this.predict(projectile, true);
         }
      }

      for (Entity entity : mc.world.getEntities()) {
         this.predict(entity, false);
      }
   }

   private void predict(Entity entity, boolean inHand) {
      if (entity instanceof ProjectileEntity projectile && projectile.getOwner() != mc.player) {
         return;
      }

      if (this.isValid(entity)) {
         if (entity instanceof ProjectileEntity pearl && pearl.getOwner() == null) {
            AbstractClientPlayerEntity closestPlayer = null;
            double closestDistance = Double.MAX_VALUE;
            for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
               double distance = player.squaredDistanceTo(pearl);
               if (distance < closestDistance) {
                  closestDistance = distance;
                  closestPlayer = player;
               }
            }
            if (closestPlayer != null) {
               pearl.setOwner(closestPlayer);
            }
         }

         List<Vec3d> positions = new ArrayList<>();
         Vec3d lastPos = entity.getEntityPos();
         Vec3d lastMotion = entity.getVelocity();
         Entity collidedEntity = null;
         int ticks = 0;
         BlockHitResult blockHitResult = null;

         for (int i = 0; i < 150; i++) {
            Vec3d motion = this.predictMotion(entity, lastMotion);
            Vec3d pos = lastPos.add(motion);
            ticks = i;
            blockHitResult = mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.COLLIDER, FluidHandling.NONE, entity));
            Entity collided = this.checkEntityCollision(entity, pos);
            if (collided != null) {
               if (collided instanceof LivingEntity && (isEntityInvisible(collided) || isEntityDead(collided))) {
                  lastPos = pos;
                  lastMotion = motion;
                  positions.add(pos);
                  continue;
               }
               positions.add(pos);
               collidedEntity = collided;
               break;
            }

            if (blockHitResult.getType() != Type.MISS) {
               positions.add(blockHitResult.getPos());
               break;
            }

            positions.add(pos);
            lastPos = pos;
            lastMotion = motion;
         }

         if (!positions.isEmpty()) {
            if (collidedEntity != null && collidedEntity instanceof LivingEntity) {
               if (isEntityInvisible(collidedEntity) || isEntityDead(collidedEntity)) {
                  return;
               }
            }

            if (inHand) {
               this.landed.add(new Landed(entity, positions.getLast(), ticks, collidedEntity, blockHitResult));
            } else {
               this.predicted.add(new Predicted(entity, positions, ticks, collidedEntity));
            }
         }
      }
   }

   private void drawGlow(MatrixStack ms, Vec3d pos, BufferBuilder buffer, float size, float alpha, float colorIndex) {
      ms.push();
      ms.translate(pos);
      ms.multiply(mc.gameRenderer.getCamera().getRotation());
      DrawUtility.drawImage(
              ms, buffer, (double)(-size / 2.0F), (double)(-size / 2.0F), 0.0, (double)size, (double)size, this.getPredictionColor(colorIndex).withAlpha(255.0F * alpha)
      );
      ms.pop();
   }

   private ColorRGBA getPredictionColor(float index) {
      if (this.colorCustom.isSelected()) {
         return this.colorFirst.getColorSafe().mix(this.colorSecond.getColorSafe(), (index % 40.0F) / 39.0F);
      }

      return Colors.getAccentColor(index * 6.0F);
   }

   private boolean isValid(Entity entity) {
      if (entity instanceof ProjectileEntity projectile) {
         if (projectile.getOwner() != mc.player) {
            return false;
         }
      }

      boolean valid = false;

      for (SelectSetting.Value selectedValue : this.entities.getSelectedValues()) {
         PredicateValue<Entity> predicateValue = (PredicateValue<Entity>)selectedValue;
         if (predicateValue.predicated(entity)) {
            valid = true;
         }
      }

      return entity instanceof TridentEntity trident && trident.returnTimer > 0
              ? false
              : valid && (Math.abs(entity.getVelocity().x + entity.getVelocity().z) > 0.01F || Math.abs(entity.getVelocity().y) > 0.2F);
   }

   private Entity checkEntityCollision(Entity movingEntity, Vec3d predictedPos) {
      Vec3d currentPos = movingEntity.getEntityPos();
      Vec3d direction = predictedPos.subtract(currentPos);
      if (direction.lengthSquared() == 0.0) {
         return null;
      } else {
         EntityHitResult hitResult = ProjectileUtil.raycast(
                 movingEntity,
                 currentPos,
                 predictedPos,
                 movingEntity.getBoundingBox().stretch(direction).expand(0.5),
                 entity -> mc.player != entity
                         && entity.isAlive()
                         && !(entity instanceof ItemEntity)
                         && !(entity instanceof ExperienceOrbEntity)
                         && !(entity instanceof LivingEntity livingEntity && (isEntityInvisible(livingEntity) || isEntityDead(livingEntity)))
                         && entity != movingEntity,
                 direction.lengthSquared()
         );
         return hitResult != null ? hitResult.getEntity() : null;
      }
   }

   private void setVelocity(ProjectileEntity entity, double x, double y, double z, float power) {
      Vec3d vec3d = this.calculateVelocity(entity, x, y, z, power);
      entity.setVelocity(vec3d);
      entity.velocityDirty = true;
      double d = vec3d.horizontalLength();
      entity.setYaw((float)(MathHelper.atan2(vec3d.x, vec3d.z) * 180.0F / (float)Math.PI));
      entity.setPitch((float)(MathHelper.atan2(vec3d.y, d) * 180.0F / (float)Math.PI));
      entity.lastYaw = entity.getYaw();
      entity.lastPitch = entity.getPitch();
   }

   private void setVelocity(ProjectileEntity entity, Entity shooter, float pitch, float yaw, float roll, float speed, float divergence) {
      float f = -MathHelper.sin(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
      float g = -MathHelper.sin((pitch + roll) * (float) (Math.PI / 180.0));
      float h = MathHelper.cos(yaw * (float) (Math.PI / 180.0)) * MathHelper.cos(pitch * (float) (Math.PI / 180.0));
      this.setVelocity(entity, f, g, h, speed);
      Vec3d vec3d = shooter.getMovement();
      entity.setVelocity(entity.getVelocity().add(vec3d.x, shooter.isOnGround() ? 0.0 : vec3d.y, vec3d.z));
   }

   private Vec3d calculateVelocity(ProjectileEntity entity, double x, double y, double z, float power) {
      return new Vec3d(x, y, z).normalize().multiply(power);
   }

   private Vec3d predictMotion(Entity entity, Vec3d motion) {
      return motion.multiply(0.99).add(0.0, -entity.getFinalGravity(), 0.0);
   }

   private boolean isEntityInvisible(Entity entity) {
      if (!(entity instanceof LivingEntity livingEntity)) {
         return false;
      }

      if (livingEntity.hasStatusEffect(StatusEffects.INVISIBILITY)) {
         return true;
      }
      if (livingEntity.isInvisible()) {
         return true;
      }
      if (livingEntity.isInvisibleTo(mc.player)) {
         return true;
      }

      return false;
   }

   private boolean isEntityDead(Entity entity) {
      if (!(entity instanceof LivingEntity livingEntity)) {
         return false;
      }

      return livingEntity.isDead() || livingEntity.getHealth() <= 0;
   }
   private String formatDuration(int ticks) {
      int seconds = ticks / 20;
      int minutes = seconds / 60;
      int remainingSeconds = seconds % 60;
      return minutes > 0 ? String.format("%d:%02d", minutes, remainingSeconds) : String.format("0:%02d", remainingSeconds);
   }

   record Landed(Entity entity, Vec3d pos, int ticks, Entity collidedEntity, BlockHitResult hitResult) {
   }

   record Predicted(Entity entity, List<Vec3d> vectors, int ticks, Entity collidedEntity) {
   }
}
