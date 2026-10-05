package fever.visual.framework.base;

import fever.visual.framework.objects.MouseButton;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class CustomScreen extends Screen {
   protected CustomScreen() {
      super(Text.empty());
   }

   public abstract void render(UIContext var1);

   public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      UIContext uiContext = UIContext.of(context, mouseX, mouseY, delta);
      this.render(uiContext);
   }

   public final boolean mouseClicked(Click click, boolean doubled) {
      MouseButton mouseButton = MouseButton.fromButtonIndex(click.button());
      this.onMouseClicked(click.x(), click.y(), mouseButton);
      return super.mouseClicked(click, doubled);
   }

   public final boolean mouseReleased(Click click) {
      MouseButton mouseButton = MouseButton.fromButtonIndex(click.button());
      this.onMouseReleased(click.x(), click.y(), mouseButton);
      return super.mouseReleased(click);
   }

   public final boolean mouseDragged(Click click, double deltaX, double deltaY) {
      MouseButton mouseButton = MouseButton.fromButtonIndex(click.button());
      this.onMouseDragged(click.x(), click.y(), mouseButton, deltaX, deltaY);
      return super.mouseDragged(click, deltaX, deltaY);
   }

   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
   }

   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
   }

   public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
   }
}
