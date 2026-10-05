package fever.visual.mixin.minecraft.client.render.entity.emotions;

import fever.visual.systems.modules.modules.visuals.emotions.Emotion;
import fever.visual.systems.modules.modules.visuals.emotions.EmotionStateHolder;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public abstract class AvatarRenderStateEmotionMixin implements EmotionStateHolder {
   @Unique
   private Emotion fevervisual$emotion;
   @Unique
   private float fevervisual$emotionTime;
   @Unique
   private float fevervisual$emotionWeight;

   @Override
   public Emotion fevervisual$getEmotion() {
      return this.fevervisual$emotion;
   }

   @Override
   public float fevervisual$getEmotionTime() {
      return this.fevervisual$emotionTime;
   }

   @Override
   public float fevervisual$getEmotionWeight() {
      return this.fevervisual$emotionWeight;
   }

   @Override
   public void fevervisual$setEmotion(Emotion emotion, float time, float weight) {
      this.fevervisual$emotion = emotion;
      this.fevervisual$emotionTime = time;
      this.fevervisual$emotionWeight = weight;
   }
}
