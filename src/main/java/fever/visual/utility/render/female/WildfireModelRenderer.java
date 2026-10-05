package fever.visual.utility.render.female;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class WildfireModelRenderer {
   private WildfireModelRenderer() {
      throw new UnsupportedOperationException();
   }

   public static class ModelBox {
      public final TexturedQuad[] quads;
      public final float posX1;
      public final float posY1;
      public final float posZ1;
      public final float posX2;
      public final float posY2;
      public final float posZ2;

      public ModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta, boolean mirror) {
         this(tW, tH, texU, texV, x, y, z, dx, dy, dz, delta, mirror, 5);
      }

      protected ModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta, boolean mirror, int quads) {
         this(tW, tH, texU, texV, x, y, z, dx, dy, dz, delta, mirror, quads, false);
      }

      protected ModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta, boolean mirror, int quads, boolean extra) {
         this.posX1 = x;
         this.posY1 = y;
         this.posZ1 = z;
         this.posX2 = x + (float)dx;
         this.posY2 = y + (float)dy;
         this.posZ2 = z + (float)dz;
         this.quads = new TexturedQuad[quads];
         float f = x + (float)dx;
         float f1 = y + (float)dy;
         float f2 = z + (float)dz;
         x -= delta;
         y -= delta;
         z -= delta;
         f += delta;
         f1 += delta;
         f2 += delta;
         if (mirror) {
            float f3 = f;
            f = x;
            x = f3;
         }

         this.initQuads(tW, tH, texU, texV, dx, dy, dz, mirror, extra, new PositionTextureVertex(f, y, z, 0.0F, 8.0F), new PositionTextureVertex(f, f1, z, 8.0F, 8.0F), new PositionTextureVertex(x, f1, z, 8.0F, 0.0F), new PositionTextureVertex(x, y, f2, 0.0F, 0.0F), new PositionTextureVertex(f, y, f2, 0.0F, 8.0F), new PositionTextureVertex(f, f1, f2, 8.0F, 8.0F), new PositionTextureVertex(x, f1, f2, 8.0F, 0.0F), new PositionTextureVertex(x, y, z, 0.0F, 0.0F));
      }

      protected void initQuads(int tW, int tH, int texU, int texV, int dx, int dy, int dz, boolean mirror, boolean extra, PositionTextureVertex vertex, PositionTextureVertex vertex1, PositionTextureVertex vertex2, PositionTextureVertex vertex3, PositionTextureVertex vertex4, PositionTextureVertex vertex5, PositionTextureVertex vertex6, PositionTextureVertex vertex7) {
         this.quads[0] = new TexturedQuad((float)(texU + dz + dx), (float)(texV + dz), (float)(texU + dz + dx + dz), (float)(texV + dz + dy), (float)tW, (float)tH, mirror, Direction.EAST, new PositionTextureVertex[]{vertex4, vertex, vertex1, vertex5});
         this.quads[1] = new TexturedQuad((float)texU, (float)(texV + dz), (float)(texU + dz), (float)(texV + dz + dy), (float)tW, (float)tH, mirror, Direction.WEST, new PositionTextureVertex[]{vertex7, vertex3, vertex6, vertex2});
         this.quads[2] = new TexturedQuad((float)(texU + dz), (float)texV, (float)(texU + dz + dx), (float)(texV + dz), (float)tW, (float)tH, mirror, Direction.DOWN, new PositionTextureVertex[]{vertex4, vertex3, vertex7, vertex});
         this.quads[3] = new TexturedQuad((float)(texU + dz), (float)(texV + dz + 4), (float)(texU + dz + dx), (float)(texV + 1 + dz + dy), (float)tW, (float)(tH - 1), mirror, Direction.UP, new PositionTextureVertex[]{vertex1, vertex2, vertex6, vertex5});
         this.quads[4] = new TexturedQuad((float)(texU + dz), (float)(texV + dz), (float)(texU + dz + dx), (float)(texV + dz + dy), (float)tW, (float)tH, mirror, Direction.NORTH, new PositionTextureVertex[]{vertex, vertex7, vertex2, vertex1});
      }
   }

   public static class OverlayModelBox extends ModelBox {
      public OverlayModelBox(boolean isLeft, int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta, boolean mirror) {
         super(tW, tH, texU, texV, x, y, z, dx, dy, dz, delta, mirror, 4, isLeft);
      }

      protected void initQuads(int tW, int tH, int texU, int texV, int dx, int dy, int dz, boolean mirror, boolean isLeft, PositionTextureVertex vertex, PositionTextureVertex vertex1, PositionTextureVertex vertex2, PositionTextureVertex vertex3, PositionTextureVertex vertex4, PositionTextureVertex vertex5, PositionTextureVertex vertex6, PositionTextureVertex vertex7) {
         if (!isLeft) {
            this.quads[0] = new TexturedQuad((float)(texU + dz + dx), (float)(texV + dz), (float)(texU + dz + dx + dz), (float)(texV + dz + dy), (float)tW, (float)tH, mirror, Direction.EAST, new PositionTextureVertex[]{vertex4, vertex, vertex1, vertex5});
         } else {
            this.quads[0] = new TexturedQuad((float)texU, (float)(texV + dz), (float)(texU + dz), (float)(texV + dz + dy), (float)tW, (float)tH, mirror, Direction.WEST, new PositionTextureVertex[]{vertex7, vertex3, vertex6, vertex2});
         }

         this.quads[1] = new TexturedQuad((float)(texU + dz), (float)texV, (float)(texU + dz + dx), (float)(texV + dz), (float)tW, (float)tH, mirror, Direction.DOWN, new PositionTextureVertex[]{vertex4, vertex3, vertex7, vertex});
         this.quads[2] = new TexturedQuad((float)(texU + dz), (float)(texV + dz + 4), (float)(texU + dz + dx), (float)(texV + 1 + dz + dy), (float)tW, (float)(tH - 1), mirror, Direction.UP, new PositionTextureVertex[]{vertex1, vertex2, vertex6, vertex5});
         this.quads[3] = new TexturedQuad((float)(texU + dz), (float)(texV + dz), (float)(texU + dz + dx), (float)(texV + dz + dy), (float)tW, (float)tH, mirror, Direction.NORTH, new PositionTextureVertex[]{vertex, vertex7, vertex2, vertex1});
      }
   }

   public static class BreastModelBox extends ModelBox {
      public BreastModelBox(int tW, int tH, int texU, int texV, float x, float y, float z, int dx, int dy, int dz, float delta, boolean mirror) {
         super(tW, tH, texU, texV, x, y, z, dx, dy, dz, delta, mirror);
      }

      protected void initQuads(int tW, int tH, int texU, int texV, int dx, int dy, int dz, boolean mirror, boolean extra, PositionTextureVertex vertex, PositionTextureVertex vertex1, PositionTextureVertex vertex2, PositionTextureVertex vertex3, PositionTextureVertex vertex4, PositionTextureVertex vertex5, PositionTextureVertex vertex6, PositionTextureVertex vertex7) {
         this.quads[0] = new TexturedQuad((float)(texU + 4 + dx), (float)(texV + 4), (float)(texU + 4 + dx + 4), (float)(texV + 4 + dy), (float)tW, (float)tH, mirror, Direction.EAST, new PositionTextureVertex[]{vertex4, vertex, vertex1, vertex5});
         this.quads[1] = new TexturedQuad((float)texU, (float)(texV + 4), (float)(texU + 4), (float)(texV + 4 + dy), (float)tW, (float)tH, mirror, Direction.WEST, new PositionTextureVertex[]{vertex7, vertex3, vertex6, vertex2});
         this.quads[2] = new TexturedQuad((float)(texU + 4), (float)texV, (float)(texU + 4 + dx), (float)(texV + 4), (float)tW, (float)tH, mirror, Direction.DOWN, new PositionTextureVertex[]{vertex4, vertex3, vertex7, vertex});
         this.quads[3] = new TexturedQuad((float)(texU + 4), (float)(texV + 4 + 4), (float)(texU + 4 + dx), (float)(texV + 1 + 4 + dy), (float)tW, (float)(tH - 1), mirror, Direction.UP, new PositionTextureVertex[]{vertex1, vertex2, vertex6, vertex5});
         this.quads[4] = new TexturedQuad((float)(texU + 4), (float)(texV + 4), (float)(texU + 4 + dx), (float)(texV + 4 + dy), (float)tW, (float)tH, mirror, Direction.NORTH, new PositionTextureVertex[]{vertex, vertex7, vertex2, vertex1});
      }
   }

   public static record PositionTextureVertex(float x, float y, float z, float u, float v) {
      public PositionTextureVertex withTexturePosition(float texU, float texV) {
         return new PositionTextureVertex(this.x, this.y, this.z, texU, texV);
      }
   }

   public static class TexturedQuad {
      public final PositionTextureVertex[] vertexPositions;
      public final Vector3f normal;

      public TexturedQuad(float u1, float v1, float u2, float v2, float texWidth, float texHeight, boolean mirrorIn, Direction directionIn, PositionTextureVertex... positionsIn) {
         if (positionsIn.length != 4) {
            throw new IllegalArgumentException("Wrong number of vertex's. Expected: 4, Received: " + positionsIn.length);
         } else {
            this.vertexPositions = positionsIn;
            float f = 0.0F / texWidth;
            float f1 = 0.0F / texHeight;
            positionsIn[0] = positionsIn[0].withTexturePosition(u2 / texWidth - f, v1 / texHeight + f1);
            positionsIn[1] = positionsIn[1].withTexturePosition(u1 / texWidth + f, v1 / texHeight + f1);
            positionsIn[2] = positionsIn[2].withTexturePosition(u1 / texWidth + f, v2 / texHeight - f1);
            positionsIn[3] = positionsIn[3].withTexturePosition(u2 / texWidth - f, v2 / texHeight - f1);
            if (mirrorIn) {
               int i = positionsIn.length;

               for(int j = 0; j < i / 2; ++j) {
                  PositionTextureVertex vertex = positionsIn[j];
                  positionsIn[j] = positionsIn[i - 1 - j];
                  positionsIn[i - 1 - j] = vertex;
               }
            }

            this.normal = directionIn.getUnitVector();
            if (mirrorIn) {
               this.normal.mul(-1.0F, 1.0F, 1.0F);
            }

         }
      }
   }
}
