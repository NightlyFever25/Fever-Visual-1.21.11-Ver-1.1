package fever.visual.systems.modules.modules.combat;

import fever.visual.utility.render.compat.GlStateManager.DstFactor;
import fever.visual.utility.render.compat.GlStateManager.SrcFactor;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.EntityUtility;
import fever.visual.utility.math.MathUtility;
import fever.visual.utility.render.CrystalRenderer;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.RenderUtility;
import net.minecraft.client.MinecraftClient;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import fever.visual.utility.render.compat.BufferRenderer;
import net.minecraft.client.render.Camera;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ModuleInfo(name = "Target ESP", category = ModuleCategory.COMBAT, desc = "Помечает активную цель")
public class TargetESP extends BaseModule {
   private static final Identifier RHOMB_TEXTURE = FeverVisual.id("images/world/cube.png");
   private static final Identifier GHOST_GLOW_TEXTURE = FeverVisual.id("images/particle/ghost-glow.png");
   private static final Identifier BLOOM_TEXTURE = FeverVisual.id("textures/bloom.png");
   private static final Identifier CHAIN_TEXTURE = FeverVisual.id("images/world/chain.png");
   private static final int CIRCLE_SEGMENTS = 64;
   private static final Vec3d[] CIRCLE_POINTS = createCirclePoints();

   private final ModeSetting mode = new ModeSetting(this, "modules.settings.target_esp.mode");
   private final ModeSetting.Value rhomb = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.rhomb").select();
   private final ModeSetting.Value ghost = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.ghost");
   private final ModeSetting.Value souls = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.souls");
   private final ModeSetting.Value circle = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.circle");
   private final ModeSetting.Value crystals = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.crystals");
   private final ModeSetting.Value chain = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.chain");
   private final ModeSetting ghostStyle = new ModeSetting(this, "modules.settings.target_esp.ghost_style", () -> !this.ghost.isSelected());
   private final ModeSetting.Value ghostTrails = new ModeSetting.Value(this.ghostStyle, "modules.settings.target_esp.ghost_style.trails").select();
   private final ModeSetting.Value ghostSouls = new ModeSetting.Value(this.ghostStyle, "modules.settings.target_esp.ghost_style.souls");
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.target_esp.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.target_esp.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.target_esp.color_mode.custom");
   private final ColorSetting colorTarget = new ColorSetting(this, "modules.settings.target_esp.custom_color", () -> !colorCustom.isSelected())
           .color(new ColorRGBA(160, 115, 255, 255));
   private final ColorSetting colorTargetSecond = new ColorSetting(this, "modules.settings.target_esp.custom_color_second", () -> !colorCustom.isSelected())
           .color(new ColorRGBA(100, 160, 255, 255));
   private final SliderSetting opacity = new SliderSetting(this, "modules.settings.target_esp.opacity").step(0.01F).min(0.7F).max(1.0F).currentValue(0.05F);
   private final Animation animation = new Animation(300L, 0.0F, Easing.BOTH_CUBIC);
   private final Animation moving = new Animation(70L, 0.0F, Easing.LINEAR);
   private LivingEntity prevTarget;
   private LivingEntity lastTarget;
   private Vec3d smoothedPos;
   private float hurtProgress;
   private float soulsRotation;
   private long ghostLastUpdateTime = System.currentTimeMillis();
   private long lastFrameTime = System.currentTimeMillis();
   private float circleStep;
   private float circleSpeed = 1.0F;
   private boolean circleFlipSpeed;
   private double smoothCircleY;
   private double smoothCircleY2;
   private final List<GhostTrail> ghostTrailPoints = new ArrayList<>();
   private final Map<Entity, Long> targetTimers = new HashMap<>();
   private static final long LOOK_TIMEOUT = 3000;
   private static final double MAX_DISTANCE = 8.0;
   private boolean wasInChat = false;
   private Entity currentTarget = null;

   private final EventListener<Render3DEvent> onRender3D = new EventListener<>() {
      @Override
      public void onEvent(Render3DEvent event) {
         renderTargetEsp(event);
      }

      @Override
      public int getPriority() {
         return -100;
      }
   };

   private void renderTargetEsp(Render3DEvent event) {
      if (EntityUtility.isInGame()) {
         float deltaTime = this.getDeltaTime();
         currentTarget = getCurrentTarget();
         LivingEntity target = null;
         if (currentTarget instanceof LivingEntity livingTarget) {
            target = livingTarget;
         }

         this.animation.setEasing(Easing.FIGMA_EASE_IN_OUT);
         this.animation.update(target != null);
         this.moving.update(this.moving.getValue() + 10.0F + 50.0F);
         this.updateCircleAnimation(deltaTime);
         if (target != null) {
            this.prevTarget = target;
            this.updateTargetAnimation(target);
         } else {
            if (this.prevTarget != null && !this.isTargetRenderable(this.prevTarget)) {
               this.prevTarget = null;
               this.animation.update(false);
            }
            this.smoothedPos = null;
            this.lastTarget = null;
            this.ghostTrailPoints.clear();
            this.ghostLastUpdateTime = System.currentTimeMillis();
            this.smoothCircleY = 0.0;
            this.smoothCircleY2 = 0.0;
         }

         if (this.prevTarget != null && this.animation.getValue() != 0.0F && this.isTargetRenderable(this.prevTarget)) {
            MatrixStack ms = event.getMatrices();
            ms.push();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            if (this.crystals.isSelected()) {
               RenderSystem.enableDepthTest();
               this.drawCrystals(ms, this.prevTarget);
            } else if (this.chain.isSelected()) {
               RenderSystem.enableDepthTest();
               this.drawChain(ms, this.prevTarget);
            } else if (this.rhomb.isSelected()) {
               RenderSystem.disableDepthTest();
               this.drawRhomb(ms, this.prevTarget);
            } else if (this.ghost.isSelected()) {
               RenderSystem.disableDepthTest();
               this.drawFeverGhost(ms, this.prevTarget);
            } else if (this.circle.isSelected()) {
               RenderSystem.enableDepthTest();
               this.drawCircle(ms, this.prevTarget);
            } else {
               RenderSystem.disableDepthTest();
               this.drawGhosts(ms, this.prevTarget);
            }

            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            ms.pop();
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      updateTarget();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      targetTimers.clear();
      wasInChat = false;
      currentTarget = null;
      ghostTrailPoints.clear();
      smoothedPos = null;
      lastTarget = null;
      smoothCircleY = 0.0;
      smoothCircleY2 = 0.0;
   }

   private float getDeltaTime() {
      long currentTime = System.currentTimeMillis();
      float deltaMs = MathHelper.clamp((float)(currentTime - this.lastFrameTime), 1.0F, 100.0F);
      this.lastFrameTime = currentTime;
      return deltaMs / (1000.0F / 60.0F);
   }

   private void updateCircleAnimation(float deltaTime) {
      this.circleSpeed = this.circleFlipSpeed ? this.circleSpeed - 0.5F * deltaTime : this.circleSpeed + 0.5F * deltaTime;
      if (this.circleSpeed > 25.0F) {
         this.circleFlipSpeed = true;
      }
      if (this.circleSpeed < -25.0F) {
         this.circleFlipSpeed = false;
      }
      this.circleStep += 0.06F * deltaTime;
   }

   private void updateTargetAnimation(LivingEntity target) {
      float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
      Vec3d targetPos = this.getRenderPos(target);
      if (this.lastTarget != target || this.smoothedPos == null) {
         this.smoothedPos = targetPos;
         this.lastTarget = target;
         this.ghostTrailPoints.clear();
         this.ghostLastUpdateTime = System.currentTimeMillis();
      } else {
         float smoothing = Math.min(1.0F, tickDelta * 1.5F);
         this.smoothedPos = new Vec3d(
                 MathHelper.lerp(smoothing, this.smoothedPos.x, targetPos.x),
                 MathHelper.lerp(smoothing, this.smoothedPos.y, targetPos.y),
                 MathHelper.lerp(smoothing, this.smoothedPos.z, targetPos.z)
         );
      }

      this.hurtProgress = target.hurtTime > 0 ? target.hurtTime / 10.0F : Math.max(0.0F, this.hurtProgress - 0.1F);
   }

   private void updateTarget() {
      if (mc.player == null || mc.world == null) return;

      long currentTime = System.currentTimeMillis();
      boolean isInChat = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

      Iterator<Map.Entry<Entity, Long>> iterator = targetTimers.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<Entity, Long> entry = iterator.next();
         Entity entity = entry.getKey();
         if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (currentTime - entry.getValue() > LOOK_TIMEOUT ||
                    isEntityDead(livingEntity) ||
                    isEntityInvisible(livingEntity) ||
                    !this.isWithinTargetDistance(livingEntity)) {
               iterator.remove();
            }
         } else {
            iterator.remove();
         }
      }

      if (wasInChat && !isInChat) {
         targetTimers.remove(mc.player);
      }

      wasInChat = isInChat;

      if (isInChat) {
         targetTimers.clear();
         return;
      }

      Entity lookedAtEntity = getEntityFromCrosshair();
      if (lookedAtEntity != null && lookedAtEntity instanceof LivingEntity) {
         LivingEntity livingEntity = (LivingEntity) lookedAtEntity;
         if (isEntityDead(livingEntity) || isEntityInvisible(livingEntity)) {
            targetTimers.remove(livingEntity);
            return;
         }
         if (this.isWithinTargetDistance(livingEntity)) {
            targetTimers.put(livingEntity, currentTime);
         } else {
            targetTimers.remove(livingEntity);
         }
      }
   }

   private Entity getCurrentTarget() {
      if (mc.player == null || mc.world == null) return null;

      Entity currentTarget = null;
      long latestTime = 0;

      for (Map.Entry<Entity, Long> entry : targetTimers.entrySet()) {
         Entity entity = entry.getKey();
         if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (isEntityDead(livingEntity) || isEntityInvisible(livingEntity)) {
               continue;
            }
            if (!this.isWithinTargetDistance(livingEntity)) {
               continue;
            }

            if (entry.getValue() > latestTime) {
               latestTime = entry.getValue();
               currentTarget = entity;
            }
         }
      }

      if (currentTarget != null) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - latestTime > LOOK_TIMEOUT) {
            targetTimers.remove(currentTarget);
            return null;
         }
      }

      return currentTarget;
   }

   private Entity getEntityFromCrosshair() {
      if (mc.player == null || mc.world == null) return null;

      HitResult hitResult = mc.crosshairTarget;
      if (hitResult instanceof EntityHitResult entityHitResult) {
         Entity entity = entityHitResult.getEntity();
         if (entity instanceof LivingEntity && !entity.equals(mc.player) &&
                 (entity instanceof PlayerEntity || entity instanceof MobEntity || entity instanceof PassiveEntity)) {
            if (isEntityDead(entity) || isEntityInvisible(entity)) {
               return null;
            }
            if (this.isWithinTargetDistance(entity)) {
               return entity;
            }
         }
      }
      return null;
   }

   private boolean isEntityDead(Entity entity) {
      if (!(entity instanceof LivingEntity)) {
         return false;
      }

      LivingEntity livingEntity = (LivingEntity) entity;
      return livingEntity.isDead() || livingEntity.getHealth() <= 0;
   }

   private boolean isEntityInvisible(Entity entity) {
      if (!(entity instanceof LivingEntity)) {
         return false;
      }

      LivingEntity livingEntity = (LivingEntity) entity;
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

   private boolean isTargetRenderable(LivingEntity entity) {
      return !this.isEntityDead(entity) && !this.isEntityInvisible(entity) && this.isWithinTargetDistance(entity);
   }

   private boolean isWithinTargetDistance(Entity entity) {
      return mc.player != null && mc.player.getEntityPos().squaredDistanceTo(entity.getEntityPos()) <= MAX_DISTANCE * MAX_DISTANCE;
   }

   private ColorRGBA getTargetColor(Entity entity, int index) {
      if (colorCustom.isSelected()) {
         return this.applyHurtTint(this.colorTarget.getColor().mix(this.colorTargetSecond.getColor(), (index % 40) / 39.0F));
      }

      return this.applyHurtTint(Colors.getAccentColor(index * 6.0F));
   }

   private ColorRGBA applyHurtTint(ColorRGBA color) {
      return this.hurtProgress > 0.0F ? color.mix(ColorRGBA.RED, MathHelper.clamp(this.hurtProgress, 0.0F, 1.0F)) : color;
   }

   private void drawRhomb(MatrixStack ms, LivingEntity target) {
      ColorRGBA color = getTargetColor(target, 0).withAlpha(255.0F * this.animation.getValue() * this.opacity.getCurrentValue());
      ColorRGBA secondColor = getTargetColor(target, 20).withAlpha(255.0F * this.animation.getValue() * this.opacity.getCurrentValue());
      RenderSystem.setShaderTexture(0, RHOMB_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      RenderUtility.prepareMatrices(ms, this.smoothedPos != null ? this.smoothedPos : this.getRenderPos(target));
      ms.translate(0.0F, target.getHeight() / 2.0F, 0.0F);
      ms.multiply(mc.gameRenderer.getCamera().getRotation());
      float rotation = (float)Math.sin((System.currentTimeMillis() % 6283L) / 1000.0F) * 360.0F;
      ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
      float size = 0.9F;
      Matrix4f matrix = ms.peek().getPositionMatrix();

      buffer.vertex(matrix, -size / 2.0F, -size / 2.0F, 0.0F).texture(0.0F, 1.0F).color(secondColor.getRGB());
      buffer.vertex(matrix, size / 2.0F, -size / 2.0F, 0.0F).texture(1.0F, 1.0F).color(color.getRGB());
      buffer.vertex(matrix, size / 2.0F, size / 2.0F, 0.0F).texture(1.0F, 0.0F).color(secondColor.getRGB());
      buffer.vertex(matrix, -size / 2.0F, size / 2.0F, 0.0F).texture(0.0F, 0.0F).color(color.getRGB());

      RenderUtility.buildBuffer(buffer);
   }

   private void drawFeverGhost(MatrixStack ms, LivingEntity target) {
      RenderSystem.setShaderTexture(0, GHOST_GLOW_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      RenderUtility.prepareMatrices(ms, this.smoothedPos != null ? this.smoothedPos : this.getRenderPos(target));
      ms.translate(0.0F, target.getHeight() * 0.5F, 0.0F);
      if (this.ghostSouls.isSelected()) {
         this.drawSoulGhosts(ms, target, buffer);
      } else {
         this.drawTrailGhosts(ms, target, buffer);
      }

      RenderUtility.buildBuffer(buffer);
   }

   private void drawTrailGhosts(MatrixStack ms, LivingEntity target, BufferBuilder buffer) {
      if (this.smoothedPos == null) {
         return;
      }

      long elapsed = System.currentTimeMillis();
      Vec3d center = this.smoothedPos.add(0.0, target.getHeight() * 0.5F, 0.0);
      double radius = 0.33;
      double angle = (0.25F * (elapsed % 67000L)) / 45.0;
      double sin = Math.sin(angle) * radius;
      double cos = Math.cos(angle) * radius;

      Vec3d orb1 = center.add(sin, cos, -cos);
      Vec3d orb2 = center.add(-sin, sin, -cos);
      Vec3d orb3 = center.add(-sin, -sin, cos);
      if (this.ghostTrailPoints.isEmpty() || this.ghostTrailPoints.getLast().distanceTo(orb1) > 0.02) {
         this.ghostTrailPoints.add(new GhostTrail(orb1, 0));
         this.ghostTrailPoints.add(new GhostTrail(orb2, 1));
         this.ghostTrailPoints.add(new GhostTrail(orb3, 2));
      }

      while (this.ghostTrailPoints.size() > 160) {
         this.ghostTrailPoints.removeFirst();
      }

      int trailSize = this.ghostTrailPoints.size();
      if (trailSize <= 1) {
         return;
      }

      for (int i = 0; i < trailSize; i++) {
         GhostTrail trail = this.ghostTrailPoints.get(i);
         float progress = (float)i / (float)(trailSize - 1);
         if (progress <= 0.0F) {
            continue;
         }

         Vec3d local = trail.position.subtract(center);
         float size = 0.72F * progress;
         ColorRGBA color = this.getGhostColor(trail.colorIndex, progress).withAlpha(progress * 235.0F * this.animation.getValue() * this.opacity.getCurrentValue());
         this.drawGhostBillboard(ms, buffer, local, size, elapsed * 0.08F + i * 7.0F, color);
      }
   }

   private void drawSoulGhosts(MatrixStack ms, LivingEntity target, BufferBuilder buffer) {
      long now = System.currentTimeMillis();
      this.soulsRotation += (1.65F * (now - this.ghostLastUpdateTime)) / 600.0F;
      this.ghostLastUpdateTime = now;

      int particles = 12;
      for (int layer = 0; layer < 9; layer += 3) {
         float layerProgress = layer / 6.0F;
         float layerY = -0.45F + layerProgress * 0.82F;
         for (int point = 0; point < particles; point++) {
            float gradient = (float)point / (float)(particles - 1);
            float phase = this.soulsRotation + point * 0.1F;
            float radius = 0.72F;
            int layerOffset = layer * layer;
            Vec3d local = new Vec3d(
                    radius * MathHelper.sin(phase + layerOffset),
                    layerY + 0.16F * MathHelper.sin(this.soulsRotation * 0.75F + point * 0.17F),
                    radius * MathHelper.cos(phase - layerOffset)
            );

            ColorRGBA color = this.getTargetColor(target, (int)(gradient * 40.0F));
            float animAlpha = this.animation.getValue() * this.opacity.getCurrentValue();
            this.drawGhostBillboard(ms, buffer, local, (0.12F + point / 155.0F) * 8.9F * animAlpha, now * 0.018F + point * 8.0F, color.withAlpha(70.0F * animAlpha));
            this.drawGhostBillboard(ms, buffer, local, (0.08F + point / 170.0F) * 6.7F * animAlpha, now * 0.026F + point * 12.0F, color.withAlpha(235.0F * animAlpha));
         }
      }
   }

   private ColorRGBA getGhostColor(int colorIndex, float progress) {
      ColorRGBA first = this.getTargetColor(this.prevTarget, colorIndex * 12);
      ColorRGBA second = this.getTargetColor(this.prevTarget, colorIndex * 12 + 20);
      return first.mix(second, progress);
   }

   private void drawGhostBillboard(MatrixStack ms, BufferBuilder buffer, Vec3d local, float size, float rotation, ColorRGBA color) {
      ms.push();
      ms.translate(local.x, local.y, local.z);
      ms.multiply(mc.gameRenderer.getCamera().getRotation());
      ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
      float half = size * 0.5F;
      Matrix4f matrix = ms.peek().getPositionMatrix();

      buffer.vertex(matrix, -half, -half, 0.0F).texture(0.0F, 1.0F).color(color.getRGB());
      buffer.vertex(matrix, half, -half, 0.0F).texture(1.0F, 1.0F).color(color.getRGB());
      buffer.vertex(matrix, half, half, 0.0F).texture(1.0F, 0.0F).color(color.getRGB());
      buffer.vertex(matrix, -half, half, 0.0F).texture(0.0F, 0.0F).color(color.getRGB());

      ms.pop();
   }

   private void drawCircle(MatrixStack ms, LivingEntity target) {
      Vec3d renderPos = this.smoothedPos != null ? this.smoothedPos : this.getRenderPos(target);
      float alpha = this.animation.getValue() * this.opacity.getCurrentValue();
      float hitEffect = Math.min(this.hurtProgress * 2.0F, 1.0F);
      float width = target.getWidth() * (1.0F + MathHelper.sin(hitEffect * (float)Math.PI) * 0.18F);
      float height = target.getHeight();
      int size = CIRCLE_SEGMENTS;

      double y = this.smoothSinAnimation(this.circleStep) * height;
      double y2 = this.smoothSinAnimation(this.circleStep - 0.35F) * height;
      this.smoothCircleY = this.lerp(this.smoothCircleY, y, 0.12);
      this.smoothCircleY2 = this.lerp(this.smoothCircleY2, y2, 0.10);

      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder quads = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      Matrix4f matrix = ms.peek().getPositionMatrix();
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      ColorRGBA firstColor = this.getTargetColor(target, 0);
      ColorRGBA secondColor = this.getTargetColor(target, 20);

      for (int i = 0; i < size; i++) {
         float t = (float)i / (float)size;
         float tNext = (float)((i + 1) % size) / (float)size;
         float gradient = 0.5F - 0.5F * MathHelper.cos(t * (float)Math.PI * 2.0F);
         float nextGradient = 0.5F - 0.5F * MathHelper.cos(tNext * (float)Math.PI * 2.0F);

         ColorRGBA current = firstColor.mix(secondColor, gradient);
         ColorRGBA next = firstColor.mix(secondColor, nextGradient);
         Vec3d cs = this.circlePoint(i, width);
         Vec3d ncs = this.circlePoint((i + 1) % size, width);
         Vec3d circlePoint = renderPos.add(cs.x, this.smoothCircleY, cs.z);
         Vec3d trailPoint = renderPos.add(cs.x, this.smoothCircleY2, cs.z);
         Vec3d nextCirclePoint = renderPos.add(ncs.x, this.smoothCircleY, ncs.z);
         Vec3d nextTrailPoint = renderPos.add(ncs.x, this.smoothCircleY2, ncs.z);

         this.drawCircleQuad(quads, matrix, cameraPos, circlePoint, nextCirclePoint, nextTrailPoint, trailPoint,
                 current.withAlpha(204.0F * alpha), next.withAlpha(204.0F * alpha), next.withAlpha(0.0F), current.withAlpha(0.0F));
         this.drawCircleQuad(quads, matrix, cameraPos, trailPoint, nextTrailPoint, nextCirclePoint, circlePoint,
                 current.withAlpha(0.0F), next.withAlpha(0.0F), next.withAlpha(204.0F * alpha), current.withAlpha(204.0F * alpha));
      }
      RenderUtility.buildBuffer(quads);

      BufferBuilder lines = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      for (int i = 0; i < size; i++) {
         float t = (float)i / (float)size;
         float tNext = (float)((i + 1) % size) / (float)size;
         ColorRGBA current = firstColor.mix(secondColor, 0.5F - 0.5F * MathHelper.cos(t * (float)Math.PI * 2.0F));
         ColorRGBA next = firstColor.mix(secondColor, 0.5F - 0.5F * MathHelper.cos(tNext * (float)Math.PI * 2.0F));
         Vec3d cs = this.circlePoint(i, width);
         Vec3d ncs = this.circlePoint((i + 1) % size, width);
         Vec3d circlePoint = renderPos.add(cs.x, this.smoothCircleY, cs.z);
         Vec3d trailPoint = renderPos.add(cs.x, this.smoothCircleY2, cs.z);
         Vec3d nextCirclePoint = renderPos.add(ncs.x, this.smoothCircleY, ncs.z);

         this.drawCircleLine(lines, matrix, cameraPos, circlePoint, trailPoint, current.withAlpha(38.0F * alpha), current.withAlpha(0.0F));
         this.drawCircleLine(lines, matrix, cameraPos, circlePoint, nextCirclePoint, current.withAlpha(255.0F * alpha), next.withAlpha(255.0F * alpha));
      }
      RenderUtility.buildBuffer(lines);
   }

   private Vec3d circlePoint(int index, float radius) {
      Vec3d point = CIRCLE_POINTS[index];
      return new Vec3d(point.x * radius, 0.0, point.z * radius);
   }

   private static Vec3d[] createCirclePoints() {
      Vec3d[] points = new Vec3d[CIRCLE_SEGMENTS];
      for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
         double angle = (double)i / (double)CIRCLE_SEGMENTS * Math.PI * 2.0;
         points[i] = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
      }
      return points;
   }

   private double smoothSinAnimation(double input) {
      double sin = (Math.sin(input) + 1.0) / 2.0;
      return -(Math.cos(Math.PI * sin) - 1.0) / 2.0;
   }

   private double lerp(double from, double to, double delta) {
      return from + (to - from) * delta;
   }

   private void drawCircleQuad(BufferBuilder buffer, Matrix4f matrix, Vec3d cam, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4,
                               ColorRGBA c1, ColorRGBA c2, ColorRGBA c3, ColorRGBA c4) {
      buffer.vertex(matrix, (float)(p1.x - cam.x), (float)(p1.y - cam.y), (float)(p1.z - cam.z)).color(c1.getRGB());
      buffer.vertex(matrix, (float)(p2.x - cam.x), (float)(p2.y - cam.y), (float)(p2.z - cam.z)).color(c2.getRGB());
      buffer.vertex(matrix, (float)(p3.x - cam.x), (float)(p3.y - cam.y), (float)(p3.z - cam.z)).color(c3.getRGB());
      buffer.vertex(matrix, (float)(p4.x - cam.x), (float)(p4.y - cam.y), (float)(p4.z - cam.z)).color(c4.getRGB());
   }

   private void drawCircleLine(BufferBuilder buffer, Matrix4f matrix, Vec3d cam, Vec3d p1, Vec3d p2, ColorRGBA c1, ColorRGBA c2) {
      buffer.vertex(matrix, (float)(p1.x - cam.x), (float)(p1.y - cam.y), (float)(p1.z - cam.z)).color(c1.getRGB());
      buffer.vertex(matrix, (float)(p2.x - cam.x), (float)(p2.y - cam.y), (float)(p2.z - cam.z)).color(c2.getRGB());
   }

   private void drawCrystals(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getCameraPos();
      float width = this.prevTarget.getWidth() * 1.5F;
      RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));
      BufferBuilder builder = CrystalRenderer.createBuffer();

      for (int i = 0; i < 360; i += 20) {
         float val = 1.2F - 0.5F * this.animation.getValue();
         float sin = (float)(MathUtility.sin((float)Math.toRadians(i + this.moving.getValue() * 0.3F)) * width * val);
         float cos = (float)(MathUtility.cos((float)Math.toRadians(i + this.moving.getValue() * 0.3F)) * width * val);
         float size = 0.1F;
         ms.push();
         ms.translate(sin, 0.1F + target.getHeight() * Math.abs(MathUtility.sin(i)), cos);
         Vec3d crystalPos = this.getRenderPos(this.prevTarget).add(sin, 1.0, cos);
         Vec3d targetPos = target.getEntityPos().add(0.0, target.getHeight() / 2.0, 0.0);
         Vector3f directionToTarget = new Vector3f(
                 (float)(targetPos.x - crystalPos.x), (float)(targetPos.y - crystalPos.y), (float)(targetPos.z - crystalPos.z)
         )
                 .normalize();
         Vector3f initialDirection = new Vector3f(0.0F, 1.0F, 0.0F);
         Quaternionf rotation = new Quaternionf().rotationTo(initialDirection, directionToTarget);
         ms.multiply(rotation);
         ColorRGBA color = getTargetColor(target, i);
         CrystalRenderer.render(ms, builder, 0.0F, 0.0F, 0.0F, size, color.withAlpha(255.0F * this.animation.getValue() * this.opacity.getCurrentValue()));
         ms.pop();
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      float bigSize = 1.0F;

      for (int i = 0; i < 360; i += 20) {
         float val = 1.2F - 0.5F * this.animation.getValue();
         float sin = (float)(MathUtility.sin((float)Math.toRadians(i + this.moving.getValue() * 0.3F)) * width * val);
         float cos = (float)(MathUtility.cos((float)Math.toRadians(i + this.moving.getValue() * 0.3F)) * width * val);
         float size = 0.1F;
         ColorRGBA color = getTargetColor(target, i);
         ms.push();
         ms.translate(sin, 0.1F + target.getHeight() * Math.abs(MathUtility.sin(i)), cos);
         ms.multiply(camera.getRotation());
         DrawUtility.drawImage(
                 ms,
                 buffer,
                 (double)(-bigSize / 2.0F),
                 (double)(-bigSize / 2.0F),
                 0.0,
                 (double)bigSize,
                 (double)bigSize,
                 color.withAlpha(255.0F * this.animation.getValue() * 0.2F * this.opacity.getCurrentValue())
         );
         ms.pop();
      }

      RenderUtility.buildBuffer(buffer);
   }

   private void drawGhosts(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      Identifier id = BLOOM_TEXTURE;
      float width = this.prevTarget.getWidth() * 1.5F;
      RenderSystem.setShaderTexture(0, id);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));
      int step = 2;
      int wormTick = 0;
      int wormCD = 0;
      int wormCount = 0;

      for (int i = 0; i < 360; i += step) {
         float size = 0.13F + 0.005F * wormTick;
         float bigSize = 0.7F + 0.005F * wormTick;
         if (wormCD > 0) {
            wormCD -= step;
         } else {
            wormTick += step;
            if (wormTick > 50) {
               wormCD = 100;
               wormTick = 0;
               wormCount++;
            } else {
               float val = Math.max(0.5F, 1.2F - 0.5F * this.animation.getValue());
               float sin = (float)(MathUtility.sin((float)Math.toRadians(i + this.moving.getValue() * 1.0F)) * width * val);
               float cos = (float)(MathUtility.cos((float)Math.toRadians(i + this.moving.getValue() * 1.0F)) * width * val);
               ms.push();
               ms.translate(
                       sin,
                       this.prevTarget.getHeight() / 1.5F
                               + this.prevTarget.getHeight() / 3.0F * MathUtility.sin(Math.toRadians(i / 2.0F + this.moving.getValue() / 5.0F)),
                       cos
               );
               ms.multiply(camera.getRotation());
               ColorRGBA color = getTargetColor(target, i);
               DrawUtility.drawImage(
                       ms,
                       builder,
                       (double)(-bigSize / 2.0F),
                       (double)(-bigSize / 2.0F),
                       (double)(-size / 2.0F),
                       (double)bigSize,
                       (double)bigSize,
                       color.withAlpha(color.getAlpha() * this.animation.getValue() * 0.05F * this.opacity.getCurrentValue())
               );
               DrawUtility.drawImage(
                       ms,
                       builder,
                       (double)(-size / 2.0F),
                       (double)(-size / 2.0F),
                       (double)(-size / 2.0F),
                       (double)size,
                       (double)size,
                       color.withAlpha(color.getAlpha() * this.animation.getValue() * this.opacity.getCurrentValue())
               );
               ms.pop();
            }
         }
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   private void drawChain(MatrixStack ms, LivingEntity target) {
      ColorRGBA color = getTargetColor(target, 0);
      ColorRGBA secondColor = getTargetColor(target, 20);
      RenderSystem.setShaderTexture(0, CHAIN_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      RenderUtility.prepareMatrices(ms, this.getRenderPos(target));

      float animValue = (System.currentTimeMillis() % 360000L) / 1000.0F * 60.0F;
      float gradusX = 20.0F * Math.min(1.0F + MathHelper.sin((float)Math.toRadians(animValue)), 1.0F);
      float gradusZ = 20.0F * (Math.min(1.0F + MathHelper.sin((float)Math.toRadians(animValue)), 2.0F) - 1.0F);
      float width = target.getWidth() * 3.0F;
      int linksStep = 18;
      int totalAngle = 720;
      float chainSize = 8.0F;
      float down = 1.5F;
      float chainScale = 0.5F;
      float alpha = this.animation.getValue() * this.opacity.getCurrentValue();
      ColorRGBA top = color.withAlpha(128.0F * alpha);
      ColorRGBA bottom = secondColor.withAlpha(128.0F * alpha);
      float rotationValue = (System.currentTimeMillis() % 720000L) / 1000.0F * 30.0F;

      for (int chainIndex = 0; chainIndex < 2; chainIndex++) {
         float val = 1.2F - 0.5F * (chainIndex == 0 ? 1.0F : 0.9F);
         ms.push();
         ms.translate(0.0F, target.getHeight() / 2.0F, 0.0F);
         ms.scale(chainScale, chainScale, chainScale);
         ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(chainIndex == 0 ? gradusX : -gradusX));
         ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(chainIndex == 0 ? gradusZ : -gradusZ));

         Matrix4f matrix = ms.peek().getPositionMatrix();
         int step = linksStep / 2;
         for (int i = 0; i < totalAngle; i += step) {
            float offsetX = (chainIndex == 0 ? gradusX : -gradusX) / 100.0F;
            float offsetZ = (chainIndex == 0 ? -gradusZ : gradusZ) / 100.0F;
            float prevSin = offsetX + MathHelper.sin((float)Math.toRadians(i - step + rotationValue)) * width * val;
            float prevCos = offsetZ + MathHelper.cos((float)Math.toRadians(i - step + rotationValue)) * width * val;
            float sin = offsetX + MathHelper.sin((float)Math.toRadians(i + rotationValue)) * width * val;
            float cos = offsetZ + MathHelper.cos((float)Math.toRadians(i + rotationValue)) * width * val;
            float u0 = (i - step) / 360.0F * chainSize;
            float u1 = i / 360.0F * chainSize;

            buffer.vertex(matrix, prevSin, -0.5F, prevCos).texture(u0, 0.0F).color(top.getRGB());
            buffer.vertex(matrix, sin, -0.5F, cos).texture(u1, 0.0F).color(top.getRGB());
            buffer.vertex(matrix, sin, -0.5F + down, cos).texture(u1, 0.99F).color(bottom.getRGB());
            buffer.vertex(matrix, prevSin, -0.5F + down, prevCos).texture(u0, 0.99F).color(bottom.getRGB());
         }
         ms.pop();
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   private Vec3d getRenderPos(LivingEntity target) {
      float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
      return new Vec3d(
              MathHelper.lerp(tickDelta, target.lastX, target.getX()),
              MathHelper.lerp(tickDelta, target.lastY, target.getY()),
              MathHelper.lerp(tickDelta, target.lastZ, target.getZ())
      );
   }

   public static Entity getTarget() {
      TargetESP instance = FeverVisual.getInstance().getModuleManager().getModule(TargetESP.class);
      if (instance == null) return null;

      Entity target = instance.currentTarget;

      if (target != null && (instance.isEntityDead(target) || instance.isEntityInvisible(target))) {
         return null;
      }
      if (target != null && instance.mc.player != null) {
         double distance = instance.mc.player.getEntityPos().distanceTo(target.getEntityPos());
         if (distance > MAX_DISTANCE) {
            return null;
         }
      }

      return target;
   }

   private static class GhostTrail {
      private final Vec3d position;
      private final int colorIndex;

      private GhostTrail(Vec3d position, int colorIndex) {
         this.position = position;
         this.colorIndex = colorIndex;
      }

      private double distanceTo(Vec3d other) {
         return this.position.distanceTo(other);
      }
   }
}
