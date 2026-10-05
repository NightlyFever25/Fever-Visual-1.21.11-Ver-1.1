package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;
import fever.visual.utility.render.compat.GlStateManager;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.EntityDeathEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "Model Collapse", category = ModuleCategory.VISUALS, desc = "Entity model collapse")
public class ModelCollapse extends BaseModule {
   private final ModeSetting breakMode = new ModeSetting(this, "Break Direction");
   private final ModeSetting.Value bottomUp = new ModeSetting.Value(this.breakMode, "Bottom Up").select();
   private final ModeSetting.Value topDown = new ModeSetting.Value(this.breakMode, "Top Down");
   private final SliderSetting waveTime = new SliderSetting(this, "Wave")
           .min(100.0F).max(1000.0F).step(25.0F).currentValue(350.0F);
   private final SliderSetting cubeSize = new SliderSetting(this, "Cube Size")
           .min(0.03F).max(0.12F).step(0.005F).currentValue(0.055F);
   private final SliderSetting impulse = new SliderSetting(this, "Impulse")
           .min(0.0F).max(3.0F).step(0.05F).currentValue(1.0F);
   private final SliderSetting lifeTime = new SliderSetting(this, "Lifetime")
           .min(1.0F).max(15.0F).step(0.5F).currentValue(6.0F);

   private final List<Collapse> collapses = new CopyOnWriteArrayList<>();

   private final EventListener<EntityDeathEvent> onDeath = event -> {
      LivingEntity entity = event.getEntity();
      if (entity == null || entity == mc.player || this.isInvisible(entity)) {
         return;
      }
      this.collapses.add(Collapse.create(entity, this.cubeSize.getCurrentValue()));
      while (this.collapses.size() > 8) {
         this.collapses.remove(0);
      }
   };

   private final EventListener<Render3DEvent> onRender = event -> {
      if (this.collapses.isEmpty()) {
         return;
      }
      MatrixStack matrices = event.getMatrices();
      Vec3d cameraPos = event.getCamera().getCameraPos();
      long now = System.currentTimeMillis();
      float lifeMs = this.lifeTime.getCurrentValue() * 1000.0F;
      float force = this.impulse.getCurrentValue();
      float waveMs = this.waveTime.getCurrentValue();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(GL11.GL_LEQUAL);
      RenderSystem.depthMask(false);
      RenderSystem.disableCull();
      try {
         for (Collapse collapse : this.collapses) {
            float t = (now - collapse.startedAt) / Math.max(1.0F, lifeMs);
            if (t >= 1.0F) {
               this.collapses.remove(collapse);
               continue;
            }
            collapse.render(matrices, cameraPos, MathHelper.clamp(t, 0.0F, 1.0F), lifeMs, waveMs, force, this.topDown.isSelected());
         }
      } finally {
         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   };

   @Override
   public void onDisable() {
      this.collapses.clear();
   }

   private boolean isInvisible(LivingEntity entity) {
      return entity.hasStatusEffect(StatusEffects.INVISIBILITY)
              || entity.isInvisible()
              || mc.player != null && entity.isInvisibleTo(mc.player);
   }

   private static final class Collapse {
      private final Vec3d pos;
      private final float bodyYaw;
      private final float width;
      private final float height;
      private final Identifier texture;
      private final Part[] parts;
      private final long startedAt = System.currentTimeMillis();

      private Collapse(Vec3d pos, float bodyYaw, float width, float height, Identifier texture, Part[] parts) {
         this.pos = pos;
         this.bodyYaw = bodyYaw;
         this.width = width;
         this.height = height;
         this.texture = texture;
         this.parts = parts;
      }

      static Collapse create(LivingEntity entity, float cubeSize) {
         float width = Math.max(entity.getWidth(), 0.35F);
         float height = Math.max(entity.getHeight(), 0.6F);
         Identifier texture = resolveTexture(entity);
         ThreadLocalRandom random = ThreadLocalRandom.current();
         List<Part> generated = new ArrayList<>();
         addCuboid(generated, random, new Vec3d(0.0D, 1.68D, 0.0D), new Vec3d(0.50D, 0.50D, 0.50D), width, height, cubeSize, 8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F);
         addCuboid(generated, random, new Vec3d(0.0D, 1.15D, 0.0D), new Vec3d(0.60D, 0.55D, 0.28D), width, height, cubeSize, 20.0F / 64.0F, 20.0F / 64.0F, 28.0F / 64.0F, 32.0F / 64.0F);
         addCuboid(generated, random, new Vec3d(-0.48D, 1.14D, 0.0D), new Vec3d(0.22D, 0.50D, 0.24D), width, height, cubeSize, 44.0F / 64.0F, 20.0F / 64.0F, 48.0F / 64.0F, 32.0F / 64.0F);
         addCuboid(generated, random, new Vec3d(0.48D, 1.14D, 0.0D), new Vec3d(0.22D, 0.50D, 0.24D), width, height, cubeSize, 36.0F / 64.0F, 52.0F / 64.0F, 40.0F / 64.0F, 64.0F / 64.0F);
         addCuboid(generated, random, new Vec3d(-0.16D, 0.43D, 0.0D), new Vec3d(0.26D, 0.90D, 0.24D), width, height, cubeSize, 4.0F / 64.0F, 20.0F / 64.0F, 8.0F / 64.0F, 32.0F / 64.0F);
         addCuboid(generated, random, new Vec3d(0.16D, 0.43D, 0.0D), new Vec3d(0.26D, 0.90D, 0.24D), width, height, cubeSize, 20.0F / 64.0F, 52.0F / 64.0F, 24.0F / 64.0F, 64.0F / 64.0F);
         Part[] parts = generated.toArray(Part[]::new);
         return new Collapse(entity.getEntityPos(), entity.bodyYaw, width, height, texture, parts);
      }

      void render(MatrixStack matrices, Vec3d cameraPos, float t, float lifeMs, float waveMs, float force, boolean topDown) {
         float elapsedMs = t * lifeMs;
         float alpha = MathHelper.clamp((1.0F - t) / 0.18F, 0.0F, 1.0F);
         if (alpha <= 0.01F || this.parts.length == 0) {
            return;
         }
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, this.texture);
         BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         for (int i = 0; i < this.parts.length; i++) {
            Part part = this.parts[i];
            float heightProgress = MathHelper.clamp((float)(part.local.y / Math.max(0.01F, this.height)), 0.0F, 1.0F);
            float releaseMs = waveMs * (topDown ? 1.0F - heightProgress : heightProgress);
            float seconds = Math.max(0.0F, elapsedMs - releaseMs) / 1000.0F;
            Vec3d local = part.local;
            if (seconds > 0.0F) {
               float landingSeconds = landingTime(part, force);
               float simulatedSeconds = Math.min(seconds, landingSeconds);
               local = local.add(part.velocity.multiply(simulatedSeconds * 0.95D * force))
                     .add(0.0D, -5.4D * simulatedSeconds * simulatedSeconds, 0.0D);
            }
            Vec3d world = transform(this.pos, this.bodyYaw, local);
            Vec3d radial = new Vec3d(world.x - this.pos.x, 0.0D, world.z - this.pos.z);
            double maxRadius = Math.max(0.65D, this.width * 1.45D);
            if (radial.lengthSquared() > maxRadius * maxRadius) {
               radial = radial.normalize().multiply(maxRadius);
               world = new Vec3d(this.pos.x + radial.x, world.y, this.pos.z + radial.z);
            }
            double floor = this.pos.y + part.size.y * 0.5D;
            if (world.y < floor) {
               world = new Vec3d(world.x, floor, world.z);
            }
            Vec3d center = world.subtract(cameraPos);
            Vec3d size = part.size.multiply(1.0D - t * 0.10D);
            emitTexturedCube(buffer, matrix, center, size, part.u1, part.v1, part.u2, part.v2, new ColorRGBA(255.0F, 255.0F, 255.0F, 255.0F * alpha));
         }
         BuiltBuffer built = buffer.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }
      }

      private static float landingTime(Part part, float force) {
         double start = Math.max(0.0D, part.local.y - part.size.y * 0.5D);
         double verticalVelocity = part.velocity.y * 0.95D * force;
         double discriminant = verticalVelocity * verticalVelocity + 21.6D * start;
         return (float)Math.max(0.0D, (verticalVelocity + Math.sqrt(Math.max(0.0D, discriminant))) / 10.8D);
      }

      private static void addCuboid(List<Part> parts, ThreadLocalRandom random, Vec3d center, Vec3d size, float width, float height, float cubeSize, float minU, float minV, float maxU, float maxV) {
         double w = width / 0.6D;
         double h = height / 1.95D;
         Vec3d scaledCenter = new Vec3d(center.x * w, center.y * h, center.z * w);
         Vec3d scaledSize = new Vec3d(size.x * w, size.y * h, Math.max(size.z * w, width * 0.16D));
         int xCount = MathHelper.clamp((int)Math.ceil(scaledSize.x / cubeSize), 1, 12);
         int yCount = MathHelper.clamp((int)Math.ceil(scaledSize.y / cubeSize), 1, 20);
         int zCount = MathHelper.clamp((int)Math.ceil(scaledSize.z / cubeSize), 1, 10);
         Vec3d cell = new Vec3d(scaledSize.x / xCount, scaledSize.y / yCount, scaledSize.z / zCount);
         for (int xi = 0; xi < xCount; xi++) {
            for (int yi = 0; yi < yCount; yi++) {
               for (int zi = 0; zi < zCount; zi++) {
                  Vec3d local = scaledCenter.add(
                     -scaledSize.x * 0.5D + cell.x * (xi + 0.5D),
                     -scaledSize.y * 0.5D + cell.y * (yi + 0.5D),
                     -scaledSize.z * 0.5D + cell.z * (zi + 0.5D)
                  );
                  Vec3d outward = new Vec3d(local.x, local.y - scaledCenter.y, local.z);
                  if (outward.lengthSquared() < 1.0E-5D) {
                     outward = new Vec3d(random.nextDouble(-0.5D, 0.5D), random.nextDouble(0.0D, 0.5D), random.nextDouble(-0.5D, 0.5D));
                  }
                  outward = outward.normalize();
                  Vec3d velocity = outward.multiply(random.nextDouble(0.035D, 0.13D)).add(
                     random.nextDouble(-0.025D, 0.025D),
                     random.nextDouble(0.045D, 0.17D),
                     random.nextDouble(-0.025D, 0.025D)
                  );
                  float u1 = MathHelper.lerp((float)xi / xCount, minU, maxU);
                  float u2 = MathHelper.lerp((float)(xi + 1) / xCount, minU, maxU);
                  float v1 = MathHelper.lerp((float)yi / yCount, minV, maxV);
                  float v2 = MathHelper.lerp((float)(yi + 1) / yCount, minV, maxV);
                  parts.add(new Part(local, cell.multiply(0.92D), velocity, random.nextFloat() * 140.0F - 70.0F, u1, v1, u2, v2, random.nextDouble()));
               }
            }
         }
      }

      @SuppressWarnings({"unchecked", "rawtypes"})
      private static Identifier resolveTexture(LivingEntity entity) {
         EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
         if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingRendererRaw) {
            LivingEntityRenderer livingRenderer = livingRendererRaw;
            LivingEntityRenderState state = (LivingEntityRenderState)livingRenderer.createRenderState();
            livingRenderer.updateRenderState(entity, state, mc.getRenderTickCounter().getTickProgress(false));
            return livingRenderer.getTexture(state);
         }
         return Identifier.ofVanilla("textures/entity/player/wide/steve.png");
      }

      private static void emitTexturedCube(BufferBuilder buffer, Matrix4f matrix, Vec3d c, Vec3d s, float u1, float v1, float u2, float v2, ColorRGBA color) {
         float x1 = (float)(c.x - s.x * 0.5D);
         float y1 = (float)(c.y - s.y * 0.5D);
         float z1 = (float)(c.z - s.z * 0.5D);
         float x2 = (float)(c.x + s.x * 0.5D);
         float y2 = (float)(c.y + s.y * 0.5D);
         float z2 = (float)(c.z + s.z * 0.5D);
         int rgba = color.getRGB();
         quad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, u1, v1, u2, v2, rgba);
         quad(buffer, matrix, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, u1, v1, u2, v2, rgba);
         quad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, u1, v1, u2, v2, rgba);
         quad(buffer, matrix, x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2, u1, v1, u2, v2, rgba);
         quad(buffer, matrix, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, u1, v1, u2, v2, rgba);
         quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, u1, v1, u2, v2, rgba);
      }

      private static void quad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, int color) {
         buffer.vertex(matrix, x1, y1, z1).texture(u1, v2).color(color);
         buffer.vertex(matrix, x2, y2, z2).texture(u2, v2).color(color);
         buffer.vertex(matrix, x3, y3, z3).texture(u2, v1).color(color);
         buffer.vertex(matrix, x4, y4, z4).texture(u1, v1).color(color);
      }

      private static Vec3d transform(Vec3d origin, float yaw, Vec3d local) {
         float radians = (float)Math.toRadians(yaw);
         Vec3d forward = new Vec3d(-MathHelper.sin(radians), 0.0D, MathHelper.cos(radians));
         Vec3d right = new Vec3d(MathHelper.cos(radians), 0.0D, MathHelper.sin(radians));
         return origin.add(right.multiply(local.x)).add(0.0D, local.y, 0.0D).add(forward.multiply(local.z));
      }

   }

   private record Part(Vec3d local, Vec3d size, Vec3d velocity, float spin, float u1, float v1, float u2, float v2, double bouncePhase) {
   }
}
