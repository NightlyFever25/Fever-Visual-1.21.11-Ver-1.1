package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;

import fever.visual.utility.render.compat.GlStateManager.DstFactor;
import fever.visual.utility.render.compat.GlStateManager.SrcFactor;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.AttackEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.Draw3DUtility;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.*;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
        name = "Hit Effect",
        category = ModuleCategory.VISUALS,
        desc = "modules.descriptions.hiteffect"
)
public class HitEffect extends BaseModule {
   private static final int MAX_ACTIVE_WAVES = 3;
   private final List<WaveEffect> waves = new ArrayList<>();
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.hiteffect.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.hiteffect.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.hiteffect.color_mode.custom");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.hiteffect.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(151, 71, 255, 255))
           .alpha(true);
   private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.hiteffect.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(100, 160, 255, 255))
           .alpha(true);

   private final EventListener<AttackEvent> onAttack = event -> {
      if (mc.player != null && event.getEntity() != null && !this.hasVisionBlockingEffect()) {
         if (event.getEntity() instanceof LivingEntity living && this.isInvisible(living)) {
            return;
         }
         Vec3d pos = event.getEntity().getEntityPos();
         if (this.waves.size() >= MAX_ACTIVE_WAVES) {
            this.waves.remove(0);
         }
         this.waves.add(new WaveEffect(pos, this.colorMode.is(this.colorCustom), this.color.getColorSafe(), this.colorSecond.getColorSafe()));
      }
   };

   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (!this.waves.isEmpty()) {
         MatrixStack ms = event.getMatrices();
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getCameraPos();
         ms.push();
         ms.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.enableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder fillBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

         for (WaveEffect wave : this.waves) {
            wave.renderFill(fillBuffer, ms);
         }

         BuiltBuffer fillBuilt = fillBuffer.endNullable();
         if (fillBuilt != null) {
            BufferRenderer.drawWithGlobalProgram(fillBuilt);
         }

         BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

         for (WaveEffect wave : this.waves) {
            wave.renderOutline(linesBuffer, ms);
         }

         BuiltBuffer linesBuilt = linesBuffer.endNullable();
         if (linesBuilt != null) {
            BufferRenderer.drawWithGlobalProgram(linesBuilt);
         }

         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
         ms.pop();
         for (int index = this.waves.size() - 1; index >= 0; index--) {
            if (this.waves.get(index).animation.getValue() >= 1.0F) {
               this.waves.remove(index);
            }
         }
      }
   };

   @Override
   public void onDisable() {
      this.waves.clear();
   }

   private boolean isInvisible(LivingEntity entity) {
      if (entity.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
      if (entity.isInvisible()) return true;
      if (mc.player != null && entity.isInvisibleTo(mc.player)) return true;
      return false;
   }

   private boolean hasVisionBlockingEffect() {
      return mc.player != null
              && (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
              || mc.player.hasStatusEffect(StatusEffects.DARKNESS));
   }

   static class BlockOutline {
      final BlockPos pos;
      final double distance;

      public BlockOutline(BlockPos pos, double distance) {
         this.pos = pos;
         this.distance = distance;
      }
   }

   static class WaveEffect {
      final Vec3d centerPos;
      final boolean customColor;
      final ColorRGBA color;
      final ColorRGBA colorSecond;
      final Animation animation = new Animation(1200L, 0.0F, Easing.LINEAR);
      final List<BlockOutline> blocks = new ArrayList<>();
      final int maxRadius = 15;

      public WaveEffect(Vec3d pos, boolean customColor, ColorRGBA color, ColorRGBA colorSecond) {
         this.centerPos = pos;
         this.customColor = customColor;
         this.color = color;
         this.colorSecond = colorSecond;
         this.animation.update(true);
         BlockPos centerBlock = BlockPos.ofFloored(pos);

         for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
               double distance = Math.sqrt(x * x + z * z);
               if (distance <= 15.0 && distance >= 1.0) {
                  BlockPos foundPos = null;

                  for (int y = 3; y >= -10; y--) {
                     BlockPos checkPos = centerBlock.add(x, y, z);
                     if (IMinecraft.mc.world != null && !IMinecraft.mc.world.getBlockState(checkPos).isAir()) {
                        foundPos = checkPos;
                        break;
                     }
                  }

                  if (foundPos != null) {
                     this.blocks.add(new BlockOutline(foundPos, distance));
                  }
               }
            }
         }
      }

      void renderFill(BufferBuilder builder, MatrixStack ms) {
         this.animation.update(true);
         float progress = this.animation.getValue();
         float currentRadius = progress * 15.0F;
         float waveThickness = 1.0F;

         for (BlockOutline block : this.blocks) {
            float distDiff = Math.abs((float)block.distance - currentRadius);
            if (!(distDiff > waveThickness)) {
               float alpha = 1.0F - distDiff / waveThickness;
               alpha = (float)Math.pow(alpha, 0.5);
               float fadeOut = 1.0F;
               if (progress > 0.7F) {
                  fadeOut = 1.0F - (progress - 0.7F) / 0.3F;
               }

               alpha *= fadeOut;
               if (!(alpha <= 0.05F)) {
                  float scale = 1.0F;
                  if (progress > 0.7F) {
                     scale = 0.5F + 0.5F * fadeOut;
                  }

                  Vec3d center = block.pos.toCenterPos();
                  double halfSize = 0.5 * scale;
                  Box box = new Box(
                          center.x - halfSize,
                          center.y - halfSize,
                          center.z - halfSize,
                          center.x + halfSize,
                          center.y + halfSize,
                          center.z + halfSize
                  );
                  Draw3DUtility.renderFilledBox(ms, builder, box, this.getColor((float)block.distance * 24.0F).withAlpha((int)(alpha * 40.0F)));
               }
            }
         }
      }

      void renderOutline(BufferBuilder builder, MatrixStack ms) {
         this.animation.update(true);
         float progress = this.animation.getValue();
         float currentRadius = progress * 15.0F;
         float waveThickness = 1.0F;

         for (BlockOutline block : this.blocks) {
            float distDiff = Math.abs((float)block.distance - currentRadius);
            if (!(distDiff > waveThickness)) {
               float alpha = 1.0F - distDiff / waveThickness;
               alpha = (float)Math.pow(alpha, 0.5);
               float fadeOut = 1.0F;
               if (progress > 0.7F) {
                  fadeOut = 1.0F - (progress - 0.7F) / 0.3F;
               }

               alpha *= fadeOut;
               if (!(alpha <= 0.05F)) {
                  float scale = 1.0F;
                  if (progress > 0.7F) {
                     scale = 0.5F + 0.5F * fadeOut;
                  }

                  Vec3d center = block.pos.toCenterPos();
                  double halfSize = 0.5 * scale;
                  Box box = new Box(
                          center.x - halfSize,
                          center.y - halfSize,
                          center.z - halfSize,
                          center.x + halfSize,
                          center.y + halfSize,
                          center.z + halfSize
                  );
                  Draw3DUtility.renderOutlinedBox(ms, builder, box, this.getColor((float)block.distance * 24.0F).withAlpha((int)(alpha * 255.0F)));
               }
            }
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
}
