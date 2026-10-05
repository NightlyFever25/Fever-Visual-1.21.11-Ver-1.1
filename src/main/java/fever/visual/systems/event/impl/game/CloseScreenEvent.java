package fever.visual.systems.event.impl.game;

import lombok.Generated;
import fever.visual.systems.event.Event;
import net.minecraft.client.gui.screen.Screen;

public class CloseScreenEvent extends Event {
   private final Screen screen;

   @Generated
   public Screen getScreen() {
      return this.screen;
   }

   @Generated
   public CloseScreenEvent(Screen screen) {
      this.screen = screen;
   }
}
