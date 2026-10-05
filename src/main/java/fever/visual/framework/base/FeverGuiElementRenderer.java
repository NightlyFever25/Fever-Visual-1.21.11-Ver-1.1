package fever.visual.framework.base;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.framework.msdf.MsdfFont;
import fever.visual.utility.render.batching.impl.FontBatching;
import fever.visual.utility.render.batching.impl.GuiShapeBatching;
import fever.visual.utility.render.batching.impl.IconBatching;
import fever.visual.utility.render.batching.impl.SdfShapeBatching;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import java.util.List;
import java.util.function.Consumer;

public final class FeverGuiElementRenderer<T extends FeverGuiElementRenderState> extends SpecialGuiElementRenderer<T> {
   private static boolean registered;
   private final Class<T> elementClass;

   private FeverGuiElementRenderer(VertexConsumerProvider.Immediate vertexConsumers, Class<T> elementClass) {
      super(vertexConsumers);
      this.elementClass = elementClass;
   }

   public static void register() {
      if (!registered) {
         SpecialGuiElementRegistry.register(
            context -> new FeverGuiElementRenderer<>(context.vertexConsumers(), FeverGuiElementRenderState.Pre.class)
         );
         SpecialGuiElementRegistry.register(
            context -> new FeverGuiElementRenderer<>(context.vertexConsumers(), FeverGuiElementRenderState.Main.class)
         );
         SpecialGuiElementRegistry.register(
            context -> new FeverGuiElementRenderer<>(context.vertexConsumers(), FeverGuiElementRenderState.Post.class)
         );
         registered = true;
      }
   }

   @Override
   public Class<T> getElementClass() {
      return this.elementClass;
   }

   @Override
   protected void render(T state, MatrixStack matrices) {
      matrices.translate(-state.x2() / 2.0F, -state.y2(), 0.0F);
      for (FeverGuiElementRenderState.CommandBatch batch : state.batches()) {
         matrices.push();
         matrices.multiplyPositionMatrix(batch.transform());
         RenderSystem.setScissor(batch.scissor());
         try {
            this.renderBatch(batch.draws(), matrices);
         } finally {
            RenderSystem.setScissor(null);
            matrices.pop();
         }
      }
   }

   private void renderBatch(List<Consumer<MatrixStack>> draws, MatrixStack matrices) {
      for (int i = 0; i < draws.size();) {
         Consumer<MatrixStack> draw = draws.get(i);
         if (draw instanceof CustomDrawContext.SolidShapeDraw) {
            SdfShapeBatching batching = SdfShapeBatching.begin();
            do {
               CustomDrawContext.SolidShapeDraw shape = (CustomDrawContext.SolidShapeDraw)draws.get(i++);
               shape.add(batching, matrices.peek().getPositionMatrix());
            } while (i < draws.size() && draws.get(i) instanceof CustomDrawContext.SolidShapeDraw);
            batching.draw();
            continue;
         }

         if (draw instanceof CustomDrawContext.MsdfStringDraw textDraw) {
            MsdfFont font = textDraw.font();
            FontBatching batching = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, font);
            do {
               draws.get(i++).accept(matrices);
            } while (i < draws.size()
               && draws.get(i) instanceof CustomDrawContext.MsdfStringDraw next
               && next.font() == font);
            batching.draw();
            continue;
         }

         if (draw instanceof CustomDrawContext.TextureDraw textureDraw) {
            net.minecraft.util.Identifier texture = textureDraw.identifier();
            IconBatching batching = new IconBatching(VertexFormats.POSITION_TEXTURE_COLOR, matrices);
            do {
               draws.get(i++).accept(matrices);
            } while (i < draws.size()
               && draws.get(i) instanceof CustomDrawContext.TextureDraw next
               && next.identifier().equals(texture));
            batching.draw();
            continue;
         }

         draw.accept(matrices);
         i++;
      }
   }

   @Override
   protected String getName() {
      return "Fever Visual";
   }
}
