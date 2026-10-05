package fever.visual.systems.modules.modules.visuals.emotions;

import java.util.Locale;

public enum Emotion {
   WAVE("WAVE", 3.6F),
   JERK("JERK", 3.0F),
   SHY("SHY", 3.4F),
   DANCE("DANCE", 4.2F),
   CLAP("CLAP", 3.0F),
   BOW("BOW", 4.2F),
   FACEPALM("FACEPALM", 3.4F),
   POINT("POINT", 2.6F),
   TWERK("TWERK", 3.6F);

   private final String displayName;
   private final float duration;

   Emotion(String displayName, float duration) {
      this.displayName = displayName;
      this.duration = duration;
   }

   public String displayName() {
      return this.displayName;
   }

   public float duration() {
      return this.duration;
   }

   public void apply(EmotionPose pose, float time) {
      switch (this) {
         case WAVE -> {
            float swing = (float)Math.sin(time * 7.0F);
            pose.rightArmRotX = -2.95F;
            pose.rightArmRotZ = -0.2F + swing * 0.34F;
            pose.leftArmRotZ = -0.09F;
            pose.leftArmRotX = 0.06F;
            pose.headRotZ = -0.1F;
            pose.headRotY = (float)Math.sin(time * 3.5F) * 0.16F;
            pose.headRotX = -0.06F;
            pose.bodyRotY = 0.1F;
            pose.upperOffY = swing * 0.12F;
         }
         case JERK -> {
            float shake = (float)Math.sin(time * 19.0F);
            pose.rightArmRotX = -0.78F + shake * 0.32F;
            pose.rightArmRotZ = -0.42F;
            pose.rightArmRotY = 0.28F;
            pose.leftArmRotZ = -0.17F;
            pose.leftArmRotX = 0.12F;
            pose.bodyRotX = 0.07F + shake * 0.05F;
            pose.headRotX = -0.28F + shake * 0.1F;
            pose.headRotZ = 0.09F;
            pose.rightLegRotZ = 0.09F;
            pose.leftLegRotZ = -0.09F;
            pose.upperOffY = shake * 0.3F;
         }
         case SHY -> {
            float sway = (float)Math.sin(time * 2.4F);
            float fidget = (float)Math.sin(time * 5.0F) * 0.045F;
            pose.rightArmRotX = -2.36F + fidget;
            pose.rightArmRotZ = 0.46F;
            pose.rightArmRotY = -0.14F;
            pose.leftArmRotX = -2.36F - fidget;
            pose.leftArmRotZ = -0.46F;
            pose.leftArmRotY = 0.14F;
            pose.headRotX = 0.3F;
            pose.headRotY = sway * 0.26F;
            pose.headRotZ = 0.05F;
            pose.bodyRotX = 0.1F;
            pose.bodyRotY = sway * 0.12F;
            pose.rightLegRotZ = 0.13F;
            pose.leftLegRotZ = -0.13F;
            pose.rightLegRotY = 0.16F;
            pose.leftLegRotY = -0.16F;
            pose.upperOffY = 0.45F;
         }
         case DANCE -> {
            float beat = (float)Math.sin(time * 6.0F);
            float slow = (float)Math.sin(time * 3.0F);
            pose.rightArmRotZ = 2.15F + beat * 0.45F;
            pose.rightArmRotX = -0.35F + beat * 0.25F;
            pose.leftArmRotZ = -2.15F + beat * 0.45F;
            pose.leftArmRotX = -0.35F - beat * 0.25F;
            pose.bodyRotY = slow * 0.34F;
            pose.bodyRotZ = beat * 0.1F;
            pose.headRotY = slow * 0.42F;
            pose.headRotZ = beat * 0.16F;
            pose.rightLegRotX = beat * 0.34F;
            pose.leftLegRotX = -beat * 0.34F;
            pose.upperOffY = -Math.abs(beat) * 0.8F;
         }
         case CLAP -> {
            float clap = ((float)Math.sin(time * 9.0F) + 1.0F) * 0.5F;
            pose.rightArmRotX = -1.45F;
            pose.leftArmRotX = -1.45F;
            pose.rightArmRotY = -0.36F - clap * 0.3F;
            pose.leftArmRotY = 0.36F + clap * 0.3F;
            pose.rightArmRotZ = 0.22F;
            pose.leftArmRotZ = -0.22F;
            pose.headRotX = 0.1F + clap * 0.07F;
            pose.bodyRotX = 0.06F;
            pose.upperOffY = clap * 0.22F;
         }
         case BOW -> {
            float phase = 0.5F - (float)Math.cos(time * 1.5F) * 0.5F;
            float bow = phase * phase * (3.0F - 2.0F * phase);
            pose.lean(1.15F * bow);
            pose.rightArmRotX += -1.15F * bow;
            pose.rightArmRotZ = -0.72F * bow;
            pose.leftArmRotX += 0.25F * bow;
            pose.leftArmRotZ = -0.2F * bow;
            pose.headRotX += 0.3F * bow;
            pose.rightLegRotX = -0.1F * bow;
         }
         case FACEPALM -> {
            float shake = (float)Math.sin(time * 1.8F);
            pose.rightArmRotX = -2.52F;
            pose.rightArmRotZ = 0.34F;
            pose.rightArmRotY = -0.14F;
            pose.leftArmRotZ = -0.12F;
            pose.leftArmRotX = 0.1F;
            pose.headRotX = 0.42F;
            pose.headRotY = shake * 0.2F;
            pose.bodyRotX = 0.14F;
            pose.upperOffY = 0.25F;
         }
         case POINT -> {
            float jab = (float)Math.sin(time * 4.0F);
            pose.rightArmRotX = -1.52F + jab * 0.09F;
            pose.rightArmRotY = -0.16F;
            pose.rightArmRotZ = 0.06F;
            pose.leftArmRotZ = -0.11F;
            pose.leftArmRotX = 0.08F;
            pose.headRotY = -0.1F;
            pose.bodyRotY = -0.16F;
            pose.upperOffY = jab * 0.07F;
         }
         case TWERK -> {
            float shake = (float)Math.sin(time * 14.0F);
            float squat = 0.24F;
            pose.lean(1.02F + shake * 0.1F);
            pose.rightLegRotX = -squat;
            pose.leftLegRotX = -squat;
            pose.rightLegRotZ = 0.22F;
            pose.leftLegRotZ = -0.22F;
            pose.upperOffY += 12.0F - 12.0F * (float)Math.cos(squat);
            pose.upperOffZ = 1.7F + shake * 1.4F;
            pose.rightArmRotX += -0.55F + shake * 0.1F;
            pose.leftArmRotX += -0.55F + shake * 0.1F;
            pose.rightArmRotZ = 0.32F;
            pose.leftArmRotZ = -0.32F;
            pose.headRotX -= 0.8F;
         }
      }
   }

   public static Emotion byDisplayName(String name) {
      if (name == null) {
         return null;
      }
      for (Emotion emotion : values()) {
         if (emotion.displayName.equalsIgnoreCase(name) || emotion.name().equalsIgnoreCase(name)) {
            return emotion;
         }
      }
      return null;
   }

   public static String normalize(String name) {
      return name.toLowerCase(Locale.ROOT).replace("_", " ");
   }
}
