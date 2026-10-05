package fever.visual.mixin.minecraft.client.gui.screen;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.mixin.accessors.HandledScreenAccessor;
import fever.visual.systems.event.impl.render.ScreenRenderEvent;
import fever.visual.systems.event.impl.window.ContainerClickEvent;
import fever.visual.systems.event.impl.window.ContainerReleaseEvent;
import fever.visual.systems.modules.modules.player.ItemScroller;
import fever.visual.systems.modules.modules.visuals.CustomInv;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.time.Timer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin implements IMinecraft {
   @Unique
   private final Timer timer = new Timer();

   @Shadow
   protected abstract boolean isPointOverSlot(Slot var1, double var2, double var4);

   @Shadow
   protected abstract void onMouseClick(Slot var1, int var2, int var3, SlotActionType var4);

   @Unique
   private float fevervisual$lastDelta = 0.0F;

   @Inject(method = "render", at = @At("HEAD"))
   private void fevervisual$captureDelta(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      this.fevervisual$lastDelta = delta;
   }

   @Inject(method = "drawForeground", at = @At("TAIL"))
   private void onRender(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
      if (mc.player == null) {
         return;
      }

      CustomDrawContext customDrawContext = CustomDrawContext.of(context);
      FeverVisual.getInstance().getEventManager().triggerEvent(new ScreenRenderEvent(customDrawContext, this.fevervisual$lastDelta));

      if (this.fevervisual$shouldDrawSlotGuides()) {
         this.fevervisual$drawSlotGuides(customDrawContext);
      }

      ItemScroller itemScroller = FeverVisual.getInstance().getModuleManager().getModule(ItemScroller.class);
      if (itemScroller == null || !itemScroller.isEnabled()) {
         return;
      }

      for (Slot slot : mc.player.currentScreenHandler.slots) {
         if (this.isPointOverSlot(slot, mouseX, mouseY)
                 && slot.isEnabled()
                 && this.timer.finished((long)itemScroller.getScrollDelay().getCurrentValue())
                 && InputUtil.isKeyPressed(mc.getWindow(), 340)
                 && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
            this.onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
            this.timer.reset();
         }
      }
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"))
   private void onMouseClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      FeverVisual.getInstance().getEventManager().triggerEvent(new ContainerClickEvent((float)click.x(), (float)click.y(), click.button()));
   }

   @Inject(method = "mouseReleased", at = @At("HEAD"))
   public void mouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
      FeverVisual.getInstance().getEventManager().triggerEvent(new ContainerReleaseEvent((float)click.x(), (float)click.y(), click.button()));
   }

   @Unique
   private boolean fevervisual$shouldDrawSlotGuides() {
      if (!(mc.currentScreen instanceof HandledScreen<?> handledScreen)) {
         return false;
      }

      if (handledScreen instanceof InventoryScreen || handledScreen instanceof CreativeInventoryScreen) {
         return false;
      }

      if (FeverVisual.getInstance().getModuleManager() == null) {
         return false;
      }

      CustomInv customInv = FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomInv.class);
      return customInv != null && customInv.isEnabled();
   }

   @Unique
   private void fevervisual$drawSlotGuides(CustomDrawContext context) {
      if (!((Object)this instanceof HandledScreen<?> handledScreen) || mc.player == null) {
         return;
      }

      HandledScreenAccessor accessor = (HandledScreenAccessor) handledScreen;
      final float borderThickness = 0.8F;
      final float innerInset = 2.5F;
      final float innerSize = 18.0F - innerInset * 2.0F;

      for (Slot slot : mc.player.currentScreenHandler.slots) {
         if (!slot.isEnabled()) {
            continue;
         }

         if (slot.x <= -1000 || slot.y <= -1000) {
            continue;
         }

         float slotX = accessor.getX() + slot.x;
         float slotY = accessor.getY() + slot.y;

         context.drawRoundedBorder(
                 slotX + 0.5F,
                 slotY + 0.5F,
                 15.0F,
                 15.0F,
                 borderThickness,
                 BorderRadius.all(3.0F),
                 Colors.getTextColor().withAlpha(42.0F)
         );
         context.drawRoundedRect(
                 slotX + innerInset,
                 slotY + innerInset,
                 innerSize,
                 innerSize,
                 BorderRadius.all(2.0F),
                 Colors.getTextColor().withAlpha(9.0F)
         );
      }
   }
}
