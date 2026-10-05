package net.minecraft.client.gui.screen.multiplayer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public final class FeverVisualServerCategoryEntry extends MultiplayerServerListWidget.Entry {
   private final Text title;

   public FeverVisualServerCategoryEntry(String title) {
      this.title = Text.literal(title);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
      MinecraftClient client = MinecraftClient.getInstance();
      context.drawTextWithShadow(client.textRenderer, this.title, this.getContentX() + 4, this.getContentMiddleY() - 4, Colors.GRAY);
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      return false;
   }

   @Override
   boolean isOfSameType(MultiplayerServerListWidget.Entry entry) {
      return entry instanceof FeverVisualServerCategoryEntry categoryEntry && categoryEntry.title.getString().equals(this.title.getString());
   }

   @Override
   public void connect() {
   }

   @Override
   public Text getNarration() {
      return this.title;
   }
}
