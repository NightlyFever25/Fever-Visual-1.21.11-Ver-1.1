package fever.visual.systems.event.impl.window;

import lombok.Generated;
import fever.visual.systems.event.Event;

public class MouseScrollEvent extends Event {
   private final double verticalAmount;

   @Generated
   public double getVerticalAmount() {
      return this.verticalAmount;
   }

   @Generated
   public MouseScrollEvent(double verticalAmount) {
      this.verticalAmount = verticalAmount;
   }
}
