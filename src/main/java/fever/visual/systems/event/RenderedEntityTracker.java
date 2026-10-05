package fever.visual.systems.event;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.Entity;

/** Entities accepted by Minecraft's frustum/entity renderer in the current world frame. */
public final class RenderedEntityTracker {
   private static final Set<Integer> RENDERED = new HashSet<>();

   private RenderedEntityTracker() {
   }

   public static void beginFrame() {
      RENDERED.clear();
   }

   public static void mark(Entity entity) {
      if (entity != null) {
         RENDERED.add(entity.getId());
      }
   }

   public static boolean contains(Entity entity) {
      return entity != null && RENDERED.contains(entity.getId());
   }
}
