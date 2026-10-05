package fever.visual.systems.modules.modules.player;

import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import net.minecraft.client.gui.screen.DeathScreen;

@ModuleInfo(name = "Auto Respawn", category = ModuleCategory.MISC, desc = "Автоматически возрождает при смерти")
public class AutoRespawn extends BaseModule {

    @Override
    public void tick() {
        if (mc.currentScreen instanceof DeathScreen && mc.player != null) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }

        super.tick();
    }

    @Override
    public void onDisable() {
    }
}