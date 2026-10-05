package fever.visual.utility.render;

import lombok.Generated;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public final class Utils implements IMinecraft {
   private static final Vector3f VIEW_FORWARD = new Vector3f();

   public static boolean isInViewHemisphere(Camera camera, double x, double y, double z, double margin) {
      Vec3d cameraPos = camera.getCameraPos();
      VIEW_FORWARD.set(0.0F, 0.0F, -1.0F);
      camera.getRotation().transform(VIEW_FORWARD);
      double dx = x - cameraPos.x;
      double dy = y - cameraPos.y;
      double dz = z - cameraPos.z;
      return dx * VIEW_FORWARD.x + dy * VIEW_FORWARD.y + dz * VIEW_FORWARD.z >= -margin;
   }

   public static boolean isInViewCone(Camera camera, double x, double y, double z, double cosine, double margin) {
      Vec3d cameraPos = camera.getCameraPos();
      VIEW_FORWARD.set(0.0F, 0.0F, -1.0F);
      camera.getRotation().transform(VIEW_FORWARD);
      double dx = x - cameraPos.x;
      double dy = y - cameraPos.y;
      double dz = z - cameraPos.z;
      double distanceSquared = dx * dx + dy * dy + dz * dz;
      if (distanceSquared < 1.0E-8) {
         return true;
      }
      double dot = dx * VIEW_FORWARD.x + dy * VIEW_FORWARD.y + dz * VIEW_FORWARD.z;
      return dot + margin >= Math.sqrt(distanceSquared) * cosine;
   }

   public static Vec2f worldToScreen(Vec3d worldCoords) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d delta = worldCoords.subtract(camera.getCameraPos());
      VIEW_FORWARD.set(0.0F, 0.0F, -1.0F);
      camera.getRotation().transform(VIEW_FORWARD);
      if (delta.x * VIEW_FORWARD.x + delta.y * VIEW_FORWARD.y + delta.z * VIEW_FORWARD.z <= 0.0) {
         return null;
      }

      Vec3d projected = mc.gameRenderer.project(worldCoords);
      float screenX = (float)((projected.x + 1.0) * 0.5 * mc.getWindow().getScaledWidth());
      float screenY = (float)((1.0 - projected.y) * 0.5 * mc.getWindow().getScaledHeight());
      return new Vec2f(screenX, screenY);
   }

   public static Vec3d getInterpolatedPos(Entity entity, float tickDelta) {
      return new Vec3d(
         MathHelper.lerp(tickDelta, entity.lastX, entity.getX()),
         MathHelper.lerp(tickDelta, entity.lastY, entity.getY()),
         MathHelper.lerp(tickDelta, entity.lastZ, entity.getZ())
      );
   }

   public static Vec3d getInterpolatedPos(Vec3d prev, Vec3d pos, float tickDelta) {
      return new Vec3d(
         MathHelper.lerp(tickDelta, prev.x, pos.getX()), MathHelper.lerp(tickDelta, prev.y, pos.getY()), MathHelper.lerp(tickDelta, prev.z, pos.getZ())
      );
   }

   @Generated
   private Utils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
