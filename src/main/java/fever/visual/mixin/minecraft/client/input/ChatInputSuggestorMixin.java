package fever.visual.mixin.minecraft.client.input;

import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.List;
import javax.annotation.Nullable;
import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.CustomChat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatInputSuggestor.SuggestionWindow;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {
   @Shadow
   @Final
   TextFieldWidget textField;
   @Shadow
   @Final
   private Screen owner;
   @Shadow
   @Final
   private TextRenderer textRenderer;
   @Shadow
   @Final
   private boolean chatScreenSized;
   @Shadow
   @Final
   private List<OrderedText> messages;
   @Shadow
   private int x;
   @Shadow
   private int width;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   @Nullable
   private SuggestionWindow window;

   @Shadow
   public abstract void show(boolean var1);

   @Inject(method = "renderMessages", at = @At("HEAD"), cancellable = true)
   private void fevervisual$renderCustomCommandMessages(DrawContext context, CallbackInfo ci) {
      if (CustomChat.renderCommandSuggestionMessages(context, this.textRenderer, this.messages, this.x, this.width, this.chatScreenSized, this.owner.height)) {
         ci.cancel();
      }
   }

   @Inject(method = "refresh", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z", remap = false), cancellable = true)
   private void injectAutoCompletion(CallbackInfo ci) {
      String text = this.textField.getText();
      String prefix = FeverVisual.getInstance().getCommandManager().getPrefix();
      if (text.startsWith(prefix)) {
         this.pendingSuggestions = FeverVisual.getInstance().getCommandManager().autoComplete(text, this.textField.getCursor());
         this.pendingSuggestions.thenRun(() -> {
            try {
               if (this.pendingSuggestions.isDone() && !this.pendingSuggestions.get().isEmpty() && this.window == null) {
                  this.show(false);
                  ci.cancel();
               }
            } catch (ExecutionException | InterruptedException var3x) {
            }
         });
      }
   }
}
