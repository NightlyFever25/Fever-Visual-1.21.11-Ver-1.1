package fever.visual.utility.render;

public final class HitColorRenderContext {
   private static final ThreadLocal<Boolean> HURT_STATE = ThreadLocal.withInitial(() -> Boolean.FALSE);

   private HitColorRenderContext() {
      throw new UnsupportedOperationException("Utility class");
   }

   public static void setHurt(boolean hurt) {
      HURT_STATE.set(hurt);
   }

   public static boolean isHurt() {
      return HURT_STATE.get();
   }

   public static void clear() {
      HURT_STATE.remove();
   }
}
