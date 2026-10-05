package fever.visual.systems.modules.modules.visuals.emotions;

import fever.visual.systems.modules.modules.visuals.Emotions;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;

public final class EmotionPlayback implements IMinecraft {
   private static final EmotionPose POSE = new EmotionPose();
   private static Emotion previewEmotion;
   private static float previewTime;

   public static void fill(EmotionStateHolder holder, Entity entity) {
      if (previewEmotion != null && entity == mc.player) {
         holder.fevervisual$setEmotion(previewEmotion, previewTime, 1.0F);
         return;
      }

      Emotions module = Emotions.getModule();
      if (module == null || !module.isEnabled() || entity != mc.player || !module.isPlaying()) {
         holder.fevervisual$setEmotion(null, 0.0F, 0.0F);
         return;
      }
      holder.fevervisual$setEmotion(module.getCurrentEmotion(), module.getPlaybackTime(), module.getPlaybackWeight());
   }

   public static void applyTo(BipedEntityModel<?> model, EmotionStateHolder holder, float walkAmount) {
      Emotion emotion = holder.fevervisual$getEmotion();
      if (emotion == null) {
         return;
      }
      POSE_RESETTER.reset(POSE);
      emotion.apply(POSE, holder.fevervisual$getEmotionTime());
      POSE.applyTo(model, holder.fevervisual$getEmotionWeight(), walkAmount);
   }

   public static void withPreview(Emotion emotion, float time, Runnable renderer) {
      Emotion previousEmotion = previewEmotion;
      float previousTime = previewTime;
      previewEmotion = emotion;
      previewTime = time;
      try {
         renderer.run();
      } finally {
         previewEmotion = previousEmotion;
         previewTime = previousTime;
      }
   }

   private static final class POSE_RESETTER {
      private static void reset(EmotionPose pose) {
         pose.headRotX = pose.headRotY = pose.headRotZ = 0.0F;
         pose.bodyRotX = pose.bodyRotY = pose.bodyRotZ = 0.0F;
         pose.rightArmRotX = pose.rightArmRotY = pose.rightArmRotZ = 0.0F;
         pose.leftArmRotX = pose.leftArmRotY = pose.leftArmRotZ = 0.0F;
         pose.rightLegRotX = pose.rightLegRotY = pose.rightLegRotZ = 0.0F;
         pose.leftLegRotX = pose.leftLegRotY = pose.leftLegRotZ = 0.0F;
         pose.headOffX = pose.headOffY = pose.headOffZ = 0.0F;
         pose.bodyOffX = pose.bodyOffY = pose.bodyOffZ = 0.0F;
         pose.rightArmOffX = pose.rightArmOffY = pose.rightArmOffZ = 0.0F;
         pose.leftArmOffX = pose.leftArmOffY = pose.leftArmOffZ = 0.0F;
         pose.rightLegOffX = pose.rightLegOffY = pose.rightLegOffZ = 0.0F;
         pose.leftLegOffX = pose.leftLegOffY = pose.leftLegOffZ = 0.0F;
         pose.upperOffY = pose.upperOffZ = 0.0F;
      }
   }

   private EmotionPlayback() {
   }
}
