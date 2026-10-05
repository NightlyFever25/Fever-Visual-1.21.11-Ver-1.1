package fever.visual.framework.objects;

public record BorderRadius(float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius) {
   public static final BorderRadius ZERO = new BorderRadius(0.0F, 0.0F, 0.0F, 0.0F);
   private static final BorderRadius HALF = new BorderRadius(0.5F, 0.5F, 0.5F, 0.5F);
   private static final BorderRadius THREE_QUARTERS = new BorderRadius(0.75F, 0.75F, 0.75F, 0.75F);
   private static final BorderRadius ONE = new BorderRadius(1.0F, 1.0F, 1.0F, 1.0F);
   private static final BorderRadius ONE_HALF = new BorderRadius(1.5F, 1.5F, 1.5F, 1.5F);
   private static final BorderRadius TWO = new BorderRadius(2.0F, 2.0F, 2.0F, 2.0F);
   private static final BorderRadius THREE = new BorderRadius(3.0F, 3.0F, 3.0F, 3.0F);
   private static final BorderRadius FOUR = new BorderRadius(4.0F, 4.0F, 4.0F, 4.0F);
   private static final BorderRadius FIVE = new BorderRadius(5.0F, 5.0F, 5.0F, 5.0F);
   private static final BorderRadius SIX = new BorderRadius(6.0F, 6.0F, 6.0F, 6.0F);
   private static final BorderRadius SEVEN = new BorderRadius(7.0F, 7.0F, 7.0F, 7.0F);
   private static final BorderRadius EIGHT = new BorderRadius(8.0F, 8.0F, 8.0F, 8.0F);
   private static final BorderRadius TEN = new BorderRadius(10.0F, 10.0F, 10.0F, 10.0F);
   private static final BorderRadius TWELVE = new BorderRadius(12.0F, 12.0F, 12.0F, 12.0F);

   public static BorderRadius all(float radius) {
      if (radius == 0.0F) return ZERO;
      if (radius == 0.5F) return HALF;
      if (radius == 0.75F) return THREE_QUARTERS;
      if (radius == 1.0F) return ONE;
      if (radius == 1.5F) return ONE_HALF;
      if (radius == 2.0F) return TWO;
      if (radius == 3.0F) return THREE;
      if (radius == 4.0F) return FOUR;
      if (radius == 5.0F) return FIVE;
      if (radius == 6.0F) return SIX;
      if (radius == 7.0F) return SEVEN;
      if (radius == 8.0F) return EIGHT;
      if (radius == 10.0F) return TEN;
      if (radius == 12.0F) return TWELVE;
      return new BorderRadius(radius, radius, radius, radius);
   }

   public static BorderRadius topLeft(float radius) {
      return new BorderRadius(radius, 0.0F, 0.0F, 0.0F);
   }

   public static BorderRadius topRight(float radius) {
      return new BorderRadius(0.0F, radius, 0.0F, 0.0F);
   }

   public static BorderRadius bottomRight(float radius) {
      return new BorderRadius(0.0F, 0.0F, radius, 0.0F);
   }

   public static BorderRadius bottomLeft(float radius) {
      return new BorderRadius(0.0F, 0.0F, 0.0F, radius);
   }

   public static BorderRadius top(float leftRadius, float rightRadius) {
      return new BorderRadius(leftRadius, rightRadius, 0.0F, 0.0F);
   }

   public static BorderRadius bottom(float leftRadius, float rightRadius) {
      return new BorderRadius(0.0F, 0.0F, rightRadius, leftRadius);
   }

   public static BorderRadius left(float topRadius, float bottomRadius) {
      return new BorderRadius(topRadius, 0.0F, 0.0F, bottomRadius);
   }

   public static BorderRadius right(float topRadius, float bottomRadius) {
      return new BorderRadius(0.0F, topRadius, bottomRadius, 0.0F);
   }

   @Override
   public String toString() {
      return "BorderRadius{topLeftRadius="
         + this.topLeftRadius
         + ", topRightRadius="
         + this.topRightRadius
         + ", bottomRightRadius="
         + this.bottomRightRadius
         + ", bottomLeftRadius="
         + this.bottomLeftRadius
         + "}";
   }
}
