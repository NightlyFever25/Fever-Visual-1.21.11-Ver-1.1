package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;

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
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.math.MathUtility;
import fever.visual.utility.render.Draw3DUtility;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.Utils;
import fever.visual.utility.time.Timer;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.*;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "World", category = ModuleCategory.VISUALS, desc = "Визуальные дополнения мира")
public class World extends BaseModule {
   private static final Identifier BLOOM_TEXTURE = FeverVisual.id("textures/bloom.png");
   private static final Box PARTICLE_BOX = new Box(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);

   private final List<Particle> particles = new ArrayList<>();
   private final Quaternionf renderRotation = new Quaternionf();
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.world.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.world.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.world.color_mode.custom");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.world.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(151, 71, 255, 255))
           .alpha(true);
   private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.world.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(100, 160, 255, 255))
           .alpha(true);
   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (this.particles.isEmpty()) {
         return;
      }

      MatrixStack ms = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getCameraPos();
      long now = System.currentTimeMillis();
      int particleIndex = 0;
      for (Particle particle : this.particles) {
         particle.alpha.update(!particle.timer.finished(particle.liveTicks));
         particle.renderPos = Utils.getInterpolatedPos(particle.prev, particle.pos, event.getTickDelta());
         particle.renderVisible = Utils.isInViewHemisphere(
                 camera, particle.renderPos.x, particle.renderPos.y, particle.renderPos.z, 2.0
         );
         if (particle.renderVisible) {
            particle.renderRot = Utils.getInterpolatedPos(particle.prevRot, particle.rotate, event.getTickDelta());
            particle.renderColor = this.getWorldColor(particleIndex * 10.0F, now);
         }
         particleIndex++;
      }
      ms.push();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (Particle particle : this.particles) {
         if (!particle.renderVisible) {
            continue;
         }
         Vec3d pos = particle.renderPos;
         float bigSize = 4.0F * particle.size;
         ColorRGBA particleColor = particle.renderColor;
         ms.push();
         RenderUtility.prepareMatrices(ms, pos);
         ms.multiply(camera.getRotation());
         DrawUtility.drawImage(
            ms,
            builder,
            (double)(-bigSize / 2.0F),
            (double)(-bigSize / 2.0F),
            0.0,
            (double)bigSize,
            (double)bigSize,
            particleColor.withAlpha(255.0F * particle.alpha.getValue() * 0.4F)
         );
         ms.pop();
      }

      BuiltBuffer builtLinesBuffer1 = builder.endNullable();
      if (builtLinesBuffer1 != null) {
         BufferRenderer.drawWithGlobalProgram(builtLinesBuffer1);
      }

      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.disableDepthTest();
      ms.pop();
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (Particle particle : this.particles) {
         if (!particle.renderVisible) {
            continue;
         }
         Vec3d pos = particle.renderPos;
         Vec3d rot = particle.renderRot;
         ColorRGBA particleColor = particle.renderColor;
         ms.push();
         ms.translate(pos.add(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()));
         ms.multiply(this.renderRotation.rotationXYZ((float)rot.x, (float)rot.y, (float)rot.z));
         ms.scale(particle.size, particle.size, particle.size);
         Draw3DUtility.renderBoxInternalDiagonals(
            ms, linesBuffer, PARTICLE_BOX, particleColor.withAlpha(255.0F * particle.alpha.getValue() * 0.4F)
         );
         Draw3DUtility.renderOutlinedBox(
            ms, linesBuffer, PARTICLE_BOX, particleColor.withAlpha(205.0F * particle.alpha.getValue())
         );
         ms.pop();
      }

      BuiltBuffer builtLinesBuffer = linesBuffer.endNullable();
      if (builtLinesBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtLinesBuffer);
      }

      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   };

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null) {
         this.particles.clear();
         return;
      }

      for (int index = this.particles.size() - 1; index >= 0; index--) {
         Particle particle = this.particles.get(index);
         if (particle.alpha.getValue() == 0.0F && particle.timer.finished(particle.liveTicks)) {
            this.particles.remove(index);
         }
      }

      for (Particle particle : this.particles) {
         particle.tick();
      }

      if (this.particles.size() < 100) {
         this.particles
            .add(
               new Particle(
                  mc.player.getEntityPos().add(MathUtility.random(-20.0, 20.0), MathUtility.random(0.0, 5.0), MathUtility.random(-20.0, 20.0)),
                  Vec3d.ZERO,
                  new Vec3d(MathUtility.random(-1.0, 1.0), MathUtility.random(0.0, 2.0), MathUtility.random(-1.0, 1.0)),
                  new Vec3d(MathUtility.random(-1.0, 1.0), MathUtility.random(-1.0, 1.0), MathUtility.random(-1.0, 1.0)),
                  (long)MathUtility.random(1500.0, 4500.0),
                  MathUtility.random(0.1F, 0.3F)
               )
            );
      }
   }

   private ColorRGBA getWorldColor(float index, long now) {
      float animatedIndex = index + (now % 3000L) / 3000.0F * 360.0F;
      if (this.colorCustom.isSelected()) {
         float normalized = (animatedIndex % 360.0F) / 180.0F;
         float mix = normalized > 1.0F ? 2.0F - normalized : normalized;
         return this.color.getColorSafe().mix(this.colorSecond.getColorSafe(), mix);
      }

      return Colors.getAccentColor(animatedIndex);
   }

   static class Particle {
      Vec3d prev;
      Vec3d prevRot;
      Vec3d pos;
      Vec3d rotate;
      Vec3d motion;
      Vec3d rotateMotion;
      final long liveTicks;
      float size;
      final Timer timer = new Timer();
      final Animation alpha = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
      Vec3d renderPos;
      Vec3d renderRot;
      ColorRGBA renderColor;
      boolean renderVisible;

      public Particle(Vec3d pos, Vec3d rotate, Vec3d motion, Vec3d rotateMotion, long liveTicks, float size) {
         this.pos = pos;
         this.rotate = rotate;
         this.motion = motion.multiply(0.04F);
         this.rotateMotion = rotateMotion.multiply(0.04F);
         this.liveTicks = liveTicks;
         this.size = size;
         this.prevRot = rotate;
         this.prev = pos;
         this.alpha.setDuration(1000L);
      }

      void tick() {
         this.prev = this.pos;
         this.prevRot = this.rotate;
         this.pos = this.pos.add(this.motion);
         this.rotate = this.rotate.add(this.rotateMotion);
         this.motion = this.motion.multiply(0.98);
         this.rotateMotion = this.rotateMotion.multiply(0.98);
      }
   }
}
