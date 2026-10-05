package fever.visual.systems.modules.modules.player;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.AttackEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.sounds.ClientSoundInstance;
import fever.visual.utility.sounds.ClientSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

@ModuleInfo(
        name = "Hit Sound",
        category = ModuleCategory.MISC,
        desc = "modules.descriptions.hit_sound"
)
public class HitSound extends BaseModule {

    private final ModeSetting soundMode = new ModeSetting(this, "modules.settings.hit_sound.sound", "modules.settings.hit_sound.sound.description");

    private final ModeSetting.Value hit1 = new ModeSetting.Value(soundMode, "modules.settings.hit_sound.hit1");
    private final ModeSetting.Value hit2 = new ModeSetting.Value(soundMode, "modules.settings.hit_sound.hit2");
    private final ModeSetting.Value hit3 = new ModeSetting.Value(soundMode, "modules.settings.hit_sound.hit3");
    private final ModeSetting.Value hit4 = new ModeSetting.Value(soundMode, "modules.settings.hit_sound.hit4");
    private final ModeSetting.Value hit5 = new ModeSetting.Value(soundMode, "modules.settings.hit_sound.hit5");
    private final ModeSetting.Value hit6 = new ModeSetting.Value(soundMode, "modules.settings.hit_sound.hit6");
    private final ModeSetting.Value hit7 = new ModeSetting.Value(soundMode, "modules.settings.hit_sound.hit7");
    private final ModeSetting.Value hit8 = new ModeSetting.Value(soundMode, "modules.settings.hit_sound.hit8");
    private final ModeSetting.Value hit9 = new ModeSetting.Value(soundMode, "modules.settings.hit_sound.hit9");

    private final SliderSetting volume = new SliderSetting(
            this, "modules.settings.hit_sound.volume", "modules.settings.hit_sound.volume.description"
    ).min(0.1f).max(2.0f).step(0.1f).currentValue(1.0f);

    private final SliderSetting pitch = new SliderSetting(
            this, "modules.settings.hit_sound.pitch", "modules.settings.hit_sound.pitch.description"
    ).min(0.5f).max(2.0f).step(0.1f).currentValue(1.0f);

    private final EventListener<AttackEvent> onAttack = event -> {
        Entity target = event.getEntity();
        if (target == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (!Thread.currentThread().getName().equals("Render thread")) return;

        ClientSoundInstance sound = getSelectedSound();
        if (sound != null) {
            ClientSoundInstance playSound = new ClientSoundInstance(
                    sound.getFileName(),
                    volume.getCurrentValue(),
                    pitch.getCurrentValue()
            );
            mc.getSoundManager().play(playSound);
        }
    };

    private ClientSoundInstance getSelectedSound() {
        if (hit1.isSelected())   return ClientSounds.GLU;
        if (hit2.isSelected())   return ClientSounds.GLU2;
        if (hit3.isSelected())   return ClientSounds.PUNCH;
        if (hit4.isSelected())   return ClientSounds.CLOCK;
        if (hit5.isSelected())   return ClientSounds.BASS;
        if (hit6.isSelected())   return ClientSounds.BEEEP;
        if (hit7.isSelected())   return ClientSounds.UWU;
        if (hit8.isSelected())   return ClientSounds.CHIME;
        if (hit9.isSelected())   return ClientSounds.BONK;
        return ClientSounds.GLU;
    }
}