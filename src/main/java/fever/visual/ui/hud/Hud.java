package fever.visual.ui.hud;

import fever.visual.ui.hud.impl.*;
import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.CloseScreenEvent;
import fever.visual.systems.event.impl.render.ChatRenderEvent;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.systems.event.impl.window.ChatClickEvent;
import fever.visual.systems.event.impl.window.ChatKeyPressEvent;
import fever.visual.systems.event.impl.window.ChatReleaseEvent;
import fever.visual.systems.event.impl.window.MouseEvent;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.ui.components.ColorPicker;
import fever.visual.ui.components.animated.AnimatedText;
import fever.visual.ui.components.popup.Popup;
import fever.visual.ui.hud.impl.*;
import fever.visual.ui.hud.impl.island.DynamicIsland;
import fever.visual.ui.hud.inline.impl.WorldElement;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.game.PlatformUtility;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.interfaces.IScaledResolution;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.time.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Hud implements IMinecraft, IScaledResolution {
   private static final float POPUP_Z = 400.0F;
   private final List<HudElement> elements = new ArrayList<>();
   private final Map<String, HudElement> elementsByName = new HashMap<>();
   private final List<HudElement> enabledElementsCache = new ArrayList<>();
   private final List<HudElement> showingEnabledElementsCache = new ArrayList<>();
   private final List<HudElement> disabledEnabledElementsCache = new ArrayList<>();
   private final List<Popup> popups = new ArrayList<>();
   private final List<ColorPicker> colorPickers = new ArrayList<>();
   public DynamicIsland island;
   private final HudHistoryManager historyManager = new HudHistoryManager();
   private final Grid grid = new Grid();
   private String desc = "";
   private AnimatedText descText;
   private final Timer timer = new Timer();
   private final EventListener<HudRenderEvent> onHud = event -> {
      this.updateElementsVisibility();
      this.releaseLabyDraggingIfMouseUp();

      if (this.enabledElementsCache.isEmpty()) {
         return;
      }

      UIContext context = UIContext.of(
              event.getContext(),
              mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().x(),
              mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().y(),
              MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false)
      );

      if (this.descText == null) {
         this.descText = new AnimatedText(Fonts.REGULAR.getFont(10.0F), 10.0F, 300L, Easing.BAKEK).centered();
      }

      this.desc = "";
      if (PlatformUtility.isChatEditorOpen() && this.hasDraggedElements()) {
         this.grid.update();
         this.grid.draw(context);
      } else if (!this.grid.getLines().isEmpty()) {
         this.grid.getLines().clear();
      }

      for (HudElement element : this.getEnabledElements()) {
         element.render(context);
         if (element.getSelecting().getValue() > 0.001F) {
            float anim = element.getAnimation().getValue() * element.getVisible().getValue();
            float scale = 0.5F + anim * 0.5F - 0.05F * element.getSelecting().getValue();
            element.getLoadingAnim().setDuration(1500L);
            element.getLoadingAnim().update(1.0F);
            if (element.getLoadingAnim().getValue() == 1.0F) {
               element.getLoadingAnim().setValue(0.0F);
            }

            RenderUtility.scale(context.getMatrices(), element.getX() + element.getWidth() / 2.0F, element.getY() + element.getHeight() / 2.0F, scale);
            context.drawLoadingRect(
                    element.getX(),
                    element.getY(),
                    element.getWidth(),
                    this.getSelectionHeight(element),
                    element.getLoadingAnim().getValue() * 2.2F - 0.5F,
                    BorderRadius.all(element instanceof DynamicIsland ? 7.0F : 6.0F),
                    ColorRGBA.WHITE.withAlpha(100.0F * element.getSelecting().getValue())
            );
            RenderUtility.end(context.getMatrices());
         }
      }

      this.descText.pos(sr.getScaledWidth() / 2.0F, 30.0F);
      if (!this.desc.contains(".description")) {
         this.descText.update(this.desc);
         this.descText.render(context);
      }

      if (PlatformUtility.isChatEditorOpen()) {
         this.renderPopupStack(context);
      }

      if (this.popups.isEmpty() && this.colorPickers.isEmpty() && !PlatformUtility.isChatEditorOpen()) {
         CursorUtility.set(CursorType.DEFAULT);
      }

      this.popups.removeIf(popupx -> popupx.getAnimation().getValue() == 0.0F && !popupx.isShowing());
   };
   private final EventListener<ChatKeyPressEvent> onColorPickerKeyPress = event -> {
      if (!this.colorPickers.isEmpty()) {
         for (ColorPicker colorPicker : this.colorPickers) {
            colorPicker.onKeyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
         }
         this.colorPickers.removeIf(picker -> !picker.isShowing());
      }
   };
   private final EventListener<ChatRenderEvent> onPostHud = event -> {
      UIContext context = UIContext.of(
              event.getContext(),
              mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().x(),
              mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().y(),
              MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false)
      );
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, 0.0F, POPUP_Z);

      for (Popup popup : this.popups) {
         if (popup.getY() + popup.getHeight() > sr.getScaledHeight()) {
            popup.setY(sr.getScaledHeight() - 10.0F - popup.getHeight());
         }
         popup.render(context);
      }

      for (ColorPicker colorPicker : this.colorPickers) {
         if (colorPicker.getY() + colorPicker.getHeight() > sr.getScaledHeight()) {
            colorPicker.setY(sr.getScaledHeight() - 10.0F - colorPicker.getHeight());
         }
         colorPicker.render(context);
      }
      this.colorPickers.removeIf(picker -> !picker.isShowing());

      context.getMatrices().pop();
   };
   private final EventListener<ChatKeyPressEvent> onKeyPress = event -> {
      int modifiers = event.getModifiers();
      int keyCode = event.getKeyCode();
      if (keyCode == 90 && (modifiers & 2) != 0) {
         FeverVisual.getInstance().getHud().getHistoryManager().undo();
      } else if (keyCode == 89 && (modifiers & 2) != 0) {
         FeverVisual.getInstance().getHud().getHistoryManager().redo();
      }
   };
   private final EventListener<ChatClickEvent> onClick = event -> {
      MouseButton mouseButton = MouseButton.fromButtonIndex(event.getButton());

      ColorPicker topColorPicker = this.getTopColorPicker(event.getX(), event.getY());
      if (topColorPicker != null) {
         topColorPicker.onMouseClicked(event.getX(), event.getY(), mouseButton);
         this.bringColorPickerToFront(topColorPicker);
         event.setHandled(true);
         return;
      }

      for (Popup popup : this.popups) {
         popup.onMouseClicked(event.getX(), event.getY(), mouseButton);
         if (popup.isHovered(event.getX(), event.getY())) {
            event.setHandled(true);
            return;
         }
      }

      if (mouseButton == MouseButton.RIGHT) {
         List<HudElement> enabled = this.getEnabledElements();
         for (int i = enabled.size() - 1; i >= 0; i--) {
            HudElement element = enabled.get(i);
            if (element.isShowing() && element.isHovered(event.getX(), event.getY())) {
               this.colorPickers.forEach(colorPicker -> colorPicker.setShowing(false));
               this.popups.forEach(popup -> popup.setShowing(false));
               if (!(element instanceof Hotbar) && element.hasVisibleSettings()) {
                  element.openSettingsPopup(event.getX(), event.getY());
               }
               event.setHandled(true);
               return;
            }
         }
      }

      List<HudElement> enabled = this.getEnabledElements();
      for (int i = enabled.size() - 1; i >= 0; i--) {
         HudElement element = enabled.get(i);
         element.onMouseClicked(event.getX(), event.getY(), mouseButton);
         if (element instanceof Hotbar) {
            continue;
         }
         if (element.isHovered(event.getX(), event.getY()) && element.isShowing() || element.isDragging()) {
            event.setHandled(true);
            return;
         }
      }

      this.colorPickers.forEach(colorPicker -> colorPicker.setShowing(false));
      this.popups.forEach(popup -> popup.setShowing(false));
   };
   private final EventListener<ChatReleaseEvent> onRelease = event -> {
      for (ColorPicker colorPicker : this.colorPickers) {
         colorPicker.onMouseReleased(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
         if (colorPicker.isHovered(event.getX(), event.getY()) || colorPicker.isPick()) {
            event.setHandled(true);
         }
      }

      for (Popup popup : this.popups) {
         popup.onMouseReleased(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
         if (popup.isHovered(event.getX(), event.getY())) {
            event.setHandled(true);
            return;
         }
      }

      for (HudElement element : this.elements) {
         boolean wasDragging = element.isDragging();
         element.onMouseReleased(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
         if (wasDragging || element.isHovered(event.getX(), event.getY()) && element.isShowing()) {
            event.setHandled(true);
         }
      }
   };
   private final EventListener<MouseEvent> onLabyMouseRelease = event -> {
      if (PlatformUtility.isLabyMod() && event.getAction() == GLFW.GLFW_RELEASE) {
         this.releaseDraggedElementsAtMouse(MouseButton.fromButtonIndex(event.getButton()));
      }
   };
   private final EventListener<CloseScreenEvent> onCloseScreen = event -> {
      if (PlatformUtility.isLabyMod()) {
         this.releaseDraggedElementsAtMouse(MouseButton.LEFT);
      }
      PlatformUtility.markChatEditorClosed();
      this.closeFloatingControls();
   };
   private void initialize() {
      FeverVisual.getInstance().getEventManager().subscribe(this);
      FeverVisual.getInstance().getEventManager().subscribe(onColorPickerKeyPress);
      Effects effects = new Effects();
      KeyBinds keyBinds = new KeyBinds();
      TargetHud targetHud = new TargetHud();
      WorldElement worldElement = new WorldElement();
      Watermark watermark = new Watermark();
      ArrayListHUD arrayListHUD = new ArrayListHUD();
      InventoryHUD inventoryHUD = new InventoryHUD();
      Hotbar hotbar = new Hotbar();
      ArmorHUD armorHUD = new ArmorHUD();
      CustomScoreboard customScoreboard = new CustomScoreboard();
      CustomTab customTab = new CustomTab();
      Keystrokes keystrokes = new Keystrokes();
      MotionGraph motionGraph = new MotionGraph();
      this.elements.addAll(List.of(effects, keyBinds,hotbar, targetHud, worldElement, watermark, armorHUD, arrayListHUD, inventoryHUD, customScoreboard, customTab, keystrokes, motionGraph, this.island = new DynamicIsland()
      ));
      for (HudElement element : this.elements) {
         this.elementsByName.put(element.getName().toLowerCase(Locale.ROOT), element);
      }
   }

   public Hud() {
      this.initialize();
   }

   public void renderChatOverlays(DrawContext drawContext, float delta) {
      UIContext context = UIContext.of(
              fever.visual.framework.base.CustomDrawContext.isolated(drawContext, fever.visual.framework.base.CustomDrawContext.Pass.POST),
              (int)GuiUtility.getMouse().x(),
              (int)GuiUtility.getMouse().y(),
              delta
      );
      this.renderPopupStack(context);
   }

   private void releaseLabyDraggingIfMouseUp() {
      if (!PlatformUtility.isLabyMod() || !this.hasDraggedElements()) {
         return;
      }

      if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
         this.releaseDraggedElementsAtMouse(MouseButton.LEFT);
      }
   }

   private boolean hasDraggedElements() {
      for (HudElement element : this.elements) {
         if (element.isDragging()) {
            return true;
         }
      }

      return false;
   }

   private void releaseDraggedElementsAtMouse(MouseButton mouseButton) {
      Vector2f mouse = GuiUtility.getMouse();
      this.releaseDraggedElements(mouse.x(), mouse.y(), mouseButton);
   }

   private void releaseDraggedElements(float mouseX, float mouseY, MouseButton mouseButton) {
      for (HudElement element : this.elements) {
         if (element.isDragging()) {
            element.onMouseReleased(mouseX, mouseY, mouseButton);
         }
      }
   }

   private void closeFloatingControls() {
      this.colorPickers.forEach(colorPicker -> colorPicker.setShowing(false));
      this.popups.forEach(popup -> popup.setShowing(false));
   }

   private float getSelectionHeight(HudElement element) {
      if (element instanceof Effects || element instanceof KeyBinds) {
         return element.getHeight();
      }

      return element instanceof HudList ? Math.max(20.0F, element.getHeight()) : element.getHeight();
   }

   public void closeFloatingControlsForVanillaChat() {
      if (!PlatformUtility.isLabyMod() && !PlatformUtility.isLunarClient()) {
         this.closeFloatingControls();
      }
   }

   private void renderPopupStack(UIContext context) {
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, 0.0F, POPUP_Z);

      for (Popup popup : this.popups) {
         if (popup.getY() + popup.getHeight() > sr.getScaledHeight()) {
            popup.setY(sr.getScaledHeight() - 10.0F - popup.getHeight());
         }
         popup.render(context);
      }

      for (ColorPicker colorPicker : this.colorPickers) {
         if (colorPicker.getY() + colorPicker.getHeight() > sr.getScaledHeight()) {
            colorPicker.setY(sr.getScaledHeight() - 10.0F - colorPicker.getHeight());
         }
         colorPicker.render(context);
      }
      this.colorPickers.removeIf(picker -> !picker.isShowing());

      context.getMatrices().pop();
   }

   public List<HudElement> enabledElements() {
      return this.showingEnabledElementsCache;
   }

   public List<HudElement> disabledElements() {
      return this.disabledEnabledElementsCache;
   }

   public <T extends HudElement> T getElementByName(String name) {
      return (T)this.elementsByName.get(name.toLowerCase(Locale.ROOT));
   }

   public List<HudElement> getEnabledElements() {
      return this.enabledElementsCache;
   }

   public void updateElementsVisibility() {
      this.enabledElementsCache.clear();
      this.showingEnabledElementsCache.clear();
      this.disabledEnabledElementsCache.clear();
      for (HudElement element : this.elements) {
         boolean shouldBeVisible = Interface.isHudElementEnabled(element.getName());
         if (shouldBeVisible) {
            this.enabledElementsCache.add(element);
         }
         if (!shouldBeVisible && element.isShowing()) {
            element.setShowing(false);
         } else if (element instanceof CustomTab && element.isShowing()) {
            element.setShowing(false);
         }
         if (shouldBeVisible) {
            if (element.isShowing()) {
               this.showingEnabledElementsCache.add(element);
            } else {
               this.disabledEnabledElementsCache.add(element);
            }
         }
      }
   }

   @Generated
   public List<HudElement> getElements() {
      return this.elements;
   }

   @Generated
   public List<Popup> getPopups() {
      return this.popups;
   }

   @Generated
   public List<ColorPicker> getColorPickers() {
      return this.colorPickers;
   }

   private ColorPicker getTopColorPicker(double mouseX, double mouseY) {
      for (int i = this.colorPickers.size() - 1; i >= 0; i--) {
         ColorPicker colorPicker = this.colorPickers.get(i);
         if (colorPicker.isHovered(mouseX, mouseY) || colorPicker.isMouseCaptured()) {
            return colorPicker;
         }
      }

      return null;
   }

   private void bringColorPickerToFront(ColorPicker colorPicker) {
      int index = this.colorPickers.indexOf(colorPicker);
      if (index >= 0 && index < this.colorPickers.size() - 1) {
         this.colorPickers.remove(index);
         this.colorPickers.add(colorPicker);
      }
   }

   @Generated
   public DynamicIsland getIsland() {
      return this.island;
   }

   @Generated
   public HudHistoryManager getHistoryManager() {
      return this.historyManager;
   }

   @Generated
   public Grid getGrid() {
      return this.grid;
   }

   @Generated
   public String getDesc() {
      return this.desc;
   }

   @Generated
   public AnimatedText getDescText() {
      return this.descText;
   }

   @Generated
   public Timer getTimer() {
      return this.timer;
   }

   @Generated
   public EventListener<HudRenderEvent> getOnHud() {
      return this.onHud;
   }

   @Generated
   public EventListener<ChatRenderEvent> getOnPostHud() {
      return this.onPostHud;
   }

   @Generated
   public EventListener<ChatKeyPressEvent> getOnKeyPress() {
      return this.onKeyPress;
   }

   @Generated
   public EventListener<ChatClickEvent> getOnClick() {
      return this.onClick;
   }

   @Generated
   public EventListener<ChatReleaseEvent> getOnRelease() {
      return this.onRelease;
   }

   @Generated
   public void setDesc(String desc) {
      this.desc = desc;
   }
}
