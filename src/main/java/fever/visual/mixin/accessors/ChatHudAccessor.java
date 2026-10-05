package fever.visual.mixin.accessors;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface ChatHudAccessor {
   @Accessor("visibleMessages")
   List<?> fevervisual$getVisibleMessages();

   @Accessor("scrolledLines")
   int fevervisual$getScrolledLines();

   @Invoker("getVisibleLineCount")
   int fevervisual$getVisibleLineCount();

   @Invoker("getWidth")
   int fevervisual$getWidth();

   @Invoker("getHeight")
   int fevervisual$getHeight();

   @Invoker("getChatScale")
   double fevervisual$getChatScale();

   @Invoker("scroll")
   void fevervisual$scroll(int scroll);
}
