package fever.visual.utility.render.compat;

import net.minecraft.client.render.BuiltBuffer;
public final class BufferRenderer {
   private BufferRenderer() {
   }

   public static void drawWithGlobalProgram(BuiltBuffer buffer) {
      RenderBridge.draw(buffer);
   }
}
