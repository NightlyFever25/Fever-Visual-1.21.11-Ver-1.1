package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.ChatRenderEvent;
import fever.visual.systems.event.impl.render.HandRenderEvent;
import fever.visual.systems.event.impl.window.ChatClickEvent;
import fever.visual.systems.event.impl.window.ChatReleaseEvent;
import fever.visual.systems.event.impl.window.MouseScrollEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.gui.GuiUtility;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@ModuleInfo(name = "View Model", category = ModuleCategory.VISUALS, desc = "modules.descriptions.view_model")
public class ViewModel extends BaseModule {
   private final ModeSetting controlMode = new ModeSetting(this, "Control");
   private final ModeSetting.Value guiControl = new ModeSetting.Value(this.controlMode, "GUI").select();
   private final ModeSetting.Value chatControl = new ModeSetting.Value(this.controlMode, "Chat");
   private final SliderSetting mainTranslateX = new SliderSetting(this, "modules.settings.view_model.main_translate_x", () -> !this.guiControl.isSelected())
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.05F);
   private final SliderSetting mainTranslateY = new SliderSetting(this, "modules.settings.view_model.main_translate_y", () -> !this.guiControl.isSelected())
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.05F);
   private final SliderSetting mainTranslateZ = new SliderSetting(this, "modules.settings.view_model.main_translate_z", () -> !this.guiControl.isSelected())
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.05F);
   private final SliderSetting mainRotateX = new SliderSetting(this, "modules.settings.view_model.main_rotate_x", () -> !this.guiControl.isSelected())
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting mainRotateY = new SliderSetting(this, "modules.settings.view_model.main_rotate_y", () -> !this.guiControl.isSelected())
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting mainRotateZ = new SliderSetting(this, "modules.settings.view_model.main_rotate_z", () -> !this.guiControl.isSelected())
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting offTranslateX = new SliderSetting(this, "modules.settings.view_model.off_translate_x", () -> !this.guiControl.isSelected())
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.05F);
   private final SliderSetting offTranslateY = new SliderSetting(this, "modules.settings.view_model.off_translate_y", () -> !this.guiControl.isSelected())
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.05F);
   private final SliderSetting offTranslateZ = new SliderSetting(this, "modules.settings.view_model.off_translate_z", () -> !this.guiControl.isSelected())
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.05F);
   private final SliderSetting offRotateX = new SliderSetting(this, "modules.settings.view_model.off_rotate_x", () -> !this.guiControl.isSelected())
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting offRotateY = new SliderSetting(this, "modules.settings.view_model.off_rotate_y", () -> !this.guiControl.isSelected())
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting offRotateZ = new SliderSetting(this, "modules.settings.view_model.off_rotate_z", () -> !this.guiControl.isSelected())
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting mainScale = new SliderSetting(this, "Main Scale", () -> !this.guiControl.isSelected())
      .min(0.2F).max(5.0F).currentValue(1.0F).step(0.05F);
   private final SliderSetting offScale = new SliderSetting(this, "Off Scale", () -> !this.guiControl.isSelected())
      .min(0.2F).max(5.0F).currentValue(1.0F).step(0.05F);

   private long lastChatClick;
   private boolean lastClickedMain;
   private boolean dragging;
   private boolean draggingMain;
   private float grabMouseX;
   private float grabMouseY;
   private float grabTranslateX;
   private float grabTranslateY;

   private final EventListener<ChatClickEvent> onChatClick = event -> {
      if (!this.chatControl.isSelected() || !(mc.currentScreen instanceof ChatScreen) || event.getButton() != 0 || mc.player == null) {
         return;
      }
      Arm clickedArm = event.getX() >= mc.getWindow().getScaledWidth() * 0.5F ? Arm.RIGHT : Arm.LEFT;
      boolean clickedMain = clickedArm == mc.player.getMainArm();
      long now = System.currentTimeMillis();
      if (clickedMain == this.lastClickedMain && now - this.lastChatClick <= 300L) {
         this.dragging = true;
         this.draggingMain = clickedMain;
         this.grabMouseX = event.getX();
         this.grabMouseY = event.getY();
         this.grabTranslateX = (clickedMain ? this.mainTranslateX : this.offTranslateX).getCurrentValue();
         this.grabTranslateY = (clickedMain ? this.mainTranslateY : this.offTranslateY).getCurrentValue();
         event.setHandled(true);
      }
      this.lastClickedMain = clickedMain;
      this.lastChatClick = now;
   };

   private final EventListener<ChatReleaseEvent> onChatRelease = event -> {
      if (event.getButton() == 0 && this.dragging) {
         this.dragging = false;
         event.setHandled(true);
      }
   };

   private final EventListener<ChatRenderEvent> onChatRender = event -> {
      if (!this.chatControl.isSelected() || !this.dragging || !(mc.currentScreen instanceof ChatScreen)) {
         return;
      }
      Vector2f mouse = GuiUtility.getMouse();
      float unitsPerPixel = 3.2F / Math.max(1.0F, mc.getWindow().getScaledHeight());
      float dx = mouse.x() - this.grabMouseX;
      float dy = mouse.y() - this.grabMouseY;
      SliderSetting xSetting = this.draggingMain ? this.mainTranslateX : this.offTranslateX;
      SliderSetting ySetting = this.draggingMain ? this.mainTranslateY : this.offTranslateY;
      xSetting.setCurrentValue(MathHelper.clamp(this.grabTranslateX + dx * unitsPerPixel, -2.0F, 2.0F));
      ySetting.setCurrentValue(MathHelper.clamp(this.grabTranslateY - dy * unitsPerPixel, -2.0F, 2.0F));
   };

   private final EventListener<MouseScrollEvent> onMouseScroll = event -> {
      if (!this.chatControl.isSelected() || !(mc.currentScreen instanceof ChatScreen)) {
         return;
      }
      boolean main = this.dragging ? this.draggingMain : GuiUtility.getMouse().x() >= mc.getWindow().getScaledWidth() * 0.5F == (mc.player != null && mc.player.getMainArm() == Arm.RIGHT);
      SliderSetting scale = main ? this.mainScale : this.offScale;
      scale.setCurrentValue(MathHelper.clamp(scale.getCurrentValue() + (event.getVerticalAmount() > 0.0D ? 0.22F : -0.22F), 0.2F, 5.0F));
   };

   private final EventListener<HandRenderEvent> onHandRender = event -> {
      MatrixStack matrices = event.getMatrices();
      boolean isMain = mc.player == null ? event.getArm() == Arm.RIGHT : event.getArm() == mc.player.getMainArm();
      float translateX = isMain ? this.mainTranslateX.getCurrentValue() : this.offTranslateX.getCurrentValue();
      float translateY = isMain ? this.mainTranslateY.getCurrentValue() : this.offTranslateY.getCurrentValue();
      float translateZ = isMain ? this.mainTranslateZ.getCurrentValue() : this.offTranslateZ.getCurrentValue();
      float rotateX = isMain ? this.mainRotateX.getCurrentValue() : this.offRotateX.getCurrentValue();
      float rotateY = isMain ? this.mainRotateY.getCurrentValue() : this.offRotateY.getCurrentValue();
      float rotateZ = isMain ? this.mainRotateZ.getCurrentValue() : this.offRotateZ.getCurrentValue();
      matrices.translate(translateX, translateY, translateZ);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotateX));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotateY));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotateZ));
      float scale = isMain ? this.mainScale.getCurrentValue() : this.offScale.getCurrentValue();
      matrices.scale(scale, scale, scale);
   };

   @Override
   public void onDisable() {
      this.dragging = false;
   }
}
