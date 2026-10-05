package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.network.ReceivePacketEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.TestikRender3D;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "Totem Angle", category = ModuleCategory.VISUALS, desc = "modules.descriptions.totem_angle")
public class TotemAngle extends BaseModule implements IMinecraft {
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.totem_angle.mode", "modules.settings.totem_angle.mode.description");
   private final ModeSetting.Value angel = new ModeSetting.Value(this.mode, "modules.settings.totem_angle.mode.angel").select();
   private final ModeSetting.Value shatter = new ModeSetting.Value(this.mode, "modules.settings.totem_angle.mode.shatter");
   private final SliderSetting riseHeight = new SliderSetting(this, "modules.settings.totem_angle.rise", "modules.settings.totem_angle.rise.description")
           .min(0.2F).max(5.0F).step(0.1F).currentValue(4.0F);
   private final SliderSetting duration = new SliderSetting(this, "modules.settings.totem_angle.duration", "modules.settings.totem_angle.duration.description")
           .min(0.2F).max(6.0F).step(0.1F).currentValue(3.0F);
   private final BooleanSetting showSelf = new BooleanSetting(this, "modules.settings.totem_angle.self", "modules.settings.totem_angle.self.description").enabled(false);
   private final SliderSetting brightness = new SliderSetting(this, "modules.settings.totem_angle.brightness", "modules.settings.totem_angle.brightness.description")
           .min(0.2F).max(3.0F).step(0.1F).currentValue(1.0F);
   private final SliderSetting transparency = new SliderSetting(this, "modules.settings.totem_angle.alpha", "modules.settings.totem_angle.alpha.description")
           .min(0.05F).max(1.0F).step(0.05F).currentValue(0.72F);
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.totem_angle.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.totem_angle.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.totem_angle.color_mode.custom");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.totem_angle.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(179, 140, 255, 255))
           .alpha(true);
   private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.totem_angle.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(100, 160, 255, 255))
           .alpha(true);

   private final Deque<GhostFrame> ghosts = new ArrayDeque<>();
   private final Deque<ShatterFrame> shatters = new ArrayDeque<>();

   private final EventListener<ReceivePacketEvent> onPacket = event -> {
      if (mc.world == null || !(event.getPacket() instanceof EntityStatusS2CPacket packet) || packet.getStatus() != 35) {
         return;
      }

      Entity entity = packet.getEntity(mc.world);
      if (!(entity instanceof LivingEntity living)) {
         return;
      }

      if (!this.showSelf.isEnabled() && mc.player != null && living.getId() == mc.player.getId()) {
         return;
      }

      if (this.shatter.isSelected()) {
         this.shatters.addLast(createShatterFrame(living));
         while (this.shatters.size() > 48) {
            this.shatters.pollFirst();
         }
      } else {
         this.ghosts.addLast(createGhostFrame(living));
         while (this.ghosts.size() > 48) {
            this.ghosts.pollFirst();
         }
      }
   };

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (mc.world == null) {
         clear();
         return;
      }

      MatrixStack matrices = event.getMatrices();
      Vec3d cameraPos = event.getCamera().getCameraPos();
      TestikRender3D.setup(true);
      try {
         renderGhosts(matrices, cameraPos, System.currentTimeMillis());
         renderShatters(matrices, cameraPos, System.currentTimeMillis());
      } finally {
         TestikRender3D.reset();
      }
   };

   @Override
   public void onDisable() {
      clear();
   }

   private void clear() {
      this.ghosts.clear();
      this.shatters.clear();
   }

   private void renderGhosts(MatrixStack matrices, Vec3d cameraPos, long now) {
      float lifeMs = this.duration.getCurrentValue() * 1000.0F;
      float rise = this.riseHeight.getCurrentValue();
      float bright = MathHelper.clamp(this.brightness.getCurrentValue(), 0.2F, 3.0F);
      float alphaSetting = MathHelper.clamp(this.transparency.getCurrentValue(), 0.05F, 1.0F);

      Iterator<GhostFrame> iterator = this.ghosts.iterator();
      while (iterator.hasNext()) {
         GhostFrame frame = iterator.next();
         float t = (now - frame.startTime()) / Math.max(1.0F, lifeMs);
         if (t >= 1.0F) {
            iterator.remove();
            continue;
         }

         double up = rise * easeOut(MathHelper.clamp(t, 0.0F, 0.85F));
         float alpha = MathHelper.clamp((float)easeOutAlpha(t) * bright * alphaSetting, 0.0F, 0.95F);
         ColorRGBA baseColor = this.getTotemColor(t * 360.0F);
         ColorRGBA outline = baseColor.withAlpha(255.0F * alpha);
         ColorRGBA fill = baseColor.withAlpha(45.0F * alpha);
         Vec3d origin = frame.pos().add(0.0D, up, 0.0D).subtract(cameraPos);
         drawGhostModel(matrices, origin, frame.bodyYaw(), frame.width(), frame.height(), fill, outline);
      }
   }

   private void renderShatters(MatrixStack matrices, Vec3d cameraPos, long now) {
      float lifeMs = this.duration.getCurrentValue() * 1000.0F;
      float power = Math.max(0.2F, this.riseHeight.getCurrentValue() * 0.35F);
      float bright = MathHelper.clamp(this.brightness.getCurrentValue(), 0.2F, 3.0F);
      float alphaSetting = MathHelper.clamp(this.transparency.getCurrentValue(), 0.05F, 1.0F);

      Iterator<ShatterFrame> iterator = this.shatters.iterator();
      while (iterator.hasNext()) {
         ShatterFrame frame = iterator.next();
         float t = (now - frame.startTime()) / Math.max(1.0F, lifeMs);
         if (t >= 1.0F) {
            iterator.remove();
            continue;
         }

         double elapsed = (now - frame.startTime()) / 1000.0D;
         float alpha = MathHelper.clamp((float)alphaProfile(t) * bright * alphaSetting, 0.0F, 0.95F);
         ColorRGBA baseColor = this.getTotemColor(t * 360.0F);
         ColorRGBA outline = baseColor.withAlpha(255.0F * alpha);
         ColorRGBA fill = baseColor.withAlpha(42.0F * alpha);

         for (PartMotion part : frame.parts()) {
            double side = part.side() + part.velSide() * elapsed * power;
            double up = part.up() + part.velUp() * elapsed * power - 2.9D * elapsed * elapsed;
            double front = part.front() + part.velFront() * elapsed * power;
            Vec3d center = transformLocal(frame.pos(), frame.bodyYaw(), side, up, front).subtract(cameraPos);
            drawPartBox(matrices, center, frame.width(), frame.height(), part.id(), fill, outline);
         }
      }
   }

   private GhostFrame createGhostFrame(LivingEntity entity) {
      return new GhostFrame(
              new Vec3d(entity.getX(), entity.getY(), entity.getZ()),
              entity.bodyYaw,
              Math.max(entity.getWidth(), 0.35F),
              Math.max(entity.getHeight(), 0.6F),
              System.currentTimeMillis()
      );
   }

   private ShatterFrame createShatterFrame(LivingEntity entity) {
      float width = Math.max(entity.getWidth(), 0.35F);
      float height = Math.max(entity.getHeight(), 0.6F);
      double wScale = width / 0.6D;
      double hScale = height / 1.95D;
      ThreadLocalRandom random = ThreadLocalRandom.current();
      PartMotion[] parts = new PartMotion[PartId.values().length];
      parts[PartId.HEAD.ordinal()] = part(0.0D, 1.60D * hScale, 0.0D, randomSigned(random, 0.22D), 0.32D, randomSigned(random, 0.22D), PartId.HEAD);
      parts[PartId.BODY.ordinal()] = part(0.0D, 1.08D * hScale, 0.0D, randomSigned(random, 0.12D), 0.20D, randomSigned(random, 0.12D), PartId.BODY);
      parts[PartId.ARM_L.ordinal()] = part(-0.48D * wScale, 1.15D * hScale, 0.0D, -0.22D, 0.17D, randomSigned(random, 0.12D), PartId.ARM_L);
      parts[PartId.ARM_R.ordinal()] = part(0.48D * wScale, 1.15D * hScale, 0.0D, 0.22D, 0.17D, randomSigned(random, 0.12D), PartId.ARM_R);
      parts[PartId.LEG_L.ordinal()] = part(-0.16D * wScale, 0.42D * hScale, 0.0D, -0.10D, 0.10D, randomSigned(random, 0.06D), PartId.LEG_L);
      parts[PartId.LEG_R.ordinal()] = part(0.16D * wScale, 0.42D * hScale, 0.0D, 0.10D, 0.10D, randomSigned(random, 0.06D), PartId.LEG_R);
      return new ShatterFrame(new Vec3d(entity.getX(), entity.getY(), entity.getZ()), entity.bodyYaw, width, height, System.currentTimeMillis(), parts);
   }

   private PartMotion part(double side, double up, double front, double velSide, double velUp, double velFront, PartId id) {
      return new PartMotion(side, up, front, velSide, velUp, velFront, id);
   }

   private void drawGhostModel(MatrixStack matrices, Vec3d origin, float bodyYaw, float width, float height, ColorRGBA fill, ColorRGBA outline) {
      double wScale = width / 0.6D;
      double hScale = height / 1.95D;
      double depth = Math.max(0.28D, width * 0.40D);
      drawLocalBox(matrices, origin, bodyYaw, 0.0D, 1.70D * hScale, 0.0D, 0.50D * wScale, 0.50D * hScale, 0.50D * wScale, fill, outline);
      drawLocalBox(matrices, origin, bodyYaw, 0.0D, 1.18D * hScale, 0.0D, 0.60D * wScale, 0.55D * hScale, depth, fill, outline);
      drawLocalBox(matrices, origin, bodyYaw, -0.48D * wScale, 1.18D * hScale, 0.02D * wScale, 0.22D * wScale, 0.50D * hScale, 0.24D * wScale, fill, outline);
      drawLocalBox(matrices, origin, bodyYaw, 0.48D * wScale, 1.18D * hScale, 0.02D * wScale, 0.22D * wScale, 0.50D * hScale, 0.24D * wScale, fill, outline);
      drawLocalBox(matrices, origin, bodyYaw, -0.16D * wScale, 0.42D * hScale, 0.0D, 0.26D * wScale, 0.90D * hScale, 0.24D * wScale, fill, outline);
      drawLocalBox(matrices, origin, bodyYaw, 0.16D * wScale, 0.42D * hScale, 0.0D, 0.26D * wScale, 0.90D * hScale, 0.24D * wScale, fill, outline);
   }

   private void drawPartBox(MatrixStack matrices, Vec3d center, float width, float height, PartId partId, ColorRGBA fill, ColorRGBA outline) {
      double wScale = width / 0.6D;
      double hScale = height / 1.95D;
      double boxW = switch (partId) {
         case HEAD -> 0.50D * wScale;
         case BODY -> 0.60D * wScale;
         case ARM_L, ARM_R -> 0.22D * wScale;
         case LEG_L, LEG_R -> 0.26D * wScale;
      };
      double boxH = switch (partId) {
         case HEAD -> 0.50D * hScale;
         case BODY -> 0.55D * hScale;
         case ARM_L, ARM_R -> 0.50D * hScale;
         case LEG_L, LEG_R -> 0.90D * hScale;
      };
      double boxD = partId == PartId.BODY ? Math.max(0.28D, width * 0.40D) : boxW;
      TestikRender3D.drawBox(matrices, centeredBox(center, boxW, boxH, boxD), fill, outline);
   }

   private void drawLocalBox(MatrixStack matrices, Vec3d origin, float bodyYaw, double side, double up, double front, double sizeX, double sizeY, double sizeZ, ColorRGBA fill, ColorRGBA outline) {
      Vec3d center = transformLocal(origin, bodyYaw, side, up, front);
      TestikRender3D.drawBox(matrices, centeredBox(center, sizeX, sizeY, sizeZ), fill, outline);
   }

   private Box centeredBox(Vec3d center, double width, double height, double depth) {
      return new Box(center.x - width * 0.5D, center.y - height * 0.5D, center.z - depth * 0.5D, center.x + width * 0.5D, center.y + height * 0.5D, center.z + depth * 0.5D);
   }

   private Vec3d transformLocal(Vec3d origin, float bodyYaw, double side, double up, double front) {
      float radians = (float)Math.toRadians(bodyYaw);
      Vec3d forward = new Vec3d(-MathHelper.sin(radians), 0.0D, MathHelper.cos(radians));
      Vec3d right = new Vec3d(MathHelper.cos(radians), 0.0D, MathHelper.sin(radians));
      return origin.add(right.multiply(side)).add(0.0D, up, 0.0D).add(forward.multiply(front));
   }

   private double easeOut(double t) {
      double inv = 1.0D - t;
      return 1.0D - inv * inv * inv;
   }

   private double easeOutAlpha(double t) {
      double inv = 1.0D - t;
      return 0.75D * (1.0D - inv * inv * inv);
   }

   private double alphaProfile(double t) {
      double in = MathHelper.clamp(t / 0.10D, 0.0D, 1.0D);
      double out = MathHelper.clamp((t - 0.65D) / 0.35D, 0.0D, 1.0D);
      return 0.85D * (1.0D - Math.pow(1.0D - in, 3.0D)) * (1.0D - out);
   }

   private double randomSigned(ThreadLocalRandom random, double range) {
      return random.nextDouble(-range, range);
   }

   private ColorRGBA getTotemColor(float index) {
      if (this.colorCustom.isSelected()) {
         float normalized = (index % 360.0F) / 180.0F;
         float mix = normalized > 1.0F ? 2.0F - normalized : normalized;
         return this.color.getColorSafe().mix(this.colorSecond.getColorSafe(), mix);
      }

      return Colors.getAccentColor(index);
   }

   private record GhostFrame(Vec3d pos, float bodyYaw, float width, float height, long startTime) {
   }

   private record ShatterFrame(Vec3d pos, float bodyYaw, float width, float height, long startTime, PartMotion[] parts) {
   }

   private record PartMotion(double side, double up, double front, double velSide, double velUp, double velFront, PartId id) {
   }

   private enum PartId {
      HEAD,
      BODY,
      ARM_L,
      ARM_R,
      LEG_L,
      LEG_R
   }
}
