package fever.visual.systems.event.impl.window;

import lombok.Generated;
import fever.visual.systems.event.Event;

public class ChatClickEvent extends Event {
   private final float x;
   private final float y;
   private final int button;
   private boolean handled;

   @Generated
   public float getX() {
      return this.x;
   }

   @Generated
   public float getY() {
      return this.y;
   }

   @Generated
   public int getButton() {
      return this.button;
   }

   @Generated
   public boolean isHandled() {
      return this.handled;
   }

   @Generated
   public void setHandled(boolean handled) {
      this.handled = handled;
   }

   @Generated
   public ChatClickEvent(float x, float y, int button) {
      this.x = x;
      this.y = y;
      this.button = button;
   }
}
