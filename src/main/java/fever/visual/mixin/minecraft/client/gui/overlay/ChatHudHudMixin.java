package fever.visual.mixin.minecraft.client.gui.overlay;

import fever.visual.utility.interfaces.ChatHudBackendContext;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.gui.hud.ChatHud$Hud")
public class ChatHudHudMixin implements ChatHudBackendContext {
   @Shadow
   private DrawContext context;

   @Override
   public DrawContext fevervisual$getContext() {
      return this.context;
   }
}
