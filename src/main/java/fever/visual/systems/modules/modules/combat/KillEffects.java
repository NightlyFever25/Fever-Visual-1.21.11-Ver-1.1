package fever.visual.systems.modules.modules.combat;

import fever.visual.utility.render.compat.BufferRenderer;

import fever.visual.utility.render.compat.GlStateManager.DstFactor;
import fever.visual.utility.render.compat.GlStateManager.SrcFactor;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.EntityDeathEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.modules.modules.visuals.MenuModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.math.MathUtility;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import fever.visual.utility.render.pipeline.KillScanPipeline;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Kill Effects", category = ModuleCategory.COMBAT, desc = "modules.descriptions.kill_effects")
public class KillEffects extends BaseModule {
   private static final Identifier BLOOM_TEXTURE = FeverVisual.id("textures/bloom.png");

   private final List<Lightning> lightnings = new ArrayList<>();
   private final List<Scan> scans = new ArrayList<>();
   private final KillScanPipeline scanPipeline = new KillScanPipeline();
   private final ModeSetting effectMode = new ModeSetting(this, "Mode");
   private final ModeSetting.Value lightningMode = new ModeSetting.Value(this.effectMode, "Lightning").select();
   private final ModeSetting.Value scanMode = new ModeSetting.Value(this.effectMode, "Scan");
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.kill_effects.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.kill_effects.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.kill_effects.color_mode.custom");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.kill_effects.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(151, 71, 255, 255))
           .alpha(true);
   private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.kill_effects.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(100, 160, 255, 255))
           .alpha(true);
   private final EventListener<EntityDeathEvent> onEntityDeath = event -> {
      if (mc.player != null
            && event.getSource() != null
            && event.getSource().getAttacker() == mc.player
            && !event.getEntity().isRemoved()
            && !this.isInvisible(event.getEntity())) {
         if (this.scanMode.isSelected()) {
            this.scans.add(new Scan(event.getEntity().getEntityPos(), event.getEntity().getHeight(), this.colorMode.is(this.colorCustom), this.color.getColorSafe(), this.colorSecond.getColorSafe()));
         } else {
            this.lightnings.add(new Lightning(event.getEntity().getEntityPos(), this.colorMode.is(this.colorCustom), this.color.getColorSafe(), this.colorSecond.getColorSafe()));
         }
      }
   };
   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (this.lightnings.isEmpty() && this.scans.isEmpty()) {
         return;
      }

      MatrixStack ms = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getCameraPos();
      ms.push();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (Lightning lightning : this.lightnings) {
         lightning.render(builder, event.getMatrices(), camera);
         if (lightning.animation.getValue() == 1.0F) {
            lightning.showing = false;
         }
      }

      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.disableDepthTest();
      ms.pop();
      this.renderScans(event, cameraPos);
      for (int index = this.lightnings.size() - 1; index >= 0; index--) {
         Lightning lightning = this.lightnings.get(index);
         if (!lightning.showing && lightning.animation.getValue() == 0.0F) {
            this.lightnings.remove(index);
         }
      }
      this.scans.removeIf(Scan::isFinished);
   };

   private void renderScans(Render3DEvent event, Vec3d cameraPos) {
      if (this.scans.isEmpty() || mc.getFramebuffer() == null) {
         return;
      }

      Framebuffer framebuffer = mc.getFramebuffer();
      Matrix4f invProjection = new Matrix4f(event.getProjectionMatrix()).invert();
      Matrix4f invView = new Matrix4f(event.getPositionMatrix()).invert();
      float viewDistance = Math.max(64.0F, mc.options.getViewDistance().getValue() * 16.0F);
      for (Scan scan : this.scans) {
         scan.render(this.scanPipeline, framebuffer, invProjection, invView, cameraPos, viewDistance);
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.lightnings.clear();
      this.scans.clear();
      this.scanPipeline.close();
   }

   private boolean isInvisible(net.minecraft.entity.Entity entity) {
      if (!(entity instanceof LivingEntity living)) {
         return false;
      }
      if (living.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
      if (living.isInvisible()) return true;
      if (mc.player != null && living.isInvisibleTo(mc.player)) return true;
      return false;
   }

   static class Lightning {
      final Vec3d pos;
      final boolean customColor;
      final ColorRGBA color;
      final ColorRGBA colorSecond;
      boolean showing = true;
      final Animation animation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
      final List<Vec3d> poses = new ArrayList<>();

      public Lightning(Vec3d pos, boolean customColor, ColorRGBA color, ColorRGBA colorSecond) {
         this.pos = pos;
         this.customColor = customColor;
         this.color = color;
         this.colorSecond = colorSecond;
         Vec3d lastPos = pos;

         for (int i = 0; i < 200; i++) {
            this.poses.add(lastPos = lastPos.add(MathUtility.random(-0.4F, 0.4F), 0.25, MathUtility.random(-0.4F, 0.4F)));
         }
      }

      void render(BufferBuilder builder, MatrixStack ms, Camera camera) {
         this.animation.setEasing(Easing.BOUNCE_IN);
         this.animation.setDuration(500L);
         this.animation.update(this.showing);

         for (Vec3d pos : this.poses) {
            float size = (float)(2.0 + 5.0 * (pos.y - this.pos.y) / 50.0);
            ms.push();
            RenderUtility.prepareMatrices(ms, pos);
            ms.multiply(camera.getRotation());
            DrawUtility.drawImage(
               ms,
               builder,
                    -size / 2.0F,
                    -size / 2.0F,
               0.0,
                    size,
                    size,
               this.getColor((float)(pos.y - this.pos.y) * 18.0F).withAlpha(255.0F * this.animation.getValue() * 0.4F)
            );
            ms.pop();
         }
      }

      private ColorRGBA getColor(float index) {
         if (this.customColor) {
            float normalized = (index % 360.0F) / 180.0F;
            float mix = normalized > 1.0F ? 2.0F - normalized : normalized;
            return this.color.mix(this.colorSecond, mix);
         }

         return Colors.getAccentColor(index);
      }
   }

   static class Scan {
      final Vec3d pos;
      final float height;
      final boolean customColor;
      final ColorRGBA color;
      final ColorRGBA colorSecond;
      final long startedAt = System.currentTimeMillis();
      final long lifeMs = 6400L;

      Scan(Vec3d pos, float height, boolean customColor, ColorRGBA color, ColorRGBA colorSecond) {
         this.pos = pos;
         this.height = Math.max(0.8F, height);
         this.customColor = customColor;
         this.color = color;
         this.colorSecond = colorSecond;
      }

      void render(KillScanPipeline pipeline, Framebuffer framebuffer, Matrix4f invProjection, Matrix4f invView, Vec3d cameraPos, float viewDistance) {
         float progress = MathHelper.clamp((System.currentTimeMillis() - this.startedAt) / (this.lifeMs * 0.5F), 0.0F, 1.0F);
         float radius = 1.0F + (viewDistance - 1.0F) * fastOut(progress);
         float flickAlpha = 1.0F - progress;
         float flickWave = Math.min(wave(flickAlpha) * 2.0F, 1.0F);
         if (flickAlpha <= 0.001F) {
            return;
         }

         ColorRGBA baseA = this.getColor(progress * 240.0F);
         ColorRGBA baseB = this.getColor(progress * 240.0F + 90.0F);
         ColorRGBA outer = baseA.withAlpha(210.0F * flickAlpha);
         ColorRGBA mid = baseB.withAlpha(160.0F * flickAlpha * (0.45F + flickWave * 0.55F));
         ColorRGBA inner = baseA.mix(baseB, 0.5F).withAlpha(105.0F * flickAlpha);
         ColorRGBA scanline = ColorRGBA.WHITE.withAlpha(145.0F * flickAlpha * flickWave);
         pipeline.apply(
            framebuffer,
            invProjection,
            invView,
            cameraPos,
            this.pos,
            radius,
            Math.max(0.45F, radius / 1.5F),
            40.0F,
            progress,
            1.0F,
            outer,
            mid,
            inner,
            scanline
         );
      }

      boolean isFinished() {
         return System.currentTimeMillis() - this.startedAt > this.lifeMs;
      }

      private ColorRGBA getColor(float index) {
         if (this.customColor) {
            float normalized = (index % 360.0F) / 180.0F;
            float mix = normalized > 1.0F ? 2.0F - normalized : normalized;
            return this.color.mix(this.colorSecond, mix);
         }
         return Colors.getAccentColor(index);
      }

      private static float expoInOut(float value) {
         if (value <= 0.0F) {
            return 0.0F;
         }
         if (value >= 1.0F) {
            return 1.0F;
         }
         return value < 0.5F
            ? (float)Math.pow(2.0D, 20.0D * value - 10.0D) / 2.0F
            : (2.0F - (float)Math.pow(2.0D, -20.0D * value + 10.0D)) / 2.0F;
      }

      private static float fastOut(float value) {
         value = MathHelper.clamp(value, 0.0F, 1.0F);
         return 1.0F - (1.0F - value) * (1.0F - value) * (1.0F - value);
      }

      private static float wave(float value) {
         return (float)Math.sin(MathHelper.clamp(value, 0.0F, 1.0F) * Math.PI);
      }
   }
}
