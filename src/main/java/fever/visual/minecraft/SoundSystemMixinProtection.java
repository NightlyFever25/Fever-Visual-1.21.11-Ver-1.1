package fever.visual.minecraft;

import fever.visual.FeverVisual;
import fever.visual.systems.event.impl.game.SoundEvent;
import fever.visual.systems.modules.modules.visuals.NoRender;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class SoundSystemMixinProtection {
   public static void playSound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
      NoRender NoRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
      if (NoRender.isEnabled()
         && NoRender.getBeacon().isSelected()
         && (
            sound.getId().equals(SoundEvents.BLOCK_BEACON_ACTIVATE.id())
               || sound.getId().equals(SoundEvents.BLOCK_BEACON_AMBIENT.id())
               || sound.getId().equals(SoundEvents.BLOCK_BEACON_POWER_SELECT.id())
               || sound.getId().equals(SoundEvents.BLOCK_BEACON_DEACTIVATE.id())
         )) {
         cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
      }

      if (NoRender.isEnabled()
         && NoRender.getWeatherSound().isSelected()
         && (
            sound.getId().equals(SoundEvents.WEATHER_RAIN.id())
               || sound.getId().equals(SoundEvents.WEATHER_RAIN_ABOVE.id())
               || sound.getId().equals(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER.id())
         )) {
         cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
      }

      if (NoRender.isEnabled()
         && NoRender.getPhantoms().isSelected()
         && (
            sound.getId().equals(SoundEvents.ENTITY_PARROT_IMITATE_PHANTOM.id())
               || sound.getId().equals(SoundEvents.ENTITY_PHANTOM_AMBIENT.id())
               || sound.getId().equals(SoundEvents.ENTITY_PHANTOM_BITE.id())
               || sound.getId().equals(SoundEvents.ENTITY_PHANTOM_FLAP.id())
               || sound.getId().equals(SoundEvents.ENTITY_PHANTOM_DEATH.id())
               || sound.getId().equals(SoundEvents.ENTITY_PHANTOM_HURT.id())
               || sound.getId().equals(SoundEvents.ENTITY_PHANTOM_SWOOP.id())
         )) {
         cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
      }

      FeverVisual.getInstance().getEventManager().triggerEvent(new SoundEvent(sound));
   }
}
