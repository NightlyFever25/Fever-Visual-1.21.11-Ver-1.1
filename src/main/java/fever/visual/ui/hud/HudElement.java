package fever.visual.ui.hud;

import fever.visual.utility.render.compat.RenderSystem;
import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.setting.Setting;
import fever.visual.systems.setting.SettingsContainer;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.ui.components.ColorPicker;
import fever.visual.ui.components.popup.Popup;
import fever.visual.ui.hud.impl.island.DynamicIsland;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.interfaces.IScaledResolution;
import fever.visual.utility.render.RenderUtility;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public abstract class   HudElement implements SettingsContainer, IMinecraft {
   protected float x;
   protected float y;
   protected float width;
   protected float height;
   protected final Animation animation = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   protected final Animation visible = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   protected final Animation selecting = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   protected final Animation dragAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation blurAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation loadingAnim = new Animation(700L, 0.0F, Easing.SMOOTH_STEP);
   private final Animation widthAnim = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation heightAnim = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   protected boolean showing;
   protected boolean select;
   private List<Setting> settings = new ArrayList<>();
   private boolean dragging;
   private float dragX;
   private float dragY;
   private float startDragX;
   private float startDragY;
   protected final String name;
   protected final String icon;
   protected final Identifier iconIdentifier;

   public HudElement(String name, String icon) {
      this.name = name;
      this.icon = icon;
      this.iconIdentifier = FeverVisual.id(icon);
   }
   private void saveClientData() {
      try {
         FeverVisual.getInstance().getFileManager().saveClientFiles();
      } catch (Exception var2) {
         FeverVisual.LOGGER.error("Failed to save HUD element data: {}", var2.getMessage());
      }
   }
   public void render(UIContext context) {
      this.update(context);
      float anim = this.animation.getValue() * this.visible.getValue();
      if (anim != 0.0F) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, anim));
         float scale = 0.5F + anim * 0.5F - 0.05F * this.selecting.getValue();
         RenderUtility.scale(context.getMatrices(), this.x + this.width / 2.0F, this.y + this.height / 2.0F, scale);
         this.renderComponent(context);
         RenderUtility.end(context.getMatrices());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   protected abstract void renderComponent(UIContext var1);

   public void update(UIContext context) {
      boolean shouldBeVisible = Interface.isHudElementEnabled(this.getName());
      if (!shouldBeVisible && this.showing) {
         this.setShowing(false);
      } else if (shouldBeVisible && !this.showing && this.show()) {
         this.setShowing(true);
      }

      float oldWidth = this.widthAnim.getValue();
      this.widthAnim.update(this.width);
      float newWidth = this.widthAnim.getValue();
      float widthDelta = newWidth - oldWidth;
      boolean isLeftSide = this.x + this.width / 2.0F < IScaledResolution.sr.getScaledWidth() / 2.0F;
      if (!isLeftSide) {
         this.x -= widthDelta;
      }

      if (widthDelta != 0.0F) {
         for (HudElement otherElement : FeverVisual.getInstance().getHud().getElements()) {
            if (otherElement != this && otherElement.isShowing()) {
               float verticalOverlap = Math.min(this.y + this.height, otherElement.y + otherElement.height) - Math.max(this.y, otherElement.y);
               if (!(verticalOverlap <= 0.0F)) {
                  if (isLeftSide) {
                     float rightEdge = this.x + newWidth;
                     float distanceToOther = otherElement.x - rightEdge;
                     if (distanceToOther >= -5.0F && distanceToOther <= 25.0F) {
                        otherElement.x += widthDelta;
                        otherElement.x = Math.max(0.0F, Math.min(otherElement.x, IScaledResolution.sr.getScaledWidth() - otherElement.width));
                     }
                  } else {
                     float leftEdge = this.x;
                     float distanceToOther = leftEdge - (otherElement.x + otherElement.width);
                     if (distanceToOther >= -5.0F && distanceToOther <= 25.0F) {
                        otherElement.x -= widthDelta;
                        otherElement.x = Math.max(0.0F, Math.min(otherElement.x, IScaledResolution.sr.getScaledWidth() - otherElement.width));
                     }
                  }
               }
            }
         }
      }

      this.width = newWidth;
      this.dragAnim.update(this.dragging);
      this.animation.setEasing(this.showing ? Easing.BAKEK : Easing.BAKEK_BACK);
      this.animation.update(this.showing);
      boolean visibleNow = this.dragging || this.show();
      this.visible.setEasing(visibleNow ? Easing.BAKEK : Easing.BAKEK_BACK);
      this.visible.update(visibleNow);
      this.selecting.update(this.select);
      this.blurAnim.update(this.animation.getValue() >= 0.6F);
      if (this.dragging) {
         this.x = Math.clamp(context.getMouseX() - this.dragX, 0.0F, IScaledResolution.sr.getScaledWidth() - this.width);
         this.y = Math.clamp(context.getMouseY() - this.dragY, 0.0F, IScaledResolution.sr.getScaledHeight() - this.height);
         if (!(this instanceof DynamicIsland)) {
            for (GridLine line : FeverVisual.getInstance().getHud().getGrid().getLines()) {
               if (line.getType() == GridLine.Type.VERTICAL) {
                  this.x = this.snapToLine(line, this.x, List.of(0.0F, this.width, this.width / 2.0F), List.of(0.0F, -this.width, -this.width / 2.0F));
               } else {
                  this.y = this.snapToLine(line, this.y, List.of(0.0F, this.height), List.of(0.0F, -this.height));
               }
            }
         }
      }

      if (this.isHovered(context) && this.animation.getValue() >= 1.0F) {
         CursorUtility.set(CursorType.HAND);
      }
   }

   public void openColorPicker(ColorSetting setting, float mouseX, float mouseY) {
      ColorPicker colorPicker = new ColorPicker(
              mouseX,
              mouseY,
              2.0F,
              setting.isAlpha(),
              setting.getColorSafe(),
              Localizator.translate(setting.getName())
      );

      colorPicker.setOnColorChange(color -> {
         setting.setColor(color);
         FeverVisual.getInstance().getFileManager().writeFile("client");
      });

      colorPicker.setOnClose(() -> {
         FeverVisual.getInstance().getFileManager().writeFile("client");
      });

      FeverVisual.getInstance().getHud().getColorPickers().add(colorPicker);
   }

   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && this.showing) {
         if (button == MouseButton.LEFT) {
            this.dragging = true;
            this.dragX = (float)(mouseX - this.x);
            this.dragY = (float)(mouseY - this.y);
            this.startDragX = this.x;
            this.startDragY = this.y;
         }
      }
   }

   public boolean openSettingsPopup(float mouseX, float mouseY) {
      if (!this.isHovered(mouseX, mouseY) || !this.showing) {
         return false;
      }

      List<Setting> visibleSettings = this.getVisibleSettings();
      if (visibleSettings.isEmpty()) {
         return false;
      }

      this.select = true;
      this.loadingAnim.setValue(0.0F);
      float popupWidth = 110.0F;
      float popupX = this.x + this.width + 6.0F;
      if (popupX + popupWidth > IScaledResolution.sr.getScaledWidth() - 6.0F) {
         popupX = this.x - popupWidth - 6.0F;
      }
      popupX = Math.clamp(popupX, 6.0F, IScaledResolution.sr.getScaledWidth() - popupWidth - 6.0F);
      float popupY = Math.clamp(this.y, 6.0F, IScaledResolution.sr.getScaledHeight() - 24.0F);
      final float colorPickerX = popupX;
      final float colorPickerY = popupY;

      Popup popup = new Popup(popupX, popupY, popupWidth, 6.0F)
              .title(Localizator.translate("settings"))
              .separator();
      popup.getAnimation().setValue(0.35F);

      for (Setting setting : this.getSettings()) {
         try {
            if (setting instanceof ColorSetting) {
               ColorSetting colorSetting = (ColorSetting) setting;
               popup.add(new HudColorSettingComponent(colorSetting, popup, () -> this.openColorPicker(colorSetting, colorPickerX, colorPickerY)));
            } else {
               popup.setting(setting);
            }
         } catch (Exception exception) {
            FeverVisual.LOGGER.error("Failed to create HUD popup setting {} for {}", setting.getName(), this.name, exception);
         }
      }

      popup.onClose(() -> this.select = false);
      FeverVisual.getInstance().getHud().getPopups().add(popup);
      return true;
   }

   private static final class HudColorSettingComponent extends MenuSettingComponent<ColorSetting> {
      private static final float COLOR_DOT_SIZE = 5.0F;
      private final Runnable openPicker;
      private final Animation hover = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);

      private HudColorSettingComponent(ColorSetting setting, CustomComponent parent, Runnable openPicker) {
         super(setting, parent);
         this.openPicker = openPicker;
      }

      @Override
      protected void renderComponent(UIContext context) {
         this.hover.update(this.isHovered(context));
         if (this.isHovered(context)) {
            CursorUtility.set(CursorType.HAND);
         }

         Font nameFont = Fonts.REGULAR.getFont(8.0F);
         float leftPadding = 10.0F;
         float headerHeight = 19.0F;
         context.drawFadeoutText(
                 nameFont,
                 Localizator.translate(this.setting.getName()),
                 this.x + leftPadding,
                 this.y + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight) - 0.5F,
                 Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hover.getValue())),
                 0.7F,
                 0.99F,
                 this.width - 28.0F
         );
         float dotX = this.x + this.width - leftPadding - 5.0F;
         float dotY = this.y + headerHeight / 2.0F;
         context.drawLegacyRoundedRect(
                 dotX - COLOR_DOT_SIZE / 2.0F,
                 dotY - COLOR_DOT_SIZE / 2.0F,
                 COLOR_DOT_SIZE,
                 COLOR_DOT_SIZE,
                 BorderRadius.all(COLOR_DOT_SIZE / 2.0F),
                 this.setting.getColorSafe()
         );
      }

      @Override
      protected void onVisibleMouseClicked(double mouseX, double mouseY, MouseButton button) {
         if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
            this.openPicker.run();
         }

         super.onVisibleMouseClicked(mouseX, mouseY, button);
      }

      @Override
      public float getHeight() {
         return this.height = 18.0F;
      }
   }

   public boolean hasVisibleSettings() {
      for (Setting setting : this.settings) {
         if (!setting.getHideCondition().getAsBoolean()) {
            return true;
         }
      }
      return false;
   }

   private List<Setting> getVisibleSettings() {
      List<Setting> visible = new ArrayList<>(this.settings.size());
      for (Setting setting : this.settings) {
         if (!setting.getHideCondition().getAsBoolean()) {
            visible.add(setting);
         }
      }
      return visible;
   }

   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (this.dragging && button == MouseButton.LEFT) {
         this.dragging = false;
         if (this.x != this.startDragX || this.y != this.startDragY) {
            FeverVisual.getInstance().getHud().getHistoryManager().registerMove(this, this.startDragX, this.startDragY, this.x, this.y);
         }

         FeverVisual.getInstance().getFileManager().writeFile("client");
      }
   }

   private float snapToLine(GridLine line, float pos, List<Float> offsets, List<Float> adjustments) {
      for (int i = 0; i < offsets.size(); i++) {
         float distance = Math.abs(pos + offsets.get(i) - line.getPos());
         if (distance < 25.0F) {
            line.setActive(true);
         }

         if (distance < 5.0F) {
            pos = line.getPos() + adjustments.get(i);
         }
      }

      return pos;
   }

   public boolean show() {
      return true;
   }

   public boolean isHovered(float mouseX, float mouseY) {
      return GuiUtility.isHovered((double)this.x, (double)this.y, (double)this.width, (double)this.height, (double)mouseX, (double)mouseY);
   }

   public boolean isHovered(double mouseX, double mouseY) {
      return GuiUtility.isHovered((double)this.x, (double)this.y, (double)this.width, (double)this.height, mouseX, mouseY);
   }

   public boolean isHovered(UIContext context) {
      return this.isHovered((float)context.getMouseX(), (float)context.getMouseY());
   }

   public void pos(float x, float y) {
      this.x = x;
      this.y = y;
   }

   @Generated
   public float getX() {
      return this.x;
   }

   @Generated
   public float getY() {
      return this.y;
   }

   @Generated
   public float getWidth() {
      return this.width;
   }

   @Generated
   public float getHeight() {
      return this.height;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public Animation getVisible() {
      return this.visible;
   }

   @Generated
   public Animation getSelecting() {
      return this.selecting;
   }

   @Generated
   public Animation getDragAnim() {
      return this.dragAnim;
   }

   @Generated
   public Animation getBlurAnim() {
      return this.blurAnim;
   }

   @Generated
   public Animation getLoadingAnim() {
      return this.loadingAnim;
   }

   @Generated
   public Animation getWidthAnim() {
      return this.widthAnim;
   }

   @Generated
   public Animation getHeightAnim() {
      return this.heightAnim;
   }

   @Generated
   public boolean isShowing() {
      return this.showing;
   }

   @Generated
   public boolean isSelect() {
      return this.select;
   }

   @Generated
   @Override
   public List<Setting> getSettings() {
      return this.settings;
   }

   @Generated
   public boolean isDragging() {
      return this.dragging;
   }

   @Generated
   public float getDragX() {
      return this.dragX;
   }

   @Generated
   public float getDragY() {
      return this.dragY;
   }

   @Generated
   public float getStartDragX() {
      return this.startDragX;
   }

   @Generated
   public float getStartDragY() {
      return this.startDragY;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String getIcon() {
      return this.icon;
   }

   @Generated
   public Identifier getIconIdentifier() {
      return this.iconIdentifier;
   }

   @Generated
   public void setX(float x) {
      this.x = x;
   }

   @Generated
   public void setY(float y) {
      this.y = y;
   }

   @Generated
   public void setWidth(float width) {
      this.width = width;
   }

   @Generated
   public void setHeight(float height) {
      this.height = height;
   }

   @Generated
   public void setShowing(boolean showing) {
      this.showing = showing;
      this.saveClientData();
   }

   @Generated
   public void setSelect(boolean select) {
      this.select = select;
   }

   @Generated
   public void setSettings(List<Setting> settings) {
      this.settings = settings;
   }

   @Generated
   public void setDragging(boolean dragging) {
      this.dragging = dragging;
   }

   @Generated
   public void setDragX(float dragX) {
      this.dragX = dragX;
   }

   @Generated
   public void setDragY(float dragY) {
      this.dragY = dragY;
   }

   @Generated
   public void setStartDragX(float startDragX) {
      this.startDragX = startDragX;
   }

   @Generated
   public void setStartDragY(float startDragY) {
      this.startDragY = startDragY;
   }
}
