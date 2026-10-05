package fever.visual.mixin.minecraft.client.render.debug;

import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
   private static final int VANILLA_LOOK_VECTOR_COLOR = -16776961;

   @Redirect(
      method = "drawHitbox",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/debug/gizmo/GizmoDrawing;arrow(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;I)Lnet/minecraft/world/debug/gizmo/VisibilityConfigurable;"),
      require = 0
   )
   private VisibilityConfigurable fevervisual$hideVanillaLookVector(Vec3d start, Vec3d end, int color) {
      if (color == VANILLA_LOOK_VECTOR_COLOR) {
         return null;
      }

      return GizmoDrawing.arrow(start, end, color);
   }
}
