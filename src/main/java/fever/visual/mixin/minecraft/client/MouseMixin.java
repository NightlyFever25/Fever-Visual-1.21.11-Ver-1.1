package fever.visual.mixin.minecraft.client;


import fever.visual.FeverVisual;
import fever.visual.systems.event.impl.window.ChatReleaseEvent;
import fever.visual.systems.event.impl.window.MouseEvent;
import fever.visual.systems.event.impl.window.MouseScrollEvent;
import fever.visual.systems.modules.modules.player.Freelook;
import fever.visual.systems.modules.modules.visuals.CustomChat;
import fever.visual.utility.game.PlatformUtility;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MouseMixin implements IMinecraft {
   @Shadow
   private double cursorDeltaX;
   @Shadow
   private double cursorDeltaY;
   @Unique
   private boolean fevervisual$hasLunarCursorPosition;
   @Unique
   private double fevervisual$lastLunarCursorX;
   @Unique
   private double fevervisual$lastLunarCursorY;

   @Inject(
           method = {"tick()V"},
           at = {@At("RETURN")}
   )
   private void tick(CallbackInfo ci) {
      if (CursorUtility.getCurrentType() != CursorUtility.getPrev()) {
         GLFW.glfwSetCursor(mc.getWindow().getHandle(), CursorUtility.getCurrentType().getCode());
      }

      CursorUtility.setPrev(CursorUtility.getCurrentType());
      CursorUtility.set(CursorType.DEFAULT);
   }

   @Inject(
           method = {"onMouseButton(JLnet/minecraft/client/input/MouseInput;I)V"},
           at = {@At("HEAD")}
   )
   private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
      if (action == 1) {
         FeverVisual.getInstance().getEventManager().triggerEvent(new MouseEvent(input.button(), action));
      } else if (action == 0) {
         if (PlatformUtility.isLabyMod()) {
            FeverVisual.getInstance().getEventManager().triggerEvent(new MouseEvent(input.button(), action));
         }

         if (mc.currentScreen instanceof ChatScreen) {
            Vector2f mouse = GuiUtility.getMouse();
            FeverVisual.getInstance().getEventManager().triggerEvent(new ChatReleaseEvent(mouse.x(), mouse.y(), input.button()));
            CustomChat.handleScrollbarRelease();
         } else if (PlatformUtility.isLabyMod()) {
            CustomChat.handleScrollbarRelease();
         }
      }
   }

   @Inject(
           method = {"onCursorPos(JDD)V"},
           at = {@At("RETURN")}
   )
   private void fevervisual$dragCustomChatScrollbar(long window, double x, double y, CallbackInfo ci) {
      this.fevervisual$updateLunarFreelook(x, y);

      if (mc.currentScreen instanceof ChatScreen) {
         CustomChat.handleScrollbarDrag(y / mc.getWindow().getScaleFactor());
      }
   }

   @Inject(
           method = {"onMouseScroll(JDD)V"},
           at = {@At("HEAD")}
   )
   private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (vertical != 0.0) {
         FeverVisual.getInstance().getEventManager().triggerEvent(new MouseScrollEvent(vertical));
      }
   }

   @Inject(
           method = {"updateMouse(D)V"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
           )},
           cancellable = true
   )
   private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
      if (Freelook.isActive && mc.player != null) {
         if (PlatformUtility.isLunarClient()) {
            ci.cancel();
            return;
         }

         double d = (Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
         double e = d * d * d;
         double f = e * 8.0;
         double deltaX = this.cursorDeltaX * f;
         double deltaY = this.cursorDeltaY * f;
         double var5 = deltaY * 0.15;
         double var7 = deltaX * 0.15;
         Freelook.prevX = Freelook.x;
         Freelook.prevY = Freelook.y;
         Freelook.x = (float)(Freelook.x + var7);
         Freelook.y = (float)MathHelper.clamp(Freelook.y + var5, -90.0, 90.0);
         ci.cancel();
      }
   }

   @Unique
   private void fevervisual$updateLunarFreelook(double x, double y) {
      if (!PlatformUtility.isLunarClient()) {
         return;
      }

      if (!Freelook.isActive || mc.player == null || mc.currentScreen != null) {
         this.fevervisual$hasLunarCursorPosition = false;
         return;
      }

      if (!this.fevervisual$hasLunarCursorPosition) {
         this.fevervisual$lastLunarCursorX = x;
         this.fevervisual$lastLunarCursorY = y;
         this.fevervisual$hasLunarCursorPosition = true;
         return;
      }

      double deltaX = x - this.fevervisual$lastLunarCursorX;
      double deltaY = y - this.fevervisual$lastLunarCursorY;
      this.fevervisual$lastLunarCursorX = x;
      this.fevervisual$lastLunarCursorY = y;
      if (deltaX == 0.0 && deltaY == 0.0) {
         return;
      }

      double sensitivity = (Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
      double multiplier = sensitivity * sensitivity * sensitivity * 8.0;
      Freelook.prevX = Freelook.x;
      Freelook.prevY = Freelook.y;
      Freelook.x = (float)(Freelook.x + deltaX * multiplier * 0.15);
      Freelook.y = (float)MathHelper.clamp(Freelook.y + deltaY * multiplier * 0.15, -90.0, 90.0);
   }
}
