package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.compat.BufferRenderer;
import fever.visual.utility.render.compat.GlStateManager;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import fever.visual.utility.render.pipeline.GlassVaporPipeline;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "Glass Vapor", category = ModuleCategory.VISUALS, desc = "Glass vapor around movement")
public class GlassVapor extends BaseModule {
   private static final int MAX_DROPS = 240;
   private static final float NEAR = 0.05F;
   private static final float RADIUS_SCALE = 1.85F;

   private final SliderSetting density = new SliderSetting(this, "Density").min(1.0F).max(12.0F).step(1.0F).currentValue(5.0F);
   private final SliderSetting spawnRate = new SliderSetting(this, "Drops Per Second").min(4.0F).max(30.0F).step(1.0F).currentValue(14.0F);
   private final SliderSetting blobSize = new SliderSetting(this, "Size").min(0.15F).max(0.55F).step(0.01F).currentValue(0.3F);
   private final SliderSetting riseHeight = new SliderSetting(this, "Rise Height").min(0.6F).max(3.0F).step(0.1F).currentValue(1.6F);
   private final SliderSetting lifetime = new SliderSetting(this, "Lifetime").min(900.0F).max(3000.0F).step(50.0F).currentValue(1600.0F);
   private final SliderSetting distort = new SliderSetting(this, "Distortion").min(0.2F).max(3.0F).step(0.1F).currentValue(1.0F);
   private final SliderSetting ripple = new SliderSetting(this, "Ripple").min(0.0F).max(3.0F).step(0.1F).currentValue(1.0F);
   private final SliderSetting rim = new SliderSetting(this, "Rim").min(0.0F).max(1.5F).step(0.05F).currentValue(0.7F);
   private final SliderSetting chroma = new SliderSetting(this, "Chroma").min(0.0F).max(1.0F).step(0.05F).currentValue(0.35F);
   private final SliderSetting tintStrength = new SliderSetting(this, "Tint Strength").min(0.0F).max(100.0F).step(1.0F).currentValue(45.0F);
   private final ColorSetting tint = new ColorSetting(this, "Tint").color(new ColorRGBA(160.0F, 220.0F, 255.0F, 255.0F)).alpha(true);

   private final List<Drop> drops = new ArrayList<>();
   private final float[] data = new float[3856];
   private final Matrix4f viewProj = new Matrix4f();
   private final Vector4f centerClip = new Vector4f();
   private final Vector4f axisClip = new Vector4f();
   private final GlassVaporPipeline pipeline = new GlassVaporPipeline();
   private Vec3d lastPos;
   private Vec3d lastSpawnPos;
   private long nextBurstMs;
   private long seq;
   private float centerU;
   private float centerV;
   private float centerLin;
   private float axisV;

   private final EventListener<Render3DEvent> onRender = event -> {
      if (mc.player == null || mc.world == null || event.getCamera() == null) {
         this.drops.clear();
         this.lastPos = null;
         this.lastSpawnPos = null;
         return;
      }
      long now = System.currentTimeMillis();
      Vec3d playerPos = mc.player.getLerpedPos(event.getTickDelta());
      Vec3d previousPos = this.lastPos;
      boolean moving = false;
      if (previousPos != null) {
         double dx = playerPos.x - previousPos.x;
         double dz = playerPos.z - previousPos.z;
         moving = mc.player.isOnGround() && dx * dx + dz * dz > 1.0E-6D;
      }
      this.spawnDrops(playerPos, moving, now);
      this.lastPos = playerPos;
      this.drops.removeIf(drop -> now - drop.spawnMs >= drop.delayMs + drop.burstMs + drop.riseMs);
      this.applyPost(event, now);
   };

   @Override
   public void onEnable() {
      this.drops.clear();
      this.lastPos = mc.player == null ? null : mc.player.getEntityPos();
      this.lastSpawnPos = null;
      this.nextBurstMs = 0L;
      this.seq = 0L;
      this.pipeline.reset();
   }

   @Override
   public void onDisable() {
      this.drops.clear();
      this.lastPos = null;
      this.lastSpawnPos = null;
      this.pipeline.close();
   }

   private void spawnDrops(Vec3d pos, boolean moving, long now) {
      if (!moving) {
         return;
      }

      float spacing = MathHelper.clamp(1.5F / Math.max(1.0F, this.density.getCurrentValue()), 0.1F, 1.5F);
      if (this.lastSpawnPos != null && this.lastSpawnPos.squaredDistanceTo(pos) < spacing * spacing) {
         return;
      }
      this.lastSpawnPos = pos;

      long seed = this.seq++;
      long minStep = (long)(1000.0F / Math.max(1.0F, this.spawnRate.getCurrentValue()));
      long burstAt = Math.max(now + (long)(40.0F + 160.0F * hashf(seed + 2L)), this.nextBurstMs);
      if (burstAt - now > 350L) {
         return;
      }
      this.nextBurstMs = burstAt + (long)(minStep * (0.85F + 0.3F * hashf(seed + 11L)));

      Drop drop = new Drop();
      drop.x = pos.x + (hashf(seed) - 0.5F) * 0.9F;
      drop.y = pos.y + 0.02D;
      drop.z = pos.z + (hashf(seed + 1L) - 0.5F) * 0.9F;
      drop.spawnMs = now;
      drop.delayMs = burstAt - now;
      drop.burstMs = 320.0F * (0.8F + 0.4F * hashf(seed + 3L));
      drop.riseMs = this.lifetime.getCurrentValue() * (0.85F + 0.3F * hashf(seed + 4L));
      drop.dropR = this.blobSize.getCurrentValue() * (0.75F + 0.5F * hashf(seed + 6L));
      drop.colorIndex = (int)(seed * 30L % 360L);
      drop.riseMul = 0.85F + hashf(seed + 7L) * 0.35F;
      drop.wobAmp = 0.1F + hashf(seed + 8L) * 0.12F;
      drop.wobSpeed = 1.6F + hashf(seed + 9L) * 1.8F;
      drop.phase = hashf(seed + 10L) * ((float)Math.PI * 2.0F);
      this.drops.add(drop);
      while (this.drops.size() > MAX_DROPS) {
         this.drops.remove(0);
      }
   }

   private void applyPost(Render3DEvent event, long now) {
      if (this.drops.isEmpty() || mc.getFramebuffer() == null) {
         return;
      }

      Framebuffer framebuffer = mc.getFramebuffer();
      Camera camera = event.getCamera();
      Vec3d cam = camera.getCameraPos();
      float far = Math.max(192.0F, (mc.options.getViewDistance().getValue() + 1) * 16.0F);
      float riseH = this.riseHeight.getCurrentValue();

      Arrays.fill(this.data, 0.0F);
      this.viewProj.set(event.getProjectionMatrix()).mul(event.getPositionMatrix());

      int count = 0;
      for (int i = this.drops.size() - 1; i >= 0 && count < MAX_DROPS; i--) {
         Drop drop = this.drops.get(i);
         float age = now - drop.spawnMs;
         if (age < drop.delayMs) {
            continue;
         }

         float rx;
         float ry;
         float h;
         float env;
         float wobbleT;
         if (age < drop.delayMs + drop.burstMs) {
            float b = smooth((age - drop.delayMs) / drop.burstMs);
            rx = drop.dropR * b;
            ry = rx * (0.55F + 0.3F * b);
            h = ry * 0.55F;
            env = smooth(Math.min(b * 2.0F, 1.0F));
            wobbleT = 0.15F * b;
         } else {
            float r = (age - drop.delayMs - drop.burstMs) / drop.riseMs;
            rx = drop.dropR * (1.0F - 0.45F * r);
            float stretch = (float)Math.sin(Math.PI * smooth(Math.min(r * 1.2F, 1.0F)));
            ry = rx * (0.85F + 1.15F * stretch);
            h = drop.dropR * 0.85F * 0.55F + riseH * drop.riseMul * (float)Math.pow(r, 1.3D);
            env = 1.0F - smooth(MathHelper.clamp((r - 0.72F) / 0.28F, 0.0F, 1.0F));
            wobbleT = 0.15F + 0.85F * r;
         }

         if (env <= 0.01F || rx <= 0.01F) {
            continue;
         }

         float wobT = now * 0.001F;
         float wx = (float)(Math.sin(wobT * drop.wobSpeed + drop.phase) * drop.wobAmp * wobbleT);
         float wz = (float)(Math.cos(wobT * drop.wobSpeed * 0.83F + drop.phase * 1.7F) * drop.wobAmp * wobbleT);
         float rxS = rx * RADIUS_SCALE;
         float ryS = ry * RADIUS_SCALE;
         count = this.packEllipsoid(
            count,
            cam,
            drop.x + wx,
            drop.y + h,
            drop.z + wz,
            rxS,
            ryS,
            rxS,
            env,
            Math.max(rxS, ryS) * 0.55F,
            drop.colorIndex,
            drop.phase,
            0.1F,
            1.0F,
            far
         );
      }

      if (count == 0) {
         return;
      }

      this.data[0] = count;
      this.data[1] = (float)framebuffer.textureWidth / (float)Math.max(1, framebuffer.textureHeight);
      this.data[2] = (now % 100000L) / 1000.0F;
      this.data[3] = 0.016F * this.distort.getCurrentValue();
      this.data[4] = this.rim.getCurrentValue();
      this.data[5] = MathHelper.clamp(this.tintStrength.getCurrentValue() / 100.0F, 0.0F, 1.0F);
      this.data[6] = 0.008F * this.ripple.getCurrentValue();
      this.data[7] = 1.0F;
      this.data[8] = NEAR;
      this.data[9] = far;
      this.data[10] = this.chroma.getCurrentValue() * 0.6F;
      this.data[11] = 0.0F;
      this.pipeline.apply(framebuffer, this.data);
   }

   private void renderWorldDrops(Render3DEvent event, long now) {
      if (this.drops.isEmpty() || event.getCamera() == null) {
         return;
      }

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, FeverVisual.id("textures/bloom.png"));
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(GL11.GL_LEQUAL);
      RenderSystem.depthMask(false);
      RenderSystem.disableCull();

      BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      boolean hasVertices = false;
      for (Drop drop : this.drops) {
         float age = now - drop.spawnMs;
         if (age < drop.delayMs) {
            continue;
         }

         float rx;
         float ry;
         float h;
         float env;
         if (age < drop.delayMs + drop.burstMs) {
            float b = smooth((age - drop.delayMs) / drop.burstMs);
            rx = drop.dropR * b;
            ry = rx * (0.55F + 0.3F * b);
            h = ry * 0.55F;
            env = smooth(Math.min(b * 2.0F, 1.0F));
         } else {
            float r = (age - drop.delayMs - drop.burstMs) / drop.riseMs;
            rx = drop.dropR * (1.0F - 0.45F * r);
            float stretch = (float)Math.sin(Math.PI * smooth(Math.min(r * 1.2F, 1.0F)));
            ry = rx * (0.85F + 1.15F * stretch);
            h = drop.dropR * 0.45F + this.riseHeight.getCurrentValue() * drop.riseMul * (float)Math.pow(r, 1.25D);
            env = 1.0F - smooth(MathHelper.clamp((r - 0.72F) / 0.28F, 0.0F, 1.0F));
         }

         if (env <= 0.01F || rx <= 0.01F) {
            continue;
         }

         float wobT = now * 0.001F;
         float wx = (float)(Math.sin(wobT * drop.wobSpeed + drop.phase) * drop.wobAmp);
         float wz = (float)(Math.cos(wobT * drop.wobSpeed * 0.83F + drop.phase * 1.7F) * drop.wobAmp);
         ColorRGBA color = Colors.getAccentColor(drop.colorIndex + age * 0.08F).mix(this.tint.getColorSafe(), MathHelper.clamp(this.tintStrength.getCurrentValue() / 100.0F, 0.0F, 1.0F) * 0.45F);
         float alpha = (70.0F + this.rim.getCurrentValue() * 55.0F) * env;
         Vec3d center = new Vec3d(drop.x + wx, drop.y + h, drop.z + wz);
         float sx = rx * RADIUS_SCALE * (1.45F + this.distort.getCurrentValue() * 0.14F);
         float sy = ry * RADIUS_SCALE * (1.45F + this.ripple.getCurrentValue() * 0.10F);
         drawWorldPlane(event.getMatrices(), buffer, center, sx, sy, 0.0F, 0.0F, color.withAlpha(alpha * 0.72F));
         drawWorldPlane(event.getMatrices(), buffer, center, sx, sy, 90.0F, 0.0F, color.withAlpha(alpha * 0.72F));
         drawWorldPlane(event.getMatrices(), buffer, center.add(0.0D, -sy * 0.42D, 0.0D), sx * 0.92F, sx * 0.92F, 0.0F, 90.0F, color.withAlpha(alpha * 0.42F));
         hasVertices = true;
      }

      BuiltBuffer built = buffer.endNullable();
      if (hasVertices && built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }

      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   private static void drawWorldPlane(MatrixStack matrices, BufferBuilder buffer, Vec3d center, float halfWidth, float halfHeight, float yaw, float pitch, ColorRGBA color) {
      matrices.push();
      RenderUtility.prepareMatrices(matrices, center);
      if (yaw != 0.0F) {
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
      }
      if (pitch != 0.0F) {
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
      }
      DrawUtility.drawImage(matrices, buffer, -halfWidth, -halfHeight, 0.0D, halfWidth * 2.0F, halfHeight * 2.0F, color);
      matrices.pop();
   }

   private int packEllipsoid(int count, Vec3d cam, double wx, double wy, double wz, float ax, float ay, float az, float envIn, float halfExtent, int colorIndex, float phase, float wobble, float shine, float far) {
      float relX = (float)(wx - cam.x);
      float relY = (float)(wy - cam.y);
      float relZ = (float)(wz - cam.z);
      float env = envIn * nearFade(relX, relY, relZ, halfExtent);
      if (env <= 0.01F || !this.projectCenter(relX, relY, relZ, far)) {
         return count;
      }

      float cu = this.centerU;
      float cv = this.centerV;
      float lin = this.centerLin;
      float e1x = this.axisU(ax, 0.0F, 0.0F);
      float e1y = this.axisV;
      float e2x = this.axisU(0.0F, ay, 0.0F);
      float e2y = this.axisV;
      float e3x = this.axisU(0.0F, 0.0F, az);
      float e3y = this.axisV;
      float bxx = e1x * e1x + e2x * e2x + e3x * e3x;
      float bxy = e1x * e1y + e2x * e2y + e3x * e3y;
      float byy = e1y * e1y + e2y * e2y + e3y * e3y;
      float mid = 0.5F * (bxx + byy);
      float disc = (float)Math.sqrt(0.25F * (bxx - byy) * (bxx - byy) + bxy * bxy);
      float l1sq = mid + disc;
      float l2sq = Math.max(mid - disc, 1.0E-12F);
      float l1 = (float)Math.sqrt(l1sq);
      float l2 = (float)Math.sqrt(l2sq);
      if (l1 < 1.0E-5F || l2 < 1.0E-5F) {
         return count;
      }
      env *= sizeFade(l1, l2);
      if (env <= 0.01F || !cullCenter(cu, cv, l1, l2)) {
         return count;
      }

      float vx;
      float vy;
      if (Math.abs(bxy) > 1.0E-10F) {
         vx = bxy;
         vy = l1sq - bxx;
      } else if (bxx >= byy) {
         vx = 1.0F;
         vy = 0.0F;
      } else {
         vx = 0.0F;
         vy = 1.0F;
      }
      float vl = (float)Math.sqrt(vx * vx + vy * vy);
      if (vl < 1.0E-12F) {
         vx = 1.0F;
         vy = 0.0F;
         vl = 1.0F;
      }
      vx /= vl;
      vy /= vl;

      int base = 16 + count * 16;
      this.data[base] = cu;
      this.data[base + 1] = cv;
      this.data[base + 2] = lin;
      this.data[base + 3] = env;
      this.data[base + 4] = vx * l1;
      this.data[base + 5] = vy * l1;
      this.data[base + 6] = -vy * l2;
      this.data[base + 7] = vx * l2;
      this.data[base + 8] = 0.0F;
      this.data[base + 9] = phase;
      this.data[base + 10] = wobble;
      this.data[base + 11] = shine;
      ColorRGBA color = Colors.getAccentColor(colorIndex).mix(this.tint.getColorSafe(), 0.35F);
      this.data[base + 12] = color.getRed() / 255.0F;
      this.data[base + 13] = color.getGreen() / 255.0F;
      this.data[base + 14] = color.getBlue() / 255.0F;
      return count + 1;
   }

   private boolean projectCenter(float relX, float relY, float relZ, float far) {
      this.centerClip.set(relX, relY, relZ, 1.0F);
      this.viewProj.transform(this.centerClip);
      if (this.centerClip.w <= 0.05F) {
         return false;
      }
      float iw = 1.0F / this.centerClip.w;
      this.centerU = this.centerClip.x * iw * 0.5F + 0.5F;
      this.centerV = this.centerClip.y * iw * 0.5F + 0.5F;
      this.centerLin = linDepth(this.centerClip.z * iw * 0.5F + 0.5F, NEAR, far);
      return true;
   }

   private float axisU(float ax, float ay, float az) {
      this.axisClip.set(ax, ay, az, 0.0F);
      this.viewProj.transform(this.axisClip);
      float iw2 = 1.0F / (this.centerClip.w * this.centerClip.w);
      float u = 0.5F * (this.axisClip.x * this.centerClip.w - this.centerClip.x * this.axisClip.w) * iw2;
      this.axisV = 0.5F * (this.axisClip.y * this.centerClip.w - this.centerClip.y * this.axisClip.w) * iw2;
      return u;
   }

   private static boolean cullCenter(float cu, float cv, float l1, float l2) {
      float ext = (l1 + l2) * 1.5F + 0.05F;
      return cu >= -ext && cu <= 1.0F + ext && cv >= -ext && cv <= 1.0F + ext;
   }

   private static float nearFade(float relX, float relY, float relZ, float halfExtent) {
      float dist = (float)Math.sqrt(relX * relX + relY * relY + relZ * relZ) - halfExtent;
      return smooth(MathHelper.clamp((dist - 0.6F) / 0.75F, 0.0F, 1.0F));
   }

   private static float sizeFade(float l1, float l2) {
      float largest = Math.max(l1, l2);
      return 1.0F - smooth(MathHelper.clamp((largest - 0.5F) / 0.35F, 0.0F, 1.0F));
   }

   private static float linDepth(float depth, float near, float far) {
      float z = depth * 2.0F - 1.0F;
      return 2.0F * near * far / (far + near - z * (far - near));
   }

   private static float smooth(float x) {
      x = MathHelper.clamp(x, 0.0F, 1.0F);
      return x * x * (3.0F - 2.0F * x);
   }

   private static float hashf(long value) {
      long n = value;
      n = (n ^ n >>> 33) * -336448155523654707L;
      n ^= n >>> 33;
      return (float)(n >>> 8 & 0xFFFFFFL) / 1.6777216E7F;
   }

   private static final class Drop {
      double x;
      double y;
      double z;
      long spawnMs;
      float delayMs;
      float burstMs;
      float riseMs;
      float dropR;
      int colorIndex;
      float riseMul;
      float wobAmp;
      float wobSpeed;
      float phase;
   }
}
