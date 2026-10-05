package fever.visual.mixin.accessors;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BipedEntityModel.class)
public interface BipedEntityModelAccessor {
   @Accessor("head")
   ModelPart fevervisual$getHead();

   @Accessor("hat")
   ModelPart fevervisual$getHat();

   @Accessor("leftArm")
   ModelPart fevervisual$getLeftArm();

   @Accessor("rightArm")
   ModelPart fevervisual$getRightArm();

   @Accessor("leftLeg")
   ModelPart fevervisual$getLeftLeg();

   @Accessor("rightLeg")
   ModelPart fevervisual$getRightLeg();
}