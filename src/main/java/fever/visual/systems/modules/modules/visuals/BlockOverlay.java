package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.modules.modules.visuals.cosmetic.BlockShaderRenderer;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

@ModuleInfo(
        name = "Block Overlay",
        category = ModuleCategory.VISUALS,
        desc = "modules.descriptions.blockoverlay"
)
public class BlockOverlay extends BaseModule {
   private static final Identifier BLOOM_TEXTURE = Identifier.of("fevervisual", "textures/bloom.png");
   private static final int[][] BOX_EDGES = new int[][]{
           {0, 1}, {1, 2}, {2, 3}, {3, 0},
           {4, 5}, {5, 6}, {6, 7}, {7, 4},
           {0, 4}, {1, 5}, {2, 6}, {3, 7}
   };

   private final BooleanSetting fill = new BooleanSetting(this, "modules.settings.blockoverlay.fill").enabled(true);
   private final BooleanSetting useShader = new BooleanSetting(this, "modules.settings.blockoverlay.use_shader", "modules.settings.blockoverlay.use_shader.desc").enabled(false);

   private final ModeSetting shaderMode = new ModeSetting(this, "modules.settings.blockoverlay.shader_mode", "modules.settings.blockoverlay.shader_mode.desc", () -> !this.useShader.isEnabled());
   private final ModeSetting.Value cobwebShader = new ModeSetting.Value(this.shaderMode, "modules.settings.blockoverlay.shader_mode.cobweb").select();
   private final ModeSetting.Value nebulaShader = new ModeSetting.Value(this.shaderMode, "modules.settings.blockoverlay.shader_mode.nebula");
   private final ModeSetting.Value plasmaShader = new ModeSetting.Value(this.shaderMode, "modules.settings.blockoverlay.shader_mode.plasma");
   private final ModeSetting.Value starfieldShader = new ModeSetting.Value(this.shaderMode, "modules.settings.blockoverlay.shader_mode.starfield");
   private final ModeSetting.Value fireworksShader = new ModeSetting.Value(this.shaderMode, "modules.settings.blockoverlay.shader_mode.fireworks");
   private final ModeSetting.Value galaxyShader = new ModeSetting.Value(this.shaderMode, "modules.settings.blockoverlay.shader_mode.galaxy");
   private final ModeSetting.Value starsShader = new ModeSetting.Value(this.shaderMode, "modules.settings.blockoverlay.shader_mode.stars");

   private final SliderSetting shaderOpacity = new SliderSetting(this, "modules.settings.blockoverlay.shader_opacity", "modules.settings.blockoverlay.shader_opacity.desc", () -> !this.useShader.isEnabled())
           .min(0.1f).max(1.0f).step(0.05f).currentValue(1.0f);
   private final SliderSetting shaderSpeed = new SliderSetting(this, "modules.settings.blockoverlay.shader_speed", "modules.settings.blockoverlay.shader_speed.desc", () -> !this.useShader.isEnabled())
           .min(0.1f).max(5.0f).step(0.1f).currentValue(1.0f);

   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.blockoverlay.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.blockoverlay.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.blockoverlay.color_mode.custom");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.blockoverlay.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(151.0f, 71.0f, 255.0f, 255.0f)).alpha(true);
   private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.blockoverlay.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(100.0f, 160.0f, 255.0f, 255.0f)).alpha(true);

   private final BooleanSetting glow = new BooleanSetting(this, "modules.settings.blockoverlay.glow", "modules.settings.blockoverlay.glow.desc").enabled(false);
   private final SliderSetting glowIntensity = new SliderSetting(this, "modules.settings.blockoverlay.glow_intensity", "modules.settings.blockoverlay.glow_intensity.desc")
           .min(0.1f).max(1.0f).step(0.05f).currentValue(0.5f);

   private final BooleanSetting cornersOnly = new BooleanSetting(this, "modules.settings.blockoverlay.corners_only", "modules.settings.blockoverlay.corners_only.desc").enabled(false);
   private final SliderSetting cornerLength = new SliderSetting(this, "modules.settings.blockoverlay.corner_length", "modules.settings.blockoverlay.corner_length.desc")
           .min(0.1f).max(0.5f).step(0.05f).currentValue(0.25f);

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (mc.crosshairTarget instanceof BlockHitResult result && result.getType() == HitResult.Type.BLOCK) {
         BlockPos pos = result.getBlockPos();
         if (mc.world == null) {
            return;
         }

         MatrixStack matrices = event.getMatrices();
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getCameraPos();
         VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
         Box box = shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos);

         double minX = box.minX - cameraPos.x;
         double minY = box.minY - cameraPos.y;
         double minZ = box.minZ - cameraPos.z;
         double maxX = box.maxX - cameraPos.x;
         double maxY = box.maxY - cameraPos.y;
         double maxZ = box.maxZ - cameraPos.z;

         ColorRGBA baseColor = this.getOverlayColor(0.0f);

         matrices.push();
         RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         RenderSystem.disableCull();

         Matrix4f matrix = matrices.peek().getPositionMatrix();

         if (this.cornersOnly.isEnabled()) {
            if (this.useShader.isEnabled()) {
               BlockShaderRenderer.ShaderMode shaderMode = this.getSelectedShaderMode();
               int alpha = (int) (this.shaderOpacity.getCurrentValue() * 255.0f);
               float speed = this.shaderSpeed.getCurrentValue();
               BlockShaderRenderer.renderShaderBox(matrix, minX, minY, minZ, maxX, maxY, maxZ, baseColor, shaderMode, alpha, speed);
            }
            this.renderCorners(matrix, minX, minY, minZ, maxX, maxY, maxZ, baseColor);
         } else {
            ColorRGBA color1 = this.getOverlayColor(0.0f).withAlpha((int) (baseColor.getAlpha() * 0.3f));
            ColorRGBA color2 = this.getOverlayColor(90.0f).withAlpha((int) (baseColor.getAlpha() * 0.6f));
            ColorRGBA color3 = this.getOverlayColor(180.0f).withAlpha((int) (baseColor.getAlpha() * 0.3f));
            ColorRGBA color4 = this.getOverlayColor(270.0f).withAlpha((int) (baseColor.getAlpha() * 0.6f));
            ColorRGBA[] gradientColors = new ColorRGBA[]{color1, color2, color3, color4};

            if (this.fill.isEnabled() || this.useShader.isEnabled()) {
               BlockShaderRenderer.ShaderMode shaderMode = this.useShader.isEnabled() ? this.getSelectedShaderMode() : BlockShaderRenderer.ShaderMode.FIREWORKS;

               if (!this.useShader.isEnabled() && this.fill.isEnabled()) {
                  RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                  BufferBuilder fillBuffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                  this.drawBoxFill(fillBuffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, gradientColors, 85);
                  RenderUtility.buildBuffer(fillBuffer);
               } else if (this.useShader.isEnabled()) {
                  int alpha = (int) (this.shaderOpacity.getCurrentValue() * 255.0f);
                  float speed = this.shaderSpeed.getCurrentValue();
                  BlockShaderRenderer.renderShaderBox(matrix, minX, minY, minZ, maxX, maxY, maxZ, baseColor, shaderMode, alpha, speed);
               }
            }

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(3.0f);
            BufferBuilder lineBuffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            this.drawBoxOutline(lineBuffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, gradientColors);
            RenderUtility.buildBuffer(lineBuffer);
         }

         if (this.glow.isEnabled()) {
            this.drawBoxGlow(matrices, minX, minY, minZ, maxX, maxY, maxZ, baseColor);
         }

         RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0f);
         matrices.pop();
      }
   };

   private ColorRGBA getOverlayColor(float index) {
      if (this.colorCustom.isSelected()) {
         return this.color.getColor().mix(this.colorSecond.getColor(), this.getCustomMix(index));
      }

      return Colors.getAccentColor(index);
   }

   private float getCustomMix(float index) {
      float normalized = (index % 360.0f) / 180.0f;
      return normalized > 1.0f ? 2.0f - normalized : normalized;
   }

   private void renderCorners(Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, ColorRGBA color) {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      float length = this.cornerLength.getCurrentValue();
      double width = maxX - minX;
      double height = maxY - minY;
      double depth = maxZ - minZ;
      double lineLength = Math.min(width, Math.min(height, depth)) * length;

      int c = replaceAlpha(color.getRGB(), 255);
      int r = c >> 16 & 0xFF;
      int g = c >> 8 & 0xFF;
      int b = c & 0xFF;
      int a = c >> 24 & 0xFF;

      RenderSystem.lineWidth(3.0f);
      BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      Vec3d[] corners = new Vec3d[]{
              new Vec3d(minX, minY, minZ), new Vec3d(maxX, minY, minZ),
              new Vec3d(maxX, minY, maxZ), new Vec3d(minX, minY, maxZ),
              new Vec3d(minX, maxY, minZ), new Vec3d(maxX, maxY, minZ),
              new Vec3d(maxX, maxY, maxZ), new Vec3d(minX, maxY, maxZ)
      };

      this.drawCornerLines(buffer, matrix, corners[0], lineLength, r, g, b, a, true, true, true);
      this.drawCornerLines(buffer, matrix, corners[1], lineLength, r, g, b, a, false, true, true);
      this.drawCornerLines(buffer, matrix, corners[2], lineLength, r, g, b, a, false, true, false);
      this.drawCornerLines(buffer, matrix, corners[3], lineLength, r, g, b, a, true, true, false);
      this.drawCornerLines(buffer, matrix, corners[4], lineLength, r, g, b, a, true, false, true);
      this.drawCornerLines(buffer, matrix, corners[5], lineLength, r, g, b, a, false, false, true);
      this.drawCornerLines(buffer, matrix, corners[6], lineLength, r, g, b, a, false, false, false);
      this.drawCornerLines(buffer, matrix, corners[7], lineLength, r, g, b, a, true, false, false);

      RenderUtility.buildBuffer(buffer);
   }

   private void drawCornerLines(BufferBuilder buffer, Matrix4f matrix, Vec3d corner, double lineLength, int r, int g, int b, int a, boolean isLeft, boolean isBottom, boolean isFront) {
      Vec3d xEnd = corner.add(isLeft ? lineLength : -lineLength, 0.0, 0.0);
      buffer.vertex(matrix, (float) corner.x, (float) corner.y, (float) corner.z).color(r, g, b, a);
      buffer.vertex(matrix, (float) xEnd.x, (float) xEnd.y, (float) xEnd.z).color(r, g, b, a);

      Vec3d yEnd = corner.add(0.0, isBottom ? lineLength : -lineLength, 0.0);
      buffer.vertex(matrix, (float) corner.x, (float) corner.y, (float) corner.z).color(r, g, b, a);
      buffer.vertex(matrix, (float) yEnd.x, (float) yEnd.y, (float) yEnd.z).color(r, g, b, a);

      Vec3d zEnd = corner.add(0.0, 0.0, isFront ? lineLength : -lineLength);
      buffer.vertex(matrix, (float) corner.x, (float) corner.y, (float) corner.z).color(r, g, b, a);
      buffer.vertex(matrix, (float) zEnd.x, (float) zEnd.y, (float) zEnd.z).color(r, g, b, a);
   }

   private void drawBoxFill(BufferBuilder buffer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, ColorRGBA[] colors, int fillAlpha) {
      int[] c = new int[4];
      int[][] rgba = new int[4][4];

      for (int i = 0; i < 4; ++i) {
         int color = colors[i].getRGB();
         c[i] = replaceAlpha(color, fillAlpha);
         rgba[i][0] = c[i] >> 16 & 0xFF;
         rgba[i][1] = c[i] >> 8 & 0xFF;
         rgba[i][2] = c[i] & 0xFF;
         rgba[i][3] = c[i] >> 24 & 0xFF;
      }

      buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);

      buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);

      buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);

      buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);

      buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);

      buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
   }

   private int replaceAlpha(int color, int alpha) {
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      return alpha << 24 | r << 16 | g << 8 | b;
   }

   private void drawBoxOutline(BufferBuilder buffer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, ColorRGBA[] colors) {
      int[] c = new int[4];
      for (int i = 0; i < 4; ++i) {
         c[i] = replaceAlpha(colors[i].getRGB(), 255);
      }

      this.drawLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, c[0], c[1]);
      this.drawLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, c[1], c[2]);
      this.drawLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, c[2], c[3]);
      this.drawLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, c[3], c[0]);
      this.drawLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, c[0], c[1]);
      this.drawLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, c[1], c[2]);
      this.drawLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, c[2], c[3]);
      this.drawLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, c[3], c[0]);
      this.drawLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, c[0], c[0]);
      this.drawLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, c[1], c[1]);
      this.drawLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, c[2], c[2]);
      this.drawLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, c[3], c[3]);
   }

   private void drawLine(BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, int color1, int color2) {
      int r1 = color1 >> 16 & 0xFF;
      int g1 = color1 >> 8 & 0xFF;
      int b1 = color1 & 0xFF;
      int a1 = color1 >> 24 & 0xFF;
      int r2 = color2 >> 16 & 0xFF;
      int g2 = color2 >> 8 & 0xFF;
      int b2 = color2 & 0xFF;
      int a2 = color2 >> 24 & 0xFF;

      buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r1, g1, b1, a1);
      buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r2, g2, b2, a2);
   }

   private void drawBoxGlow(MatrixStack matrices, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, ColorRGBA color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);

      Vec3d[] corners = new Vec3d[]{
              new Vec3d(minX, minY, minZ), new Vec3d(maxX, minY, minZ),
              new Vec3d(maxX, minY, maxZ), new Vec3d(minX, minY, maxZ),
              new Vec3d(minX, maxY, minZ), new Vec3d(maxX, maxY, minZ),
              new Vec3d(maxX, maxY, maxZ), new Vec3d(minX, maxY, maxZ)
      };

      for (int[] edge : BOX_EDGES) {
         Vec3d start = corners[edge[0]];
         Vec3d end = corners[edge[1]];
         float distance = (float) start.distanceTo(end);

         for (int i = 0; i < 10; ++i) {
            float t = (float) i / 10.0f;
            Vec3d pos = start.lerp(end, t);

            matrices.push();
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.multiply(mc.gameRenderer.getCamera().getRotation());

            float intensity = this.glowIntensity.getCurrentValue();
            float size = distance / 4.0f;

            BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            drawImage(matrices, buffer, -size / 2.0f, -size / 2.0f, 0.0f, size, size, color.withAlpha((int) (76.5f * intensity)));
            RenderUtility.buildBuffer(buffer);

            float bigSize = distance * 1.5f;
            buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            drawImage(matrices, buffer, -bigSize / 2.0f, -bigSize / 2.0f, 0.0f, bigSize, bigSize, color.withAlpha((int) (5.1f * intensity)));
            RenderUtility.buildBuffer(buffer);

            matrices.pop();
         }
      }

      RenderSystem.enableDepthTest();
   }

   private void drawImage(MatrixStack matrices, BufferBuilder buffer, float x, float y, float z, float width, float height, ColorRGBA color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      buffer.vertex(matrix, x, y + height, z).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).texture(0.0f, 1.0f);
      buffer.vertex(matrix, x + width, y + height, z).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).texture(1.0f, 1.0f);
      buffer.vertex(matrix, x + width, y, z).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).texture(1.0f, 0.0f);
      buffer.vertex(matrix, x, y, z).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).texture(0.0f, 0.0f);
   }

   private BlockShaderRenderer.ShaderMode getSelectedShaderMode() {
      ModeSetting.Value selected = this.shaderMode.getValue();
      if (selected == this.cobwebShader) {
         return BlockShaderRenderer.ShaderMode.COBWEB;
      }
      if (selected == this.nebulaShader) {
         return BlockShaderRenderer.ShaderMode.NEBULA;
      }
      if (selected == this.plasmaShader) {
         return BlockShaderRenderer.ShaderMode.PLASMA;
      }
      if (selected == this.starfieldShader) {
         return BlockShaderRenderer.ShaderMode.STARFIELD;
      }
      if (selected == this.fireworksShader) {
         return BlockShaderRenderer.ShaderMode.FIREWORKS;
      }
      if (selected == this.galaxyShader) {
         return BlockShaderRenderer.ShaderMode.GALAXY;
      }
      if (selected == this.starsShader) {
         return BlockShaderRenderer.ShaderMode.STARS;
      }
      return BlockShaderRenderer.ShaderMode.COBWEB;
   }
}
