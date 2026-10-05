package fever.visual.mixin.minecraft.client.render.entity.emotions;

import fever.visual.systems.modules.modules.visuals.emotions.EmotionPlayback;
import fever.visual.systems.modules.modules.visuals.emotions.EmotionStateHolder;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class HumanoidModelEmotionMixin {
   @Inject(method = "setAngles", at = @At("TAIL"), require = 0)
   private void fevervisual$applyEmotion(BipedEntityRenderState state, CallbackInfo ci) {
      if (state instanceof PlayerEntityRenderState) {
         EmotionPlayback.applyTo((BipedEntityModel<?>)(Object)this, (EmotionStateHolder)state, state.limbSwingAmplitude);
      }
   }
}
