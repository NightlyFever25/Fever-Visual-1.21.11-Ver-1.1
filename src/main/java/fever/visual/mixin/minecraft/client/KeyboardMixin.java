package fever.visual.mixin.minecraft.client;

import fever.visual.FeverVisual;
import fever.visual.systems.event.impl.window.KeyPressEvent;
import fever.visual.utility.game.PlatformUtility;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin implements IMinecraft {
   @Inject(method = "onKey(JILnet/minecraft/client/input/KeyInput;)V", at = @At("HEAD"))
   public void triggerKeyEvent(long window, int action, KeyInput input, CallbackInfo ci) {
      int key = input.key();
      if (key != -1) {
         FeverVisual.getInstance().getEventManager().triggerEvent(new KeyPressEvent(action, key));
         if (PlatformUtility.isLabyMod() && action == GLFW.GLFW_PRESS) {
            if (key == GLFW.GLFW_KEY_T || key == GLFW.GLFW_KEY_SLASH || key == 46) {
               PlatformUtility.markChatEditorOpen();
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
               PlatformUtility.markChatEditorClosed();
            }
         }

         if (mc.currentScreen == null) {
            if (key == 46 && action == 1) {
               mc.setScreen(new ChatScreen("", false));
            }
         }
      }
   }
}
