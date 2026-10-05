package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.network.ReceivePacketEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ButtonSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.render.pipeline.ExplosionWavePipeline;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@ModuleInfo(
   name = "Explosion Wave",
   category = ModuleCategory.VISUALS,
   desc = "Shockwave effect on explosions"
)
public class ExplosionWave extends BaseModule {
   private static final int MAX_WAVES = 8;
   private static ExplosionWave activeInstance;

   private final SliderSetting waveTime = new SliderSetting(this, "Wave Time").min(600.0F).max(3000.0F).step(50.0F).currentValue(2000.0F);
   private final SliderSetting waveRadius = new SliderSetting(this, "Wave Radius").min(5.0F).max(30.0F).step(0.5F).currentValue(15.0F);
   private final SliderSetting maxDistance = new SliderSetting(this, "Distance").min(10.0F).max(96.0F).step(1.0F).currentValue(50.0F);
   private final SliderSetting distortStrength = new SliderSetting(this, "Distortion").min(0.2F).max(3.0F).step(0.1F).currentValue(3.0F);
   private final SliderSetting waveThickness = new SliderSetting(this, "Thickness").min(10.0F).max(60.0F).step(1.0F).currentValue(25.0F);
   private final BooleanSetting chromatic = new BooleanSetting(this, "Chromatic").enable();
   private final SliderSetting chromaticStrength = new SliderSetting(this, "Chromatic Strength", () -> !this.chromatic.isEnabled()).min(0.2F).max(3.0F).step(0.1F).currentValue(1.5F);
   private final BooleanSetting flash = new BooleanSetting(this, "Flash").enable();
   private final SliderSetting flashStrength = new SliderSetting(this, "Flash Strength", () -> !this.flash.isEnabled()).min(10.0F).max(100.0F).step(1.0F).currentValue(50.0F);
   private final BooleanSetting shake = new BooleanSetting(this, "Shake").enable();
   private final SliderSetting shakeStrength = new SliderSetting(this, "Shake Strength", () -> !this.shake.isEnabled()).min(0.2F).max(3.0F).step(0.1F).currentValue(1.5F);
   private final SliderSetting shakeTime = new SliderSetting(this, "Shake Time", () -> !this.shake.isEnabled()).min(200.0F).max(1500.0F).step(50.0F).currentValue(1000.0F);
   private final ButtonSetting test = new ButtonSetting(this, "Test").action(() -> {
      if (mc.player != null) {
         this.queueBlast(mc.player.getEntityPos(), 4.0F);
      }
   });

   private final Queue<PendingBlast> pending = new ArrayDeque<>();
   private final List<Wave> waves = new ArrayList<>();
   private final List<RecentBlast> recentBlasts = new ArrayList<>();
   private final Matrix4f invViewProj = new Matrix4f();
   private final Matrix4f viewProj = new Matrix4f();
   private final Vector4f projScratch = new Vector4f();
   private final float[] uniformScratch = new float[120];
   private final ExplosionWavePipeline pipeline = new ExplosionWavePipeline();

   private final EventListener<ReceivePacketEvent> onPacket = event -> this.onPacket(event.getPacket());
   private final EventListener<Render3DEvent> onRender3D = event -> {
      this.drainPending();
      this.removeExpired();
      this.applyPost(event);
   };

   @Override
   public void onEnable() {
      activeInstance = this;
   }

   @Override
   public void onDisable() {
      this.pending.clear();
      this.waves.clear();
      this.recentBlasts.clear();
      this.pipeline.close();
      if (activeInstance == this) {
         activeInstance = null;
      }
   }

   private void onPacket(Packet<?> packet) {
      if (packet instanceof ExplosionS2CPacket explosion) {
         if (!this.isWindBurst(explosion)) {
            this.queueBlast(explosion.center(), explosion.radius());
         }
         return;
      }
      if (packet instanceof PlaySoundS2CPacket sound) {
         if (sound.getSound() != null && sound.getSound().value() == SoundEvents.ENTITY_GENERIC_EXPLODE.value()) {
            this.queueBlast(new Vec3d(sound.getX(), sound.getY(), sound.getZ()), 4.0F);
         }
         return;
      }
      if (packet instanceof ParticleS2CPacket particles && particles.getParameters() != null) {
         ParticleType<?> type = particles.getParameters().getType();
         if (type == ParticleTypes.EXPLOSION_EMITTER) {
            this.queueBlast(new Vec3d(particles.getX(), particles.getY(), particles.getZ()), 4.0F);
         } else if (type == ParticleTypes.EXPLOSION) {
            this.queueBlast(new Vec3d(particles.getX(), particles.getY(), particles.getZ()), 2.0F);
         }
      }
   }

   private boolean isWindBurst(ExplosionS2CPacket packet) {
      ParticleType<?> type = packet.explosionParticle() == null ? null : packet.explosionParticle().getType();
      return type == ParticleTypes.GUST || type == ParticleTypes.SMALL_GUST || type == ParticleTypes.GUST_EMITTER_SMALL || type == ParticleTypes.GUST_EMITTER_LARGE;
   }

   private void queueBlast(Vec3d pos, float power) {
      if (isFiniteAndSafe(pos)) {
         this.pending.add(new PendingBlast(pos, Math.max(power, 0.6F)));
      }
   }

   private void drainPending() {
      if (mc.player == null) {
         return;
      }
      long now = System.currentTimeMillis();
      this.recentBlasts.removeIf(recent -> now - recent.time() > 400L);
      PendingBlast blast;
      while ((blast = this.pending.poll()) != null) {
         double dist = mc.player.getEntityPos().distanceTo(blast.center());
         float maxDist = this.maxDistance.getCurrentValue();
         if (!isFiniteAndSafe(blast.center()) || dist > maxDist || this.nearRecent(blast.center())) {
            continue;
         }

         float falloff = (float)Math.pow(1.0D - dist / maxDist, 1.5D);
         float power = MathHelper.clamp(blast.power() / 4.0F, 0.6F, 2.5F);
         this.recentBlasts.add(new RecentBlast(blast.center(), now));
         if (this.waves.size() >= MAX_WAVES) {
            this.waves.remove(0);
         }
         this.waves.add(new Wave(blast.center(), power, falloff, now));
      }
   }

   private boolean nearRecent(Vec3d center) {
      for (RecentBlast recent : this.recentBlasts) {
         if (recent.pos().squaredDistanceTo(center) < 12.25D) {
            return true;
         }
      }
      return false;
   }

   private void removeExpired() {
      float life = Math.max(1.0F, this.waveTime.getCurrentValue());
      Iterator<Wave> iterator = this.waves.iterator();
      while (iterator.hasNext()) {
         Wave wave = iterator.next();
         if (wave.progress(life) >= 1.0F || !isFiniteAndSafe(wave.pos())) {
            iterator.remove();
         }
      }
   }

   private void applyPost(Render3DEvent event) {
      if (this.waves.isEmpty() || mc.getFramebuffer() == null || event.getCamera() == null) {
         return;
      }

      Framebuffer framebuffer = mc.getFramebuffer();
      if (framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
         return;
      }

      Matrix4f projectionMatrix = new Matrix4f(event.getProjectionMatrix());
      Matrix4f positionMatrix = new Matrix4f(event.getPositionMatrix());
      Camera camera = event.getCamera();
      float life = Math.max(1.0F, this.waveTime.getCurrentValue());
      float maxRadiusSetting = this.waveRadius.getCurrentValue();
      float baseAmp = 0.03F * this.distortStrength.getCurrentValue();
      float aspect = (float)framebuffer.textureWidth / (float)Math.max(1, framebuffer.textureHeight);
      Vec3d cam = camera.getCameraPos();

      this.invViewProj.set(projectionMatrix).mul(positionMatrix).invert();
      this.viewProj.set(projectionMatrix).mul(positionMatrix);
      float[] data = this.uniformScratch;
      int count = 0;
      float globalFlash = 0.0F;

      for (Wave wave : this.waves) {
         if (count >= MAX_WAVES) {
            break;
         }
         float progress = wave.progress(life);
         if (progress < 0.0F || progress >= 1.0F) {
            continue;
         }

         float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
         float maxRadius = maxRadiusSetting * wave.power();
         float radius = Math.max(eased * maxRadius, 0.05F);
         float thickness = Math.max(0.3F, maxRadius * this.waveThickness.getCurrentValue() / 100.0F);
         float fadeIn = clamp(progress / 0.05F, 0.0F, 1.0F);
         float fadeOut = clamp((1.0F - progress) / 0.4F, 0.0F, 1.0F);
         float env = fadeIn * fadeOut * wave.falloff();
         if (env <= 0.001F) {
            continue;
         }

         float waveFlash = this.flash.isEnabled() ? Math.max(0.0F, 1.0F - progress / 0.12F) * wave.falloff() : 0.0F;
         globalFlash = Math.max(globalFlash, waveFlash * this.flashStrength.getCurrentValue() / 100.0F);
         float cx = (float)(wave.pos().x - cam.x);
         float cy = (float)(wave.pos().y - cam.y);
         float cz = (float)(wave.pos().z - cam.z);
         this.projScratch.set(cx, cy, cz, 1.0F);
         this.viewProj.transform(this.projScratch);

         float sx = 0.0F;
         float sy = 0.0F;
         float valid = 0.0F;
         if (this.projScratch.w > 0.001F) {
            sx = this.projScratch.x / this.projScratch.w * 0.5F + 0.5F;
            sy = this.projScratch.y / this.projScratch.w * 0.5F + 0.5F;
            valid = 1.0F;
         }

         int base = 24 + count * 12;
         data[base] = cx;
         data[base + 1] = cy;
         data[base + 2] = cz;
         data[base + 3] = radius;
         data[base + 4] = thickness;
         data[base + 5] = baseAmp * env;
         data[base + 6] = env;
         data[base + 7] = waveFlash;
         data[base + 8] = sx;
         data[base + 9] = sy;
         data[base + 10] = valid;
         data[base + 11] = (float)Math.sqrt(cx * cx + cy * cy + cz * cz);
         count++;
      }

      if (count == 0) {
         return;
      }

      data[0] = count;
      data[1] = aspect;
      data[2] = this.chromatic.isEnabled() ? 0.35F * this.chromaticStrength.getCurrentValue() : 0.0F;
      data[3] = globalFlash;
      data[4] = 0.7F;
      data[5] = 0.0F;
      data[6] = 0.0F;
      data[7] = 0.0F;
      this.invViewProj.get(data, 8);
      this.pipeline.apply(framebuffer, data);
   }

   public static float getShakeYaw() {
      return activeInstance == null ? 0.0F : activeInstance.currentShake(0.85F);
   }

   public static float getShakePitch() {
      return activeInstance == null ? 0.0F : activeInstance.currentShake(1.35F) * 0.55F;
   }

   private float currentShake(float phase) {
      if (!this.isEnabled() || !this.shake.isEnabled() || this.waves.isEmpty()) {
         return 0.0F;
      }
      long now = System.currentTimeMillis();
      float amount = 0.0F;
      for (Wave wave : this.waves) {
         float progress = wave.progress(Math.max(1.0F, this.shakeTime.getCurrentValue()));
         if (progress < 1.0F) {
            amount += (float)Math.sin((now - wave.startTime()) * 0.045F + phase) * (1.0F - progress) * 3.5F * this.shakeStrength.getCurrentValue() * wave.falloff() * wave.power();
         }
      }
      return MathHelper.clamp(amount, -3.0F, 3.0F);
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static boolean isFiniteAndSafe(Vec3d pos) {
      return pos != null && isFiniteAndSafe(pos.x) && isFiniteAndSafe(pos.y) && isFiniteAndSafe(pos.z);
   }

   private static boolean isFiniteAndSafe(double value) {
      return Double.isFinite(value) && Math.abs(value) <= 3.0E7D;
   }

   private record PendingBlast(Vec3d center, float power) {
   }

   private record RecentBlast(Vec3d pos, long time) {
   }

   private record Wave(Vec3d pos, float power, float falloff, long startTime) {
      float progress(float lifeMs) {
         return (System.currentTimeMillis() - this.startTime) / Math.max(1.0F, lifeMs);
      }
   }
}
