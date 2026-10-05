package fever.visual.utility.sounds;

import lombok.Generated;
import fever.visual.FeverVisual;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

public class ClientSoundInstance extends PositionedSoundInstance {
   private static final float DEFAULT_PITCH = 1.0F;
   private final String fileName;
   private SoundInstance currentSoundInstance;

   public ClientSoundInstance(String fileName, float volume) {
      super(
              Identifier.of(FeverVisual.MOD_ID + ":" + fileName),
              SoundCategory.MASTER,
              volume,
              1.0F,
              SoundInstance.createRandom(),
              false,
              0,
              AttenuationType.NONE,
              0.0,
              0.0,
              0.0,
              true
      );
      this.fileName = fileName;
   }

   public ClientSoundInstance(String fileName, float volume, float pitch) {
      super(
              Identifier.of(FeverVisual.MOD_ID + ":" + fileName),
              SoundCategory.MASTER,
              volume,
              pitch,
              SoundInstance.createRandom(),
              false,
              0,
              AttenuationType.NONE,
              0.0,
              0.0,
              0.0,
              true
      );
      this.fileName = fileName;
   }

   public void play(float volume) {
      stop();

      ClientSoundInstance sound = new ClientSoundInstance(this.fileName, volume);
      try {
         MinecraftClient.getInstance().getSoundManager().play(sound);
         currentSoundInstance = sound;
      } catch (Throwable ignored) {
         currentSoundInstance = null;
      }
   }

   public void play(float volume, float pitch) {
      stop();

      ClientSoundInstance sound = new ClientSoundInstance(this.fileName, volume, pitch);
      try {
         MinecraftClient.getInstance().getSoundManager().play(sound);
         currentSoundInstance = sound;
      } catch (Throwable ignored) {
         currentSoundInstance = null;
      }
   }

   public void play() {
      play(1.0F);
   }

   public void stop() {
      if (currentSoundInstance != null) {
         try {
            MinecraftClient.getInstance().getSoundManager().stop(currentSoundInstance);
         } catch (Throwable ignored) {
         }
         currentSoundInstance = null;
      }
   }

   public boolean isPlaying() {
      try {
         return currentSoundInstance != null &&
                 MinecraftClient.getInstance().getSoundManager().isPlaying(currentSoundInstance);
      } catch (Throwable ignored) {
         return false;
      }
   }

   @Generated
   public String getFileName() {
      return this.fileName;
   }
}
