package fever.visual.systems.event.impl.window;

import lombok.Generated;
import fever.visual.systems.event.Event;

public class MouseEvent extends Event {
   private final int button;
   private final int action;

   @Generated
   public int getButton() {
      return this.button;
   }

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public MouseEvent(int button, int action) {
      this.button = button;
      this.action = action;
   }
}
