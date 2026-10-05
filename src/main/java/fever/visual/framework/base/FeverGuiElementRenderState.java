package fever.visual.framework.base;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public class FeverGuiElementRenderState implements SpecialGuiElementRenderState {
   private final int width;
   private final int height;
   private final ScreenRect bounds;
   private final List<CommandBatch> batches = new ArrayList<>();

   FeverGuiElementRenderState(int width, int height) {
      this.width = width;
      this.height = height;
      this.bounds = new ScreenRect(0, 0, width, height);
   }

   void add(Matrix4f transform, @Nullable ScreenRect scissor, Consumer<MatrixStack> draw) {
      int size = this.batches.size();
      if (size != 0) {
         CommandBatch last = this.batches.get(size - 1);
         if (last.matches(transform, scissor)) {
            last.add(draw);
            return;
         }
      }

      this.batches.add(new CommandBatch(new Matrix4f(transform), scissor, draw));
   }

   List<CommandBatch> batches() {
      return this.batches;
   }

   @Override
   public int x1() {
      return 0;
   }

   @Override
   public int x2() {
      return this.width;
   }

   @Override
   public int y1() {
      return 0;
   }

   @Override
   public int y2() {
      return this.height;
   }

   @Override
   public float scale() {
      return 1.0F;
   }

   @Override
   public ScreenRect scissorArea() {
      return null;
   }

   @Override
   public ScreenRect bounds() {
      return this.bounds;
   }

   static final class CommandBatch {
      private final Matrix4f transform;
      private final @Nullable ScreenRect scissor;
      private final List<Consumer<MatrixStack>> draws = new ArrayList<>();

      CommandBatch(Matrix4f transform, @Nullable ScreenRect scissor, Consumer<MatrixStack> draw) {
         this.transform = transform;
         this.scissor = scissor;
         this.draws.add(draw);
      }

      boolean matches(Matrix4f transform, @Nullable ScreenRect scissor) {
         return this.transform.equals(transform) && java.util.Objects.equals(this.scissor, scissor);
      }

      void add(Consumer<MatrixStack> draw) {
         this.draws.add(draw);
      }

      Matrix4f transform() {
         return this.transform;
      }

      @Nullable ScreenRect scissor() {
         return this.scissor;
      }

      List<Consumer<MatrixStack>> draws() {
         return this.draws;
      }
   }

   public static final class Pre extends FeverGuiElementRenderState {
      Pre(int width, int height) {
         super(width, height);
      }
   }

   public static final class Main extends FeverGuiElementRenderState {
      Main(int width, int height) {
         super(width, height);
      }
   }

   public static final class Post extends FeverGuiElementRenderState {
      Post(int width, int height) {
         super(width, height);
      }
   }
}
