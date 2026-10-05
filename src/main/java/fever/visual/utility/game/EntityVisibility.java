package fever.visual.utility.game;

import fever.visual.systems.event.RenderedEntityTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class EntityVisibility {
   private static final MinecraftClient MC = MinecraftClient.getInstance();

   private EntityVisibility() {
   }

   public static boolean isRenderedAndVisible(LivingEntity entity, float tickDelta) {
      if (entity == null || MC.player == null || MC.world == null || !RenderedEntityTracker.contains(entity)) {
         return false;
      }
      if (!entity.isAlive() || entity.isRemoved() || entity.hasStatusEffect(StatusEffects.INVISIBILITY)
            || entity.isInvisible() || entity.isInvisibleTo(MC.player)) {
         return false;
      }
      if (entity == MC.player) {
         return true;
      }

      Vec3d from = MC.gameRenderer.getCamera().getCameraPos();
      Vec3d position = entity.getLerpedPos(tickDelta);
      return rayClear(from, position.add(0.0D, entity.getStandingEyeHeight(), 0.0D), entity)
            || rayClear(from, position.add(0.0D, entity.getHeight() * 0.5D, 0.0D), entity);
   }

   private static boolean rayClear(Vec3d from, Vec3d to, LivingEntity context) {
      return MC.world.raycast(new RaycastContext(
            from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, context
      )).getType() == HitResult.Type.MISS;
   }
}
