package fever.visual.systems.modules.modules.combat;


import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.HandRenderEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@ModuleInfo(
        name = "Swing Animation",
        category = ModuleCategory.COMBAT,
        desc = "Изменяет анимации рук при взмахе"
)
public class SwingAnimation extends BaseModule {
   private final ModeSetting swingType = new ModeSetting(this, "modules.settings.swing_animation.type");
   private final ModeSetting.Value chop = new ModeSetting.Value(this.swingType, "modules.settings.swing_animation.type.chop").select();
   private final ModeSetting.Value swipe = new ModeSetting.Value(this.swingType, "modules.settings.swing_animation.type.swipe");
   private final ModeSetting.Value down = new ModeSetting.Value(this.swingType, "modules.settings.swing_animation.type.down");
   private final ModeSetting.Value smooth = new ModeSetting.Value(this.swingType, "modules.settings.swing_animation.type.smooth");
   private final ModeSetting.Value smooth2 = new ModeSetting.Value(this.swingType, "modules.settings.swing_animation.type.smooth2");
   private final ModeSetting.Value power = new ModeSetting.Value(this.swingType, "modules.settings.swing_animation.type.power");
   private final ModeSetting.Value feast = new ModeSetting.Value(this.swingType, "modules.settings.swing_animation.type.feast");
   private final ModeSetting.Value twist = new ModeSetting.Value(this.swingType, "modules.settings.swing_animation.type.twist");
   private final SliderSetting hitStrength = new SliderSetting(this, "modules.settings.swing_animation.hit_strength")
           .min(0.5F).max(3.0F).step(0.05F).currentValue(1.0F);
   private final SliderSetting swingSpeed = new SliderSetting(this, "modules.settings.swing_animation.speed")
           .min(0.5F).max(4.0F).step(0.05F).currentValue(1.0F);
   private final SliderSetting itemSize = new SliderSetting(this, "modules.settings.swing_animation.item_size")
           .min(0.1F).max(2.0F).step(0.05F).currentValue(1.0F);
   private final EventListener<HandRenderEvent> onHandRender = event -> {
      if (this.isEnabled()) {
         if (event.getArm() == Arm.RIGHT) {
            ItemStack itemStack = event.getItemStack();
            if (!this.shouldApplyAnimation(itemStack)) {
               return;
            }
            MatrixStack matrices = event.getMatrices();
            float swingProgress = event.getSwingProgress();
            this.applyFeverAnimation(matrices, event.getArm(), swingProgress);
            float scale = this.itemSize.getCurrentValue();
            matrices.scale(scale, scale, scale);
            event.setFullTransform(true);
            event.cancel();
         }
      }
   };

   private void applyFeverAnimation(MatrixStack matrix, Arm arm, float swingProgress) {
      int i = arm == Arm.RIGHT ? 1 : -1;
      float sin1 = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
      float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float)Math.PI);
      float sinSmooth = (float)(Math.sin(swingProgress * Math.PI) * 0.5F);
      float strength = this.hitStrength.getCurrentValue();

      if (this.chop.isSelected()) {
         matrix.translate(0.56F * i, -0.44F, -0.72F);
         matrix.translate(0.0F, 0.33F * -0.6F, 0.0F);
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * i));
         matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * -20.0F * i * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
         matrix.translate(0.4F, 0.2F, 0.2F);
         matrix.translate(-0.5F, 0.08F, 0.0F);
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
      } else if (this.twist.isSelected()) {
         matrix.translate(i * 0.56F, -0.36F, -0.72F);
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80.0F * i));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((sin1 - sin2) * 60.0F * i * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F));
         matrix.translate(0.0F, -0.1F, 0.05F);
      } else if (this.swipe.isSelected()) {
         matrix.translate(0.56F * i, -0.32F, -0.72F);
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F * i));
         matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * i));
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -120.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
      } else if (this.down.isSelected()) {
         matrix.translate(i * 0.56F, -0.32F, -0.72F);
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F * i));
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength));
         matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
      } else if (this.smooth.isSelected()) {
         matrix.translate(i * 0.56F, -0.42F, -0.72F);
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + sin1 * -20.0F * strength)));
         matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * sin2 * -20.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
         matrix.translate(0.0F, -0.1F, 0.0F);
      } else if (this.smooth2.isSelected()) {
         matrix.translate(i * 0.56F, -0.42F, -0.72F);
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
         matrix.translate(0.0F, -0.1F, 0.0F);
      } else if (this.power.isSelected()) {
         matrix.translate(i * 0.56F, -0.32F, -0.72F);
         matrix.translate((-sinSmooth * sinSmooth * sin1) * i * strength, 0.0F, 0.0F);
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61.0F * i));
         matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength));
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -30.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60.0F * strength));
      } else if (this.feast.isSelected()) {
         matrix.translate(i * 0.56F, -0.32F, -0.72F);
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * i));
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * i * strength));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45.0F * strength));
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * i));
         matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
         matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * i));
      }
   }

   public float getDurationMultiplier() {
      return this.swingSpeed.getCurrentValue();
   }

   public boolean shouldApplyAnimation(ItemStack itemStack) {
      Item item = itemStack.getItem();
      return item != Items.AIR
              && item != Items.FILLED_MAP
              && item != Items.CROSSBOW
              && item != Items.BOW
              && item != Items.TRIDENT
              && item.getUseAction(itemStack) != UseAction.DRINK
              && item.getUseAction(itemStack) != UseAction.EAT;
   }
}
