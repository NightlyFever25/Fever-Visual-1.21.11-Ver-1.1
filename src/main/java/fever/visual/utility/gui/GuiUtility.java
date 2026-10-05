package fever.visual.utility.gui;

import lombok.Generated;
import fever.visual.framework.base.CustomComponent;
import fever.visual.framework.base.UIContext;
import fever.visual.systems.setting.Setting;
import fever.visual.systems.setting.settings.BezierSetting;
import fever.visual.systems.setting.settings.BindSetting;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ButtonSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.RangeSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.systems.setting.settings.StringSetting;
import fever.visual.ui.menu.dropdown.components.settings.MenuSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.BezierSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.BindSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.BooleanSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.ButtonSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.ColorSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.ModeSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.RangeSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.SelectSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.SliderSettingComponent;
import fever.visual.ui.menu.dropdown.components.settings.impl.StringSettingComponent;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.interfaces.IScaledResolution;
import fever.visual.utility.render.obj.Rect;
import net.minecraft.client.util.math.Vector2f;

public final class GuiUtility {
   public static float getMiddleOfBox(float objectHeight, float boxHeight) {
      return (float)Math.ceil(boxHeight / 2.0F - objectHeight / 2.0F);
   }

   public static double getMiddleOfBox(double objectHeight, double boxHeight) {
      return Math.ceil(boxHeight / 2.0 - objectHeight / 2.0);
   }

   public static boolean isHovered(double x, double y, double width, double height, int mouseX, int mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
   }

   public static boolean isHovered(double x, double y, double width, double height, UIContext context) {
      return isHovered(x, y, width, height, context.getMouseX(), context.getMouseY());
   }

   public static boolean isHovered(Rect rect, double mouseX, double mouseY) {
      return isHovered((double)rect.getX(), (double)rect.getY(), (double)rect.getWidth(), (double)rect.getHeight(), mouseX, mouseY);
   }

   public static boolean isHovered(CustomComponent rect, double mouseX, double mouseY) {
      return isHovered((double)rect.getX(), (double)rect.getY(), (double)rect.getWidth(), (double)rect.getHeight(), mouseX, mouseY);
   }

   public static boolean isHovered(double x, double y, double width, double height, double mouseX, double mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
   }

   public static float getSliderValue(float min, float max, float start, float size, double mouse) {
      return (float)(Math.min(1.0, Math.max(0.0, (mouse - start) / size)) * (max - min)) + min;
   }

   public static float getSliderValueWithoutClamp(float min, float max, float start, float size, double mouse) {
      return (float)((mouse - start) / size * (max - min)) + min;
   }

   public static float getPercent(float value, float min, float max) {
      return (value - min) / (max - min);
   }

   public static Vector2f getMouse() {
      return new Vector2f(
         (float)(IMinecraft.mc.mouse.getX() / IScaledResolution.sr.getScaleFactor()),
         (float)(IMinecraft.mc.mouse.getY() / IScaledResolution.sr.getScaleFactor())
      );
   }

   public static MenuSettingComponent settinge(Setting setting, CustomComponent parent) {
      MenuSettingComponent settingComponent = null;
      if (setting instanceof BooleanSetting s) {
         settingComponent = new BooleanSettingComponent(s, parent);
      } else if (setting instanceof BindSetting s) {
         settingComponent = new BindSettingComponent(s, parent);
      } else if (setting instanceof ColorSetting s) {
         settingComponent = new ColorSettingComponent(s, parent);
      } else if (setting instanceof ModeSetting s) {
         settingComponent = new ModeSettingComponent(s, parent);
      } else if (setting instanceof RangeSetting s) {
         settingComponent = new RangeSettingComponent(s, parent);
      } else if (setting instanceof BezierSetting s) {
         settingComponent = new BezierSettingComponent(s, parent);
      } else if (setting instanceof ButtonSetting s) {
         settingComponent = new ButtonSettingComponent(s, parent);
      } else if (setting instanceof SelectSetting s) {
         settingComponent = new SelectSettingComponent(s, parent);
      } else if (setting instanceof SliderSetting s) {
         settingComponent = new SliderSettingComponent(s, parent);
      } else if (setting instanceof StringSetting s) {
         settingComponent = new StringSettingComponent(s, parent);
      }

      if (settingComponent != null) {
         settingComponent.onInit();
      }

      return settingComponent;
   }

   @Generated
   private GuiUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
