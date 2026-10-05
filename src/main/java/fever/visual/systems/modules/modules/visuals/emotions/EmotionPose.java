package fever.visual.systems.modules.modules.visuals.emotions;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.math.MathHelper;

public class EmotionPose {
   public float headRotX, headRotY, headRotZ;
   public float bodyRotX, bodyRotY, bodyRotZ;
   public float rightArmRotX, rightArmRotY, rightArmRotZ;
   public float leftArmRotX, leftArmRotY, leftArmRotZ;
   public float rightLegRotX, rightLegRotY, rightLegRotZ;
   public float leftLegRotX, leftLegRotY, leftLegRotZ;
   public float headOffX, headOffY, headOffZ;
   public float bodyOffX, bodyOffY, bodyOffZ;
   public float rightArmOffX, rightArmOffY, rightArmOffZ;
   public float leftArmOffX, leftArmOffY, leftArmOffZ;
   public float rightLegOffX, rightLegOffY, rightLegOffZ;
   public float leftLegOffX, leftLegOffY, leftLegOffZ;
   public float upperOffY, upperOffZ;

   public void lean(float angle) {
      this.bodyRotX += angle;
      this.headRotX += angle;
      this.rightArmRotX += angle;
      this.leftArmRotX += angle;
      float cos = (float)Math.cos(angle);
      float sin = (float)Math.sin(angle);
      float torsoY = 12.0F - 12.0F * cos;
      float torsoZ = -12.0F * sin;
      this.headOffY += torsoY;
      this.headOffZ += torsoZ;
      this.bodyOffY += torsoY;
      this.bodyOffZ += torsoZ;
      float armY = 12.0F - 10.0F * cos - 2.0F;
      float armZ = -10.0F * sin;
      this.rightArmOffY += armY;
      this.rightArmOffZ += armZ;
      this.leftArmOffY += armY;
      this.leftArmOffZ += armZ;
   }

   public void applyTo(BipedEntityModel<?> model, float weight, float walkAmount) {
      if (weight <= 0.0F) {
         return;
      }
      float w = Math.min(weight, 1.0F);
      float legW = w * (1.0F - MathHelper.clamp(walkAmount, 0.0F, 1.0F));
      applyAdditive(model.head, this.headRotX, this.headRotY, this.headRotZ, this.headOffX, this.headOffY + this.upperOffY, this.headOffZ + this.upperOffZ, w);
      apply(model.body, this.bodyRotX, this.bodyRotY, this.bodyRotZ, this.bodyOffX, this.bodyOffY + this.upperOffY, this.bodyOffZ + this.upperOffZ, w);
      apply(model.rightArm, this.rightArmRotX, this.rightArmRotY, this.rightArmRotZ, this.rightArmOffX, this.rightArmOffY + this.upperOffY, this.rightArmOffZ + this.upperOffZ, w);
      apply(model.leftArm, this.leftArmRotX, this.leftArmRotY, this.leftArmRotZ, this.leftArmOffX, this.leftArmOffY + this.upperOffY, this.leftArmOffZ + this.upperOffZ, w);
      apply(model.rightLeg, this.rightLegRotX, this.rightLegRotY, this.rightLegRotZ, this.rightLegOffX, this.rightLegOffY, this.rightLegOffZ, legW);
      apply(model.leftLeg, this.leftLegRotX, this.leftLegRotY, this.leftLegRotZ, this.leftLegOffX, this.leftLegOffY, this.leftLegOffZ, legW);
   }

   private static void applyAdditive(ModelPart part, float rotX, float rotY, float rotZ, float offX, float offY, float offZ, float weight) {
      ModelTransform base = part.getDefaultTransform();
      part.pitch += rotX * weight;
      part.yaw += rotY * weight;
      part.roll += rotZ * weight;
      part.originX = MathHelper.lerp(weight, part.originX, base.x() + offX);
      part.originY = MathHelper.lerp(weight, part.originY, base.y() + offY);
      part.originZ = MathHelper.lerp(weight, part.originZ, base.z() + offZ);
   }

   private static void apply(ModelPart part, float rotX, float rotY, float rotZ, float offX, float offY, float offZ, float weight) {
      ModelTransform base = part.getDefaultTransform();
      part.pitch = MathHelper.lerp(weight, part.pitch, rotX);
      part.yaw = MathHelper.lerp(weight, part.yaw, rotY);
      part.roll = MathHelper.lerp(weight, part.roll, rotZ);
      part.originX = MathHelper.lerp(weight, part.originX, base.x() + offX);
      part.originY = MathHelper.lerp(weight, part.originY, base.y() + offY);
      part.originZ = MathHelper.lerp(weight, part.originZ, base.z() + offZ);
   }
}
