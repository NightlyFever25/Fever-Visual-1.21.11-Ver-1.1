package fever.visual.ui.hud.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.framework.objects.gradient.Gradient;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.EntityUtility;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.ScissorUtility;
import fever.visual.utility.time.Timer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TargetHud extends HudElement {
   private static final long LOOK_TIMEOUT = 3000L;
   private static final double MAX_DISTANCE = 6.0;
   private static final float PANEL_WIDTH = 120.0F;
   private static final float PANEL_HEIGHT = 45.0F;
   private static final float HEAD_SIZE = 39.0F;
   private static final float HEAD_PADDING = 3.0F;
   private static final int HEALTH_PARTICLE_LIFETIME = 85;
   private static final int MAX_HEALTH_PARTICLES = 67;
   private static final int HEALTH_PARTICLES_PER_HIT = 32;
   private static final Identifier HEALTH_PARTICLE_TEXTURE = FeverVisual.id("textures/bloom.png");
   private static final Identifier COPY_ICON = FeverVisual.id("icons/hud/copy.png");
   private static final Identifier CHECK_ICON = FeverVisual.id("icons/check.png");
   private final BooleanSetting transparentBackground = new BooleanSetting(this, "hud.targethud.transparent_background");

   private final Animation content = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation health = new Animation(300L, 0.0F, Easing.BAKEK);
   private final Animation absorption = new Animation(300L, 0.0F, Easing.BAKEK);
   private final Animation healthNumber = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation copy = new Animation(300L, 0.0F, Easing.BAKEK);
   private final Animation success = new Animation(500L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation eatingPulse = new Animation(150L, 0.0F, Easing.BAKEK);
   private final Animation hitPulse = new Animation(220L, 0.0F, Easing.BAKEK);

   private final Timer copyTimer = new Timer();
   private final Map<Entity, Long> targetTimers = new HashMap<>();
   private final List<HealthParticle> healthParticles = new ArrayList<>();
   private final Random particleRandom = new Random();
   private LivingEntity target;
   private LivingEntity currentTarget;
   private boolean copied;
   private boolean wasInChat;
   private int lastTargetId = -1;
   private int lastParticleHurtTime = 0;

   public TargetHud() {
      super("hud.targethud", "icons/hud/target.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = PANEL_WIDTH;
      this.height = PANEL_HEIGHT;
      this.updateTarget();
      this.currentTarget = this.getTarget();
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      LivingEntity newTarget = this.currentTarget;
      if (newTarget != null) {
         this.target = newTarget;
      }

      if (this.target == null || this.animation.getValue() == 0.0F) {
         return;
      }

      this.content.update(this.animation.getValue() * this.visible.getValue() >= 1.0F);
      float contentValue = this.content.getValue();
      float alpha = 255.0F * contentValue;
      if (alpha <= 0.0F) {
         return;
      }

      boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      ColorRGBA bgColor = Colors.getBackgroundColor()
              .withAlpha(255.0F * (dark ? 0.8F - 0.6F * Interface.glass() : 0.7F));
      if (this.transparentBackground.isEnabled()) {
         bgColor = bgColor.withAlpha(bgColor.getAlpha() * 0.35F);
      }

      float healthValue = this.getHealth(this.target);
      float maxHealth = Math.max(1.0F, this.target.getMaxHealth());
      float healthPercent = Math.clamp(healthValue / maxHealth, 0.0F, 1.0F);
      float absorptionPercent = Math.clamp(this.target.getAbsorptionAmount() / maxHealth, 0.0F, 1.0F);
      this.health.update(healthPercent);
      this.absorption.update(absorptionPercent);
      this.healthNumber.update(healthValue);

      boolean hoverName = GuiUtility.isHovered(this.x + HEAD_PADDING + HEAD_SIZE + 6.0F, this.y + 7.0F, 62.0, 9.0, context);
      if (!hoverName || this.copyTimer.finished(1000L)) {
         this.copied = false;
      }

      boolean isEating = this.target.isUsingItem() && this.target.getActiveItem().contains(DataComponentTypes.FOOD);
      this.eatingPulse.update(isEating);
      this.hitPulse.update(this.target.hurtTime > 0);
      this.copy.update(hoverName);
      this.success.update(this.copied);

      this.drawPanel(context, bgColor, alpha);
      this.drawTargetContent(context, alpha, healthValue, healthPercent);
   }

   private void drawPanel(UIContext context, ColorRGBA bgColor, float alpha) {
      context.drawShadow(
              this.x - 5.0F,
              this.y - 5.0F,
              this.width + 10.0F,
              this.height + 10.0F,
              15.0F,
              BorderRadius.all(8.0F),
              ColorRGBA.BLACK.withAlpha(63.75F * this.dragAnim.getValue())
      );

      if (Interface.showMinimalizm()) {
         context.drawBlurredRect(
                 this.x,
                 this.y,
                 this.width,
                 this.height,
                 45.0F,
                 8.0F,
                 BorderRadius.all(8.0F),
                 ColorRGBA.WHITE.withAlpha(alpha * Interface.minimalizm())
         );
      }

      if (Interface.showGlass()) {
         context.drawLiquidGlass(
                 this.x,
                 this.y,
                 this.width,
                 this.height,
                 8.0F,
                 0.08F - 0.07F * this.dragAnim.getValue(),
                 BorderRadius.all(8.0F),
                 ColorRGBA.WHITE.withAlpha(alpha * Interface.glass())
         );
      }

      context.drawSquircle(
              this.x,
              this.y,
              this.width,
              this.height,
              8.0F,
              BorderRadius.all(8.0F),
              bgColor.withAlpha(bgColor.getAlpha() * this.content.getValue())
      );
   }

   private void drawTargetContent(UIContext context, float alpha, float healthValue, float healthPercent) {
      Font nameFont = Fonts.MEDIUM.getFont(8.0F);
      Font hpFont = Fonts.MEDIUM.getFont(7.0F);
      float headX = this.x + HEAD_PADDING;
      float headY = this.y + HEAD_PADDING;
      float textX = this.x + HEAD_PADDING + HEAD_SIZE + 6.0F;

      context.drawHead(
              this.target,
              headX,
              headY,
              HEAD_SIZE,
              BorderRadius.all(6.0F),
              this.getHeadColor(alpha)
      );

      String name = this.target.getName().getString();
      float nameWidth = this.width - HEAD_SIZE - 18.0F;
      context.drawFadeoutText(
              nameFont,
              name,
              textX + 8.0F * this.copy.getValue(),
              this.y + 7.0F,
              Colors.getTextColor().withAlpha(alpha),
              0.7F,
              1.0F,
              nameWidth - 8.0F * this.copy.getValue()
      );

      this.drawCopyIcons(context, alpha, textX);

      context.drawText(
              hpFont,
              "HP: " + this.formatHealth(this.healthNumber.getValue()),
              textX,
              this.y + 22.0F,
              Colors.getTextColor().withAlpha(alpha * 0.9F)
      );

      float barX = textX;
      float barY = this.y + 33.0F;
      float barWidth = this.width - HEAD_PADDING - HEAD_SIZE - 16.0F;
      float barHeight = 7.0F;
      float barRadius = barHeight / 2.0F;
      ColorRGBA firstHealthBarColor = Colors.getAccentColor(0.0F).withAlpha(alpha);
      ColorRGBA secondHealthBarColor = Colors.getAccentColor(90.0F).withAlpha(alpha);
      float animatedHealth = Math.clamp(this.health.getValue(), 0.0F, 1.0F);
      float barEndX = barX + barWidth * animatedHealth;
      this.updateHealthParticles(barEndX - 5.0F, this.y + 30.0F);

      context.drawRoundedRect(
              barX,
              barY,
              barWidth,
              barHeight,
              BorderRadius.all(2.0F),
              Colors.getAdditionalColor().withAlpha(alpha * (1.0F - 0.7F * Interface.glass()))
      );

      float hitGlow = this.hitPulse.getValue();
      if (hitGlow > 0.0F) {
         context.drawRoundedRect(
                 barX,
                 barY,
                 barWidth * animatedHealth,
                 barHeight,
                 BorderRadius.all(3.0F),
                 Colors.RED.withAlpha(alpha * 0.24F * hitGlow)
         );
      }

      context.drawRoundedRect(
              barX,
              barY,
              barWidth * animatedHealth,
              barHeight,
              BorderRadius.all(2.0F),
              Gradient.of(firstHealthBarColor, firstHealthBarColor, secondHealthBarColor, secondHealthBarColor)
      );

      float absorptionWidth = barWidth * Math.clamp(this.absorption.getValue(), 0.0F, 1.0F);
      if (absorptionWidth > 0.2F) {
         context.drawRoundedRect(
                 barX + barWidth - absorptionWidth,
                 barY,
                 absorptionWidth,
                 barHeight,
                 BorderRadius.all(2.0F),
                 new ColorRGBA(255, 220, 81, (int) alpha)
         );
      }

      this.drawHealthParticles(context, alpha);
   }

   private void drawCopyIcons(UIContext context, float alpha, float textX) {
      RenderUtility.rotate(
              context.getMatrices(),
              textX - 2.0F + 5.0F * this.copy.getValue(),
              this.y + 10.0F,
              90.0F * this.success.getValue()
      );
      context.drawTexture(
              COPY_ICON,
              textX - 5.0F + 5.0F * this.copy.getValue(),
              this.y + 7.0F,
              6.0F,
              6.0F,
              Colors.getTextColor().withAlpha(alpha * this.copy.getValue() * (1.0F - this.success.getValue()))
      );
      RenderUtility.end(context.getMatrices());

      RenderUtility.rotate(
              context.getMatrices(),
              textX - 2.0F + 5.0F * this.copy.getValue(),
              this.y + 10.0F,
              -90.0F + 90.0F * this.success.getValue()
      );
      context.drawTexture(
              CHECK_ICON,
              textX - 5.0F + 5.0F * this.copy.getValue(),
              this.y + 7.0F,
              6.0F,
              6.0F,
              Colors.GREEN.withAlpha(alpha * this.copy.getValue() * this.success.getValue())
      );
      RenderUtility.end(context.getMatrices());
   }

   private void updateHealthParticles(float x, float y) {
      int targetId = this.target.getId();
      if (this.lastTargetId != targetId) {
         this.lastTargetId = targetId;
         this.lastParticleHurtTime = 0;
         this.healthParticles.clear();
         return;
      }

      if (this.target.hurtTime <= 0) {
         this.lastParticleHurtTime = 0;
         return;
      }

      if (this.target.hurtTime <= this.lastParticleHurtTime) {
         this.lastParticleHurtTime = this.target.hurtTime;
         return;
      }

      this.lastParticleHurtTime = this.target.hurtTime;
      float hurtPercent = Math.clamp(this.target.hurtTime / 10.0F, 0.0F, 1.0F);
      int count = Math.max(4, Math.round(HEALTH_PARTICLES_PER_HIT * hurtPercent));
      if (count > 0) {
         for (int i = 0; i < count; i++) {
            if (this.healthParticles.size() >= MAX_HEALTH_PARTICLES) {
               this.healthParticles.remove(this.healthParticles.size() - 1);
            }
            float vx = -0.4F + this.particleRandom.nextFloat() * 1.2F;
            float vy = -1.0F + this.particleRandom.nextFloat() * 0.8F;
            this.healthParticles.add(0, new HealthParticle(x, y, vx, vy, 0));
         }
      }
   }

   private void drawHealthParticles(UIContext context, float alpha) {
      for (int i = this.healthParticles.size() - 1; i >= 0; i--) {
         if (this.healthParticles.get(i).time > HEALTH_PARTICLE_LIFETIME) {
            this.healthParticles.remove(i);
         }
      }

      if (this.healthParticles.isEmpty()) {
         return;
      }

      ColorRGBA particleColor = Colors.getAccentColor(90.0F);
      for (HealthParticle particle : this.healthParticles) {
         float factor = particle.time / 2.0F;
         float particleAlpha = 255.0F / (factor + 1.0F);
         if (particleAlpha <= 2.0F) {
            particle.update();
            continue;
         }
         particle.update();
         context.drawTexture(
                 HEALTH_PARTICLE_TEXTURE,
                 particle.x,
                 particle.y,
                 16.0F,
                 16.0F,
                 particleColor.withAlpha(Math.min(alpha, particleAlpha))
         );
      }
   }

   private ColorRGBA getHeadColor(float alpha) {
      float hurt = Math.max(Math.clamp(this.target.hurtTime / 10.0F, 0.0F, 1.0F), this.hitPulse.getValue());
      float eating = this.eatingPulse.getValue();
      return new ColorRGBA(
              255,
              (int) (255.0F - 155.0F * hurt + 20.0F * eating),
              (int) (255.0F - 155.0F * hurt + 20.0F * eating),
              (int) alpha
      );
   }

   private float getHealth(LivingEntity entity) {
      return entity instanceof PlayerEntity player ? EntityUtility.getHealth(player) : entity.getHealth();
   }

   private String formatHealth(float value) {
      if (value >= 999.0F) {
         return "?";
      }

      float rounded = Math.round(value * 10.0F) / 10.0F;
      if (rounded == (int) rounded) {
         return String.valueOf((int) rounded);
      }

      return TextUtility.formatNumber(rounded).replace(",", ".");
   }

   private void updateTarget() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      long currentTime = System.currentTimeMillis();
      boolean isInChat = mc.currentScreen instanceof ChatScreen;

      Iterator<Map.Entry<Entity, Long>> iterator = this.targetTimers.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<Entity, Long> entry = iterator.next();
         Entity entity = entry.getKey();
         if (entity instanceof LivingEntity livingEntity) {
            if (currentTime - entry.getValue() > LOOK_TIMEOUT
                    || this.isEntityDead(livingEntity)
                    || this.isEntityInvisible(livingEntity)) {
               iterator.remove();
            }
         } else {
            iterator.remove();
         }
      }

      if (this.wasInChat && !isInChat) {
         this.targetTimers.remove(mc.player);
      }

      this.wasInChat = isInChat;

      if (isInChat) {
         this.targetTimers.put(mc.player, currentTime);
         return;
      }

      Entity lookedAtEntity = this.getEntityFromCrosshair();
      if (lookedAtEntity instanceof LivingEntity livingEntity) {
         if (this.isEntityDead(livingEntity) || this.isEntityInvisible(livingEntity)) {
            this.targetTimers.remove(livingEntity);
            return;
         }

         double distance = mc.player.getEntityPos().distanceTo(livingEntity.getEntityPos());
         if (distance <= MAX_DISTANCE) {
            this.targetTimers.put(livingEntity, currentTime);
         } else {
            this.targetTimers.remove(livingEntity);
         }
      }
   }

   private LivingEntity getCurrentTarget() {
      if (mc.player == null || mc.world == null) {
         return null;
      }

      if (mc.currentScreen instanceof ChatScreen) {
         return mc.player;
      }

      LivingEntity currentTarget = null;
      long latestTime = 0L;

      for (Map.Entry<Entity, Long> entry : this.targetTimers.entrySet()) {
         Entity entity = entry.getKey();
         if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity.equals(mc.player)
                    || this.isEntityDead(livingEntity)
                    || this.isEntityInvisible(livingEntity)) {
               continue;
            }

            if (entry.getValue() > latestTime) {
               latestTime = entry.getValue();
               currentTarget = livingEntity;
            }
         }
      }

      if (currentTarget != null && System.currentTimeMillis() - latestTime > LOOK_TIMEOUT) {
         this.targetTimers.remove(currentTarget);
         return null;
      }

      return currentTarget;
   }

   private Entity getEntityFromCrosshair() {
      if (mc.player == null || mc.world == null) {
         return null;
      }

      HitResult hitResult = mc.crosshairTarget;
      if (hitResult instanceof EntityHitResult entityHitResult) {
         Entity entity = entityHitResult.getEntity();
         if (entity instanceof LivingEntity && !entity.equals(mc.player)) {
            return entity;
         }
      }

      return null;
   }

   private boolean isEntityDead(LivingEntity entity) {
      return entity.isDead() || entity.getHealth() <= 0.0F;
   }

   private boolean isEntityInvisible(LivingEntity entity) {
      return entity.hasStatusEffect(StatusEffects.INVISIBILITY)
              || entity.isInvisible()
              || entity.isInvisibleTo(mc.player);
   }

   private LivingEntity getTarget() {
      LivingEntity currentTarget = this.getCurrentTarget();
      if (currentTarget != null) {
         return currentTarget;
      }

      if (mc.currentScreen instanceof ChatScreen) {
         return mc.player;
      }

      return null;
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (GuiUtility.isHovered(this.x + HEAD_PADDING + HEAD_SIZE + 6.0F, this.y + 7.0F, 62.0, 9.0, mouseX, mouseY)) {
         LivingEntity activeTarget = this.getTarget();
         TextUtility.copyText(activeTarget == null ? mc.player.getName().getString() : activeTarget.getName().getString());
         this.copyTimer.reset();
         this.copied = true;
      } else {
         super.onMouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean show() {
      return this.currentTarget != null;
   }

   private static class HealthParticle {
      private float x;
      private float y;
      private float vx;
      private float vy;
      private int time;

      private HealthParticle(float x, float y, float vx, float vy, int time) {
         this.x = x;
         this.y = y;
         this.vx = vx;
         this.vy = vy;
         this.time = time;
      }

      private void update() {
         this.vx /= 1.01F;
         this.vy /= 1.01F;
         this.x += this.vx;
         this.y += this.vy;
         this.time++;
      }
   }
}
