package fever.visual.systems.modules.modules.player;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.AttackEvent;
import fever.visual.systems.event.impl.game.EntityDeathEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.sounds.ClientSoundInstance;
import fever.visual.utility.sounds.ClientSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@ModuleInfo(
        name = "Kill Sound",
        category = ModuleCategory.MISC,
        desc = "modules.descriptions.kill_sound"
)
public class KillSound extends BaseModule {

    private final Random random = new Random();
    private final Map<UUID, Long> playedDeaths = new HashMap<>();
    private final Map<Integer, WatchedTarget> watchedTargets = new HashMap<>();

    private final BooleanSetting playSound = new BooleanSetting(
            this, "modules.settings.kill_sound.play_sound", "modules.settings.kill_sound.play_sound.description"
    ).enabled(true);

    private final SliderSetting volume = new SliderSetting(
            this, "modules.settings.kill_sound.volume", "modules.settings.kill_sound.volume.description"
    ).min(0.0f).max(100.0f).step(1.0f).currentValue(100.0f);

    private final BooleanSetting mobs = new BooleanSetting(
            this, "modules.settings.kill_sound.mobs", "modules.settings.kill_sound.mobs.description"
    ).enabled(true);

    private final ModeSetting soundType = new ModeSetting(
            this, "modules.settings.kill_sound.sound_type", "modules.settings.kill_sound.sound_type.description"
    );

    private final ModeSetting.Value kill1 = new ModeSetting.Value(soundType, "modules.settings.kill_sound.kill1");
    private final ModeSetting.Value kill2 = new ModeSetting.Value(soundType, "modules.settings.kill_sound.kill2");
    private final ModeSetting.Value kill3 = new ModeSetting.Value(soundType, "modules.settings.kill_sound.kill3");
    private final ModeSetting.Value kill4 = new ModeSetting.Value(soundType, "modules.settings.kill_sound.kill4");
    private final ModeSetting.Value kill5 = new ModeSetting.Value(soundType, "modules.settings.kill_sound.kill5");
    private final ModeSetting.Value kill6 = new ModeSetting.Value(soundType, "modules.settings.kill_sound.kill6");
    private final ModeSetting.Value randomMode = new ModeSetting.Value(soundType, "modules.settings.kill_sound.random").select();

    private final EventListener<AttackEvent> onAttack = event -> {
        if (mc.world == null || mc.player == null || !(event.getEntity() instanceof LivingEntity entity)) return;
        if (!shouldTrack(entity)) return;

        this.watchedTargets.put(entity.getId(), new WatchedTarget(entity.getUuid(), System.currentTimeMillis()));
    };

    private final EventListener<EntityDeathEvent> onEntityDeath = event -> {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!isKilledByPlayer(event, mc.player)) return;
        if (!mobs.isEnabled() && !(entity instanceof PlayerEntity)) return;
        if (entity == mc.player) return;
        if (this.wasRecentlyPlayed(entity.getUuid())) return;

        this.playKillSound();
    };

    @Override
    public void tick() {
        if (mc.world == null || mc.player == null) {
            this.watchedTargets.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, WatchedTarget>> iterator = this.watchedTargets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, WatchedTarget> entry = iterator.next();
            WatchedTarget watched = entry.getValue();
            if (now - watched.attackTimeMs() > 4000L) {
                iterator.remove();
                continue;
            }

            Entity rawEntity = mc.world.getEntityById(entry.getKey());
            if (!(rawEntity instanceof LivingEntity entity)) {
                if (!this.wasRecentlyPlayed(watched.uuid())) {
                    this.playKillSound();
                }
                iterator.remove();
                continue;
            }

            if (entity == mc.player || !shouldTrack(entity)) {
                iterator.remove();
                continue;
            }

            if (entity.isDead() || entity.isRemoved() || entity.getHealth() <= 0.0F) {
                if (!this.wasRecentlyPlayed(entity.getUuid())) {
                    this.playKillSound();
                }
                iterator.remove();
            }
        }
    }

    @Override
    public void onDisable() {
        this.playedDeaths.clear();
        this.watchedTargets.clear();
    }

    private boolean isKilledByPlayer(EntityDeathEvent event, PlayerEntity player) {
        if (event.getSource() != null && event.getSource().getAttacker() == player) {
            return true;
        }

        return event.getKillerEntity() == player;
    }

    private boolean shouldTrack(LivingEntity entity) {
        if (entity == mc.player) {
            return false;
        }

        return entity instanceof PlayerEntity || mobs.isEnabled();
    }

    private boolean wasRecentlyPlayed(UUID uuid) {
        long now = System.currentTimeMillis();
        this.playedDeaths.entrySet().removeIf(entry -> now - entry.getValue() > 1500L);
        Long lastPlayed = this.playedDeaths.get(uuid);
        if (lastPlayed != null && now - lastPlayed <= 1500L) {
            return true;
        }

        this.playedDeaths.put(uuid, now);
        return false;
    }

    private void playKillSound() {
        if (!playSound.isEnabled()) {
            return;
        }

        ClientSoundInstance sound = getSelectedSound();
        if (sound != null) {
            sound.play(volume.getCurrentValue() / 100.0f);
        }
    }

    private ClientSoundInstance getSelectedSound() {
        if (soundType.is(kill1)) return ClientSounds.GIRL_1;
        if (soundType.is(kill2)) return ClientSounds.GIRL_2;
        if (soundType.is(kill3)) return ClientSounds.GIRL_3;
        if (soundType.is(kill4)) return ClientSounds.GIRL_4;
        if (soundType.is(kill5)) return ClientSounds.GIRL_5;
        if (soundType.is(kill6)) return ClientSounds.FRAG;
        if (soundType.is(randomMode)) {
            ClientSoundInstance[] sounds = {
                    ClientSounds.GIRL_1, ClientSounds.GIRL_2, ClientSounds.GIRL_4,
                    ClientSounds.GIRL_3, ClientSounds.GIRL_5, ClientSounds.FRAG
            };
            return sounds[random.nextInt(sounds.length)];
        }
        return ClientSounds.FRAG;
    }

    private record WatchedTarget(UUID uuid, long attackTimeMs) {
    }
}
