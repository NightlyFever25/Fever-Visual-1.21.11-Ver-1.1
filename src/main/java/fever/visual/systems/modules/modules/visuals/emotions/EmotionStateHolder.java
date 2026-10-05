package fever.visual.systems.modules.modules.visuals.emotions;

public interface EmotionStateHolder {
   Emotion fevervisual$getEmotion();
   float fevervisual$getEmotionTime();
   float fevervisual$getEmotionWeight();
   void fevervisual$setEmotion(Emotion emotion, float time, float weight);
}
