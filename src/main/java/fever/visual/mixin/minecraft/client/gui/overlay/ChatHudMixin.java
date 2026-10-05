package fever.visual.mixin.minecraft.client.gui.overlay;

import fever.visual.systems.modules.modules.visuals.CustomChat;
import fever.visual.utility.game.PlatformUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("HEAD"), cancellable = true)
   private void fevervisual$skipHudChatBehindOpenCustomChat(
      DrawContext context,
      TextRenderer textRenderer,
      int currentTick,
      int mouseX,
      int mouseY,
      boolean interactable,
      boolean bl,
      CallbackInfo ci
   ) {
      if (PlatformUtility.isLabyMod() && CustomChat.renderChatFrame(context, currentTick, interactable)) {
         ci.cancel();
         return;
      }

      CustomChat.beginChatRender();
      if (!interactable && CustomChat.isActive() && MinecraftClient.getInstance().currentScreen instanceof ChatScreen) {
         ci.cancel();
      }
   }

   @ModifyConstant(
      method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V",
      constant = @Constant(intValue = 9),
      require = 0
   )
   private int fevervisual$customChatLineHeight(int original) {
      return CustomChat.getLineHeight(original);
   }

   @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
   private void fevervisual$keepCustomChatHistory(boolean clearHistory, CallbackInfo ci) {
      if (CustomChat.shouldKeepHistory()) {
         ci.cancel();
      }
   }
}
