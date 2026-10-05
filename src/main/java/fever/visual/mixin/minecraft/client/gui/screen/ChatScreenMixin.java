package fever.visual.mixin.minecraft.client.gui.screen;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.systems.event.impl.render.ChatRenderEvent;
import fever.visual.systems.event.impl.window.ChatClickEvent;
import fever.visual.systems.modules.modules.visuals.CustomChat;
import fever.visual.utility.game.PlatformUtility;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen implements IMinecraft {
   @Shadow
   protected TextFieldWidget chatField;
   @Shadow
   private ChatInputSuggestor chatInputSuggestor;

   protected ChatScreenMixin(Text title) {
      super(title);
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void fevervisual$beginCustomChatInputRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      PlatformUtility.markChatEditorOpen();
      CustomChat.beginChatScreenRender();
   }

   @Inject(method = "removed", at = @At("HEAD"))
   private void fevervisual$closeVanillaHudSettingsOnRemoved(CallbackInfo ci) {
      FeverVisual.getInstance().getHud().closeFloatingControlsForVanillaChat();
      PlatformUtility.markChatEditorClosed();
   }

   @Inject(method = "sendMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
   private void onSendMessage(String text, boolean addToHistory, CallbackInfo ci) {
      if (FeverVisual.getInstance().handleSafeModeReturn(text)) {
         if (addToHistory) {
            mc.inGameHud.getChatHud().addToMessageHistory(text);
         }

         ci.cancel();
         return;
      }

      if (FeverVisual.getInstance().getCommandManager().dispatch(text)) {
         mc.inGameHud.getChatHud().addToMessageHistory(text);
         ci.cancel();
      }
   }

   @Inject(method = "render", at = @At("RETURN"))
   public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      context.createNewRootLayer();
      FeverVisual.getInstance().getEventManager().triggerEvent(
              new ChatRenderEvent(CustomDrawContext.isolated(context), delta)
      );
      CustomChat.renderScrollbar(context);
      CustomChat.endChatScreenRender();
   }

   @Inject(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"),
      require = 0
   )
   private void fevervisual$drawCustomChatInputBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.chatField != null) {
         CustomChat.renderInputBackground(
            context,
            this.chatField.getX() - 4,
            this.chatField.getY() - 4,
            this.chatField.getX() + this.chatField.getWidth() + 4,
            this.chatField.getY() + this.chatField.getHeight() + 4
         );
      }
   }

   @ModifyArg(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"),
      index = 4,
      require = 0
   )
   private int fevervisual$hideVanillaChatInputBackground(int color) {
      return CustomChat.isActive() ? 0 : color;
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void onMouseClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      if (CustomChat.handleScrollbarClick(click.x(), click.y(), click.button())) {
         cir.setReturnValue(true);
         return;
      }

      ChatClickEvent event = new ChatClickEvent((float)click.x(), (float)click.y(), click.button());
      FeverVisual.getInstance().getEventManager().triggerEvent(event);
      if (event.isHandled()) {
         cir.setReturnValue(true);
      }
   }

}
