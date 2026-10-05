package fever.visual.mixin.minecraft.client.render.entity.emotions;

import fever.visual.systems.modules.modules.visuals.emotions.EmotionPlayback;
import fever.visual.systems.modules.modules.visuals.emotions.EmotionStateHolder;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class AvatarRendererEmotionMixin {
   @Inject(method = "updateRenderState", at = @At("TAIL"), require = 0)
   private void fevervisual$extractEmotion(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
      EmotionPlayback.fill((EmotionStateHolder)state, (Entity)entity);
   }
}
